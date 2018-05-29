/*
 * Each MokBlock is a building block of the MokChain
 * 
 */
package aa.project.blockchain;

import aa.project.utility.Utility;
import java.util.Arrays;
import java.util.Date;

/**
 *
 * @author Martius
 */
public final class MokBlock {

    private final String[] TRANSACTIONS;
    private final String HASH_LAST_BLOCK;
    private final String HASH_MERKLE_BLOCK;
    private final String TARGET;
    private final String HASH;
    private final long TIMESTAMP;
    private final int NONCE;

    public MokBlock(String[] transactions, String hashLastBlock, String target, int nonce) {
        this.TRANSACTIONS = transactions;
        this.HASH_LAST_BLOCK = hashLastBlock;
        this.HASH_MERKLE_BLOCK = this.computeHash(transactions);
        this.TARGET = target;
        this.TIMESTAMP = new Date().getTime();
        this.NONCE = nonce;
        this.HASH = this.computeHash();
    }

    public String computeHash(String[] transactions) {
        String txString = Arrays.toString(transactions);
        return Utility.computeHash(txString);
    }

    public String computeHash() {
        String blockData = HASH_LAST_BLOCK + HASH_MERKLE_BLOCK + TIMESTAMP + NONCE;
        return Utility.computeHash(blockData);
    }

    // Getters
    public String[] getTransactions() {
        return TRANSACTIONS;
    }

    public String getHashLastBlock() {
        return HASH_LAST_BLOCK;
    }

    public String getHashMerkleBlock() {
        return HASH_MERKLE_BLOCK;
    }

    public String getTarget() {
        return TARGET;
    }

    public String getHash() {
        return HASH;
    }

    public long getTimestamp() {
        return TIMESTAMP;
    }

    public int getNonce() {
        return NONCE;
    }
}
