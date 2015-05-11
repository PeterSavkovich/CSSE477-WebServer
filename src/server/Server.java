/*
 * Server.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */
 
package server;

import gui.WebServer;

import java.util.Comparator;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import protocol.*;

/**
 * This represents a welcoming server for the incoming
 * TCP request from a HTTP client such as a web browser. 
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable, IServer {
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;
	private HashMap<String, Constructor<?>> requests;
	public PluginHandler pluginHandler;
	private HashMap<String, Integer> requestsInLastMinute;
	
	// DO YOU SEE HOW LITTLE I CARE???
	public static Cache<String, FileCondom> cache = CacheBuilder.newBuilder().initialCapacity(50).expireAfterAccess(10, TimeUnit.SECONDS).build();
		
	private long connections;
	private long serviceTime;
	
	private WebServer window;
	/**
	 * @param rootDirectory
	 * @param port
	 */
	public Server(String rootDirectory, int port, WebServer window) {
		this.rootDirectory = rootDirectory;
		this.port = port;
		this.stop = false;
		this.connections = 0;
		this.serviceTime = 0;
		this.window = window;
		this.requests = new HashMap<String,Constructor<?>>();
		this.pluginHandler = new PluginHandler();
		this.requestsInLastMinute = new HashMap<String,Integer>();
		this.loadClasses();
		(new Thread(new HttpResponseFactory())).start();
	}

	private void loadClasses() {
		// TODO We will go back and make this actually watch a directory in a later revision
		try {
			this.requests.put(Protocol.GET,GetRequest.class.getConstructor());
			this.requests.put(Protocol.POST, PostRequest.class.getConstructor());
			this.requests.put(Protocol.PUT, PutRequest.class.getConstructor());
			this.requests.put(Protocol.DELETE, DeleteRequest.class.getConstructor());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public HashMap<String,Constructor<?>> getRequests() {
		return this.requests;
	}

	/**
	 * Gets the root directory for this web server.
	 * 
	 * @return the rootDirectory
	 */
	public String getRootDirectory() {
		return rootDirectory;
	}


	/**
	 * Gets the port number for this web server.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Returns connections serviced per second. 
	 * Synchronized to be used in threaded environment.
	 * 
	 * @return
	 */
	public synchronized double getServiceRate() {
		if(this.serviceTime == 0)
			return Long.MIN_VALUE;
		double rate = this.connections/(double)this.serviceTime;
		rate = rate * 1000;
		return rate;
	}
	
	/**
	 * Increments number of connection by the supplied value.
	 * Synchronized to be used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}
	
	/**
	 * Increments the service time by the supplied value.
	 * Synchronized to be used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming
	 * TCP connection request and creates a {@link ConnectionHandler} for
	 * the request.
	 */
	public void run() {
		while (!this.stop) {
		try {
			this.welcomeSocket = new ServerSocket(port);
			new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						try {
							Thread.sleep(60000); //Sleep 1 minute
						} catch (InterruptedException e) {
							e.printStackTrace();
						} 
						System.out.println("Clearing blacklist!");
						Server.this.requestsInLastMinute.clear();
					}
				}
			}).start();
			
			// Now keep welcoming new connections until stop flag is set to true
			while(true) {
				// Listen for incoming socket connection
				// This method block until somebody makes a request
				final Socket connectionSocket = this.welcomeSocket.accept();
				
				// Come out of the loop if the stop flag is set
				if(this.stop)
					break;
				
				InetAddress iadd = connectionSocket.getInetAddress();
				int port = connectionSocket.getLocalPort();
				String addressCombo = iadd.toString() + ":" + port;
				Integer rILM = this.requestsInLastMinute.get(addressCombo);
				if (rILM == null) {
					rILM = 0;
				}
				if (rILM > Integer.MAX_VALUE) {
					System.out.println("BLOCKED: " + rILM + " REQUESTS IN A MINUTE");
					long start = System.currentTimeMillis();
					OutputStream outStream = connectionSocket.getOutputStream();
					IHttpResponse response = (IHttpResponse) HttpResponseFactory.responses.get(429).getConstructor(String.class).newInstance(Protocol.CLOSE);
					response.write(outStream);
					connectionSocket.close();
					rILM++;
					this.requestsInLastMinute.put(addressCombo,rILM);
					this.incrementConnections(1);
					long end = System.currentTimeMillis();
					this.incrementServiceTime(end-start);
				} else {
					// Create a handler for this incoming connection and start the handler in a new thread
					ConnectionHandler handler = new ConnectionHandler(this, connectionSocket);
					final Thread service = new Thread(handler);
					service.start();

					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(2000);
								if (service.isAlive()) {
									OutputStream outStream = connectionSocket.getOutputStream();
									IHttpResponse response = (IHttpResponse) HttpResponseFactory.responses.get(408).getConstructor(String.class).newInstance(Protocol.CLOSE);
									response.write(outStream);
									connectionSocket.close();
									service.stop();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
					rILM++;
					this.requestsInLastMinute.put(addressCombo,rILM);
				}
				
			}
			this.welcomeSocket.close();
		}
		catch(Exception e) {
			window.showSocketException(e);
		}
		}
	}
	
	/**
	 * Stops the server from listening further.
	 */
	public synchronized void stop() {
		if(this.stop)
			return;
		
		// Set the stop flag to be true
		this.stop = true;
		try {
			// This will force welcomeSocket to come out of the blocked accept() method 
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);
			
			// We do not have any other job for this socket so just close it
			socket.close();
		}
		catch(Exception e){}
	}
	
	/**
	 * Checks if the server is stopeed or not.
	 * @return
	 */
	public boolean isStoped() {
		if(this.welcomeSocket != null)
			return this.welcomeSocket.isClosed();
		return true;
	}

	/**
	 * @return
	 */
	public PluginHandler getPluginHandler() {
		// TODO Auto-generated method stub
		return this.pluginHandler;
	}
}
