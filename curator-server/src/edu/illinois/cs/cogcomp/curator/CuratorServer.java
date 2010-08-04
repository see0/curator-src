package edu.illinois.cs.cogcomp.curator;

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
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.curator.CuratorHandler;
import edu.illinois.cs.cogcomp.thrift.curator.Curator;

public class CuratorServer {
	private static Logger logger = LoggerFactory.getLogger(CuratorServer.class);

	/*
	 * most this code is boiler plate. All you need to modify is createOptions
	 * and main to get a server working!
	 */
	public static Options createOptions() {
		Option port = OptionBuilder.withArgName("port").hasArg()
				.withDescription("port to open server on").create("port");
		Option threads = OptionBuilder.withArgName("threads").hasArg()
				.withDescription("number of threads to run").create("threads");
		Option config = OptionBuilder.withArgName("config").hasArg()
				.withDescription("configuration file (curator.properties)")
				.create("config");
		Option annotators = OptionBuilder.withArgName("annotators").hasArg()
				.withDescription("annotators configuration file (annotators.xml)")
				.create("annotators");
		Option help = new Option("help", "print this message");
		Options options = new Options();
		options.addOption(port);
		options.addOption(threads);
		options.addOption(config);
		options.addOption(annotators);
		options.addOption(help);
		return options;
	}

	public static void main(String[] args) {
		int threads = 1;
		int port = 9090;
		String configFile = "";
		String annotatorsFile = "";
		CommandLineParser parser = new GnuParser();
		Options options = createOptions();
		HelpFormatter hformat = new HelpFormatter();
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			logger.error(e.getMessage());
			hformat.printHelp("java " + CuratorServer.class.getName(), options,
					true);
			System.exit(1);
		}
		if (line.hasOption("help")) {
			hformat.printHelp("java " + CuratorServer.class.getName(), options,
					true);
			System.exit(1);
		}

		port = Integer.parseInt(line.getOptionValue("port", "9090"));

		try {
			threads = Integer.parseInt(line.getOptionValue("threads", "1"));
		} catch (NumberFormatException e) {
			logger.warn("Couldn't interpret {} as a number.",
					line.getOptionValue("threads"));
		}
		if (threads < 0) {
			threads = 1;
		} else if (threads == 0) {
			threads = 25;
		}

		configFile = line.getOptionValue("config", "");
		annotatorsFile = line.getOptionValue("annotators", "");
		Curator.Iface handler = new CuratorHandler(configFile, annotatorsFile);
		Curator.Processor processor = new Curator.Processor(handler);

		runServer(processor, port, threads);
	}

	public static void runServer(TProcessor processor, int port, int threads) {

		TNonblockingServerTransport serverTransport;
		TServer server;
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
			Runtime.getRuntime().addShutdownHook(
					new Thread(new ShutdownListener(server, serverTransport),
							"Server Shutdown Listener"));
			logger.info("Starting the server on port {} with {} threads", port,
					threads);
			server.serve();
		} catch (TTransportException e) {
			logger.error("Thrift Transport error");
			logger.error(e.toString());
			System.exit(1);
		}
	}

}

class ShutdownListener implements Runnable {
	private TServer server;
	private TServerTransport transport;

	public ShutdownListener(TServer server, TServerTransport transport) {
		this.server = server;
		this.transport = transport;
	}

	public void run() {
		if (server != null) {
			server.stop();
		}
		if (transport != null) {
			transport.interrupt();
			transport.close();
		}
	}
}
