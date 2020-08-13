import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Contains common methods for writing keys to files and reading them back
 */
public class KeyIO {
	/**
	 * Writes a key to a file
	 * @param key The key to write
	 * @param file The file to which to write
	 */
	public static void writeKeyToFile(Key key, File file) throws IOException {
		FileOutputStream stream = new FileOutputStream(file);
		stream.write(key.getEncoded());
		stream.close();
	}
	
	/**
	 * Reads a key from a file
	 * @param file The file from which to read
	 * @param type The type of key
	 * @return The read key
	 */
	public static Key readKeyFromFile(File file, Type type) throws IOException {
		DataInputStream stream = new DataInputStream(new FileInputStream(file));
		byte[] bytes = new byte[(int)file.length()];
		stream.readFully(bytes);
		stream.close();
		try {
			switch(type) {
			case PUBLIC_RSA:	
				return KeyFactory.getInstance("RSA").generatePublic(
						new X509EncodedKeySpec(bytes));
			case PRIVATE_RSA:
				return KeyFactory.getInstance("RSA").generatePrivate(
						new PKCS8EncodedKeySpec(bytes));
			case PUBLIC_DSA:
				return KeyFactory.getInstance("DSA").generatePublic(
						new X509EncodedKeySpec(bytes));
			case PRIVATE_DSA:
				return KeyFactory.getInstance("DSA").generatePrivate(
						new PKCS8EncodedKeySpec(bytes));
			}
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Enum for specifying the key type
	 */
	enum Type {PUBLIC_RSA, PRIVATE_RSA, PUBLIC_DSA, PRIVATE_DSA};
}
