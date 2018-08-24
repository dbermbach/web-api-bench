/**
 * 
 */
package de.tuberlin.mcc.dbe.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * @author Dave
 */
public class AvailabilityCorrelationAnalyser {

	private static String inFolderHttps = "C:\\temp\\https_2018";
	private static String inFolderHttp = "C:\\temp\\http_2018";
	static String tmpFolder = "C:\\temp\\delete_me_anytime";

	/** mapping from api string to out queue */
	private static HashMap<String, PriorityQueue<FileEntry>> outQueues = new HashMap<>();
	private static HashMap<PriorityQueue<FileEntry>, String> reverseLookup = new HashMap<>();

	/** mapping from api string to out writer */
	private static HashMap<String, PrintWriter> outTmpWriters = new HashMap<>();

	static HashMap<String, String> apiAlias = new HashMap<>();

	private static PriorityQueue<FileEntry> currentMinQueue = null;
	private static PriorityQueue<FileEntry> currentMaxQueue = null;
	final static int BUFFERSIZE = 50;

	private static int linesRead = 1;

	static Scanner scan = new Scanner(System.in);

	static {
		try {
			File tmpFldr = new File(tmpFolder);
			if (tmpFldr.exists()) {
				System.out.println("Folder " + tmpFolder + " already exists, exiting.");
				System.exit(-1);
			}
			tmpFldr.mkdir();
			if (!tmpFldr.exists() || !tmpFldr.isDirectory()) {
				System.out.println("Failed to create temp folder.");
				System.exit(-1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		HashMap<String, BufferedReader> httpReaders = new HashMap<>(), httpsReaders = new HashMap<>();
		String region;
		for (File file : new File(inFolderHttp).listFiles()) {
			System.out.println("Enter region name for input file " + file.getAbsolutePath()
					+ "\nNote: It is important to use the same"
					+ " region name for both http and https files.");
			region = scan.next();
			httpReaders.put(region, createBufferedReader(file));
		}
		for (File file : new File(inFolderHttps).listFiles()) {
			System.out.println("Enter region name for input file " + file.getAbsolutePath()
					+ "\nNote: It is important to use the same"
					+ " region name for both http and https files.");
			region = scan.next();
			httpsReaders.put(region, createBufferedReader(file));
		}
		for (int i = 0; i < 5; i++)
			readNextLines(httpReaders, httpsReaders);
		boolean doneReading = false, flag = true;
		while (flag) {
			String api;

			if (doneReading) {
				System.out.println("All files have been read completely, emptying buffers.");
				// empty queues and terminate
				while (currentMaxQueue.size() > 0) {
					// System.out.println("Done reading, emptying buffers. MinQSize="
					// + currentMinQueue.size() + ", maxQSize=" + currentMaxQueue.size());
					api = reverseLookup.get(currentMaxQueue);
					writeNextIntermediateLines(api, currentMaxQueue.size());
				}
			} else if (currentMaxQueue.size() >= BUFFERSIZE) {
				// maintain a healthy buffer in the queues
				while (currentMaxQueue.size() >= BUFFERSIZE) {
					// System.out.println("Writing lines. MinQSize=" + currentMinQueue.size()
					// + ", maxQSize=" + currentMaxQueue.size());
					api = reverseLookup.get(currentMaxQueue);
					writeNextIntermediateLines(api, currentMaxQueue.size() + 1 - BUFFERSIZE);
				}
			} else {
				int counter = 0;
				while (!doneReading && currentMinQueue.size() <= BUFFERSIZE && counter++ < 5) {
					// System.out.println("Reading more lines. MinQSize=" + currentMinQueue.size()
					// + ", maxQSize=" + currentMaxQueue.size());
					if (!readNextLines(httpReaders, httpsReaders)) {
						doneReading = true;
					}
				}
			}
			if (currentMaxQueue.size() == 0)
				flag = false;
			// for debugging purposes
			if (currentMaxQueue.size() - 50 > currentMinQueue.size()) {
				StringBuilder sb = new StringBuilder("Q-Status: ");
				for (String s : outQueues.keySet()) {
					sb.append(s + "(" + outQueues.get(s).size() + "), ");
				}
				System.out.println(sb);
				Thread.sleep(1000);
			}

		}
		closeWriters();
		System.out.println("Done: creating intermediate files.");
		PerApiProcessor.runSecondStage();
	}

	private static void closeWriters() {
		for (PrintWriter pw : outTmpWriters.values())
			pw.close();
	}

	/**
	 * @return false if no more lines could be read
	 */
	private static boolean readNextLines(HashMap<String, BufferedReader> httpReaders,
			HashMap<String, BufferedReader> httpsReaders) throws Exception {
		String line = null;
		HashSet<String> closeMe = new HashSet<>();
		for (String region : httpReaders.keySet()) {
			line = httpReaders.get(region).readLine();
			if (line == null) {
				closeMe.add(region);
				continue;
			}
			FileEntry fe = new FileEntry(line, region);
			if (!outQueues.containsKey(fe.api)) {
				outQueues.put(fe.api, new PriorityQueue<>());
				reverseLookup.put(outQueues.get(fe.api), fe.api);
				outTmpWriters.put(fe.api, new PrintWriter(tmpFolder + "/" + fe.api + ".csv"));
				outTmpWriters.get(fe.api).println(FileEntry.getCSVHeader());
			}
			outQueues.get(fe.api).add(fe);
			if (++linesRead % 50000 == 0)
				System.out.println("Processing line number " + linesRead);
		}
		for (String close : closeMe) {
			httpReaders.get(close).close();
			httpReaders.remove(close);
			System.out.println("Closed http reader for region file with alias " + close);
			// Thread.sleep(1000);
		}
		closeMe.clear();
		for (String region : httpsReaders.keySet()) {
			line = httpsReaders.get(region).readLine();
			if (line == null) {
				closeMe.add(region);
				continue;
			}
			FileEntry fe = new FileEntry(line, region);
			if (!outQueues.containsKey(fe.api)) {
				outQueues.put(fe.api, new PriorityQueue<>());
				reverseLookup.put(outQueues.get(fe.api), fe.api);
				outTmpWriters.put(fe.api, new PrintWriter(tmpFolder + "/" + fe.api + ".csv"));
				outTmpWriters.get(fe.api).println(FileEntry.getCSVHeader());
			}
			outQueues.get(fe.api).add(fe);

			if (++linesRead % 50000 == 0)
				System.out.println("Processing line number " + linesRead);

		}
		for (String close : closeMe) {
			httpsReaders.get(close).close();
			httpsReaders.remove(close);
			System.out.println("Closed https reader for region file with alias " + close);
			// Thread.sleep(1000);
		}
		updateMaxMinQueues();
		return !(httpReaders.isEmpty() && httpsReaders.isEmpty());
	}

	/**
	 * @param file
	 */
	private static BufferedReader createBufferedReader(File file) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(file));
		br.readLine();
		return br;
	}

	private static void writeNextIntermediateLine(String api) {
		if (!outQueues.get(api).isEmpty()) {
			outTmpWriters.get(api).println(outQueues.get(api).remove().toCSV());
		}

	}

	private static void writeNextIntermediateLines(String api, int numberOfLines) {
		for (int i = 0; i < numberOfLines; i++)
			writeNextIntermediateLine(api);
		updateMaxMinQueues();
	}

	private static void updateMaxMinQueues() {
		int minSize = Integer.MAX_VALUE, maxSize = 0;
		for (PriorityQueue<FileEntry> q : outQueues.values()) {
			if (q.size() < minSize) {
				currentMinQueue = q;
				minSize = q.size();
			}
			if (q.size() > maxSize) {
				currentMaxQueue = q;
				maxSize = q.size();
			}
		}
	}

}

class PerApiProcessor {

