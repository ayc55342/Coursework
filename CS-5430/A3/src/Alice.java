import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.json.*;

import java.io.*;

public class Alice extends Thread {

	private static final int DEFAULT_PORT = 4000;
	
	private static Key alicePublicRSA;
	private static Key alicePrivateRSA;
	private static Key alicePublicDSA;
	private static Key alicePrivateDSA;
	private static Key bobPublicRSA;
	private static Key bobPublicDSA;
	
	private Socket client;
	private Mode mode;
	private SecretKey sessionKey;
	private IvParameterSpec iv;
	private SecretKey MACKey;
	
	/**
	 * Constructor. Defaults to plaintext mode
	 * @param hostname The hostname of the server to connect to 
	 * @param port The port to connect to
	 */
	public Alice(String hostname, int port) throws IOException {
		this(hostname,port,Mode.PLAINTEXT);
	}
	
	/**
	 * Constructor. Takes both port and mode
	 * @param hostname The hostname of the server to connect to
	 * @param port The port to connect to
	 * @param m The mode in which to run
	 */
	public Alice(String hostname, int port, Mode m) throws IOException {
		System.out.format("Connecting to server on on host %s, port %d\n", 
				(hostname == null)?"loopback":hostname, port);
		client = new Socket(hostname, port);
		mode = m;
	}
	
	/**
	 * Starts the program
	 */
	public void run() {
		final Scanner scanner = new Scanner( System.in );
		try {		
			//Connect to server
			OutputStream outputToServer = client.getOutputStream();
			DataOutputStream output = new DataOutputStream(outputToServer);
			
			//Generate and send session key
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			generateSymmetricEncryptionKey(cipher);
//			System.out.format("Session Key: %s\n", 
//					new String(Base64.getEncoder().encode(sessionKey.getEncoded())),StandardCharsets.UTF_8);
//			System.out.format("IV: %s\n",
//					Base64.getEncoder().encodeToString(iv.getIV()));
			sendSessionKey(sessionKey,output,true);
			generateMACKey();
			sendSessionKey(MACKey,output,false);
			
			
			//Initialize MAC and cipher algorithms
			cipher.init(Cipher.ENCRYPT_MODE, sessionKey, iv); 
			Mac mac =  Mac.getInstance("HmacSHA256");
			mac.init(MACKey);
			
			//Main loop
			System.out.println("Enter messages to send:");
			int messageNumber = 0;
			while(true) {
				String message = scanner.nextLine(); // might be unsafe	
				messageNumber++;
				if (message.equals("quit")) {
					System.out.println("Shutting Down");
					scanner.close();
					client.close();
					System.exit(0);
				}
				sendMessage(messageNumber, message, cipher, mac, output);
				
			}
		
		} catch(IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException 
				| InvalidAlgorithmParameterException e) {
			System.err.println("Error sending message");
			System.exit(0);
		}
	}
	
