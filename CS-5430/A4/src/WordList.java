import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A class representing a large wordlist which can be queried
 * Has utility methods for reading in its contents from a file
 * @author reubenrappaport
 *
 */
public class WordList {
	//The set of words in the list
	Set<String> wordlist;
	
	// We initialize the set with an initial capacity equal to the expected size / load factor 
	// in order to avoid having to expand it
	// private static final int EXPECTED_SIZE = (int)5.1e6; // for OpenWall wordlist
	// private static final int EXPECTED_SIZE = 236000; // for Unix dictionary
	private static final int EXPECTED_SIZE = 314700; // 236000 / 0.75 load factor
	
	//The load factor with which we initialize the set
	private static final float LOAD_FACTOR = 0.75f;
	
	/**
	 * Constructor. Initialized the hash set using the parameters defined above
	 */
	public WordList() {
		wordlist = new HashSet<String>(EXPECTED_SIZE, LOAD_FACTOR);
	}
	
	/**
	 * Loads the wordlist from a file
	 * @param filename The file from which to load
	 * @throws FileNotFoundException If the file does not exist
	 */
	public void loadFromFile(String filename) throws FileNotFoundException {
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String s;
		try {
			s = reader.readLine();
			while(s != null) {
				if (s.length() > 0 && s.charAt(0) != '#') { //Check if the line is a comment
					wordlist.add(s);
				}
				s = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns true if the wordlist contains s false otherwise
	 * @param s The string to check if the wordlist contains
	 * @return True if the wordlist contains s false otherwise
	 */
	public boolean contains(String s) {
		return wordlist.contains(s);
	}
}
