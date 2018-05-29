/*
 * Represents a blockchain data structure
 * Essentially a synchronized linked list of MokBlocks
 */
package aa.project.blockchain;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Martius
 */
public class MokChain {

    private final List<MokBlock> CHAIN;
    private final AtomicInteger DIFFICULTY;

    public MokChain(int difficulty) {
        CHAIN = Collections.synchronizedList(new LinkedList<>());
        DIFFICULTY = new AtomicInteger(difficulty);
    }

    public AtomicInteger getDifficulty() {
        return DIFFICULTY;
    }

    public synchronized boolean addBlock(MokBlock block) {
        if (isChainValid(block)) {
            CHAIN.add(block);
            return true;
        }
        return false;
    }

    public MokBlock getLatestBlock() {
        if (CHAIN.isEmpty()) {
            return null;
        }
        return CHAIN.get(CHAIN.size() - 1);
    }

    public String getLatestBlockHash() {
        MokBlock mokBlock = getLatestBlock();
        String latestBlockHash = mokBlock != null ? mokBlock.getHash() : "0";
        return latestBlockHash;
    }

    public synchronized int getSize() {
        return CHAIN.size();
    }

    // checks if the chain is valid 
    // the chain is valid if the hash of the previous block is used
    // to compute the hash of the current block
    // forming a linked blockchain
    public boolean isChainValid() {
        MokBlock currentBlock;
        MokBlock lastBlock;

        if (CHAIN.size() < 2) {
            return true;
        }

        for (int i = 1; i < CHAIN.size(); i++) {
            currentBlock = CHAIN.get(i);
            lastBlock = CHAIN.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.computeHash())) {
                System.out.println("Current block hash is not equal");
                return false;
            }

            if (!lastBlock.getHash().equals(lastBlock.computeHash())) {
                System.out.println("Previous block hash is not equal");
                return false;
            }
        }

        return true;
    }

    // also checks if the chain is valid 
    // but only checks the latest 2 blocks so that it doesnt 
    // have to recompute the entire chain
    public boolean isChainValid(MokBlock latestBlock) {

        MokBlock currentBlock = latestBlock;
        MokBlock lastBlock;
        System.out.println("Verifying integrity of new MokBlock! Current MokChain length: " + CHAIN.size());

        if (CHAIN.isEmpty()) {
            return true;
        }

        lastBlock = CHAIN.get(CHAIN.size() - 1);

        if (!currentBlock.getHash().equals(currentBlock.computeHash())) {
            System.out.println("Current block hash is not equal");
            return false;
        }

        if (!lastBlock.getHash().equals(currentBlock.getHashLastBlock())) {
            System.out.println("Previous block hash is not equal");
            return false;
        }

        return true;
    }
}
