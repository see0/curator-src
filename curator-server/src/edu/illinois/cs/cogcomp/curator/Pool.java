package edu.illinois.cs.cogcomp.curator;

import org.apache.thrift.transport.TTransportException;

/**
* 
* @author James Clarke
* 
*/
public interface Pool {

	/**
	 * Returns a client. You must remember to release the client once you are
	 * done!
	 * 
	 * Will keep looping until either an available client is found or all
	 * clients are offline.
	 * 
	 * @return the client
	 * @throws TTransportException
	 *             if all clients are offline.
	 */
	public abstract Object getClient() throws TTransportException;

	/**
	 * Releases a client and makes it available for others to use. Part of your
	 * contract is to release clients once you are finished with them.
	 * 
	 * @param client
	 */
	public abstract void releaseClient(Object client);

	public abstract String getStatusReport();

}