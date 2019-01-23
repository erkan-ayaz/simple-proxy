package org.piateam.sp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class E2ETest {
	private boolean			closeRemote;
	int						localPort	= 42500;
	int						remotePort	= 42000;
	String					host		= "0.0.0.0";
	private ServerSocket	serverSocket;
	private Thread proxyThread;

	@Before
	public void before() throws IOException {
		Configurator.setRootLevel(Level.DEBUG);
		closeRemote = false;
		initializeRemote();
		initializeProxy();
	}

	private void initializeRemote() throws IOException {
		serverSocket = new ServerSocket(remotePort);
		new Thread() {
			public void run() {
				while (!closeRemote) {
					Socket accept;
					try {
						accept = serverSocket.accept();
					} catch (IOException e1) {
						e1.printStackTrace();
						continue;
					}
					new Thread() {
						public void run() {
							try {
								InputStream inputStream = accept.getInputStream();
								final byte[] request = new byte[1024];
								OutputStream outputStream = accept.getOutputStream();
								while (inputStream.read(request) != -1) {
									System.out.println("Request: " + new String(request));
									outputStream.write(("Hello [" + accept.getRemoteSocketAddress() + "]").getBytes());
									outputStream.flush();
									break;
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}.start();
				}
			}
		}.start();
	}

	private void initializeProxy() throws IOException {
		String[] arguments = new String[6];
		arguments[0] = "-h";
		arguments[1] = host;
		arguments[2] = "-r";
		arguments[3] = String.valueOf(remotePort);
		arguments[4] = "-l";
		arguments[5] = String.valueOf(localPort);
		proxyThread = new Thread() {
			public void run() {
				try {
					SimpleProxy.main(arguments);
				} catch (IOException e) {
				}
			}
		};
		proxyThread.start();
	}

	@Test
	public void hello() throws IOException {
		try (Socket socket = new Socket(host, localPort)) {
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write("Hello".getBytes());
			InputStream inputStream = socket.getInputStream();
			final byte[] request = new byte[1024];
			while (inputStream.read(request) != -1) {
				System.out.println("Response: " + new String(request));
				break;
			}
		}
		closeRemote = true;
	}

	@After
	public void after() {
		while (!closeRemote) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		shutdownProxy();
		shutdownRemote();
	}

	private void shutdownProxy() {
		proxyThread.interrupt();
	}

	private void shutdownRemote() {
		closeRemote = true;
		close(serverSocket);
	}

	private void close(Closeable closeable) {
		try {
			if (Objects.nonNull(closeable)) {
				closeable.close();
			}
		} catch (IOException e) {
		}
	}
}
