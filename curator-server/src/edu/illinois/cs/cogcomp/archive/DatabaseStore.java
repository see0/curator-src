package edu.illinois.cs.cogcomp.archive;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseStore extends Observable {

	protected final Logger logger = LoggerFactory
			.getLogger(edu.illinois.cs.cogcomp.archive.DatabaseStore.class);
	private final TSerializer serializer;
	private final TDeserializer deserializer;
	private final String name;
	private PreparedStatement getStatement;
	private PreparedStatement insertStatement;
	private PreparedStatement updateStatement;
	private PreparedStatement updateAccessStatement;
	private PreparedStatement deleteStatement;
	private PreparedStatement countStatement;

	private final AtomicInteger fetchCount = new AtomicInteger(0);
	private final AtomicInteger fetchTime = new AtomicInteger(0);
	private final AtomicInteger storeCount = new AtomicInteger(0);
	private final AtomicInteger storeTime = new AtomicInteger(0);

	private final Map<String, Long> accessLog = new ConcurrentHashMap<String, Long>();

	public DatabaseStore(String tableName, Connection db) {
		this(tableName, db, new TSerializer(
				new TBinaryProtocol.Factory()), new TDeserializer(
				new TBinaryProtocol.Factory()));
	}

	public DatabaseStore(String tableName, Connection db,
			TSerializer s, TDeserializer d) {
		this.serializer = s;
		this.deserializer = d;
		this.name = tableName;
		try {
			Statement stmt = db.createStatement();

			String schema = "create table if not exists " + tableName + "("
					+ "sha1_hash char(40) not null, " + "datum blob not null, "
					+ "last_update bigint not null, "
					+ "last_access bigint not null, "
					+ "primary key (sha1_hash)" + ");";
			stmt.executeUpdate(schema);

		} catch (SQLException e) {
			logger.error("Error creating underlying SQL table", e);
			System.exit(1);
		}

		// init prepared statements
		try {
			getStatement = db.prepareStatement("select datum from " + tableName
					+ " where sha1_hash = ?");
			insertStatement = db.prepareStatement("insert into " + tableName
					+ " values (?, ?, ?, ?)");
			updateStatement = db
					.prepareStatement("update "
							+ tableName
							+ " set datum = ?, last_update = ?, last_access = ? where sha1_hash = ?");
			updateAccessStatement = db.prepareStatement("update " + tableName
					+ " set last_access = ? where sha1_hash = ?");
			deleteStatement = db.prepareStatement("delete from " + tableName
					+ " where last_access < ?");
			countStatement = db.prepareStatement("select count(*) from "
					+ tableName);
		} catch (SQLException e) {
			logger.error("Error creating prepared statements", e);
			System.exit(1);
		}
		logger.info("Database Store {} initialized.", tableName);
	}

	private byte[] serialize(TBase datum) throws TException {
		return serializer.serialize(datum);
	}

	private <T extends TBase> T deserialize(byte[] bytes, Class<T> clazz) throws TException {
		T datum = null;
		try {
			datum = clazz.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		deserializer.deserialize(datum, bytes);
		return datum;
	}

	public synchronized <T extends TBase> boolean store(T datum, Class<T> clazz) throws TException {
		long startTime = System.currentTimeMillis();
		String identifier = Identifier.getId(datum);
		long time = System.currentTimeMillis();
		byte[] blob = serialize(datum);
		InputStream is = new ByteArrayInputStream(blob);
		logger.debug("Storing datum in {} with digest [{}]", name, identifier);
		boolean result = false;
		try {
			if (getById(identifier, clazz) == null) {
				insertStatement.clearParameters();
				insertStatement.setString(1, identifier);
				insertStatement.setBinaryStream(2, is);
				insertStatement.setLong(3, time);
				insertStatement.setLong(4, time);
				result = insertStatement.executeUpdate() == 1;
			} else {
				updateStatement.clearParameters();
				updateStatement.setString(4, identifier);
				updateStatement.setBinaryStream(1, is);
				updateStatement.setLong(2, time);
				updateStatement.setLong(3, time);
				result = updateStatement.executeUpdate() == 1;
			}
			long endTime = System.currentTimeMillis();
			// we already updated the access time so lets remove it from the log
			if (accessLog.containsKey(identifier)) {
				accessLog.remove(identifier);
			}
			storeCount.incrementAndGet();
			storeTime.addAndGet((int) (endTime - startTime));
			return result;
		} catch (SQLException e) {
			logger.warn("Error creating prepared statements", e);
			throw new TException("Error storing datum", e);
		}
	}

	public synchronized <T extends TBase> T getById(String identifier, Class<T> clazz) throws TException {
		long startTime = System.currentTimeMillis();
		final ResultSet rs;
		T datum = null;
		try {
			getStatement.clearParameters();
			getStatement.setString(1, identifier);
			rs = getStatement.executeQuery();
			if (rs.next()) {
				Blob blob = rs.getBlob("datum");
				datum = deserialize(blob.getBytes(0, (int) blob.length()), clazz);

				// update access log
				accessLog.put(identifier, System.currentTimeMillis());
				// tell the observers we've modified access log
				setChanged();
				notifyObservers(accessLog.size());

				long endTime = System.currentTimeMillis();
				logger.debug("Retrieved record in {}ms", endTime - startTime);
				// store the details for logging
				fetchCount.incrementAndGet();
				fetchTime.addAndGet((int) (endTime - startTime));
			}
		} catch (SQLException e) {
			logger.warn("Error creating prepared statements", e);
			logger.error("Error getting datum for identifier: {}", identifier);
			throw new TException("Underlying database error", e);
		}
		return datum;
	}

	/**
	 * Iterates over the cache of accessed identifiers and updates their access
	 * time.
	 * 
	 * @throws TException
	 */
	public synchronized void updateAccess() throws TException {
		long startTime = System.currentTimeMillis();
		Set<String> doneIdents = new HashSet<String>();
		for (String identifier : accessLog.keySet()) {
			long time = accessLog.get(identifier);
			try {
				updateAccessStatement.clearParameters();
				updateAccessStatement.setLong(1, time);
				updateAccessStatement.setString(2, identifier);
				updateAccessStatement.executeUpdate();
				doneIdents.add(identifier);
			} catch (SQLException e) {
				logger.warn("Error creating prepared statements", e);
				throw new TException("Underlying database error", e);
			}
		}
		for (String ident : doneIdents) {
			accessLog.remove(ident);
		}
		long endTime = System.currentTimeMillis();
		logger.info("Finished updating access times in " + name
				+ " for {} items in {}ms", doneIdents.size(), endTime
				- startTime);
	}

	/**
	 * Expires (removes) data which is older than expireAge milliseconds.
	 * 
	 * @param expireAge
	 * @throws TException
	 */
	public void expire(long expireAge) throws TException {
		try {
			deleteStatement.clearParameters();
			deleteStatement.setLong(1, System.currentTimeMillis() - expireAge);
			int n = deleteStatement.executeUpdate();
			logger.info("Deleted {} old items in {}", n, name);
		} catch (SQLException e) {
			logger.warn("Error creating prepared statements", e);
			throw new TException("Underlying database error", e);
		}
	}

	/**
	 * Provide a status report of the database store.
	 * 
	 * @return the report
	 */
	public String getStatusReport() {
		int fc = fetchCount.getAndSet(0);
		int ft = fetchTime.getAndSet(0);
		int sc = storeCount.getAndSet(0);
		int st = storeTime.getAndSet(0);
		int fa = fc == 0 ? 0 : ft / fc;
		int sa = sc == 0 ? 0 : st / sc;

		long count = 0;
		try {
			ResultSet r = countStatement.executeQuery();
			if (r.next())
				count = r.getLong(1);
		} catch (SQLException e) {
			logger.error("Error getting count of items: {}", e);
		}
		String result = String.format(
				"%s datastore | fetches: %d %dms | stores: %d %dms | items: %d",
				name, fc, fa, sc, sa, count);
		return result;
	}
}
