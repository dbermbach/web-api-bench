/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Dave
 *
 */
public class HttpGetCSVLogger implements Runnable {

	public final static HttpGetCSVLogger LOGGER = new HttpGetCSVLogger();

	static {
		new Thread(LOGGER).start();
	}

	private final String outputfile = "httpget_output.csv";

	private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

	private boolean running = true;

	void terminate() {
		running = false;
	}

	void log(String msg) {
		queue.add(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		PrintWriter pw;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(outputfile, true)));
			pw.println(HttpGetRunner.getOutputFormat());
		} catch (Exception e) {
			System.out
					.println("Terminating because file output could not be created:\n"
							+ e);
			GlobalErrorLogger.log(this, "logger terminating", e.toString());
			return;
		}
		System.out.println("HttpGetCSVLogger started.");
		String line;
		while (running) {
			line = queue.poll();
			if (line == null) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				continue;
			}
			// got a message
			pw.println(line);
			pw.flush();
		}
		// we have been asked to terminate, now empty the queue...
		line = queue.poll();
		while (line != null) {
			pw.println(line);
			line = queue.poll();
		}
		pw.close();
		System.out.println("HttpGetCSVLogger is now terminated.");
	}

}
