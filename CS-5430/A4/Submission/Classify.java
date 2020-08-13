import java.io.FileNotFoundException;
import java.util.*;

/**
 * The main program driver for the classifier application
 *
 */
public class Classify {

	/**
	 * Runs a comprehensive8 check (see Kelly et al) on pass using the input dictionary
	 * That is this function:
	 *  - Checks that the string contains a lowercase letter
	 *  - Checks that the string contains an uppercase letter
	 *  - Checks that the string contains a number
	 *  - Checks that the string contains a symbol/special character
	 *  - Checks that the string is not a dictionary word
	 *  - Checks that the string with all the non-alphabetic characters stripped out is not a
	 *    dictionary word
	 *  - Checks that no sequential alphabetic substring of the string (of length > 1),
	 *    split on non-alphabetic characters, is a dictionary word
	 * If the password passes all the above checks the function returns true
	 * Otherwise it returns false
	 * @param pass The password to evaluate
	 * @param dictionary The dictionary to run checks against
	 * @return Either true or false according to the above procedure
	 */
	public static boolean comprehensive8(String pass, WordList dictionary) {
		int len = pass.length();
		Character cha;
		String chastring;
		boolean UppercaseFound = false;
		boolean LowercaseFound = false;
		boolean DigitFound = false;
		boolean SymbolFound = false;
		
		// Check if password is at least 8 characters
		if (len < 8) {
			return false;
		}
		
		// First pass: 
		
		// Check if password contains uppercase letter, lowercase letter, digit, symbol
		for (int i = 0; i < len; i++) {
			cha = pass.charAt(i);
			if (!UppercaseFound) {
				UppercaseFound = Character.isUpperCase(cha);
			}
			if (!LowercaseFound) {
				LowercaseFound = Character.isLowerCase(cha);
			}
			if (!DigitFound) {
				DigitFound = Character.isDigit(cha);
			}
			if (!SymbolFound) {
				chastring = cha.toString();
				SymbolFound = chastring.matches("[^\\p{L}\\p{Nd}]+"); // Check for symbol with regex
			}
		}	
		if ((!UppercaseFound) || (!LowercaseFound) || (!DigitFound) || (!SymbolFound)) {
			return false;
		}
		
		// Doing a check on the entire number/symbol-stripped password is 
		// inconsistent with the current methodology.
		// Something like W45o56r##d would --> Word when stripped, thus would detect as weak:
		// But the password itself really doesn't contain a dictionary word.
		/*
		// Strip out substrings, do dictionary check:
		String strippedPass = "";
		
		
		// For reference: these don't handle non-English characters correctly
		//strippedPass = pass.replaceAll("[^\\p{Alpha}]+", "");
		//strippedPass = pass.replaceAll("[^\\p{Lower}\\p{Upper}]+", "");
		
		
		strippedPass = pass.replaceAll("[^\\p{L}\\p{Nd}]+", "");
		strippedPass = strippedPass.replaceAll("[\\p{Digit}]+", ""); // Doesn't remove arabic numbers
		String lowercaseStrippedPass = strippedPass.toLowerCase();
		
		String capitalizedStrippedPass = capitalize(strippedPass);
		*/
		
		// Checks whether any sequential alphabetic substring of password, 
		// with length > 1, is itself a word (e.g. Key#54pass)
		// Checks each substring that is separated by non-alphabetic characters, 
		// rather than checking password stripped of non-alphabetic characters.
		// (Otherwise something like A!345bcdfgh, when stripped to Abcdfgh, 
		// would ping as containing the word "Ab".) 
		
		String[] substrings = pass.split("[^\\p{Alpha}]"); 
		// ^ Only works for English letters, but this is what the Unix dictionary has anyway
	    List<String> substringsList = new ArrayList<String>();
	    for(String s : substrings) {
	       if(s != null && s.length() > 0) {
	    	   substringsList.add(s);	// takes care of removing "" strings post-split
	       }
	    }
	    substrings = substringsList.toArray(new String[substringsList.size()]);
		
		boolean lowercaseSubstringIsWord = false;
		boolean capitalizedSubstringIsWord = false;
		String subs = "";
		String lowercaseSubs = "";
		String capitalizedSubs = "";
		for (int k = 0; k < substrings.length; k++) {
			if (substrings[k].length() > 1) {
			    for (int i = 2; i < substrings[k].length(); i++) {
			        for (int j = 0; j+i <= substrings[k].length(); j++) {
					  subs = substrings[k].substring(j, j+i); 
					  lowercaseSubs = subs.toLowerCase();
					  lowercaseSubstringIsWord = dictionary.contains(lowercaseSubs);
					  capitalizedSubs = capitalize(subs);
					  capitalizedSubstringIsWord = dictionary.contains(capitalizedSubs);
					  
					  // No words in Unix dictionary that are neither capitalized nor lowercase
					  if (lowercaseSubstringIsWord || capitalizedSubstringIsWord) {
						  return false;
					  }
			        }
			    }
			}
		}
		
		// These shouldn't be needed with the substring checking: rather, it is inconsistent
		// to check them with the current methodology.
		/*
		boolean strippedIsWord = dictionary.contains(strippedPass);
		if (strippedIsWord) {
			return false;
		}
		
		boolean lowercaseStrippedPassIsWord = dictionary.contains(lowercaseStrippedPass);
		if (lowercaseStrippedPassIsWord) {
			return false;
		}
		
		boolean capitalizedStrippedPassIsWord = dictionary.contains(capitalizedStrippedPass);
		if (capitalizedStrippedPassIsWord) {
			return false;
		}
		*/
		
		// Also should never happen with Unix dictionary: no numbers in words,
		// so if it gets this far it's not going to be a word
		/*
		boolean wholeIsWord = dictionary.contains(pass);
		if (wholeIsWord) {
			return false;
		}
		*/
		
		return true;
	}
	
	/**
	 * Runs a basic16 check (see Kelley et al)
	 * @param pass The password to check
	 * @return true if pass passes basic16, false otherwise
	 */
	public static boolean basic16(String pass) {
		return pass.length() >= 16;
	}
	
	/**
	 * Capitalizes an input string (makes lowercase with first letter capitalized)
	 * @param s The string to capitalize
	 * @return cap Capitalized version of s
	 */
	public static String capitalize(String s) {
		s.toLowerCase();
		String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
		return cap;
	}
	
	/**
	 * Main driver method
	 */
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Usage: classify <password>");
			System.exit(0);
		}
		
		//long starttime = System.currentTimeMillis();
		
		//Load unix dictionary as wordlist
		WordList w = new WordList();
		try {
			//long loadStart = System.currentTimeMillis();
			w.loadFromFile("/usr/share/dict/words/");
			//long loadEnd = System.currentTimeMillis();
			//System.out.format("Finished Loading File. Load time: %d\n", loadEnd - loadStart);
		} catch (FileNotFoundException e) {
			System.out.println("Unix dictionary not found.");
			System.exit(0);
		}
		
		//Run comprehensive8 and basic16 on first command line arg. Returns strong if either passes
		// Runs basic16 first, to prevent passwords already designated strong 
		// from being checked again, and potentially requiring 
		// time-intensive processing of substrings in comprehensive8.
		boolean passesBasic16 = basic16(args[0]);
		boolean passesComprehensive8 = false;
		if (passesBasic16) {
			System.out.println("strong");
		}
		else {
			passesComprehensive8 = comprehensive8(args[0], w);
			System.out.println(passesComprehensive8 ? "strong" : "weak");
			//System.out.println("Time elapsed: " + (System.currentTimeMillis() - starttime));
		}
	}
}
