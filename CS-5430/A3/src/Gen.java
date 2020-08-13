import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.crypto.*;
import java.security.*;

public class Gen {
	
	private static KeyPair generateDSAKeys() {
		KeyPair dsapair = null;
		try {
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG","SUN");
			KeyPairGenerator dsaGen = KeyPairGenerator.getInstance("DSA","SUN");
			dsaGen.initialize(1024, random);
			dsapair = dsaGen.genKeyPair();
			return dsapair;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
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
	
	public static void main(String[] args) { // later, generate()

		KeyPair alicekeypairRSA = generate2048bitRSAKeys();
		PrivateKey aliceprivateRSA = alicekeypairRSA.getPrivate();
		PublicKey alicepublicRSA = alicekeypairRSA.getPublic();
		
		KeyPair bobkeypairRSA = generate2048bitRSAKeys();
		PrivateKey bobprivateRSA = bobkeypairRSA.getPrivate();
		PublicKey bobpublicRSA = bobkeypairRSA.getPublic();
		
		KeyPair alicekeypairDSA = generateDSAKeys();
		PrivateKey aliceprivateDSA = alicekeypairDSA.getPrivate();
		PublicKey alicepublicDSA = alicekeypairDSA.getPublic();
		
		KeyPair bobkeypairDSA = generateDSAKeys();
		PrivateKey bobprivateDSA = bobkeypairDSA.getPrivate();
		PublicKey bobpublicDSA = bobkeypairDSA.getPublic();

		
		// In case of highly improbable scenario of key overlap
		while (aliceprivateRSA.equals(bobprivateRSA) || alicepublicRSA.equals(bobpublicRSA)) {
			bobkeypairRSA = generate2048bitRSAKeys();
			bobprivateRSA = bobkeypairRSA.getPrivate();
			bobpublicRSA = bobkeypairRSA.getPublic();
		}
		while (aliceprivateDSA.equals(bobprivateDSA) || alicepublicDSA.equals(bobpublicDSA)) {
			bobkeypairDSA = generateDSAKeys();
			bobprivateDSA = bobkeypairDSA.getPrivate();
			bobpublicDSA = bobkeypairDSA.getPublic();
		}

		// Write keys to file
		try {
			KeyIO.writeKeyToFile(alicepublicRSA, new File("alicepublicRSA.key"));
			KeyIO.writeKeyToFile(aliceprivateRSA, new File("aliceprivateRSA.key"));
			KeyIO.writeKeyToFile(bobpublicRSA, new File("bobpublicRSA.key"));
			KeyIO.writeKeyToFile(bobprivateRSA, new File("bobprivateRSA.key"));
			KeyIO.writeKeyToFile(alicepublicDSA, new File("alicepublicDSA.key"));
			KeyIO.writeKeyToFile(aliceprivateDSA, new File("aliceprivateDSA.key"));
			KeyIO.writeKeyToFile(bobpublicDSA, new File("bobpublicDSA.key"));
			KeyIO.writeKeyToFile(bobprivateDSA, new File("bobprivateDSA.key"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
