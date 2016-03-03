/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author Dave
 * 
 *         test supported cipher suites via:
 *         https://github.com/jvehent/cipherscan
 *
 */
public class HttpsGetRunner implements Runnable {

	/** target host */
	private String target;

	/** time in between sending http gets */
	private static long secondsBetweenGets = 300;

	/** run() will terminate when this is set to true */
	private boolean running = true;

	/**
	 * 
	 * @param target
	 *            target host
	 * 
	 */
	HttpsGetRunner(String target) {
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
		System.out.println("HttpsGetRunner for target " + target + " started.");
		String url = "https://" + target;
		while (running) {
			runHttpsGet(url);
			try {
				Thread.sleep(secondsBetweenGets * 1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		System.out.println("HttpsGetRunner for target " + target
				+ " terminated.");
	}

	/**
	 * politely asks this HttpGetRunner to terminate
	 */
	void terminate() {
		running = false;
	}

	/**
	 * sends an https get
	 * 
	 *
	 * 
	 */
	private void runHttpsGet(String url) {
		long start = System.currentTimeMillis();
		try {
			HttpsURLConnection con = (HttpsURLConnection) new URL(url)
					.openConnection();
			if (con.getUseCaches())
				con.setUseCaches(false);
			byte [] res = StreamUtils.readFully(con.getInputStream());
			long latency = System.currentTimeMillis() - start;
			HttpsGetCSVLogger.LOGGER.log(url + ";" + start + ";"
					+ con.getResponseCode() + ";" + latency + ";"
					+ con.getCipherSuite()+";"+(res==null?"0":res.length));
			con.disconnect();

		} catch (Exception e) {
			System.out.println("Exception while sending https get to " + target
					+ ":\n" + e);
			HttpsGetCSVLogger.LOGGER.log(url + ";" + start + ";exception;"
					+ e.getMessage());
			e.printStackTrace();
			GlobalErrorLogger.log(this, target.substring(0, target.indexOf('/')), e.toString());
		}

	}

	static String getOutputFormat() {
		return "target_host;start;http_status;latency_ms;cipher_suite;response_size_byte";
	}

}
