/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Dave
 *
 */
public class PingRunner implements Runnable {

	/** numbers of packets sent */
	private static int noOfPackets = 5;

	/**
	 * terminates after receiving noOfPackets responses or after
	 * secondsToTimeout seconds
	 */
	private static int secondsToTimeout = 5;

	/** target host */
	private String target;

	/** time in between sending pings */
	private static long secondsBetweenPings = 300;

	/** run() will terminate when this is set to true */
	private boolean running = true;

	/**
	 * 
	 * @param target
	 *            target host
	 * 
	 */
	PingRunner(String target) {
		super();
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("PingRunner for target " + target + " started.");
		while (running) {
			runPing(false);
			try {
				Thread.sleep(secondsBetweenPings * 1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		System.out.println("PingRunner for target " + target + " terminated.");
	}

	/**
	 * politely asks this PingRunner to terminate
	 */
	void terminate() {
		running = false;
	}

	/**
	 * executes a ping
	 * 
	 * @param inDebugMode
	 *            if true will not actually ping but rather use the dummy
	 *            methods
	 * 
	 * 
	 */
	private void runPing(boolean inDebugMode) {
		double[] res = null;
		String line = null, result = null;
		long start = System.currentTimeMillis();
		try {
			res = new double[2];
			if (!inDebugMode) {
				ProcessBuilder pb = new ProcessBuilder("ping", "-w "
						+ +secondsToTimeout, "-c " + noOfPackets, target);
				Process p = pb.start();

				BufferedReader br = new BufferedReader(new InputStreamReader(
						p.getInputStream()));

				while ((line = br.readLine()) != null) {
					// System.out.println(line);
					result += line + "\n";
				}
				p.waitFor();
				// System.out.println("got a result:\n"+result);
			} else {
				// dummy test mode
				if (Math.random() > 0.5)
					result = dummySuccessfulPing();
				else
					result = dummyFailedPing();
			}
			long end = System.currentTimeMillis();
			// parse results
			result = result.trim();
			// System.out.println(result);
			line = result.substring(result.indexOf("received") + 10);
			// System.out.println(line);
			res[0] = 1 - (Double.parseDouble(line.substring(0,
					line.indexOf('%'))) / 100.0);
			if (line.indexOf("rtt") > -1) {
				res[1] = Double.parseDouble(line.split("/")[4]);
			} else {
				res[1] = -1;
			}
			// persist results
			PingCSVLogger.LOGGER.log(target + ";" + start + ";" + end + ";"
					+ res[0] + ";" + res[1]);
			// System.out.println("Result for " + target + ": pingability="
			// + res[0] + ", avg. latency=" + res[1]);
		} catch (Exception e) {
			System.out.println("Exception while pinging " + target + ":\n" + e);
			PingCSVLogger.LOGGER.log(target + ";" + start + ";exception;"
					+ e.getMessage());
			e.printStackTrace();
			GlobalErrorLogger.log(this, target, e.toString());
		}

	}

	/**
	 * successful response for testing
	 * 
	 * @return
	 */
	String dummySuccessfulPing() {
		return "PING google.com (74.125.24.102) 56(84) bytes of data."
				+ "\n64 bytes from de-in-f102.1e100.net (74.125.24.102): icmp_seq=1 ttl=52 time=0.914 ms"
				+ "\n64 bytes from de-in-f102.1e100.net (74.125.24.102): icmp_seq=2 ttl=52 time=0.922 ms"
				+ "\n64 bytes from de-in-f102.1e100.net (74.125.24.102): icmp_seq=3 ttl=52 time=0.987 ms"
				+ "\n64 bytes from de-in-f102.1e100.net (74.125.24.102): icmp_seq=4 ttl=52 time=0.938 ms"
				+ "\n64 bytes from de-in-f102.1e100.net (74.125.24.102): icmp_seq=5 ttl=52 time=0.922 ms"
				+ "\n\n--- google.com ping statistics ---"
				+ "\n5 packets transmitted, 5 received, 0% packet loss, time 4006ms"
				+ "\nrtt min/avg/max/mdev = 0.920/0.940/0.960/0.041 ms";
	}

	/**
	 * failed response for testing
	 * 
	 * @return
	 */
	String dummyFailedPing() {
		return "PING amazon.de (178.236.6.250) 56(84) bytes of data."
				+ "\n\n--- amazon.de ping statistics ---"
				+ "\n5 packets transmitted, 0 received, 100% packet loss, time 5000ms";
	}

	static String getOutputFormat() {
		return "target_host;start;end;pingability;avg_latency";
	}

}
