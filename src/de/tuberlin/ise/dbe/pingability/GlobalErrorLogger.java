/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Dave
 *
 */
public class GlobalErrorLogger {

	static PrintWriter pw;

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"MMMdd','HH':'mm':'ss'h'");

	static void open() {
		try {
			pw = new PrintWriter("errors.log");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		pw.println(sdf.format(new Date()) + ": Error log opened.");
		pw.flush();
	}

	synchronized static void log(String who, String target, String what) {
		pw.println("[" + who + "] [" + sdf.format(new Date()) + "] [" + target
				+ "] " + what);
		pw.flush();
	}

	synchronized static void log(Object who, String target, String what) {
		log(who.getClass().getSimpleName(), target, what);
	}

	static void close() {
		pw.println(sdf.format(new Date()) + ": Error log closed.");
		pw.flush();
		pw.close();
	}

}
