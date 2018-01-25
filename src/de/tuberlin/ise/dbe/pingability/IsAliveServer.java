package de.tuberlin.ise.dbe.pingability;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class IsAliveServer extends AbstractHandler {

	/** map from target to html status line for pings */
	private ConcurrentHashMap<String, String> pings = new ConcurrentHashMap<>();

	/** map from target to html status line for http get */
	private ConcurrentHashMap<String, String> httpgets = new ConcurrentHashMap<>();

	/** map from target to html status line for https get */
	private ConcurrentHashMap<String, String> httpsgets = new ConcurrentHashMap<>();

	/** map from target to html status line for cipherscan */
	private ConcurrentHashMap<String, String> cipherscans = new ConcurrentHashMap<>();

	private static Server server;
	private static IsAliveServer handler;

	public static void startServer() {
		if (server != null) {
			System.out.println("IsAliveServer was already started.");
			return;
		}
		server = new Server(8083);
		handler = new IsAliveServer();
		server.setHandler(handler);
		try {
			server.start();
			System.out.println("IsAliveServer is now running.");
		} catch (Exception e) {
			System.out.println("Could not start IsAliveServer (exiting), stacktrace:");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void stopServer() {
		try {
			server.stop();
			server.join();
			System.out.println("IsAliveServer is stopped.");
		} catch (Exception e) {
			System.out.println("Error while stopping IsAliveServer:");
			e.printStackTrace();
		}
	}

	public static void addPingrun(String target, long date, String result) {
		if (handler == null) {
			System.out.println("IsAliveServer is not yet started.");
			return;
		}
		handler.pings.put(target, assembleLine(target, date, result));
	}

	public static void addHttpgetrun(String target, long date, String result) {
		if (handler == null) {
			System.out.println("IsAliveServer is not yet started.");
			return;
		}
		handler.httpgets.put(target, assembleLine(target, date, result));
	}

	public static void addHttpsgetrun(String target, long date, String result) {
		if (handler == null) {
			System.out.println("IsAliveServer is not yet started.");
			return;
		}
		handler.httpsgets.put(target, assembleLine(target, date, result));
	}

	public static void addCipherscanrun(String target, long date, String result) {
		if (handler == null) {
			System.out.println("IsAliveServer is not yet started.");
			return;
		}
		handler.cipherscans.put(target, assembleLine(target, date, result));
	}

	private static String assembleLine(String target, long date, String result) {
		if (target.contains("/"))
			return "<tr><td>" + new Date(date) + "</td><td>" + result + "</td><td>"
					+ target.substring(0, target.indexOf("/")) + "</td></tr>";
		else
			return "<tr><td>" + new Date(date) + "</td><td>" + result + "</td><td>" + target
					+ "</td></tr>";
	}

	private static String getTableHeader() {
		return "<tr><th>Timestamp</th><th>Result</th><th>Target</th></tr>";
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		// Declare response encoding and types
		response.setContentType("text/html; charset=utf-8");

		// Declare response status code
		response.setStatus(HttpServletResponse.SC_OK);

		// Write back response
		response.getWriter()
				.println(
						"<!DOCTYPE html><html><head><style>table, th, td {border: 1px solid black;}</style></head><body>");

		response.getWriter().println("<h1>Web API Benchmark is still alive</h1>");

		response.getWriter().println("<h2>Ping:</h2>");
		response.getWriter().println("<table>" + getTableHeader());
		for (String s : pings.values())
			response.getWriter().println(s);
		response.getWriter().println("</table>");

		response.getWriter().println("<h2>HTTP GET:</h2>");
		response.getWriter().println("<table>" + getTableHeader());
		for (String s : httpgets.values())
			response.getWriter().println(s);
		response.getWriter().println("</table>");

		response.getWriter().println("<h2>HTTPS GET:</h2>");
		response.getWriter().println("<table>" + getTableHeader());
		for (String s : httpsgets.values())
			response.getWriter().println(s);
		response.getWriter().println("</table>");

		response.getWriter().println("<h2>Cipherscan:</h2>");
		response.getWriter().println("<table>" + getTableHeader());
		for (String s : cipherscans.values())
			response.getWriter().println(s);
		response.getWriter().println("</table>");
		response.getWriter().println("</body></html>");

		// Inform jetty that this request has now been handled
		baseRequest.setHandled(true);
	}

	public static void main(String[] args) throws Exception {
		startServer();
		handler.pings.put("test",
				assembleLine("test target", System.currentTimeMillis(), "test result"));
		handler.httpgets.put("test",
				assembleLine("test target", System.currentTimeMillis(), "test result"));
		handler.httpsgets.put("test",
				assembleLine("test target/abc", System.currentTimeMillis(), "test result"));
		handler.cipherscans.put("test",
				assembleLine("test target", System.currentTimeMillis(), "test result"));

	}
}