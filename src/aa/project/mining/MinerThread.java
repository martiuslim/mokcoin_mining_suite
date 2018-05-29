/*
 * Each miner runs several threads to split the computational load 
 * 
 */
package aa.project.mining;

import aa.project.blockchain.MokBlock;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Martius
 */
public class MinerThread implements Runnable {

    private final Miner MINER;
    private final String[] TRANSACTIONS;
    private final String HASH_LAST_BLOCK;
    private final String TARGET;
    private final int DIFFICULTY;
    private final int STARTING_NONCE;
    private final AtomicBoolean ACTIVE = new AtomicBoolean(true);

    public MinerThread(Miner miner, String[] transactions, String hashLastBlock, String target, int difficulty, int nonce) {
        this.MINER = miner;
        this.TRANSACTIONS = transactions;
        this.HASH_LAST_BLOCK = hashLastBlock;
        this.TARGET = target;
        this.DIFFICULTY = difficulty;
        this.STARTING_NONCE = nonce;
    }

    @Override
    public void run() {
        int nonce = STARTING_NONCE;
        int endingNonce = STARTING_NONCE + 1_000_000;

        // computes hashes while active
        // thread polls parent miner every 100000 hashes to check if a block has been found
        while (ACTIVE.get() && nonce < endingNonce) {
            if (nonce % 100_000 == 0) {
                System.out.println("Checking if MokBlock has been found, current nonce: " + nonce);
                boolean checked = ACTIVE.compareAndSet(true, !MINER.getMokFound().get());
            }

            // while miner is still mining
            // compute hashes 
            // if a computed hash fits the designated difficulty
            // try to submit the block to the miningpool
            if (ACTIVE.get()) {
                MokBlock mok = new MokBlock(TRANSACTIONS, HASH_LAST_BLOCK, TARGET, nonce);
                String hashCurrentBlock = mok.getHash();
                if (hashCurrentBlock.substring(0, DIFFICULTY).equals(TARGET)) {
                    if (ACTIVE.compareAndSet(true, false) && !MINER.getMokFound().get()) {
                        try {
                            boolean reported = MINER.reportSuccessfulMokBlock(mok);
                            System.out.println(
                                    "\n=========="
                                    + "\nMokBlock mined! Hash: " + hashCurrentBlock
                                    + "\nSubmitting MokBlock to MokChain: " + reported
                                    + "\nMiner Thread completed & shutting down: " + !ACTIVE.get()
                                    + "\n==========\n"
                            );
                            Thread.sleep(10);

                        } catch (InterruptedException e) {
                            System.out.println(e.getMessage());
                        }
                    } else {
                        System.out.println(
                                "\n=========="
                                + "\nMokBlock already mined!"
                                + "\nMiner Thread completed & shutting down: " + !ACTIVE.get()
                                + "\n==========\n"
                        );
                    }
                }
            } else {
                System.out.println("Block mined by another thread, shutting down MinerThread!");
            }
            nonce++;
        }
        MINER.updateThreadCount(false);
    }
}
