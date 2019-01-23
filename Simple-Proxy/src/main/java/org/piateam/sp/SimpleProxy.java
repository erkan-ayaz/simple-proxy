package org.piateam.sp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.piateam.sp.CommandLineParser.Configuration;

/**
 * Simple Proxy
 * 
 * @author ayaz
 */
public class SimpleProxy {
	private static final Logger logger = LoggerFactory.getLogger(SimpleProxy.class);

	public static void main(String[] arguments) throws IOException {
		Configurator.setRootLevel(Level.INFO);
		CommandLineParser.parse(arguments);
		try {
			logger.debug("[main()] ... [OK]", Configuration.config());
			serve();
		} catch (Throwable e) {
			logger.error("[main()] Failed!!! [NOK]", e);
		}
	}

	public static void serve() throws IOException {
		try (ServerSocket serverSocket = new ServerSocket(Configuration.localPort, 50, InetAddress.getByName("0.0.0.0"))) {
			while (true) {
				// Variables to hold the sockets to the client and to the server.
				Socket client;
				try {
					client = serverSocket.accept();
				} catch (Throwable throwable) {
					logger.warn("[serve()] Accepting client connection failed!!! [NOK]", throwable);
					continue;
				}
				try (Socket server = new Socket(Configuration.host, Configuration.remotePort)) {
					//					final InputStream from_client = client.getInputStream();
					//					final OutputStream to_client = client.getOutputStream();
					//					final InputStream from_server = server.getInputStream();
					//					final OutputStream to_server = server.getOutputStream();
					transmitRequest(client, server);
					transmitResponse(client, server);
				} catch (IOException e) {
					handleServerConnectionIssue(client, e);
				} finally {
					close(client);
				}
			}
		}
	}

	private static void handleServerConnectionIssue(Socket client, IOException e) {
		try {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
			out.println("Proxy server cannot connect to " + Configuration.host + ":" + Configuration.remotePort + ":\n" + e);
			out.flush();
			close(client);
		} catch (Throwable throwable) {
			logger.warn("[handleServerConnectionIssue()] Handling Server connection issue failed!!! [NOK]", throwable);
		}
	}

	private static void transmitRequest(Socket client, Socket server) {
		new Thread() {
			public void run() {
				final byte[] request = new byte[1024];
				int bytes_read;
				try (OutputStream outputStream = server.getOutputStream()) {
					InputStream inputStream = client.getInputStream();
					while ((bytes_read = inputStream.read(request)) != -1) {
						outputStream.write(request, 0, bytes_read);
						logger.debug("[run()] [Message: {}] to Server... [OK]", new String(request, "UTF-8"));
						outputStream.flush();
					}
				} catch (IOException e) {
					logger.warn("[transmitRequest()] Failed!!! [NOK]");
				}
				// the client closed the connection to us, so  close our
				// connection to the server.  This will also cause the
				// server-to-client loop in the main thread exit.
			}
		}.start();
	}

	private static void transmitResponse(Socket client, Socket server) {
		final byte[] response = new byte[4096];
		int bytes_read;
		try {
			InputStream inputStream = server.getInputStream();
			OutputStream outputStream = client.getOutputStream();
			while ((bytes_read = inputStream.read(response)) != -1) {
				sleep(1);
				outputStream.write(response, 0, bytes_read);
				outputStream.flush();
			}
		} catch (IOException e) {
			logger.warn("[server()] Transmitting request to server failed!!! [NOK]", e);
		}
	}

	private static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			logger.warn("[sleep()] Failed!!! [NOK]", e);
		}
	}

	private static void close(Closeable closeable) {
		try {
			closeable.close();
		} catch (Throwable e) {
			logger.warn("[close()] Closing {} failed!!! [NOK]", closeable);
		}
	}
}