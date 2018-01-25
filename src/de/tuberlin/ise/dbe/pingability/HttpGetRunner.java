/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Dave
 *
 */
public class HttpGetRunner implements Runnable {


	

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
	HttpGetRunner(String target) {
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
		System.out.println("HttpGetRunner for target " + target + " started.");
		String url = "http://"+target;
		while (running) {
			runHttpGet(url);
			try {
				Thread.sleep(secondsBetweenGets * 1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		System.out.println("HttpGetRunner for target " + target + " terminated.");
	}

	/**
	 * politely asks this HttpGetRunner to terminate
	 */
	void terminate() {
		running = false;
	}

	/**
	 * sends an http get
	 * 
	 *
	 * 
	 */
	private void runHttpGet(String url) {
		long start = System.currentTimeMillis();
		try {
			
			HttpURLConnection con = (HttpURLConnection) new URL(url)
					.openConnection();
			if (con.getUseCaches())
				con.setUseCaches(false);
			byte [] res = StreamUtils.readFully(con.getInputStream());
			long latency = System.currentTimeMillis()-start;
			HttpGetCSVLogger.LOGGER.log(url+";"+start+";"+con.getResponseCode()+";"+latency+";"+(res==null?"0":res.length));
			IsAliveServer.addHttpgetrun(target, start, "HTTP Code "+con.getResponseCode());
			con.disconnect();
			
		} catch (Exception e) {
			System.out.println("Exception while sending http get to " + target + ":\n" + e);
			HttpGetCSVLogger.LOGGER.log(url+";"+start+";exception;"+e.getMessage());
			e.printStackTrace();
			GlobalErrorLogger.log(this, target.substring(0, target.indexOf('/')), e.toString());
			IsAliveServer.addHttpgetrun(target, System.currentTimeMillis(), "Error ("
					+ e.getClass().getSimpleName() + "):" + e.getMessage());
		}

	}

	

	static String getOutputFormat() {
		return "target_host;start;http_status;latency_ms;response_size_byte";
	}

}
