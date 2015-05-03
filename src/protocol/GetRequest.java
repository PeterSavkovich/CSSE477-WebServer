/*
 * GetRequest.java
 * Apr 26, 2015
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
 
package protocol;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class GetRequest extends HttpRequest {

	/* (non-Javadoc)
	 * @see protocol.IHttpRequest#handleRequest(protocol.IServer)
	 */
	@Override
	public HttpResponse handleRequest(IServer server) throws Exception {
		// TODO Use that watch directory thing when we get around to it.
		HttpResponseFactory.addResponses();

		// TODO Auto-generated method stub
		String uri = this.getUri();
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);
		if (requestWill404(server)) {
			return (HttpResponse) HttpResponseFactory.responses.get(Protocol.NOT_FOUND_CODE).getConstructor(String.class).newInstance(Protocol.CLOSE);		}
		else if (file.isDirectory()) {
			String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
			file = new File(location);
		}
		//return HttpResponseFactory.create200OK(file, Protocol.CLOSE);
		// OH NO WAIT, THIS IS SO MUCH BETTER
		return (HttpResponse) HttpResponseFactory.responses.get(Protocol.OK_CODE).getConstructor(File.class, String.class).newInstance(file, Protocol.CLOSE);
		// Yes, you have to know which constructor you are calling.
		// You had to know that anyway though. Suck it up.
	}
	
	private boolean requestWill404(IServer server) {
		File file = new File(server.getRootDirectory() + this.getUri());
		File indexFile = new File(server.getRootDirectory() + this.getUri() + System.getProperty("file.separator") + Protocol.DEFAULT_FILE);
		
		return (!file.exists() || (file.isDirectory() && !indexFile.exists()));
	}

}
