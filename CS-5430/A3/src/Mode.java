/**
 * Enum specifying the possible modes of operation for demo system
 */
public enum Mode {
	PLAINTEXT,
	ENCRYPTION,
	MAC,
	MAC_ENCRYPTION;
	
	public static Mode fromString(String s) {
		switch(s.toUpperCase()) {
		case "ENCRYPTION":
		case "ENC":
			return ENCRYPTION;
		case "MAC": 
			return MAC;
		case "MACENCRYPTION":
		case "MACENC":
		case "MAC_ENCRYPTION": 
		case "MAC_ENC":
		case "ENCRYPTION THEN MAC":
			return MAC_ENCRYPTION;
		default: 
			return PLAINTEXT;
		}
	}
	
	public String toString() {
		switch(this) {
		case MAC:
			return "MAC";
		case ENCRYPTION:
			return "encryption";
		case MAC_ENCRYPTION:
			return "encryption then MAC";
		default:
			return "plaintext";
		}
	}
}