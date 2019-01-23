package org.piateam.sp;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

class CommandLineParser {
	static class Configuration {
		static String	host;
		static int		remotePort;
		static int		localPort;

		public static Object config() {
			StringBuilder builder = new StringBuilder("{Configuration: ");
			builder.append("[Host: ").append(host).append("], ");
			builder.append("[Remote Port: ").append(remotePort).append("], ");
			builder.append("[Local Port: ").append(localPort).append("]");
			return builder.append('}');
		}
	}

	private static final DefaultParser	parser	= new DefaultParser();
	private static final Options		OPTIONS	= new Options();
	static {
		OPTIONS.addOption(new Option("h", "host", true, "host"));
		OPTIONS.addOption(new Option("r", "remote", true, "remote port"));
		OPTIONS.addOption(new Option("l", "local", true, "local port"));
	}

	public static final void parse(String... arguments) {
		try {
			CommandLine command = parser.parse(OPTIONS, arguments);
			if (command.hasOption('h')) {
				Configuration.host = command.getOptionValue('h');
			} else {
				throw new RuntimeException("h/host is mandatory!");
			}
			if (command.hasOption('r')) {
				Configuration.remotePort = Integer.parseInt(command.getOptionValue('r'));
			} else {
				throw new RuntimeException("r/remote port is mandatory!");
			}
			if (command.hasOption('l')) {
				Configuration.localPort = Integer.parseInt(command.getOptionValue('l'));
			} else {
				throw new RuntimeException("l/local port is mandatory!");
			}
		} catch (ParseException e) {
			throw new RuntimeException("Parsing command line parameters failed!", e);
		} catch (RuntimeException exception) {
			throw exception;
		} catch (Throwable e) {
			throw new RuntimeException("Parsing command line parameters failed!", e);
		}
	}
}
