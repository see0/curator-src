package edu.illinois.cs.cogcomp.server;

import java.lang.reflect.Constructor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server class for any handler which implements a thrift generated IFace. Uses
 * reflection to create the server given just the implementation of the IFace (a
 * handler).
 * 
 * @author James Clarke
 * 
 */
public class ThriftServer implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(ThriftServer.class);
	private TNonblockingServerTransport serverTransport;
	private TServer server;
	private TProcessor processor;
	private int port;
	private int threads;

	public ThriftServer(TProcessor processor, int port, int threads) {
		this.processor = processor;
		this.port = port;
		this.threads = threads;
	}

	public void run() {
		try {
			serverTransport = new TNonblockingServerSocket(port);

			if (threads == 1) {
				server = new TNonblockingServer(processor, serverTransport);
			} else {
				THsHaServer.Options serverOptions = new THsHaServer.Options();
				serverOptions.minWorkerThreads = threads;
				server = new THsHaServer(new TProcessorFactory(processor),
						serverTransport, new TFramedTransport.Factory(),
						new TFramedTransport.Factory(),
						new TBinaryProtocol.Factory(),
						new TBinaryProtocol.Factory(), serverOptions);
			}
			logger.info("Starting the server on port {} with {} threads", port,
					threads);
			server.serve();

		} catch (TTransportException e) {
			logger.error("Thrift Transport error");
			logger.error(e.toString());
		}
	}

	public void stopServer() {
		logger.info("Stopping Server");
		if (server != null) {
			server.stop();
		}
		if (serverTransport != null) {
			serverTransport.interrupt();
			serverTransport.close();
		}
		logger.info("Server stopped.");
	}

	public static Options createOptions() {
		Option port = OptionBuilder.withArgName("port").hasArg()
				.withDescription("port to open server on").create("port");
		Option handler = OptionBuilder.withArgName("handler class").hasArg()
				.isRequired().withDescription("Handler to create server from")
				.create("handler");
		Option threads = OptionBuilder.withArgName("number of threads")
				.hasArg()
				.withDescription("Number of threads for server to use").create(
						"threads");
		Option help = new Option("help", "print this message");
		Options options = new Options();
		options.addOption(port);
		options.addOption(handler);
		options.addOption(threads);
		options.addOption(help);
		return options;
	}

	public static void main(String[] args) {
		CommandLineParser parser = new GnuParser();
		Options options = createOptions();
		HelpFormatter hformat = new HelpFormatter();
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			logger.error(e.getMessage());
			hformat.printHelp("java " + ThriftServer.class.getName(), options,
					true);
			System.exit(1);
		}
		if (line.hasOption("help")) {
			hformat.printHelp("java " + ThriftServer.class.getName(), options,
					true);
			System.exit(1);
		}

		int port = Integer.parseInt(line.getOptionValue("port", "9090"));
		int threads = 1;
		try {
			threads = Integer.parseInt(line.getOptionValue("threads", "1"));
		} catch (NumberFormatException e) {
			logger.warn("Couldn't interpret {} as a number.", line
					.getOptionValue("threads"));
		}
		if (threads < 0) {
			threads = 1;
		} else if (threads == 0) {
			threads = 25;
		}
		Class<?> handlerClass = null;
		try {
			handlerClass = Class.forName(line.getOptionValue("handler"));
		} catch (ClassNotFoundException e1) {
			logger.error("Could not find handler class: {}", line
					.getOptionValue("handler"));
			System.exit(1);
		}

		logger.info("Attempting to use {} as a handler.", handlerClass
				.getName());

		Class<?> ifaceInterface = handlerClass.getInterfaces()[0];
		Class<?> serviceClass = ifaceInterface.getEnclosingClass();

		Object handler = null;
		Object processor = null;
		try {
			if (handlerClass.getDeclaredConstructors().length == 1) {
				logger.info("Only one constructor for handler.");
				Constructor<?> constr = handlerClass
						.getConstructor(new Class[] {});
				handler = constr.newInstance(new Object[] {});
			} else {
				Class<?>[] paramTypes = new Class<?>[line.getArgs().length];
				for (int i = 0; i < line.getArgs().length; i++) {
					paramTypes[i] = String.class;
				}
				Constructor<?> constr = handlerClass.getConstructor(paramTypes);
				logger.info("Found constructor for supplied arguments");
				handler = constr.newInstance((Object[]) line.getArgs());
			}
			logger.info("Handler created.");
			logger.info("Attempting to create Processor.");

			for (Class<?> c : serviceClass.getDeclaredClasses()) {
				if (c.getSimpleName().equals("Processor")) {
					logger.info("Found Processor class: {}", c.getName());
					Constructor<?> constr = c
							.getConstructor(new Class[] { ifaceInterface });
					processor = constr.newInstance(new Object[] { handler });
					logger.info("Processor created.");
				}
			}
		} catch (Exception e) {
			logger.error("Failed to instaniate required classes.");
			logger.info("{}", e);
			System.exit(1);
		}

		ThriftServer ts = new ThriftServer((TProcessor) processor, port,
				threads);
		Runtime.getRuntime().addShutdownHook(
				new Thread(new ShutdownListener(ts),
						"Thrift Server Shutdown Listener"));
		Thread t = new Thread(ts, "ThriftServer");
		t.setDaemon(false);
		t.start();
	}

}

class ShutdownListener implements Runnable {
	private ThriftServer server;

	public ShutdownListener(ThriftServer server) {
		this.server = server;
	}

	public void run() {
		server.stopServer();
	}

}