	/**
	 * Sends message over output stream output in a fashion specifed by the current mode
	 * Uses cipher and digest if applicable 
	 * @param messageNumber The number of the message
	 * @param message The message to send
	 * @param cipher The cipher to use  if applicable
	 * @param digest The message digest to use if applicable
	 * @param output The output stream to send over
	 * @throws IOException If there is an error
	 */
	private void sendMessage(int messageNumber, String message, Cipher cipher, Mac mac,
			DataOutputStream output) throws IOException {
		JsonObjectBuilder msgPlusNumber = Json.createObjectBuilder();
		msgPlusNumber.add("Message Number", messageNumber);
		msgPlusNumber.add("Message", message);
		JsonObject jsonmsgPlusNumber = msgPlusNumber.build();
		byte[] plaintext;
		try {
			plaintext = jsonmsgPlusNumber.toString().getBytes("UTF-8");
			byte[] ciphertext = Base64.getEncoder().encode(cipher.doFinal(plaintext));
			switch(mode) {
			case PLAINTEXT:
				output.writeUTF(jsonmsgPlusNumber.toString());
				break;
			case ENCRYPTION:
				String ciphervalue = new String(ciphertext, StandardCharsets.UTF_8);
				output.writeUTF(ciphervalue);
				break;
			case MAC:
				byte[] plaintextMAC = Base64.getEncoder().encode(mac.doFinal(plaintext));
				JsonObjectBuilder toSendMAC = Json.createObjectBuilder();
				toSendMAC.add("Numbered Message", msgPlusNumber);
				String plaintextMACstring = new String(plaintextMAC, StandardCharsets.UTF_8);
				toSendMAC.add("MAC", plaintextMACstring);
				JsonObject jsontoSendMAC = toSendMAC.build();
				output.writeUTF(jsontoSendMAC.toString());
				break;
			case MAC_ENCRYPTION:
				byte[] ciphertextMAC = Base64.getEncoder().encode(mac.doFinal(ciphertext));
				JsonObjectBuilder toSendEncryptionMAC = Json.createObjectBuilder();
				String ciphertextstring = new String(ciphertext, StandardCharsets.UTF_8);
				toSendEncryptionMAC.add("Encrypted Message", ciphertextstring);
				String ciphertextmacstring = new String(ciphertextMAC, StandardCharsets.UTF_8);
				toSendEncryptionMAC.add("MAC", ciphertextmacstring);
				JsonObject jsontoSendEncryptionMAC = toSendEncryptionMAC.build();
				output.writeUTF(jsontoSendEncryptionMAC.toString());
			}
		} catch (UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a session key and iv over stream
	 * @param stream The stream to send it over
	 * @throws IOException If there is an error
	 */
	private void sendSessionKey(SecretKey key, DataOutputStream stream, boolean aes) throws IOException {
		try {
			System.out.format("Sending %s Key\n", (aes)?"Session":"MAC");
			
			JsonObjectBuilder toSign = Json.createObjectBuilder();
			toSign.add("Recipient","Bob").add("Timestamp", System.currentTimeMillis());
			
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, bobPublicRSA);
			
			JsonObjectBuilder toEncrypt = Json.createObjectBuilder();
			toEncrypt.add("Sender", "Alice");
			toEncrypt.add("Session Key", Base64.getEncoder().encodeToString(key.getEncoded()));
			toEncrypt.add("IV", Base64.getEncoder().encodeToString(iv.getIV()));
			
			JsonObject jsontoEncrypt = toEncrypt.build();
			
			byte[] ciphertext = cipher.doFinal(jsontoEncrypt.toString().getBytes("UTF-8"));
			toSign.add("Encrypted Key", Base64.getEncoder().encodeToString(ciphertext));
			JsonObject jsontoSign = toSign.build();
						
			Signature dsa = Signature.getInstance("SHA1withDSA");
			dsa.initSign((PrivateKey)alicePrivateDSA);
			dsa.update(jsontoSign.toString().getBytes("UTF-8"));
			byte[] signature = dsa.sign();
			
			JsonObjectBuilder message = Json.createObjectBuilder();
			message.add("Message", toSign);
			message.add("Signature", Base64.getEncoder().encodeToString(signature));
			JsonObject jsonmessage = message.build();
			
			stream.writeUTF(jsonmessage.toString());
		} 
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException 
				 | SignatureException | IllegalBlockSizeException | BadPaddingException
				 | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates an AES symmetric encryption key and an initialization vector
	 */
	private void generateSymmetricEncryptionKey(Cipher cipher) {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			sessionKey = keyGen.generateKey();
			SecureRandom randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");
			byte[] ivBytes = new byte[cipher.getBlockSize()];
			randomSecureRandom.nextBytes(ivBytes);
			iv = new IvParameterSpec(ivBytes);
		} catch (NoSuchAlgorithmException e) {}
	}
	
	/**
	 * Generates a MAC key
	 */
	private void generateMACKey() {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
			keyGen.init(256);
			MACKey = keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {}
	}
	
	/**
	 * Main method. Default host is loopback, default port is 4000, default mode is plaintext
	 * Call as one of
	 * Alice
	 * Alice <mode>
	 * Alice <port> <mode>
	 * Alice <host> <port> <mode>
	 */
	public static void main(String[] args) {
		//Parse arguments
		String host = null; //loopback
		int port = DEFAULT_PORT;
		Mode m = Mode.PLAINTEXT;
		switch(args.length) {
		case 0: 
			break;
		case 1:
			m = Mode.fromString(args[0]);
			break;
		case 2:
			port = Integer.parseInt(args[0]);
			m = Mode.fromString(args[1]);
			break;
		default:
			host = args[0];
			port = Integer.parseInt(args[1]);
			m = Mode.fromString(args[2]);
		}
		System.out.format("Starting in %s mode\n", m.toString());
		
		//Load keys from filesystem
		try {
			alicePublicRSA = KeyIO.readKeyFromFile(new File("alicepublicRSA.key"), KeyIO.Type.PUBLIC_RSA);
			alicePrivateRSA = KeyIO.readKeyFromFile(new File("aliceprivateRSA.key"), KeyIO.Type.PRIVATE_RSA);
			alicePublicDSA = KeyIO.readKeyFromFile(new File("alicepublicDSA.key"), KeyIO.Type.PUBLIC_DSA);
			alicePrivateDSA = KeyIO.readKeyFromFile(new File("aliceprivateDSA.key"), KeyIO.Type.PRIVATE_DSA);
			bobPublicRSA = KeyIO.readKeyFromFile(new File("bobpublicRSA.key"), KeyIO.Type.PUBLIC_RSA);
			bobPublicDSA = KeyIO.readKeyFromFile(new File("bobpublicDSA.key"), KeyIO.Type.PUBLIC_DSA);
		} catch (IOException e) {
			System.err.println("Error reading keys from file");
			if (m != Mode.PLAINTEXT) {
				System.exit(0);
			}
		}
		
		//Start program
		try {
			Thread alice = new Alice(host, port, m);
			alice.start();
		} catch(IOException e) {
			System.err.println("Error setting up client");
		}
		
	}

}
