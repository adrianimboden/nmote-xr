/*
 * Copyright (c) Nmote Ltd. 2003-2014. All rights reserved. 
 * See LICENSE doc in a root of project folder for additional information.
 */

package com.nmote.xr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

// TODO GET method should return xhtml with a list of methods and method help
// @PMD:REVIEWED:AtLeastOneConstructor: by vjeko on 2005.12.28 01:18
public class XRServlet extends HttpServlet {

	public static final String ENDPOINT_KEY = "com.nmote.xr.Endpoint"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory.getLogger(XRServlet.class);

	private static final long serialVersionUID = -5739181478381832593L;
	private static final String TEXT_XML = "text/xml"; //$NON-NLS-1$

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		endpointKey = config.getInitParameter("endpointKey");
		if (endpointKey == null) {
			endpointKey = ENDPOINT_KEY;
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {

		Endpoint endpoint = (Endpoint) getServletContext().getAttribute(endpointKey);
		if (endpoint == null)
			throw new ServletException("no Endpoint found in a servlet context with name:" + endpointKey); //$NON-NLS-1$ 

		MethodResponse result;
		if (TEXT_XML.equals(request.getContentType())) {
			try {
				XmlRpcParseSupport parseSupport = new XmlRpcParseSupport();
				// @PMD:REVIEWED:ShortVariable: by vjeko on 2005.12.28 01:17
				InputStream in = request.getInputStream();
				MethodCall call = parseSupport.handler.parseMethodCall(in, parseSupport.xmlReader);
				result = endpoint.call(call);
			} catch (SAXException e) {
				result = new MethodResponse(Fault.newSystemFault(Fault.INVALID_XML_REQUEST, e));
			} catch (RuntimeException e) {
				result = new MethodResponse(Fault.newSystemFault(Fault.SERVER_ERROR, e));
			}
		} else {
			result = new MethodResponse(Fault.newSystemFault(1013, request.getContentType()));
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XmlRpcWriter xrw = new XmlRpcWriter(new OutputStreamWriter(baos, "utf-8")); //$NON-NLS-1$
		xrw.writeMethodResponse(result);
		xrw.close();
		response.setContentType(TEXT_XML);
		response.setContentLength(baos.size());
		OutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.close();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		long started = System.currentTimeMillis();
		try {
			super.service(req, resp);
		} finally {
			long elapsed = System.currentTimeMillis() - started;
			LOG.debug("Elapsed " + elapsed + " ms");
		}
	}

	private String endpointKey;
}
