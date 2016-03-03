/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Dave
 *
 */
public class PingCSVLogger implements Runnable {

	public final static PingCSVLogger LOGGER = new PingCSVLogger();

	static {
		new Thread(LOGGER).start();
	}
	
	

	private final String outputfile = "ping_output.csv";

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
			pw = new PrintWriter(outputfile);
			pw.println(PingRunner.getOutputFormat());
		} catch (Exception e) {
			System.out
					.println("Terminating because file output could not be created:\n"
							+ e);
			GlobalErrorLogger.log(this, "logger terminating", e.toString());
			return;
		}
		System.out.println("PingCSVLogger started.");
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
		System.out.println("PingCSVLogger is now terminated.");
	}

}
