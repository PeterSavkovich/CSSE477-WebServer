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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * This is a factory to produce various kind of HTTP responses.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class HttpResponseFactory {
	public static HashMap<Integer, Class<?>> responses = new HashMap<Integer, Class<?>>();
	private static boolean addedResponses = false;
	
	public static void addResponses() {
		// TODO Add a runnable interface, make this use that watch directory service instead.
		if (!addedResponses) {
			responses.put(Protocol.OK_CODE, OK200Response.class);
			responses.put(Protocol.MOVED_PERMANENTLY_CODE, Moved301Response.class);
			responses.put(Protocol.BAD_REQUEST_CODE, BadRequest400Response.class);
			responses.put(Protocol.NOT_FOUND_CODE, NotFound404Response.class);
			responses.put(Protocol.NOT_SUPPORTED_CODE, NotSupported505Response.class);
		}
	}
	/**
	 * Convenience method for adding general header to the supplied response object.
	 * 
	 * @param response The {@link HttpResponse} object whose header needs to be filled in.
	 * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
	 */
	protected static void fillGeneralHeader(HttpResponse response, String connection) {
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
}
