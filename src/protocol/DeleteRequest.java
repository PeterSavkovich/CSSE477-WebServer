/*
 * DeleteRequest.java
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

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class DeleteRequest extends HttpRequest {

	/* (non-Javadoc)
	 * @see protocol.IHttpRequest#handleRequest(protocol.IServer)
	 */
	@Override
	public HttpResponse handleRequest(IServer server) throws Exception {
		// TODO Use that watch directory thing when we get around to it.
		HttpResponseFactory.addResponses();

		// We choose to get rid of everything but the last path component.
		String[] pathComponents = this.getUri().split(Protocol.SLASH + "");
		String uri = Protocol.SLASH + pathComponents[pathComponents.length-1];
		
		File file = new File(server.getRootDirectory() + uri);
		if ((file.exists() && !file.canWrite()) || file.isDirectory()) {
			return (HttpResponse) HttpResponseFactory.responses.get(Protocol.BAD_REQUEST_CODE).getConstructor(String.class).newInstance(Protocol.CLOSE);
		} else {
			try {
				file.delete();
			} catch (Exception e) {
				e.printStackTrace();
				return (HttpResponse) HttpResponseFactory.responses.get(Protocol.BAD_REQUEST_CODE).getConstructor(String.class).newInstance(Protocol.CLOSE);
			}
		}
		// return HttpResponseFactory.create200OK(Protocol.CLOSE);
		// OH NO WAIT, THIS IS SO MUCH BETTER
		return (HttpResponse) HttpResponseFactory.responses.get(200).getConstructor(String.class).newInstance(Protocol.CLOSE);
		// Yes, you have to know which constructor you are calling.
		// You had to know that anyway though. Suck it up.


	}

}
