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
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import protocol.IHttpRequest;
import protocol.IHttpResponse;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class PluginHandler {

	// Handles a list of the plugins. Will be updated by the watch directory.
	public Map<String, IPlugin> plugins;

	/**
	 * 
	 */
	public PluginHandler() {
		plugins = new HashMap<String, IPlugin>();
	}

	public void loadup() {
		File rootFolder = new File("plugins/");
		File[] pluginFolders = rootFolder.listFiles();
		for (File pluginFolder : pluginFolders) {
			loadPluginsFromPluginFolder(pluginFolder);
		}
	}

	public boolean tryPlugin(String rootContext) {
		File rootFolder = new File("plugins/");
		File[] pluginFolders = rootFolder.listFiles();
		for (File pluginFolder : pluginFolders) {
			if (pluginFolder.getName() == rootContext) {
				loadPluginsFromPluginFolder(pluginFolder);
				return true;
			}
		}
		return false;
	}

	public void loadPluginsFromPluginFolder(File pluginFolder) {
		File[] pluginFiles = pluginFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		
		for (File file : pluginFiles) {
			IPlugin plugin = loadPluginFromJar(file.getName(),
											   file.getAbsolutePath());
			if (plugin != null) {
				plugins.put(plugin.getRootContext(), plugin);
			}
		}
	}
	
	// TODO: Make this exception not stupid.
	public IHttpResponse processRequest(IHttpRequest request, Server server) throws Exception {
		String rootContext = pullRootContextFromURI(request.getUri());
		IPlugin plugin = plugins.get(rootContext);
		if (plugin != null) {
			return plugin.processRequest(request, server);
		} else {
			if (tryPlugin(rootContext)) {
				return plugins.get(rootContext).processRequest(request, server);
			} else {
				throw new Exception();
			}
		}
	}

	// This method is used to update the plugin directory.
	public void addNewPlugin(String rootContext, IPlugin plugin) {
		this.plugins.put(rootContext, plugin);
	}

	public static String pullRootContextFromURI(String URI) {
		String[] splitURI = URI.split("/");
		return splitURI[3];
	}

	private static IPlugin loadPluginFromJar(String filename, String filepath) {
		try {
			URLClassLoader ucl = new URLClassLoader(new URL[] { new URL(
					"jar:file:plugins/" + filename + "!/") });
			JarInputStream jarFile = new JarInputStream(new FileInputStream(
					filepath));
			JarEntry entry;
			while (true) {
				entry = jarFile.getNextJarEntry();
				if (entry == null) {
					break;
				}
				if (entry.getName().endsWith(".class")) {
					String classname = entry.getName().replaceAll("/", "\\.");
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
					}
				}
			}
			jarFile.close();
		} catch (Exception e1) {
		}
		return null;
	}

}
