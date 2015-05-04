/*
 * ServletRouteManager.java
 * May 4, 2015
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

import java.util.HashMap;

import protocol.HttpRequest;
import protocol.IHttpRequest;
import protocol.IHttpResponse;
import protocol.IServer;
import protocol.Protocol;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class PluginRouteManager {

	private HashMap<String, IPlugin> getRouteHandler;
	private HashMap<String, IPlugin> postRouteHandler;
	private HashMap<String, IPlugin> putRouteHandler;
	private HashMap<String, IPlugin> deleteRouteHandler;
	private HashMap<String, IPlugin> otherRouteHandler;
	
	/**
	 * 
	 */
	public PluginRouteManager() {
		this.getRouteHandler = new HashMap<String, IPlugin>();
		this.postRouteHandler = new HashMap<String, IPlugin>();
		this.putRouteHandler = new HashMap<String, IPlugin>();
		this.deleteRouteHandler = new HashMap<String, IPlugin>();
		this.otherRouteHandler = new HashMap<String, IPlugin>();
	}
	
	// Given the resource, handle the request.
	public IHttpResponse routeRequest(String resource, IHttpRequest request, IServer server) throws Exception {
		String method = ((HttpRequest)request).getMethod();
		if (method.equals(Protocol.GET)) {
			System.out.println(resource);
			return this.routeForGetRequest(resource).processRequest(request, server);
		} else if (method.equals(Protocol.POST)) {
			return this.routeForPostRequest(resource).processRequest(request, server);
		} else if (method.equals(Protocol.PUT)) {
			return this.routeForPutRequest(resource).processRequest(request, server);
		} else if (method.equals(Protocol.DELETE)) {
			return this.routeForDeleteRequest(resource).processRequest(request, server);
		} else {
			return this.routeForOtherRequest(resource).processRequest(request, server);
		}
	}
	
	public void addGetRoute(String uri, IPlugin resource) {
		System.out.println("Added resource: " + uri);
		this.getRouteHandler.put(uri, resource);
	}
	
	public IPlugin routeForGetRequest(String resource) {
		return this.getRouteHandler.get(resource);
	}

	
	public void addPostRoute(String uri, IPlugin resource) {
		this.postRouteHandler.put(uri, resource);
	}
	
	public IPlugin routeForPostRequest(String resource) {
		return this.postRouteHandler.get(resource);
	}

	
	public void addPutRoute(String uri, IPlugin resource) {
		this.putRouteHandler.put(uri, resource);
	}
	
	public IPlugin routeForPutRequest(String resource) {
		return this.putRouteHandler.get(resource);
	}

	
	public void addDeleteRoute(String uri, IPlugin resource) {
		this.deleteRouteHandler.put(uri, resource);
	}
	
	public IPlugin routeForDeleteRequest(String resource) {
		return this.deleteRouteHandler.get(resource);
	}

	
	public void addOtherRoute(String uri, IPlugin resource) {
		this.otherRouteHandler.put(uri, resource);
	}
	
	public IPlugin routeForOtherRequest(String resource) {
		return this.otherRouteHandler.get(resource);
	}
}
