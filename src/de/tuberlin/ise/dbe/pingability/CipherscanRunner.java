/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Dave
 */
public class CipherscanRunner implements Runnable {

	/** target host */
	private List<String> targets;

	/** time in between sending pings */
	private static long secondsBetweenCipherscans = 60 * 60 * 12;

	/** run() will terminate when this is set to true */
	private boolean running = true;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yy'-'MM'-'dd'_'HH'-'mm");

	/**
	 * @param targets target hosts
	 */
	CipherscanRunner(List<String> targets) {
		super();
		this.targets = targets;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("CipherscanRunner started.");
		while (running) {
			for (String target : targets)
				runCipherscan(target);
			long startNextRun = System.currentTimeMillis() + (secondsBetweenCipherscans * 1000);
			while (running && System.currentTimeMillis() < startNextRun) {
				try {
					Thread.sleep(startNextRun - System.currentTimeMillis());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		System.out.println("CipherscanRunner terminated.");
	}

	/**
	 * politely asks this CipherscanRunner to terminate
	 */
	void terminate() {
		running = false;
	}

	/**
	 * executes a cipherscan
	 */
	private void runCipherscan(String target) {
		System.out.println("Starting cipherscan for " + target);
		try {
			ProcessBuilder pb = new ProcessBuilder("./cipherscan", target);
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line, result = "";
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				result += line + "\n";
			}
			p.waitFor();
			PrintWriter pw = new PrintWriter(target + "_" + sdf.format(new Date()) + ".txt");
			pw.println(result);
			pw.close();
			br.close();
			IsAliveServer.addCipherscanrun(target, System.currentTimeMillis(), "OK");

		} catch (Exception e) {
			System.out.println("Exception while running cipherscan for " + target + ":\n" + e);
			e.printStackTrace();
			GlobalErrorLogger.log(this, target, e.toString());
			IsAliveServer.addCipherscanrun(target, System.currentTimeMillis(), "Error ("
					+ e.getClass().getSimpleName() + "):" + e.getMessage());
		}

	}

}
