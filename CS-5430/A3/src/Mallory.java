import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Mallory {
	private static final int DEFAULT_SERVER_PORT = 4000;
	private static final int DEFAULT_BOB_PORT = 4001;
	
	private static ServerSocket server;
	private static Socket fromAlice;
	private static Socket toBob;
	private static Mode mode;
	
	private static Key alicePublicRSA;
	private static Key alicePublicDSA;
	private static Key bobPublicRSA;
	private static Key bobPublicDSA;
	
	private static BlockingQueue<String> incomingMessages;
	private static BlockingQueue<String> outgoingMessages;
	
	private static List<String> oldMessages;

	/**
	 * The main method. Default host is loopback, default port is 4000, default mode is plaintext
	 * Call as one of
	 * Mallory
	 * Mallory <mode>
	 * Mallory <serverPort> <mode>
	 * Mallory <bobPort> <serverPort> <mode>
	 * Mallory <hostname> <bobPort> <serverPort> <mode>
	 * @param args
	 */
	public static void main(String[] args) {
		//Parse args
		String hostname = null;
		int bobPort = DEFAULT_BOB_PORT;
		int serverPort = DEFAULT_SERVER_PORT;
		mode = Mode.PLAINTEXT;
		switch(args.length) {
		case 0: 
			break;
		case 1: 
			mode = Mode.fromString(args[0]);
			break;
		case 2:
			serverPort = Integer.parseInt(args[0]);
			mode = Mode.fromString(args[1]);
			break;
		case 3:
			bobPort = Integer.parseInt(args[0]);
			serverPort = Integer.parseInt(args[1]);
			mode = Mode.fromString(args[2]);
			break;
		default:
			hostname = args[0];
			bobPort = Integer.parseInt(args[1]);
			serverPort = Integer.parseInt(args[2]);
			mode = Mode.fromString(args[3]);
			break;
		}
		System.out.format("Starting in %s mode\n", mode.toString());
		
		//Load keys from filesystem
		try {
			alicePublicRSA = KeyIO.readKeyFromFile(new File("alicepublicRSA.key"), KeyIO.Type.PUBLIC_RSA);
			alicePublicDSA = KeyIO.readKeyFromFile(new File("alicepublicDSA.key"), KeyIO.Type.PUBLIC_DSA);
			alicePublicRSA = KeyIO.readKeyFromFile(new File("bobpublicRSA.key"), KeyIO.Type.PUBLIC_RSA);
			alicePublicDSA = KeyIO.readKeyFromFile(new File("bobpublicDSA.key"), KeyIO.Type.PUBLIC_DSA);
		} catch (IOException e) {
			System.err.println("Error loading keys");
		}
		
		//Initialize data structures
		incomingMessages = new LinkedBlockingQueue<>();
		outgoingMessages = new LinkedBlockingQueue<>();
		oldMessages = new ArrayList<>();
		
		//Set up connections
		try {
			System.out.format("Connecting to Bob on host %s, port %d\n",
					(hostname == null)?"loopback":hostname, bobPort);
			toBob = new Socket(hostname,bobPort);
			System.out.println("Connected to Bob");
			server = new ServerSocket(serverPort);
			System.out.format("Waiting for Alice on port %d\n", serverPort);
			fromAlice = server.accept();
			System.out.println("Connected to Alice");
		} catch (IOException e) {
			System.err.println("Error setting up connections");
			System.exit(0);
		}
		
		//Start threads
		Runnable processIncoming = new Runnable() {public void run() {handleIncoming();}};
		Runnable processOutgoing = new Runnable() {public void run() {handleOutgoing();}};
		Runnable handleUserInput = new Runnable() {public void run() {handleUserInput();}};
		Thread t1 = new Thread(processIncoming);
		Thread t2 = new Thread(processOutgoing);
		Thread t3 = new Thread(handleUserInput);
		t1.start();
		t2.start();
		t3.start();
		
	}
	
	/**
	 * Thread to handle incoming messages and add them to the queue to wait
	 * for processing
	 */
	private static void handleIncoming() {
		try {
			DataInputStream incoming = new DataInputStream(fromAlice.getInputStream());
			outgoingMessages.put(incoming.readUTF()); //Pass through session key
			outgoingMessages.put(incoming.readUTF()); //Pass through MAC key
			while(true) {
				String message = incoming.readUTF();
				incomingMessages.put(message);
			}
		} catch (EOFException e) {
			System.out.println("Alice closed connection\nShutting Down");
			System.exit(0);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Thread to pull outgoing messages off the queue and send them
	 */
	private static void handleOutgoing() {
		try {
			DataOutputStream outgoing = new DataOutputStream(toBob.getOutputStream());
			while(true) {
				String message = outgoingMessages.take();
				outgoing.writeUTF(message);
			}
		} catch (IOException | InterruptedException e) {
			System.err.println("Error sending message");
			System.exit(0);
		}
	}
	
	/**
	 * Thread to handle user input
	 */
	private static void handleUserInput() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Welcome to the interface for Mallory, the malicious Dolev-Yao" +
		" attacker who controls the network. Type next to get started or type help for help");
		while(true) {
			String input = scanner.nextLine();
			switch(input) {
			case "quit":
			case "q":
				quit();
			case "replay":
			case "r":
				replayMessages(scanner);
				break;
			case "next":
			case "n":
				examineNextMessage(scanner);
				break;
			case "help":
			case "h":
			default:
				printGeneralHelp();
			}
			System.out.println("\nWhat do you want to do?");
		}
	}

	/**
	 * Prints out general help for the user
	 */
	private static void printGeneralHelp() {
		System.out.println(
				"Usage:\n"
				+ "help, h - print this information\n"
				+ "quit, q - exit the program\n"
				+ "replay, r - replay past messages\n"
				+ "next, n - wait for an incoming message");
	}
	
	/**
	 * Prints out message processing help for the user
	 */
	private static void printMessageHelp() {
		System.out.println(
				"Usage:\n"
				+ "help, h - print this information\n"
				+ "quit, q - exit the program\n"
				+ "forward, f - forward the message to Bob\n"
				+ "modify, m - modify the message by entering your own values for its fields\n"
				+ "delete, d - delete the message (it will still be saved for replay)");
	}
	
	
	/**
	 * Prints out replay help for the user
	 */
	private static void printReplayHelp() {
		System.out.println(
				"Usage:\n"
				+ "help, h - print this information\n"
				+ "quit, q - exit the program\n"
				+ "forward, f - view the next old message\n"
				+ "back, b - view the previous old message\n"
				+ "send, s - send the current old message to bob\n"
				+ "cancel, c - go back to the main menu");
	}

	/**
	 * Quits the program
	 */
	private static void quit() {
		System.out.println("Shutting Down");
		System.exit(0);
	}
	
	/**
	 * Examines the next message from Alice and lets Mallory modify or forward it
	 * @param scanner A scanner for user input
	 */
	private static void examineNextMessage(Scanner scanner) {
		try {
			System.out.println("Waiting for a message to arrive");
			String messageString = incomingMessages.take();
			oldMessages.add(messageString);
			System.out.println("A message has arrived!\n");
			formatMessage(messageString);
			System.out.println("Press f to forward this message, m to modify it, or d to delete it. "
					+ "Press h for help.");
			boolean validInput = false;
			String newMessage = messageString;
			while(!validInput) {
				String input = scanner.nextLine();
				switch(input) {
				case "help":
				case "h":
					printMessageHelp();
					break;
				case "forward":
				case "f":
					validInput = true;
					break;
				case "modify":
				case "m":
					validInput = true;
					newMessage = modifyMessage(messageString,scanner);
					break;
				case "delete":
				case "d":
					System.out.println("Message deleted");
					return;
				case "quit":
				case "q":
					quit();
				}
			}
			System.out.println("Sending message");
			outgoingMessages.put(newMessage);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Takes user input to modify a message
	 * @param messageString The message as a string
	 * @return The modified message
	 */
	private static String modifyMessage(String messageString, Scanner scanner) {
		JsonObjectBuilder newMessage = Json.createObjectBuilder();
		JsonObject message = null;
		if (mode != Mode.ENCRYPTION) {
			message = Json.createReader(new StringReader(messageString)).readObject();
		}
		switch(mode) {
		case PLAINTEXT:
			System.out.format("\nEnter a new message number. The old message number is: %d\n",
					message.getInt("Message Number"));
			newMessage.add("Message Number", scanner.nextInt());
			scanner.nextLine();
			System.out.format("\nEnter a new message. The old message is: %s\n", 
					message.getString("Message"));
			newMessage.add("Message", scanner.nextLine());
			break;
		case ENCRYPTION:
			System.out.format("\nEnter a new message. The old message is: %s\n",
					messageString);
			return scanner.nextLine();
		case MAC:
			JsonObjectBuilder numberedMessage = Json.createObjectBuilder();
			System.out.format("\nEnter a new message number. The old message number is: %d\n", 
					message.getJsonObject("Numbered Message").getInt("Message Number"));
			numberedMessage.add("Message Number", scanner.nextInt());
			scanner.nextLine();
			System.out.format("\nEnter a new message. The old message is: %s\n", 
					message.getJsonObject("Numbered Message").getString("Message"));
			numberedMessage.add("Message", scanner.nextLine());
			System.out.format("\nEnter a new tag. The old tag is: %s\n",
					message.getString("MAC"));
			newMessage.add("Numbered Message",numberedMessage);
			newMessage.add("MAC", scanner.nextLine());
			break;
		case MAC_ENCRYPTION:
			System.out.format("\nEnter a new message. The old message is: %s\n",
					message.getString("Encrypted Message"));
			newMessage.add("Encrypted Message", scanner.nextLine());
			System.out.format("\nEnter a new tag. The old tag is: %s\n", 
					message.getString("MAC"));
			newMessage.add("MAC", scanner.nextLine());
		}
		return newMessage.build().toString();
	}

	/**
	 * Replays past messages from Alice to Bob
	 * @param scanner A scanner for user input
	 */
	private static void replayMessages(Scanner scanner) {
		if (oldMessages.size() > 0) {
			System.out.println("Press f to view the next message, b to view the previous one, or c to cancel "
					+ "and go back to the main menu. Press h for help. Press s to send an old message.");
			ListIterator<String> itr = oldMessages.listIterator();
			String currentMessage = itr.next();
			boolean goingForward = true;
			formatMessage(currentMessage);
			while(true) {
				String input = scanner.nextLine();
				switch(input) {
				case "help":
				case "h":
					printReplayHelp();
					break;
				case "send":
				case "s":
					try {
						outgoingMessages.put(currentMessage);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
				case "forward":
				case "f":
					if (!goingForward) {
						itr.next();
					}
					goingForward = true;
					if (itr.hasNext()) {
						currentMessage = itr.next();
						formatMessage(currentMessage);
					}
					else {
						System.out.println("This is the last message");
					}
					break;
				case "back":
				case "b":
					if (goingForward) {
						itr.previous();
					}
					goingForward = false;
					if (itr.hasPrevious()) {
						currentMessage = itr.previous();
						formatMessage(currentMessage);
					}
					else {
						System.out.println("This is the first message");
					}
					break;
				case "cancel":
				case "c":
					return;
				case "quit":
				case "q":
					quit();
				}
			}
			
		}
		else {
			System.out.println("No messages have been received");
		}
	}

	/**
	 * Prints out messageString in a nicely formatted fashion
	 * @param messageString The message to print
	 */
	private static void formatMessage(String messageString) {
		JsonObject message = null;
		if (mode != Mode.ENCRYPTION) {
			message = Json.createReader(new StringReader(messageString)).readObject();
		}
		switch(mode) {
		case PLAINTEXT:
			System.out.format("Message Number: %d\n\nMessage: %s\n\n",
					message.getInt("Message Number"), message.getString("Message"));
			break;
		case ENCRYPTION:
			System.out.format("Encrypted Message: %s\n\n",messageString);
			break;
		case MAC:
			System.out.format("Message Number: %d\n\nMessage: %s\n\nTag: %s\n\n", 
					message.getJsonObject("Numbered Message").getInt("Message Number"),
					message.getJsonObject("Numbered Message").getString("Message"),
					message.getString("MAC"));
			break;
		case MAC_ENCRYPTION:
			System.out.format("Encrypted Message: %s\n\nTag: %s\n\n",
					message.getString("Encrypted Message"),
					message.getString("MAC"));
		}
	}

}
