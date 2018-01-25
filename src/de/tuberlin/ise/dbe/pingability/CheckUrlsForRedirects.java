/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Dave
 *
 */
public class CheckUrlsForRedirects {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out
					.println("Start with location of config file as first parameter and verbose as word if detailled output is desired");
			return;
		}
		boolean verbose = args.length >= 2
				&& args[1].equalsIgnoreCase("verbose");

		try {
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			String line;
			while ((line = br.readLine()) != null) {
				if (!(line.trim().length() == 0)) {
					String url = "http://" + line.trim();
					System.out.print(url);
					HttpURLConnection con = (HttpURLConnection) new URL(url)
							.openConnection();
					if (verbose) {
						System.out.println(" => " + con.getResponseCode()
								+ "(" + con.getResponseMessage() + ")");
						if (con.getResponseCode() / 100 == 3) {
							System.out.println("=> got redirect to "
									+ con.getHeaderField("Location"));
						}
					} else {
						if (con.getResponseCode() / 100 == 3) {
							System.out.println(" sends redirect to "
									+ con.getHeaderField("Location") + " [http status code:"+con.getResponseCode()
									+ ", " + con.getResponseMessage() + "]");
						}
					}
					con.disconnect();
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Could not read config file, terminating now:\n");e.printStackTrace();
			System.exit(-1);
		}

	}

}
