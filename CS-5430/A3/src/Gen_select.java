import java.util.*;
import java.lang.*;
import javax.crypto.*;
import java.security.*;

public class Gen_select {
	// Variant that lets user selectively choose keys to generate
	
	private static KeyPair generate2048bitRSAKeys() {
		KeyPair pair = null;
		try {
			KeyPairGenerator keypairGen = KeyPairGenerator.getInstance("RSA");
			keypairGen.initialize(2048);
			pair = keypairGen.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return pair;
	}

	private static KeyGenerator keygeninstance() {
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return keyGen;
	}
	
	public static void main(String[] args) { // later, generate()

        final Scanner scanner = new Scanner( System.in );
		
		SecureRandom rng = new SecureRandom();

		KeyGenerator symmetrickeygenerator = keygeninstance();
		symmetrickeygenerator.init(256, rng);

		KeyPair alicekeypair = null;
		PrivateKey aliceprivate = null;
		PublicKey alicepublic = null;
		
		KeyPair bobkeypair = null;
		PrivateKey bobprivate = null;
		PublicKey bobpublic = null;

		SecretKey sessionkey = null;

		SecretKey aliceMACkey = null;
		SecretKey bobMACkey = null;

		
		System.out.println("Select keys you wish to generate by typing designated letters: \n");
		System.out.println("A: Alice public/private key pair \n"
				+ "B: Bob public/private key pair \nS: Session key \n"
				+ "M: Alice MAC key \nN: Bob MAC key \nE: All of the above \n");
		String input = scanner.nextLine();

		if (input.contains("A") || input.contains("E")) {
			alicekeypair = generate2048bitRSAKeys();
			aliceprivate = alicekeypair.getPrivate();
			alicepublic = alicekeypair.getPublic();
			System.out.println("Alice Private key: \n" + aliceprivate);
			System.out.println("Alice Public key: \n" + alicepublic);
		}
		
		if (input.contains("B") || input.contains("E")) {
			bobkeypair = generate2048bitRSAKeys();
			bobprivate = bobkeypair.getPrivate();
			bobpublic = bobkeypair.getPublic();
			System.out.println("Bob Private key: \n" + bobprivate);
			System.out.println("Bob Public key: \n" + bobpublic);
		}
		
		// In case of highly improbable scenario of key overlap
		while (aliceprivate == bobprivate || alicepublic == bobpublic) {
			bobkeypair = generate2048bitRSAKeys();
			bobprivate = bobkeypair.getPrivate();
			bobpublic = bobkeypair.getPublic();
			System.out.println("Key overlap detected.");
			System.out.println("New Bob Private key: \n" + bobprivate);
			System.out.println("New Bob Public key: \n" + bobpublic);
		}
		
		if (input.contains("S") || input.contains("E")) {
			sessionkey = symmetrickeygenerator.generateKey();
			System.out.println("Session key: \n" + sessionkey);
		}
		
		if (input.contains("M") || input.contains("E")) {
			aliceMACkey = symmetrickeygenerator.generateKey();
			System.out.println("Alice MAC key: \n" + aliceMACkey);
		}

		if (input.contains("N") || input.contains("E")) {
			bobMACkey = symmetrickeygenerator.generateKey();
			System.out.println("Bob MAC key: \n" + bobMACkey);
		}
		
		// Again in improbable case of key overlap
		while (sessionkey == aliceMACkey || sessionkey == bobMACkey
				|| aliceMACkey == bobMACkey) {
			System.out.println("Key overlap detected.");
			if (sessionkey == aliceMACkey) {
				aliceMACkey = symmetrickeygenerator.generateKey();
				System.out.println("New Alice MAC key: \n" + aliceMACkey);
			}
			else {
				bobMACkey = symmetrickeygenerator.generateKey();
				System.out.println("New Bob MAC key: \n" + bobMACkey);
			}
		}
						
	}
	
}
