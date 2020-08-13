import java.io.*;
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
		
		// Strip out substrings, do dictionary check:
		String strippedPass = "";
		
		strippedPass = pass.replaceAll("[^\\p{L}\\p{Nd}]+", "");
		strippedPass = strippedPass.replaceAll("[\\p{Digit}]+", ""); // Doesn't remove arabic numbers
		String lowercaseStrippedPass = strippedPass.toLowerCase();
		
		String capitalizedStrippedPass = capitalize(strippedPass);
		
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
		
		//Load unix dictionary as wordlist
		WordList w = new WordList();
		try {
			w.loadFromFile("/usr/share/dict/words/");
		} catch (FileNotFoundException e) {
			System.out.println("Unix dictionary not found.");
			System.exit(0);
		}
		
		Scanner sc = new Scanner(System.in);

		String password = sc.nextLine();
		sc.close();
		
		//Run comprehensive8 and basic16 on string from standard input.
		boolean passesBasic16 = basic16(password);
		boolean passesComprehensive8 = comprehensive8(password, w);
		System.out.println((passesComprehensive8 || passesBasic16) ? "strong" : "weak");
	}
}
