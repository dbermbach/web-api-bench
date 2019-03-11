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
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author Dave
 */
public class BasicAnalyser {

	/**
	 * is cleared before the next file is analysed mapping: api => inner map; day => result set
	 */
	private static TreeMap<String, TreeMap<Calendar, ResultEntryPerDay>> dailyResultsPerAPIPerRegion = new TreeMap<>();

	/** mapping: api => inner map; day => result set */
	private static TreeMap<String, TreeMap<Calendar, ResultEntryPerDay>> dailyResultsPerAPI = new TreeMap<>();

	/** mapping: api => inner map; region => result set */
	private static TreeMap<String, TreeMap<String, ResultEntryFullTest>> resultsPerAPIAggregated = new TreeMap<>();

	/** mapping: api => inner map; region => series of latency values */
	private static TreeMap<String, TreeMap<String, ArrayList<Integer>>> latencies = new TreeMap<>();

	private static PrintWriter resOut;
	private static PrintWriter allRegionDailyOut;
	private static PrintWriter latsOut;
	private static PrintWriter latsSummary;
	private static String resOutFileName = "ping2018";
	private static String inFolder = "C:\\temp\\ping_2018";

	static {
		try {
			resOut = new PrintWriter(resOutFileName + "_availability_report.txt");
			allRegionDailyOut = new PrintWriter(resOutFileName + "daily_availability.csv");
			latsOut = new PrintWriter(resOutFileName + "_daily latency_report.txt");
			latsSummary = new PrintWriter(resOutFileName + "_latency_summary_report.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	static int exceptionLength;
	static int okayLength;
	static boolean analysingPing = false;
	static int pingPackageNumber = 5;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File f = new File(inFolder);
		for (File file : f.listFiles()) {
			analyseFile(file);
		}
		printAggregates();
		resOut.close();
		exportDailyAvailabilities();
		allRegionDailyOut.close();
		System.out.println("Exporting latencies...");
		exportLatencies();
		exportLatencySummary();
		latsOut.close();
		latsSummary.close();
		System.out.println("DONE.");
	}

	/**
	 * 
	 */
	private static void exportDailyAvailabilities() {
		for (String api : dailyResultsPerAPI.keySet()) {
			allRegionDailyOut.println(api);
			allRegionDailyOut.println(ResultEntryPerDay.getAvailabilityAsCSVHeader());
			for (ResultEntryPerDay repd : dailyResultsPerAPI.get(api).values())
				allRegionDailyOut.println(repd.getAvailabilityAsCSV());
		}

	}

	/**
	 * 
	 */
	private static void exportLatencySummary() {
		double mean, stddev;
		int max, min, counter;
		latsSummary.println("Aggregated latency values");
		for (String api : latencies.keySet()) {
			latsSummary.println("\nResults for API " + api);
			for (String region : latencies.get(api).keySet()) {
				counter = 0;
				mean = 0;
				stddev = 0;
				max = 0;
				min = Integer.MAX_VALUE;
				for (int val : latencies.get(api).get(region)) {
					if (val < 0)
						continue;
					mean += val;
					counter++;
					if (val < min)
						min = val;
					if (val > max)
						max = val;
				}
				mean /= counter;
				for (int val : latencies.get(api).get(region)) {
					if (val < 0)
						continue;
					stddev += (val - mean) * (val - mean);
				}
				stddev /= (counter - 1);
				stddev = Math.sqrt(stddev);
				latsSummary.println(region + ": mean=" + mean + ", min=" + min + ", max=" + max
						+ ", stddev=" + stddev + ", values=" + counter);
			}
		}
	}

	/**
	 * 
	 */
	private static void exportLatencies() throws Exception {
		PrintWriter pw = new PrintWriter(resOutFileName + "_latencies.csv");
		StringBuilder line = new StringBuilder(
				"Latency values by API and region - error requests are denoted as -100;\n");
		int maxNumberOfValues = 0;
		for (String api : latencies.keySet()) {
			line.append(api);
			for (int i = 0; i < latencies.get(api).size(); i++)
				line.append(';');
		}
		line.append('\n');
		for (String api : latencies.keySet()) {
			for (String region : latencies.get(api).keySet()) {
				line.append(region + ";");
				int size = latencies.get(api).get(region).size();
				if (size > maxNumberOfValues)
					maxNumberOfValues = size;
			}
		}
		pw.println(line);
		// now build csv lines
		for (int i = 0; i < maxNumberOfValues; i++) {
			line = new StringBuilder();
			for (String api : latencies.keySet()) {
				for (String region : latencies.get(api).keySet()) {
					ArrayList<Integer> list = latencies.get(api).get(region);
					if (i < list.size() - 1)
						line.append(list.get(i));
					line.append(';');
				}
			}
			pw.println(line);
		}
		pw.close();
	}

	/**
	 * 
	 */
	private static void printAggregates() {
		for (String api : resultsPerAPIAggregated.keySet()) {
			doublePrint("Aggregated availability dailyResultsPerAPIPerRegion for API " + api);
			int avail = 0, unavail = 0, code4 = 0, code5 = 0, other = 0;
			for (Entry<String, ResultEntryFullTest> entry : resultsPerAPIAggregated.get(api)
					.entrySet()) {
				avail += entry.getValue().available;
				unavail += entry.getValue().unavailable;
				code4 += entry.getValue().code400;
				code5 += entry.getValue().code500;
				other += entry.getValue().otherNonAvail;
				doublePrint("Availability in region " + entry.getKey() + " was "
						+ entry.getValue().overallAvail + "% --> (avail="
						+ entry.getValue().available + ", unavail=" + entry.getValue().unavailable
						+ ", 4xx=" + entry.getValue().code400 + ", 5xx=" + entry.getValue().code500
						+ ", otherNonavail=" + entry.getValue().otherNonAvail + ")");
			}
			doublePrint("Total availability was " + ((int) (avail * 100 / (avail + unavail)))
					+ "% --> (avail=" + avail + ", unavail=" + unavail + ", 4xx=" + code4
					+ ", 5xx=" + code5 + ", otherNonavail=" + other + ")");

		}

	}

	private static void analyseFile(File file) throws Exception {
		dailyResultsPerAPIPerRegion.clear();
		boolean analyseHttp = false;
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = br.readLine();
		if (line == null) {
			System.out.println("File " + file + " was empty.");
			br.close();
			return;
		}
		if (file.getName().startsWith("https_")) {
			analyseHttp = false;
		} else if (file.getName().startsWith("http_")) {
			analyseHttp = true;
		} else if (file.getName().startsWith("ping_")) {
			analysingPing = true;
		} else {
			System.out.println("File " + file + " was not in the expected format.");
			br.close();
			return;
		}

		if (analyseHttp) {
			exceptionLength = 4;
			okayLength = 5;
		} else {
			exceptionLength = 4;
			okayLength = 6;
		}

		while ((line = br.readLine()) != null)
			analyseLine(line, file.getName());
		br.close();

		doublePrint("Results for file " + file.getName() + ":");
		latsOut.println("Results from region " + file.getName());
		for (String api : dailyResultsPerAPIPerRegion.keySet()) {
			doublePrint("Got entries for " + dailyResultsPerAPIPerRegion.get(api).size()
					+ " days in API " + api);
		}
		for (String api : dailyResultsPerAPIPerRegion.keySet()) {
			doublePrint("Results for API " + api);
			latsOut.println("Average daily latency for API " + api);
			for (ResultEntryPerDay re : dailyResultsPerAPIPerRegion.get(api).values()) {
				String print = re.toString();
				latsOut.println(re.getDailyLatencyString());
				if (print != null) {
					doublePrint(print);
				}
			}
		}

	}

	private static void analyseLine(String line, String filename) {
		String[] splits = line.split(";");
		ResultEntryFullTest reft = getOrCreateFullTimeResultSet(splits[0], filename);
		ArrayList<Integer> latencies = getOrCreateLatencyArrayList(splits[0], filename);
		// process ping results and return at end of if
		if (analysingPing) {
			try {

				String api = splits[0];
				long start = Long.parseLong(splits[1]);
				// long end = Long.parseLong(splits[2]);
				double pingability;
				if (splits[2].equals("exception"))
					pingability = 0;
				else
					pingability = Double.parseDouble(splits[3]);
//				double avglatency = Double.parseDouble(splits[4]);

				ResultEntryPerDay dayEntry = getOrCreatePerDayResultSet(api, filename, start,
						dailyResultsPerAPIPerRegion);
				ResultEntryPerDay allRegionDayEntry = getOrCreatePerDayResultSet(api, filename,
						start, dailyResultsPerAPI);

				int avails = (int) Math.round(pingability * pingPackageNumber);
				int unavails = pingPackageNumber - avails;
				for (int i = 0; i < avails; i++) {
					dayEntry.addAvailable();
					allRegionDayEntry.addAvailable();
					reft.addAvailable();
				}
				for (int i = 0; i < unavails; i++) {
					dayEntry.addOtherNonavail();
					reft.addOtherNonavail();
					allRegionDayEntry.addOtherNonavail();
				}

//				latencies.add((int) Math.round(avglatency));
//				dayEntry.addLatency((int) Math.round(avglatency));

			} catch (Exception e) {
				System.out.println("Could not process line: " + line);
				e.printStackTrace();
			}
			return; // don't continue to http/https analysis - terminate method here
		}
		// process http/https results

		if (splits.length == exceptionLength) {
			// error case
			ResultEntryPerDay dayEntry = getOrCreatePerDayResultSet(splits[0], filename,
					Long.parseLong(splits[1]), dailyResultsPerAPIPerRegion);
			ResultEntryPerDay allRegionDayEntry = getOrCreatePerDayResultSet(splits[0], filename,
					Long.parseLong(splits[1]), dailyResultsPerAPI);
			String exceptionMsg = splits[splits.length - 1].trim();
			if (exceptionMsg.startsWith("Server returned HTTP response code:")) {
				exceptionMsg = exceptionMsg.replaceAll("Server returned HTTP response code: ", "")
						.substring(0, 3);
				try {
					int code = Integer.parseInt(exceptionMsg);
					if (code < 500) {
						dayEntry.addCode400();
						allRegionDayEntry.addCode400();
						reft.addCode400();
					} else {
						dayEntry.addCode500();
						allRegionDayEntry.addCode500();
						reft.addCode500();
					}
				} catch (Exception e) {
					System.out.println("Could not parse response code from "
							+ splits[splits.length - 1]);
					dayEntry.addOtherNonavail();
					reft.addOtherNonavail();
					allRegionDayEntry.addOtherNonavail();
				}
			} else {
				dayEntry.addOtherNonavail();
				allRegionDayEntry.addOtherNonavail();
				reft.addOtherNonavail();
			}
			latencies.add(-100);

		} else if (splits.length == okayLength) {
			// okay case
			ResultEntryPerDay dayEntry = getOrCreatePerDayResultSet(splits[0], filename,
					Long.parseLong(splits[1]), dailyResultsPerAPIPerRegion);
			ResultEntryPerDay allRegionDayEntry = getOrCreatePerDayResultSet(splits[0], filename,
					Long.parseLong(splits[1]), dailyResultsPerAPI);
			dayEntry.addAvailable();
			allRegionDayEntry.addAvailable();
			reft.addAvailable();
			latencies.add(Integer.parseInt(splits[3]));
			dayEntry.addLatency(Integer.parseInt(splits[3]));
		} else {
			System.out.println("Line contained the wrong number of entries:\n" + line);
		}

	}

	static void doublePrint(String res) {
		resOut.println(res);
		System.out.println(res);
	}

	private static ResultEntryPerDay getOrCreatePerDayResultSet(String api, String region,
			long timestamp, TreeMap<String, TreeMap<Calendar, ResultEntryPerDay>> outerMap) {
		TreeMap<Calendar, ResultEntryPerDay> map = outerMap.get(api);
		if (map == null) {
			map = new TreeMap<>();
			outerMap.put(api, map);
		}
		ResultEntryPerDay dayEntry = null;
		for (ResultEntryPerDay re : map.values()) {
			if (re.isOnThisDay(timestamp)) {
				dayEntry = re;
				break;
			}
		}
		if (dayEntry == null) {
			dayEntry = new ResultEntryPerDay(timestamp);
			map.put(dayEntry.start, dayEntry);
		}
		return dayEntry;
	}

	private static ArrayList<Integer> getOrCreateLatencyArrayList(String api, String region) {
		TreeMap<String, ArrayList<Integer>> t = latencies.get(api);
		if (t == null) {
			t = new TreeMap<>();
			latencies.put(api, t);
		}
		ArrayList<Integer> list = t.get(region);
		if (list == null) {
			list = new ArrayList<>();
			t.put(region, list);
		}
		return list;
	}

	private static ResultEntryFullTest getOrCreateFullTimeResultSet(String api, String region) {
		TreeMap<String, ResultEntryFullTest> entriesByRegion = resultsPerAPIAggregated.get(api);
		if (entriesByRegion == null) {
			entriesByRegion = new TreeMap<>();
			resultsPerAPIAggregated.put(api, entriesByRegion);
		}
		ResultEntryFullTest reft = entriesByRegion.get(region);
		if (reft == null) {
			reft = new ResultEntryFullTest();
			entriesByRegion.put(region, reft);
		}
		return reft;
	}

}

class ResultEntryPerDay {

	/** if false only non-100% availabilities will be returned by toString() */
	static boolean detailedPrinting = false;

	public ResultEntryPerDay(long timestamp) {
		start = Calendar.getInstance();
		end = Calendar.getInstance();
		start.setTimeInMillis(timestamp);
		start.set(Calendar.HOUR_OF_DAY, 0);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		end.setTimeInMillis(start.getTimeInMillis());
		end.set(Calendar.DAY_OF_MONTH, start.get(Calendar.DAY_OF_MONTH) + 1);
	}

	int available = 0;
	int unavailable = 0;
	int code400 = 0;
	int code500 = 0;
	int otherNonAvail = 0;
	int overallAvail = 0;
	List<Integer> latencies = new ArrayList<>();
	Calendar start;
	Calendar end;

	public String toString() {
		if (detailedPrinting || overallAvail < 100)
			return "Availability on " + Util.asMonthDay(start) + " was " + overallAvail
					+ "% --> (avail=" + available + ", unavail=" + unavailable + ", 4xx=" + code400
					+ ", 5xx=" + code500 + ", otherNonavail=" + otherNonAvail + ")";
		else
			return null;
	}

	public String getAvailabilityAsCSV() {
		return Util.asMonthDay(start) + ";" + available + ";" + unavailable + ";" + code400 + ";"
				+ code500 + ";" + otherNonAvail;
	}

	public static String getAvailabilityAsCSVHeader() {
		return "day;available;unavailable;code_4XX;code_5XX;other_unavailable";
	}

	private void updateAvail() {
		overallAvail = ((int) (available * 10000.0 / (available + unavailable)) / 100);
	}

	public boolean isOnThisDay(long timestamp) {
		return start.getTimeInMillis() <= timestamp && end.getTimeInMillis() > timestamp;
	}

	public void addAvailable() {
		available++;
		updateAvail();
	}

	private void incrUnavailable() {
		unavailable++;
		updateAvail();
	}

	public void addCode400() {
		code400++;
		incrUnavailable();
	}

	public void addCode500() {
		code500++;
		incrUnavailable();
	}

	public void addOtherNonavail() {
		otherNonAvail++;
		incrUnavailable();
	}

	public void addLatency(int latency) {
		latencies.add(latency);
	}

	public String getDailyLatencyString() {
		double d = 0;
		for (int i : latencies)
			d += i;
		d /= latencies.size();
		return Util.asMonthDay(start) + ";" + (int) Math.round(d);
	}

}

class ResultEntryFullTest {
	int available = 0;
	int unavailable = 0;
	int code400 = 0;
	int code500 = 0;
	int otherNonAvail = 0;
	int overallAvail = 0;

	private void updateAvail() {
		overallAvail = ((int) (available * 10000.0 / (available + unavailable)) / 100);
	}

	public void addAvailable() {
		available++;
		updateAvail();
	}

	private void incrUnavailable() {
		unavailable++;
		updateAvail();
	}

	public void addCode400() {
		code400++;
		incrUnavailable();
	}

	public void addCode500() {
		code500++;
		incrUnavailable();
	}

	public void addOtherNonavail() {
		otherNonAvail++;
		incrUnavailable();
	}
}

class Util {

	public static String asMonthDay(String timestamp) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(Long.parseLong(timestamp));
		return asMonthDay(c);
	}

	public static String asMonthDay(Calendar c) {
		String res = null;
		switch (c.get(Calendar.MONTH)) {
		case Calendar.JANUARY:
			res = "Jan";
			break;
		case Calendar.FEBRUARY:
			res = "Feb";
			break;
		case Calendar.MARCH:
			res = "Mar";
			break;
		case Calendar.APRIL:
			res = "Apr";
			break;
		case Calendar.MAY:
			res = "May";
			break;
		case Calendar.JUNE:
			res = "Jun";
			break;
		case Calendar.JULY:
			res = "Jul";
			break;
		case Calendar.AUGUST:
			res = "Aug";
			break;
		case Calendar.SEPTEMBER:
			res = "Sep";
			break;
		case Calendar.OCTOBER:
			res = "Oct";
			break;
		case Calendar.NOVEMBER:
			res = "Nov";
			break;
		case Calendar.DECEMBER:
			res = "Dec";
			break;
		}
		return res + " " + (c.get(Calendar.DAY_OF_MONTH));
	}
}