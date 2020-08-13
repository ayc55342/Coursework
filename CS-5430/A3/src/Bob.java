import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;

/**
 * Bob portion of demo crypto system
 * Reads input from the network and prints it out. In MAC mode detects modifications.
 */
public class Bob extends Thread {
	private static final int DEFAULT_PORT = 4001;
	private static final long MINUTE_IN_MILLIS = 60000;
	
	private static Key bobPublicRSA;
	private static Key bobPrivateRSA;
	private static Key bobPublicDSA;
	private static Key bobPrivateDSA;
	private static Key alicePublicRSA;
	private static Key alicePublicDSA;
	
	private ServerSocket server;
	private Mode mode;
	private Key sessionKey;
	private IvParameterSpec iv;
	private SecretKey MACKey;
	private int messageNumber = 0;
	
	/**
	 * Main method. Default port is 1000, default mode is plaintext
	 * Call as one of 
	 * Bob
	 * Bob <mode>
	 * Bob <port> <mode>
	 */
	public static void main(String[] args) {
		//Parse arguments
		int port = DEFAULT_PORT;
		Mode m = Mode.PLAINTEXT;
		switch(args.length) {
		case 0:
			break;
		case 1:
			m = Mode.fromString(args[0]);
			break;
		default:
			port = Integer.parseInt(args[0]);
			m = Mode.fromString(args[1]);
			break;
		}
		System.out.format("Starting in %s mode\n", m.toString());
		
		//Load keys from filesystem
		try {
			bobPublicRSA = KeyIO.readKeyFromFile(new File("bobpublicRSA.key"), KeyIO.Type.PUBLIC_RSA);
			bobPrivateRSA = KeyIO.readKeyFromFile(new File("bobprivateRSA.key"), KeyIO.Type.PRIVATE_RSA);
			bobPublicDSA = KeyIO.readKeyFromFile(new File("bobpublicDSA.key"), KeyIO.Type.PUBLIC_DSA);
			bobPrivateDSA = KeyIO.readKeyFromFile(new File("bobprivateDSA.key"), KeyIO.Type.PRIVATE_DSA);
			alicePublicRSA = KeyIO.readKeyFromFile(new File("alicepublicRSA.key"), KeyIO.Type.PUBLIC_RSA);
			alicePublicDSA = KeyIO.readKeyFromFile(new File("alicepublicDSA.key"), KeyIO.Type.PUBLIC_DSA);
		} catch (IOException e) {
			System.err.println("Error reading keys from file");
			if (m != Mode.PLAINTEXT) {
				System.exit(0);
			}
		}
		
		try {
			//Start server
			Bob bob = new Bob(port,m);
			bob.start();
			
			//Scan for user input to shut down system
			while(true) {
				Scanner scanner = new Scanner(System.in);
				String input = scanner.next();
				if (input.equals("quit")) {
					System.out.println("Shutting Down");
					try {
						bob.server.close();
					} catch (IOException e) {
						System.err.println(
								"An error occurred trying to close the socket");
					}
					scanner.close();
					System.exit(0);
				}
			}
		} catch (IOException e) {
			System.err.println("Error attempting to start server");
		}
	}
	
