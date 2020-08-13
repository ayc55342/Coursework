import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes an authorization log file to detect possible intrusion
 *
 */
public class IntrusionDetector {
	//One minute in milliseconds
	public static long TIME_THRESHOLD = 60000; 
	
	//Maximum allowed attempts per user within time threshold
	public static int ATTEMPTS_PER_USER_THRESHOLD = 3;
	
	//Maximum allowed concurrent attempts within time threshold
	public static int CONCURRENT_ATTEMPT_THRESHOLD = 5; 
	
	//The date format used in log files
	public static SimpleDateFormat logDateFormat = new SimpleDateFormat("MMM d HH:mm:ss");
	
	//The pattern of a log line documenting a failed SSH attempt
	public static Pattern failedSSHPattern = Pattern.compile(
			"^(.+) vagrant sshd\\[[0-9]+\\]: Failed password for (.+) from (.+) port .+");
	
	//The pattern used for the process name
	public static Pattern processNamePattern = Pattern.compile("([0-9]+)@.+");
	
	/**
	 * Reads in the log file specified as the first argument and analyzes it to detect intrusion
	 * then outputs a message to append to syslog
	 * @param args The path to the log file to analyze
	 */
	public static void main(String[] args) {
		// Make sure an argument was given
		if (args.length == 0) {
			System.out.println("Usage: analyze /path/to/log/file");
		}
		else {
			try {
				//Initialize reader and data structures
				BufferedReader r = new BufferedReader(new FileReader(args[0]));
				HashMap<String, ArrayList<Entry<String,Date>>> attempts = new HashMap<>();
				ArrayList<Entry<String,Date>> allAttempts = new ArrayList<>();
				
				//Parse log file
				String line = r.readLine();
				while (line != null) {
					try {
						Matcher m = failedSSHPattern.matcher(line);
						if(m.find()) {
							Date date = logDateFormat.parse(m.group(1));
							String user = m.group(2);
							String source = m.group(3);
							allAttempts.add(new AbstractMap.SimpleEntry<>(user, date));
							ArrayList<Entry<String,Date>> userLog = attempts.get(source);
							if (userLog == null) {
								userLog = new ArrayList<>();
								userLog.add(new AbstractMap.SimpleEntry<>(user, date));
								attempts.put(source, userLog);
							}
							else {
								userLog.add(new AbstractMap.SimpleEntry<>(user, date));
							}
						}
					} catch (ParseException e) {}
					line = r.readLine();
				}
				r.close();
				
				//Check whether a user makes too many attempts within the time threshold
				for(Entry<String,ArrayList<Entry<String,Date>>> entry : attempts.entrySet()) {
					checkIfMoreThanCountThresholdWithinTimeThreshold(entry.getValue(), 
																	ATTEMPTS_PER_USER_THRESHOLD);
				}
				
				//Check whether too many users attempt to ssh concurrently
				checkIfMoreThanCountThresholdWithinTimeThreshold(allAttempts, CONCURRENT_ATTEMPT_THRESHOLD);
				
				//No problems found
				outputResult(false,0);
				
			} catch (FileNotFoundException e) {
				System.err.printf("Could not find file \"%s\"", args[0]);
			} catch (IOException e) {
				System.err.println("Error reading file");
			}
		}
	}

	/**
	 * Checks whether there are more than countThreshold values within TIME_THRESHOLD
	 * in ordered list timestamps
	 * @param timestamps An ordered list of timestamps
	 * @param countThreshold The maximum number of values within TIME_THREHOLD
	 * @return Whether the condition is satisfied
	 */
	private static void checkIfMoreThanCountThresholdWithinTimeThreshold
											(List<Entry<String,Date>> timestamps, int countThreshold) {
		Set<String> affected = new HashSet<>();
		int count = 1;
		for(int i = 0; i < timestamps.size(); i++) {
			long intervalStart = timestamps.get(i).getValue().getTime();
			count = 1;
			affected.clear();
			affected.add(timestamps.get(i).getKey());
			while(i + count < timestamps.size() && 
					timestamps.get(i+count).getValue().getTime() - intervalStart <= TIME_THRESHOLD) {
				affected.add(timestamps.get(i+count).getKey());
				count++;
			}
			if (count > countThreshold) {
				outputResult(true,affected.size());
			}
		}
	}
	
	/**
	 * Outputs a line suitable to append to syslog
	 * @param intrusion true if an intrusion was detected
	 * @param affected If intrusion is true then the number of users affected
	 */
	private static void outputResult(boolean intrusion, int affected) {
		StringBuilder sb = new StringBuilder();
		Date now = new Date();
		sb.append(logDateFormat.format(now));
		sb.append(" vagrant analyze");
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		Matcher m = processNamePattern.matcher(processName);
		if(m.find()) {
			sb.append("[");
			sb.append(m.group(1));
			sb.append("]");
		}
		sb.append(": ");
		if (intrusion) {
			sb.append("Analyzer detected INTRUSION. Approximately ");
			sb.append(affected);
			sb.append((affected == 1?" user was ":" users were ") + "affected");
		}
		else {
			sb.append("Analyzed log is OK");
		}
		System.out.println(sb.toString());
		System.exit(0);
	}

}
