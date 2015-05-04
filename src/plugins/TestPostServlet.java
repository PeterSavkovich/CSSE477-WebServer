/*
 * TestPostServlet.java
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
import java.io.FileWriter;

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
public class TestPostServlet implements IPlugin {

	/**
	 * 
	 */
	public TestPostServlet() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see server.IPlugin#processRequest(protocol.IHttpRequest, protocol.IServer)
	 */
	@Override
	public IHttpResponse processRequest(IHttpRequest request, IServer server)
			throws Exception {
		System.out.println("TEST POST SERVLET GOT HIT UP");
		HttpResponseFactory.addResponses();
		String[] pathComponents = request.getUri().split(Protocol.SLASH + "");
		String uri = Protocol.SLASH + pathComponents[pathComponents.length-1];
		
		File file = new File(server.getRootDirectory() + uri);
		if ((file.exists() && !file.canWrite()) || file.isDirectory()) {
			return (HttpResponse) HttpResponseFactory.responses.get(Protocol.BAD_REQUEST_CODE).getConstructor(String.class).newInstance(Protocol.CLOSE);
		} else {
			try {
				file.createNewFile();  // Only creates a file if it doesn't exist
				FileWriter fw = new FileWriter(file, false);
				fw.write(request.getBody());
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
				return (HttpResponse) HttpResponseFactory.responses.get(Protocol.BAD_REQUEST_CODE).getConstructor(File.class, String.class).newInstance(file, Protocol.CLOSE);			}
		}
		return (HttpResponse) HttpResponseFactory.responses.get(Protocol.OK_CODE).getConstructor(File.class, String.class).newInstance(file, Protocol.CLOSE);
		//return request.handleRequest(server);
	}

}
