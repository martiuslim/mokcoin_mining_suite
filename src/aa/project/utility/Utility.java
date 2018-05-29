/*
 * Utility class for generating pseudo-random transactions and hashing functions
 */
package aa.project.utility;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 *
 * @author Martius
 */
public class Utility {

    // http://www.sha1-online.com/sha256-java/
    // This method seeds a string with a random number 
    // Then hashes it so that the resulting hash string is different
    // Simulates transactions to be grouped in a block for the blockchain
    public static String[] generateTransactions(int numTransactions) {
        Random rng = new Random();
        String[] txArray = new String[numTransactions];

        for (int i = 0; i < numTransactions; i++) {
            String payload = String.format("Mok says: Good. (%d times)", rng.nextInt());
            txArray[i] = computeHash(payload);
        }

        return txArray;
    }

    // This method is a convenience method to return 
    // the hashed version of a string
    public static String computeHash(String payload) {
        StringBuilder sb = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            sb = new StringBuilder();
            for (int j = 0; j < hash.length; j++) {
                sb.append(Integer.toString((hash[j] & 0xff) + 0x100, 16).substring(1));
            }

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Something went wrong!");
            e.printStackTrace();
        }
        return sb != null ? sb.toString() : null;
    }
}
