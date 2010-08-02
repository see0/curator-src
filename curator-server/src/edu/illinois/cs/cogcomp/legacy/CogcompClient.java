/**
 * 
 */
package edu.illinois.cs.cogcomp.legacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read one of the cogcomp demos. Based on Quang's code for Descartes. Modified
 * by James Clarke to throw more exceptions.
 * 
 * @author Vivek Srikumar Nov 25, 2008
 * 
 * 
 * 
 */
public class CogcompClient {
	private static Logger logger = LoggerFactory.getLogger(CogcompClient.class);

	protected final String host;
	protected final int port;

	protected Socket socket = null;
	protected PrintWriter out = null;
	protected BufferedReader in = null;

	protected boolean inSession;

	private final boolean allowHandshakes;

	public CogcompClient(String host, int port, boolean allowHandshakes)
			throws IOException {
		this.host = host;
		this.port = port;
		this.allowHandshakes = allowHandshakes;
		this.inSession = false;

		closeSession();
	}

	public CogcompClient(String host, int port) throws IOException {
		this(host, port, false);
	}

	public void startSession() throws UnknownHostException, ConnectException, IOException {
		// System.out.println("[Starting session...]");
		logger.debug("starting session");
		inSession = true;

		try {
			socket = new Socket(host, port);
			socket.setSoTimeout(30000);
		} catch (UnknownHostException e) {

			logger.error(e.toString() +" ["+ host +";" + port+"]");
			throw e;
		} catch (ConnectException e) {
			logger.error(e.toString()+" [" +host+":"+port+"]");
			try {
				Thread.sleep(25000);
			} catch (InterruptedException e1) {
			}
			throw e;
			
		} catch (IOException e) {

			logger.error(e.toString()+" [" +host+":"+port+"]", e);
			throw e;
		}

		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
		} catch (IOException e) {

			logger.error(e.toString()+" [" +host+":"+port+"]", e);
			throw e;
		}
		logger.debug("session started");
		// System.out.println("Started a new session");
	}

	public void closeSession() throws IOException {
		inSession = false;

		if (out != null)
			out.close();
		if (in != null)
			in.close();

		if (socket != null)
			socket.close();

		in = null;
		socket = null;
		out = null;
	}

	@SuppressWarnings("unchecked")
	public List<String> getOutput(String input, boolean singleLine)
			throws UnknownHostException, IOException {
		logger.debug("Getting server output for input: " + input);

		// System.out.println("Connecting to server");

		List<String> output = null;

		boolean done = false;
		boolean error = false;
		int t = 100;
		int retries = 5;
		while (!done) {
			try {
				output = getOutputFromNetwork(input, singleLine);
				done = true;
				if (error) {
					logger.debug("Connection recovered");
					error = false;
				}
			} catch (IOException e) {
				error = true;
				// logger.error(e.toString());
				if (t > 100 * retries * 2) {
					if (e instanceof UnknownHostException) {
						throw (UnknownHostException) e;
					} else {
						throw e;
					}
				}
			}

			if (error) {
				logger.debug("Sleeping for " + t + " milliseconds.");
				t *= 2;
				try {
					Thread.sleep(t);
				} catch (Exception ex) {
				}
			}
		}

		// System.out.println(output);
		return output;
	}

	public List<String> getOutput(String input) throws UnknownHostException,
			IOException {
		return this.getOutput(input, false);
	}

	/**
	 * @param input
	 * @return
	 * @throws Exception
	 */
	private List<String> getOutputFromNetwork(String input, boolean singleLine)
			throws UnknownHostException, IOException {
		logger.debug("Connecting to server");
		ArrayList<String> output = new ArrayList<String>();

		boolean error = false;
		boolean stopped = true;

		if (!allowHandshakes) {
			startSession();
		} else {
			if (!inSession)
				startSession();
		}

		try {

			stopped = false;
			logger.debug("sending output");
			out.println(input);
			logger.debug("output sent");
			// System.out.println(input);

			if (!singleLine) {
				String line;

				while ((line = in.readLine()) != null && line.length() > 0) {
					// System.out.println(line);
					output.add(line);

				}
				logger.debug("finished reading");
			} else {
				logger.debug("about to read");
				output.add(in.readLine());
				logger.debug("done reading");
			}

			stopped = true;

			error = false;
		} catch (IOException ex) {
			// logger.error("{}",ex);
			logger.warn(ex.toString() + " " + host + ":" + port);
			throw ex;
		} finally {
			if (!allowHandshakes)
				closeSession();

			if (!stopped)
				error = true;
		}

		if (error)
			throw new IOException(
					"Unable to get output. Refer previous log messages.");

		return output;
	}

}
