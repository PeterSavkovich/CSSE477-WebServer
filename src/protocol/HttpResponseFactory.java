/*
 * HttpResponseFactory.java
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
 
package protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import responses.BadRequest400Response;
import responses.Moved301Response;
import responses.NotFound404Response;
import responses.NotSupported505Response;
import responses.OK200Response;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;


/**
 * This is a factory to produce various kind of HTTP responses.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class HttpResponseFactory implements Runnable {
	public static HashMap<Integer, Class<?>> responses = new HashMap<Integer, Class<?>>();
	private static WatchService watcher;
	private static Map<WatchKey,Path> keys;
	private static boolean addedResponses = false;
	
	public HttpResponseFactory() {
		try {
			watcher = FileSystems.getDefault().newWatchService();
			keys = new HashMap<WatchKey, Path>();
		} catch (IOException e) {
			// We can't do anything about this.
			e.printStackTrace();
		}
		addResponses();
	}
	
	public static void addResponses() {
		File dir = new File("responses");
		if (!dir.exists()) {
			dir.mkdir();
		}
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		for (File jarfile : files) {
			addResponse(jarfile);
		}
		registerPath(Paths.get(dir.getAbsolutePath()));		
		// TODO Add a runnable interface, make this use that watch directory service instead.
//		if (!addedResponses) {
//			responses.put(Protocol.OK_CODE, OK200Response.class);
//			responses.put(Protocol.MOVED_PERMANENTLY_CODE, Moved301Response.class);
//			responses.put(Protocol.BAD_REQUEST_CODE, BadRequest400Response.class);
//			responses.put(Protocol.NOT_FOUND_CODE, NotFound404Response.class);
//			responses.put(Protocol.NOT_SUPPORTED_CODE, NotSupported505Response.class);
//		}
	}
	
	static String stripExtension(String str) {
		if (str == null)
			return null;
		int pos = str.lastIndexOf(".");
		if (pos == -1)
			return str;
		return str.substring(0, pos);
	}

	
	private static void addResponse(File jarfile) {
		String statusCode = stripExtension(jarfile.getName());
		try {
			URLClassLoader ucl = new URLClassLoader(new URL[] { new URL("jar:file:" + jarfile.getAbsolutePath() + "!/") });
			JarInputStream jarStream = new JarInputStream(new FileInputStream(jarfile.getAbsolutePath()));
			JarEntry entry;
			entry = jarStream.getNextJarEntry();
			if (entry.getName().endsWith(".class")) {
				String className = entry.getName().replaceAll("/", "\\.");
				className = className.substring(0,className.length()-6); // Remove trailing ".class"
				try {
					Class <?> loadedClass = Class.forName(className,true,ucl);
					responses.put(Integer.parseInt(statusCode), loadedClass);
				} catch (ClassNotFoundException e) {
					// We can't do anything!
					e.printStackTrace();
				}
			}
			jarStream.close();
		} catch (Exception e) {
			// Something went wrong, but we don't know how to fix it.
			// Ignore it.
			e.printStackTrace();
		}
	}
	
	private static void removeResponse(File jarfile) {
		String statusCode = stripExtension(jarfile.getName());
		responses.remove(statusCode);
	}
	
	private static void registerPath(Path dir) {
		WatchKey key;
		try {
			key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			keys.put(key, dir);
		} catch (IOException e) {
			// We really can't do anything about this.
			e.printStackTrace();
		}
	}
	/**
	 * Convenience method for adding general header to the supplied response object.
	 * 
	 * @param response The {@link HttpResponse} object whose header needs to be filled in.
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 */
	public static void fillGeneralHeader(HttpResponse response, String connection) {
		// Lets add Connection header
		response.put(Protocol.CONNECTION, connection);

		// Lets add current date
		Date date = Calendar.getInstance().getTime();
		response.put(Protocol.DATE, date.toString());
		
		// Lets add server info
		response.put(Protocol.Server, Protocol.getServerInfo());

		// Lets add extra header with provider info
		response.put(Protocol.PROVIDER, Protocol.AUTHOR);
	}
	

	/**
	 * Creates a {@link HttpResponse} object for sending internal server error response.
	 * 
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 * @return A {@link HttpResponse} object represent 500 status.
	 */
	public static HttpResponse create500InternalServerError(String connection) {
		HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.INTERNAL_SERVER_ERROR_CODE, 
				Protocol.INTERNAL_SERVER_ERROR_TEXT, new HashMap<String, String>(), null);
		
		// Lets fill up the header fields with more information
		fillGeneralHeader(response, connection);
		
		return response;
	}
	@Override
	public void run() {
		while (true) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}
	
			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();
	
				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}
	
				// Context for directory entry event is the file name of entry
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path name = ev.context();
				
				// TODO: OH GOD THIS IS SO UGLY
				File jarfile = new File(dir.toString(),name.toString());
	
				if (kind == ENTRY_CREATE) {
					addResponse(jarfile);
				} else if (kind == ENTRY_DELETE) {
					removeResponse(jarfile);
				}
			}
	
			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);
	
				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}		
}
