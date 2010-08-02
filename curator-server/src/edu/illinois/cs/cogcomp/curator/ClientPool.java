package edu.illinois.cs.cogcomp.curator;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Client Pool
 * 
 * Manages client connections (connections to other Thrift services). addClients
 * should only be called during program initialization.
 * 
 * @author James Clarke
 * 
 */
public class ClientPool implements Pool {
	private final List<ClientHolder> pool;
	private final Map<Object, ClientHolder> releaseMap;
	private final int timeout;

	public ClientPool(int timeout) {
		pool = new LinkedList<ClientHolder>();
		releaseMap = new HashMap<Object, ClientHolder>();
		this.timeout = timeout;
	}

	/**
	 * Creates clients for the given identifiers.
	 * 
	 * @param name
	 *            - how to reference these clients
	 * @param identifiers
	 *            - array of host:port strings
	 * @param count
	 *            - how many clients per host:port
	 * @param clientClass
	 *            - what class is the client
	 */
	public void addClients(String[] identifiers, int count, Class<?> clientClass) {
		if (identifiers == null)
			return;
		int j = 0;
		for (int i = 0; i < count; i++) {
			for (String identifier : identifiers) {
				String[] split = identifier.split(":");
				String host = split[0];
				int port = Integer.valueOf(split[1]);

				ClientHolder holder = new ThriftClientHolder(host, port,
						timeout, clientClass);
				pool.add(holder);
				releaseMap.put(holder.client, holder);
				j++;
			}
		}
	}

	/**
	 * Add Cogcomp Legacy Clients to the pool.
	 * 
	 * @param name
	 *            how we identify these clients
	 * @param identifiers
	 *            array of servers as host:port
	 * @param count
	 *            how many clients per server
	 * @param clientClass
	 *            what class
	 */
	public void addLegacyClients(String[] identifiers, int count,
			Class<?> clientClass) {
		if (identifiers == null)
			return;
		int j = 0;
		for (int i = 0; i < count; i++) {
			for (String identifier : identifiers) {
				String[] split = identifier.split(":");
				String host = split[0];
				int port = Integer.valueOf(split[1]);
				ClientHolder holder = new LegacyClientHolder(host, port,
						clientClass);
				pool.add(holder);
				releaseMap.put(holder.client, holder);
				j++;
			}
		}
	}

	
	/**
	 * Does this pool have any clients?
	 * @return the result
	 */
	public boolean hasClients() {
		return !pool.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see edu.illinois.cs.cogcomp.curator.Pool#getClient()
	 */
	public Object getClient() throws TTransportException {
		Object client = null;

		while (client == null) {
			int errorCount = 0;
			for (ClientHolder h : pool) {
				try {
					client = h.getClient();
				} catch (TTransportException e) {
					errorCount++;
					// only throw an exception if all the clients are throwing
					// exceptions
					if (errorCount >= pool.size()) {
						throw e;
					}
				}
				if (client != null) {
					break;
				}
			}
			if (client == null) {
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (InterruptedException e) {
				}
			}
		}
		return client;
	}

	/* (non-Javadoc)
	 * @see edu.illinois.cs.cogcomp.curator.Pool#releaseClient(java.lang.Object)
	 */
	public void releaseClient(Object client) {
		releaseMap.get(client).release();
		synchronized (this) {
			this.notify();
		}
	}

	/* (non-Javadoc)
	 * @see edu.illinois.cs.cogcomp.curator.Pool#getStatusReport()
	 */
	public String getStatusReport() {
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < pool.size(); i++) {
			result.append(i);
			result.append(":");
			result.append(pool.get(i).isAvailable() ? "a" : "b");
			if (i < pool.size() - 1)
				result.append(" ");
		}
		return result.toString();
	}

	/**
	 * Abstract class to hold clients and tell us if they are available or not.
	 * 
	 * @author James Clarke
	 * 
	 */
	public abstract class ClientHolder {
		private Object client;
		private final AtomicBoolean available = new AtomicBoolean(true);

		/**
		 * Returns the client object if available, null otherwise.
		 * 
		 * @return
		 * @throws TTransportException
		 */
		public abstract Object getClient() throws TTransportException;

		/**
		 * Releases the client (makes it available again).
		 */
		public abstract void release();

		public boolean isAvailable() {
			return available.get();
		}
	}

	/**
	 * A class that holds the client and transport and state of the client.
	 * 
	 * @author James Clarke
	 * 
	 */
	public class ThriftClientHolder extends ClientHolder {
		private TTransport transport;

		public ThriftClientHolder(String host, int port, int timeout,
				Class<?> clientClass) {
			this.transport = new TSocket(host, port, timeout);
			this.transport = new TFramedTransport(transport);
			TProtocol protocol = new TBinaryProtocol(transport);
			try {
				Constructor<?> c = clientClass.getConstructor(TProtocol.class);
				super.client = c.newInstance(protocol);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}

		@Override
		public Object getClient() throws TTransportException {
			if (super.available.compareAndSet(true, false)) {
				try {
				transport.open();
				} catch (TTransportException e) {
					this.release();
					throw e;
				}
				return super.client;
			} else {
				return null;
			}
		}

		@Override
		public void release() {
			if (transport.isOpen())
				transport.close();
			super.available.set(true);
		}

	}

	/**
	 * Holds legacy clients.
	 * 
	 * @author James Clarke
	 * 
	 */
	public class LegacyClientHolder extends ClientHolder {

		public LegacyClientHolder(String host, int port, Class<?> clientClass) {
			try {
				super.client = clientClass.getConstructor(String.class,
						int.class).newInstance(host, port);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		@Override
		public Object getClient() throws TTransportException {
			if (super.available.compareAndSet(true, false)) {
				return super.client;
			} else {
				return null;
			}
		}

		@Override
		public void release() {
			super.available.set(true);
		}

	}
}