	static PrintWriter out;

	static {
		try {
			out = new PrintWriter(AvailabilityCorrelationAnalyser.tmpFolder
					+ "/aggregate_availability_results.csv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	static void runSecondStage() throws Exception {
		System.out.println("Processing second stage.");
		for (File f : new File(AvailabilityCorrelationAnalyser.tmpFolder).listFiles()) {
			if (!f.isFile())
				continue;
			System.out.println("Processing input file " + f);
			analyzeFile(f);
		}
		System.out.println("Done: Processing second stage.");
		out.close();
	}

	/**
	 * @param f
	 */
	private static void analyzeFile(File f) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		FileEntry entry;
		StatsCollector stats = new StatsCollector();
		ArrayList<FileEntry> buffer = new ArrayList<>();
		br.readLine();
		while ((line = br.readLine()) != null) {
			// add next entry to buffer
			entry = FileEntry.parseCSV(line, f.getName());
			buffer.add(entry);
			// remove surplus entries in buffer
			while (!buffer.isEmpty() && buffer.size() > AvailabilityCorrelationAnalyser.BUFFERSIZE) {
				buffer.remove(0);
			}
			// continue if buffer not full
			if (buffer.size() < AvailabilityCorrelationAnalyser.BUFFERSIZE)
				continue;
			// analyze first entry in buffer
			if (buffer.get(0).available) {
				// is available => no need for further checks
				continue;
			} else {
				analyzeBuffer(buffer, stats);
			}
		}
		// analyze remainder of buffer
		while (buffer.size() > 20) {
			if (!buffer.get(0).available)
				analyzeBuffer(buffer, stats);
			buffer.remove(0);
		}
		br.close();
		out.println("Results for API " + f.getName().substring(0, f.getName().indexOf(".csv")));
		out.println(stats.toCSV());
	}

	/**
	 * @param buffer
	 * @param stats
	 */
	private static void analyzeBuffer(ArrayList<FileEntry> buffer, StatsCollector stats) {
		String region = buffer.get(0).region;
		boolean encrypted = buffer.get(0).encrypted;
		boolean protocolChange = false, regionChange = false, foundOtherProtocol = false;
		for (int i = 1; i < buffer.size(); i++) {
			FileEntry fe = buffer.get(i);
			if (protocolChange && regionChange) {
				// already found results
				break;
			}
			if ((fe.encrypted == encrypted) && fe.region.equals(region)) {
				// found the next entry with same region and protocol
				break;
			}
			if ((fe.encrypted != encrypted) && fe.region.equals(region))
				foundOtherProtocol = true;
			if (fe.available && (fe.encrypted != encrypted) && fe.region.equals(region)) {
				// protocol change would have worked
				protocolChange = true;
			}
			if (fe.available && !fe.region.equals(region)) {
				// region change would have worked
				regionChange = true;
			}
		}
		if (!foundOtherProtocol) {
			stats.increment(ResultEntry.NO_DATA_FOR_PROTOCOL_CHANGE);
		} else if (protocolChange) {
			// protocol change would have worked
			if (encrypted) {
				// was originally https
				stats.increment(ResultEntry.HTTPS2HTTP_SUCCESS);
			} else {
				// was originally http
				stats.increment(ResultEntry.HTTP2HTTPS_SUCCESS);
			}
		} else {
			// protocol change would not have worked
			if (encrypted) {
				// was originally https
				stats.increment(ResultEntry.HTTPS2HTTP_FAIL);
			} else {
				// was originally http
				stats.increment(ResultEntry.HTTP2HTTPS_FAIL);
			}
		}
		if (regionChange) {
			// region change would have worked
			stats.increment(ResultEntry.REGION_CHANGE_SUCCESS);
		} else
			stats.increment(ResultEntry.REGION_CHANGE_FAIL);

	}

	private static class StatsCollector {
		Map<ResultEntry, Integer> resultCounter = new TreeMap<>();

		public StatsCollector() {
			for (ResultEntry re : ResultEntry.values())
				resultCounter.put(re, 0);
		}

		public String toCSV() {
			StringBuilder sb = new StringBuilder();
			for (ResultEntry entry : resultCounter.keySet()) {
				sb.append(entry + ";" + resultCounter.get(entry) + "\n");
			}
			return sb.toString().trim();
		}

		public void increment(ResultEntry re) {
			resultCounter.put(re, resultCounter.get(re) + 1);
//			System.out.println("Incrementing " + re + " to " + resultCounter.get(re));
		}
	}

	private enum ResultEntry {
		HTTP2HTTPS_FAIL, HTTP2HTTPS_SUCCESS, HTTPS2HTTP_FAIL, HTTPS2HTTP_SUCCESS, REGION_CHANGE_FAIL, REGION_CHANGE_SUCCESS, NO_DATA_FOR_PROTOCOL_CHANGE;
	}

}

class FileEntry implements Comparable<FileEntry> {
	Long timestamp;
	String api;
	boolean encrypted;
	String region;
	boolean available;

