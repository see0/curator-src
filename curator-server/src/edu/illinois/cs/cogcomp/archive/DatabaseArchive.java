package edu.illinois.cs.cogcomp.archive;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.curator.MultiRecord;
import edu.illinois.cs.cogcomp.thrift.curator.Record;


/**
 * Database Archive uses a h2 SQL database as the backend to store data.
 * 
 * @author James Clarke
 * 
 */
public class DatabaseArchive implements Archive {
	protected final Logger logger = LoggerFactory
			.getLogger(edu.illinois.cs.cogcomp.archive.DatabaseArchive.class);

	private final String dburl;
	private final String user;
	private final String password;

	private Connection db;

	private final Map<Class<?>, DatabaseStore> stores = new HashMap<Class<?>, DatabaseStore>();

	public DatabaseArchive() {
		Configuration config = null;
		try {
			config = new PropertiesConfiguration("configs/database.properties");
		} catch (ConfigurationException e) {
			logger.warn("Error reading configuration file. {}", e);
		}

		dburl = String.format("jdbc:h2:%s;DB_CLOSE_ON_EXIT=FALSE", config
				.getString("database.url", "mem:db1"));
		user = config.getString("database.user", "sa");
		password = config.getString("database.password", "");

		// init the database
		init();

		// set the config values
		long maintenanceTime = config.getLong("database.maintenancetime", 300) * 60 * 1000;
		long expireAge = config.getLong("database.expiretime", 30) * 24 * 60
				* 60 * 1000;
		int updateCount = config.getInt("database.updatecount", 1000);

		final long reportTime = config.getLong("database.reporttime", 5) * 60 * 1000;

		// add an observer for access logs
		for (DatabaseStore store : stores.values())
			store.addObserver(new DatabaseAccessListener(updateCount));

		// start the maintenance thread
		Thread maintenance = new Thread(new DatabaseMaintenanceWorker(
				maintenanceTime, expireAge, this), "Database Maintainer");
		maintenance.start();

		// start the reporter
		Thread reporter = new Thread("Database Report") {
			public void run() {
				for (;;) {
					for (DatabaseStore store : stores.values())
					logger.info(store.getStatusReport());
					try {
						Thread.sleep(reportTime);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		reporter.start();

		// add shutdown manager
		Runtime.getRuntime().addShutdownHook(
				new Thread(new ShutdownListener(this),
						"Database Shutdown Listener"));
	}

	/**
	 * Initialize the database. Open connection, create table and populate
	 * prepared statements.
	 */
	private synchronized void init() {
		try {
			Class.forName("org.h2.Driver");
			db = DriverManager.getConnection(dburl, user, password);
		} catch (ClassNotFoundException e) {
			logger.error("Cannot find driver for database", e);
			System.exit(1);
		} catch (SQLException e) {
			logger.error("Error connecting to database", e);
			System.exit(1);
		}
		//record store
		stores.put(Record.class, new DatabaseStore("records", db));
		//multirecord store
		stores.put(MultiRecord.class, new DatabaseStore("multirecords", db));

		logger.info("Database initialized.");
	}

	public boolean close() throws TException {
		updateRecordAccess();
		for (DatabaseStore store : stores.values()) {
			logger.info(store.getStatusReport());
		}
		try {
			db.close();
		} catch (SQLException e) {
			throw new TException("Error closing database", e);
		}
		logger.info("Database connection closed.");
		return true;
	}

	public synchronized <T extends TBase> T getById(String identifier, Class<T> clazz) throws TException {
		DatabaseStore store = stores.get(clazz);
		T datum = store.getById(identifier, clazz);
		return datum;
	}

	public synchronized <T extends TBase> T get(String text, boolean ws, Class<T> clazz)
			throws TException {
		return getById(Identifier.getId(text, ws), clazz);
	}

	public synchronized <T extends TBase> T get(List<String> text, Class<T> clazz)
			throws TException {
		return getById(Identifier.getId(text), clazz);
	}

	public synchronized <T extends TBase> boolean store(T datum, Class<T> clazz) throws TException {
		DatabaseStore store = stores.get(clazz);
		return store.store(datum, clazz);
	}

	public void updateRecordAccess() throws TException {
		logger.info("Updating access times on data");
		for (DatabaseStore store : stores.values())
			store.updateAccess();
	}

	public void expireRecords(long expireAge) throws TException {
		for (DatabaseStore store : stores.values())
			store.expire(expireAge);
	}
}

/**
 * Shutdown Listener
 * 
 * Gracefully shutdown the database.
 */
class ShutdownListener implements Runnable {
	private DatabaseArchive dba;

	public ShutdownListener(DatabaseArchive dba) {
		this.dba = dba;
	}

	public void run() {
		try {
			dba.close();
		} catch (TException e) {
			dba.logger.error("Exception closing database.", e);
		}
	}
}

/**
 * Database Access Listener
 * 
 * An Observer that gets notified whenever an access is made to the database.
 * Responsible for updating access times of records once the cache reaches a
 * certain size.
 */
class DatabaseAccessListener implements Observer, Runnable {
	private DatabaseStore dbs;
	private int updatecount;
	private volatile boolean isRunning;

	public DatabaseAccessListener() {
		this(1000);
	}

	public DatabaseAccessListener(int updatecount) {
		this.updatecount = updatecount;
		this.isRunning = false;
	}

	public void update(Observable o, Object arg) {
		if (isRunning)
			return;
		int size = (Integer) arg;
		if (size > updatecount) {
			this.dbs = (DatabaseStore) o;
			Thread t = new Thread(this, "Database Update Listener");
			isRunning = true;
			t.start();
		}
	}

	public void run() {
		try {
			dbs.updateAccess();
		} catch (TException e) {
			dbs.logger.error("Error updating record access.", e);
		}
		isRunning = false;
	}
}

/**
 * Database Maintenance Worker
 * 
 * Responsible for keeping the database in shipshape. Performs maintenance every
 * sleepTime milliseconds. Its duties consist of expiring old records and
 * updating record access times.
 * 
 */
class DatabaseMaintenanceWorker implements Runnable {
	private static Logger logger = LoggerFactory
			.getLogger(DatabaseMaintenanceWorker.class);
	private long expireAge;
	private long sleepTime;
	private DatabaseArchive dba;

	public DatabaseMaintenanceWorker(long delay, long expireAge,
			DatabaseArchive dba) {
		this.sleepTime = delay;
		this.expireAge = expireAge;
		this.dba = dba;
	}

	
	public void run() {
		try {
			for (;;) {
				logger.info("Performing rountine maintenance");
				try {
					dba.updateRecordAccess();
					dba.expireRecords(expireAge);
				} catch (TException e) {
					logger.error("Error performing maintenance.", e);
				}
				logger.info("Maintenance complete.");
				Thread.sleep(sleepTime);
			}
		} catch (InterruptedException e) {

		}
	}
}
