/*
 * TestDynamicLoadServlet.java
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
 
package plugins;

import java.io.File;

import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IHttpRequest;
import protocol.IHttpResponse;
import protocol.IServer;
import protocol.Protocol;
import server.IPlugin;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class TestDynamicLoadServlet implements IPlugin {

	/* (non-Javadoc)
	 * @see server.IPlugin#processRequest(protocol.IHttpRequest, protocol.IServer)
	 */
	@Override
	public IHttpResponse processRequest(IHttpRequest request, IServer server)
			throws Exception {
		System.out.println("TEST GET SERVLET GOT HIT UP");
		
		String[] uriL = request.getUri().split("/");
		String uri = uriL[0] + "/";
		for (int i = 3; i < uriL.length; i++) {
			uri += uriL[i] + "/";
		}
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);
		if (requestWill404(server, uri)) {
			return (HttpResponse) HttpResponseFactory.responses.get(Protocol.NOT_FOUND_CODE).getConstructor(String.class).newInstance(Protocol.CLOSE);		}
		else if (file.isDirectory()) {
			String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
			file = new File(location);
		}
		//return HttpResponseFactory.create200OK(file, Protocol.CLOSE);
		// OH NO WAIT, THIS IS SO MUCH BETTER
		return (HttpResponse) HttpResponseFactory.responses.get(Protocol.OK_CODE).getConstructor(File.class, String.class).newInstance(file, Protocol.CLOSE);
	}
	
	private boolean requestWill404(IServer server, String uri) {
		File file = new File(server.getRootDirectory() + uri);
		File indexFile = new File(server.getRootDirectory() + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE);
		
		return (!file.exists() || (file.isDirectory() && !indexFile.exists()));
	}

}
