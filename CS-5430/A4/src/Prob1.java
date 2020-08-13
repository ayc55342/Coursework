import javax.crypto.*;

import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Random;

public class Prob1 {
	
	private static final Random rand = new SecureRandom();
	
	// Hashes a password
	public static String SHA_512_Hash(String password, String salt){
		String generatedPassword = null;
		    try {
		         MessageDigest md = MessageDigest.getInstance("SHA-512");
		         md.update(salt.getBytes("UTF-8"));
		         byte[] bytes = md.digest(password.getBytes("UTF-8"));
		         StringBuilder sb = new StringBuilder();
		         for(int i = 0; i < bytes.length; i++){
		            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		         }
		         generatedPassword = sb.toString();
		        } 
		       catch (NoSuchAlgorithmException | UnsupportedEncodingException e){
		        e.printStackTrace();
		       }
		    return generatedPassword;
		}
	
	// Generates a 4-byte salt
	public static byte[] genSalt() {
		byte[] newsalt = new byte[4];
		rand.nextBytes(newsalt);
		return newsalt;
	}
	
	public static String genAlphanumeric() {
		final String alpha = "0123456789abcdefghijklmnopqrstuvwxyz";
		
		//Segment probability space based on combinatoric distribution
		double onethreshold = 36.0 / 2821109907456.0;
		double twothreshold = onethreshold + 1296.0 / 2821109907456.0;
		double threethreshold = twothreshold + 46656.0 / 2821109907456.0;
		double fourthreshold = threethreshold + 1679616.0 / 2821109907456.0;
		double fivethreshold = fourthreshold + 60466176.0 / 2821109907456.0;
		double sixthreshold = fivethreshold + 2176782336.0 / 2821109907456.0;
		double seventhreshold = sixthreshold + 78364164096.0 / 2821109907456.0;

		int len;
		double seed = Math.random();
		if (seed < onethreshold) {
			len = 1;
		}
		else if (seed < twothreshold) {
			len = 2;
		}
		else if (seed < threethreshold) {
			len = 3;
		}
		else if (seed < fourthreshold) {
			len = 4;
		}
		else if (seed < fivethreshold) {
			len = 5;
		}
		else if (seed < sixthreshold) {
			len = 6;
		}
		else if (seed < seventhreshold) {
			len = 7;
		}
		else len = 8;

		StringBuilder build = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			build.append(alpha.charAt(rand.nextInt(alpha.length())));
		}
		
		return build.toString();
	}
		
	public static void main(String[] args) {
		
		String mode = args[0];
		int start = Integer.parseInt(args[1]);
		int end = Integer.parseInt(args[2]);
		int numSalts = Integer.parseInt(args[3]);

		File myspace = new File("myspace.txt");
		File output = new File("output.txt");
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(myspace), 6400);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String read = "";
		byte[] salt = new byte[4];
		String saltstring = "";
		String hashed = "";

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(output));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		if (mode.equals("myspace")) {
		
			try {		
			for (int i = 0; i < start; i++) {
					br.readLine();
			}
			long startTime = System.currentTimeMillis();
			System.out.println("Start time:" + startTime);
			for (int i = start; i < end; i++) {
				read = br.readLine();
				for (int j = 0; j < numSalts; j++) {
					salt = genSalt();
					saltstring = new String(salt, StandardCharsets.UTF_8);
					hashed = SHA_512_Hash(read, saltstring);
					bw.write(read + ", " + saltstring + ", " + hashed);
					bw.newLine();
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("End time:" + endTime);
			System.out.println("Time elapsed:" + (endTime - startTime));
			System.out.println("Average time per password with single salt" + ((double) (endTime - startTime)/((end - start)*numSalts)));
			System.out.println("Average time spent per password:" + ((double) (endTime - startTime)/(end - start)));
			System.out.println("Average time for 1 salt/password:" + ((double) (endTime - startTime)/numSalts));
			bw.close();
			br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		else {
			int total = end - start;
			String[] passwords = new String[total];
			for (int i = 0; i < total; i++) {
				passwords[i] = genAlphanumeric();
			}
			
			try {		
			for (int i = 0; i < start; i++) {
					br.readLine();
			}
			long startTime = System.currentTimeMillis();
			System.out.println("Start time:" + startTime);
			for (int i = start; i < end; i++) {
				read = passwords[i];
				hashed = SHA_512_Hash(read, "");
				bw.write(read + ", " + hashed);
				bw.newLine();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("End time:" + endTime);
			System.out.println("Time elapsed:" + (endTime - startTime));
			System.out.println("Average time spent per password:" + ((double) (endTime - startTime)/(end - start)));
			bw.close();
			br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
}