	public FileEntry(String line, String region) {
		String[] splits = line.split(";");
		timestamp = Long.parseLong(splits[1]);
		String apiTmp = splits[0].substring(splits[0].indexOf("//"));// .replaceAll("[^A-Za-z0-9]",
																		// "")
		// .substring(0, 20);
		if (AvailabilityCorrelationAnalyser.apiAlias.containsKey(apiTmp)) {
			api = AvailabilityCorrelationAnalyser.apiAlias.get(apiTmp);
		} else {
			System.out.println("Please, enter alias name for API " + apiTmp);
			api = AvailabilityCorrelationAnalyser.scan.next();
			AvailabilityCorrelationAnalyser.apiAlias.put(apiTmp, api);
		}

		available = splits[2].startsWith("2") || splits[2].startsWith("3");
		encrypted = splits[0].startsWith("https");
		this.region = region;
	}

	private FileEntry() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(FileEntry o) {
		return -1 * o.timestamp.compareTo(this.timestamp);
	}

	public String toCSV() {
		return timestamp + ";" + encrypted + ";" + region + ";" + available + ";";
	}

	public static String getCSVHeader() {
		return "timestamp;encrypted_call;region;available;";
	}

	public static FileEntry parseCSV(String line, String api) {
		String[] splits = line.split(";");
		FileEntry res = new FileEntry();
		res.timestamp = Long.parseLong(splits[0]);
		res.encrypted = Boolean.parseBoolean(splits[1]);
		res.region = splits[2];
		res.available = Boolean.parseBoolean(splits[3]);
		res.api = api.substring(0, api.indexOf(".csv"));
		return res;
	}
}