	/**
	 * Starts the socket handler thread
	 */
	public void run() {
		try {
			//Wait for a connection
			System.out.println("Waiting for client on port " + server.getLocalPort());
			Socket connection = server.accept();
			DataInputStream incoming = new DataInputStream(connection.getInputStream());
			System.out.println("Connected");
			
			//Receive session and MAC keys
			if (!receiveSessionKey(incoming, true)) {
				System.err.println("Failed to receive session key\nShutting Down");
				System.exit(0);
			}
			else {
//				System.out.format("Session Key: %s\n", 
//						Base64.getEncoder().encodeToString(sessionKey.getEncoded()));
//				System.out.format("IV: %s\n", Base64.getEncoder().encodeToString(iv.getIV()));
			}
			if (!receiveSessionKey(incoming, false)) {
				System.err.println("Failed to receive MAC key\nShutting Down");
				System.exit(0);
			}
			
			//Initalize MAC and cipher algorithms
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, sessionKey, iv);
			Mac mac =  Mac.getInstance("HmacSHA256");
			mac.init(MACKey);
			
			//Main loop
			System.out.println("Waiting for messages");
			while(true) {
				String message = incoming.readUTF();
				receiveMessage(message, cipher, mac);
			}
		} catch (EOFException e) {
			System.out.println("Other side closed connection\nShutting Down");
			System.exit(0);
		} catch (InvalidKeyException e) {
			System.err.println("Received session key is invalid\nShutting Down");
			System.exit(0);
		}
		catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Processes and displays a received message in a fashion dictated by the current mode
	 * @param message The received message
	 * @param cipher The cipher to use if applicable
	 * @param digest The message digest to use if applicable
	 * @return The new message number
	 */
	private void receiveMessage(String message, Cipher cipher, Mac mac) {
		try {
			String numberedMessage = message;
			String toDecrypt = message;
			switch(mode) {
			case PLAINTEXT:
				break;
			case MAC:
				JsonObject msgPlusMAC = Json.createReader(new StringReader(message)).readObject();
				numberedMessage = msgPlusMAC.getJsonObject("Numbered Message").toString();
				verifyMAC(numberedMessage, msgPlusMAC.getString("MAC"), mac);
				break;
			case MAC_ENCRYPTION:
				JsonObject encMsgPlusMAC = Json.createReader(new StringReader(message)).readObject();
				toDecrypt = encMsgPlusMAC.getString("Encrypted Message");
				verifyMAC(toDecrypt, encMsgPlusMAC.getString("MAC"), mac);
			case ENCRYPTION:
				byte[] ciphertext = null;
				try {
					ciphertext = Base64.getDecoder().decode(toDecrypt);
				} catch (IllegalArgumentException e) {
					System.err.println("Received non Base64 encoded message - Likely the result of tampering");
					return;
				}
				numberedMessage = new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);
			}
			JsonObject parsedMessage = Json.createReader(new StringReader(numberedMessage)).readObject();
			updateMessageNumber(parsedMessage.getInt("Message Number"));
			System.out.println(parsedMessage.getString("Message"));
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			System.err.println("Error decrypting message - Likely the result of tampering");
		}
	}
	
	/**
	 * Verifies that the MAC of message matches the given MAC and if not prints a warning
	 * @param message The message to verify
	 * @param MAC The received MAC for this message
	 * @param digest The Message Digest to use
	 */
	private void verifyMAC(String message, String MAC, Mac mac) {
		String newMAC = Base64.getEncoder().encodeToString(
				mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		if (!newMAC.equals(MAC)) {
			System.err.println(
					"Warning: Message and its MAC do not match - likely the result of tampering");
		}
	}
	
	/**
	 * Updates the message number to received number if appropriate, otherwise prints a warning
	 * @param receivedNumber The number of the received message
	 */
	private void updateMessageNumber(int receivedNumber) {
		if (receivedNumber == messageNumber + 1) {
			messageNumber = receivedNumber;
		}
		else if (receivedNumber > messageNumber) {
			messageNumber = receivedNumber;
			System.err.println(
					"Warning Message number increased by more than one - messages have likely been dropped");
		}
		else  {
			System.err.println(
					"Warning: Message received out of order - likely the result of a replay attack");
		}
	}

	/**
	 * Receives a session key
	 * @param stream The stream to receive over
	 * @return Whether the operation succeeded
	 * @throws IOException If an error occurs
	 */
	public boolean receiveSessionKey(DataInputStream stream, boolean aes) throws IOException {
		try {
			System.out.format("Waiting for %s key\n",(aes)?"session":"MAC");
			String signedMessage = stream.readUTF();
			long receivedTimestamp = System.currentTimeMillis();
			System.out.println("Received key transport message");
			
			JsonObject signedMessageObject = Json.createReader(new StringReader(signedMessage)).readObject();
			String signature = signedMessageObject.getString("Signature");
			JsonObject message = signedMessageObject.getJsonObject("Message");
		
			Signature dsa = Signature.getInstance("SHA1withDSA");
			dsa.initVerify((PublicKey) alicePublicDSA);
			dsa.update(message.toString().getBytes("UTF-8"));
			boolean verifies = dsa.verify(Base64.getDecoder().decode(signature.getBytes("UTF-8")));
			if (!verifies) {
				System.err.println("Key transport signature did not verify");
				return false;
			}
			
			String recipient = message.getString("Recipient");
			long sentTimestamp = message.getJsonNumber("Timestamp").longValue();
			byte[] ciphertext = Base64.getDecoder().decode(
					message.getString("Encrypted Key").getBytes("UTF-8"));
			
			if (!recipient.equals("Bob")) {
				System.err.println("Key transport recipient incorrect");
				return false;
			}
			else if (receivedTimestamp - MINUTE_IN_MILLIS  > sentTimestamp) {
				System.err.println("Key transport timestamp too old");
				return false;
			}
			
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, bobPrivateRSA);
			String plaintext = new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);
			
			JsonObject encryptedKey = Json.createReader(new StringReader(plaintext)).readObject();
			
			if (!encryptedKey.getString("Sender").equals("Alice")) {
				System.err.println("Key transport sender incorrect");
				return false;
			}
			byte[] key = Base64.getDecoder().decode(encryptedKey.getString("Session Key"));
			if (aes) {
				sessionKey = new SecretKeySpec(key,0,key.length,"AES");
				iv = new IvParameterSpec(Base64.getDecoder().decode(encryptedKey.getString("IV")));
			}
			else {
				MACKey = new SecretKeySpec(key,0,key.length,"HmacSHA256");
			}
			
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException 
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		
		System.out.format("Successfully received %s key\n",(aes)?"session":"MAC");
		return true;
	}
	
	/**
	 * Constructor. Defaults to plaintext mode
	 * @param port The port on which to open the server
	 */
	private Bob(int port) throws IOException {
		this(port,Mode.PLAINTEXT);
	}
	
	/**
	 * Constructor. Takes both port and mode
	 * @param port The port on which to open the server
	 * @param m The mode to run in 
	 */
	private Bob(int port, Mode m) throws IOException {
		server = new ServerSocket(port);
		mode = m;
	}

}
