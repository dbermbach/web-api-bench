/**
 * 
 */
package de.tuberlin.ise.dbe.pingability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Dave
 *
 */
public class Starter {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		GlobalErrorLogger.open();
		if (args.length < 4) {
			System.out
					.println("Start with location of config files as parameters: 1) ping 2) http get 3) https get 4) cipherscan");
			return;
		}
		List<String> pingTargets = readConfigFile(args[0]);
		List<String> httpTargets = readConfigFile(args[1]);
		List<String> httpsTargets = readConfigFile(args[2]);
		List<String> cipherscanTargets = readConfigFile(args[3]);

		ArrayList<PingRunner> pingRunners = new ArrayList<>();
		ArrayList<HttpGetRunner> httpGetRunners = new ArrayList<>();
		ArrayList<HttpsGetRunner> httpsGetRunners = new ArrayList<>();

		CipherscanRunner cipherscanrunner = new CipherscanRunner(
				cipherscanTargets);
		new Thread(cipherscanrunner).start();
		Thread.sleep(60000);

		for (String target : pingTargets) {
			PingRunner pr = new PingRunner(target);
			new Thread(pr).start();
			pingRunners.add(pr);
		}
		Thread.sleep(60000);
		for (String target : httpTargets) {
			HttpGetRunner hgr = new HttpGetRunner(target);
			new Thread(hgr).start();
			httpGetRunners.add(hgr);
		}
		Thread.sleep(60000);
		for (String target : httpsTargets) {
			HttpsGetRunner hgr = new HttpsGetRunner(target);
			new Thread(hgr).start();
			httpsGetRunners.add(hgr);
		}

		System.out.println("Benchmarks are running now.");
		System.out.println("Type \"exit\" to terminate.");
		Scanner scan = new Scanner(System.in);
		while (!scan.nextLine().equals("exit")) {
			System.out.println("Type \"exit\" to terminate.");
		}
		for (PingRunner pr : pingRunners)
			pr.terminate();
		for (HttpGetRunner hgr : httpGetRunners)
			hgr.terminate();
		for (HttpsGetRunner hgr : httpsGetRunners)
			hgr.terminate();
		cipherscanrunner.terminate();
		System.out
				.println("All Runners have been asked to terminate, waiting for 15s now...");
		GlobalErrorLogger.close();
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
		}
		PingCSVLogger.LOGGER.terminate();
		HttpGetCSVLogger.LOGGER.terminate();
		HttpsGetCSVLogger.LOGGER.terminate();
		scan.close();
		System.out.println("Byebye.");
	}

	private static List<String> readConfigFile(String filename) {
		List<String> result = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (!(line.trim().length() == 0 || line.startsWith("#"))) {
					result.add(line.trim());
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Could not read config file " + filename
					+ ", terminating now:\n" + e);
			System.exit(-1);
		}
		return result;
	}

}
