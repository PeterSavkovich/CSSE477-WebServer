/*
 * PluginHandler.java
 * May 3, 2015
 *
 * Simple Web Server (SWS) for EE407/507 and CS455/555
 * 
 * Copyright (C) 2011 Chandan Raj Rupakheti, Clarkson University
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
 * Contact Us:
 * Chandan Raj Rupakheti (rupakhcr@clarkson.edu)
 * Department of Electrical and Computer Engineering
 * Clarkson University
 * Potsdam
 * NY 13699-5722
 * http://clarkson.edu/~rupakhcr
 */

package server;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import protocol.HttpRequest;
import protocol.IHttpRequest;
import protocol.IHttpResponse;
import protocol.Protocol;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class PluginHandler {

	// Handles a list of the plugins. Will be updated by the watch directory.
	public Map<String, PluginRouteManager> plugins;

	/**
	 * 
	 */
	public PluginHandler() {
		plugins = new HashMap<String, PluginRouteManager>();
		loadup();
	}

	// Load in all plug-in files at startup.
	public void loadup() {
		File rootFolder = new File("plugins/");
		File[] pluginFolders = rootFolder.listFiles();
		for (File pluginFolder : pluginFolders) {
			loadPluginsFromPluginFolder(pluginFolder);
		}
	}

	// For a config file, create the route manager that handles it.
	public void loadRouteManager(File config, File pluginFolder) {
		try { 
			BufferedReader br = new BufferedReader(new FileReader(config));
			String line;
			PluginRouteManager prm = new PluginRouteManager();
			while ((line = br.readLine()) != null) {
				String[] sp = line.split(" ");
				System.out.println("In the prm maker: " + sp[0]);
				IPlugin plugin = loadPluginFromJar(sp[2] + ".jar", pluginFolder.getAbsolutePath() + "\\" + sp[2] + ".jar");
				if (sp[0].equals("GET")) {
					System.out.println("Adding that get");
					prm.addGetRoute(sp[1], plugin);
				} else if (sp[0].equals("POST")) {
					prm.addPostRoute(sp[1], plugin);
				} else if (sp[0].equals("PUT")) {
					prm.addPutRoute(sp[1], plugin);
				} else if (sp[0].equals("DELETE")) {
					prm.addDeleteRoute(sp[1], plugin);
				} else {
					prm.addOtherRoute(sp[1], plugin);
				}
			}
			System.out.println("prm: " + pluginFolder.getName());
			plugins.put(pluginFolder.getName(), prm);
			br.close();
		} catch (Exception e) {
			// Fuck your mom
		}
	}
	
	// Search for a plug-in that doesn't exist currently.
	public boolean tryPlugin(String rootContext) {
		File rootFolder = new File("plugins/"+rootContext+"/");
		if (!rootFolder.exists() || !rootFolder.isDirectory()) {
			return false;
		}
		loadPluginsFromPluginFolder(rootFolder);
		return true;
	}
	
	// Remove the extension from a file name.
	static String stripExtension(String str) {
		if (str == null)
			return null;
		int pos = str.lastIndexOf(".");
		if (pos == -1)
			return str;
		return str.substring(0, pos);
	}

	// Given a plugin folder, load in the config file.
	public void loadPluginsFromPluginFolder(File pluginFolder) {
		File[] configFiles = pluginFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".config");
			}
		});
		
		if (configFiles.length == 1) {
			loadRouteManager(configFiles[0], pluginFolder);
		} else {
			// Probably throw an exception here
		}
	}
	
	// TODO: Make this exception not stupid.
	public IHttpResponse processRequest(IHttpRequest request, Server server) throws Exception {
		String rootContext = pullRootContextFromURI(request.getUri());
	    System.out.println(rootContext);
		PluginRouteManager prm = plugins.get(rootContext);
		if (prm != null) {
			return prm.routeRequest(pullResourceFromURI(request.getUri()), request, server);
		} else {
			if (tryPlugin(rootContext)) {
				return plugins.get(rootContext).routeRequest(pullResourceFromURI(request.getUri()), request, server);
			} else {
				throw new Exception();
			}
		}
	}

	// This method is used to update the plugin directory.
	public void addNewPlugin(String rootContext, PluginRouteManager prm) {
		this.plugins.put(rootContext, prm);
	}

	// TODO: Make this less stupid
	public static String pullRootContextFromURI(String URI) {
		String[] splitURI = URI.split("/");
		return splitURI[1];
	}

	// TODO: Make this less stupid
	public static String pullResourceFromURI(String URI) {
		String[] splitURI = URI.split("/");
		return splitURI[2];
	}
	
	// Given file information, pulls the Plugin out of the jar.
	private IPlugin loadPluginFromJar(String filename, String filepath) {
		try {
			URLClassLoader ucl = new URLClassLoader(new URL[] { new URL(
					"jar:file:plugins/"+stripExtension(filename)+"/" + filename + "!/") }, this.getClass().getClassLoader());
			JarInputStream jarFile = new JarInputStream(new FileInputStream(
					filepath));
			JarEntry entry;
			while (true) {
				entry = jarFile.getNextJarEntry();
				if (entry == null) {
					break;
				}
				if (entry.getName().endsWith(".class")) {
//					String[] classnameArr = entry.getName().split("/");
//					
//					String classname = classnameArr[classnameArr.length-1];
					String classname = entry.getName().replaceAll("/", ".");
					classname = classname.substring(0, classname.length() - 6);
					try {
						Class<?> loadedClass = Class.forName(classname, true,
								ucl);
						if (IPlugin.class.isAssignableFrom(loadedClass)) {
							IPlugin plugin = (IPlugin) loadedClass
									.getConstructor().newInstance();
							return plugin;
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
			jarFile.close();
		} catch (Exception e1) {
		}
		return null;
	}

}
