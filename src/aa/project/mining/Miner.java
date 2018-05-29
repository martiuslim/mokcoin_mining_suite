/*
 * Each miner represents a host that contributes computational power to the 
 * mining pool.
 *
 */
package aa.project.mining;

import aa.project.blockchain.MokBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Martius
 */
public class Miner {

    private final AtomicBoolean jobAvailable = new AtomicBoolean(true);
    private final AtomicBoolean mokFound = new AtomicBoolean();
    private final AtomicBoolean mining = new AtomicBoolean();
    private final AtomicBoolean threadsCompleted = new AtomicBoolean();
    private final AtomicBoolean gotNewJob = new AtomicBoolean();
    private final AtomicInteger currentJobNumber = new AtomicInteger();
    private final AtomicInteger moksMined = new AtomicInteger();
    private volatile AtomicReference<JsonObject> job = new AtomicReference<>();
    private volatile MokBlock mokBlock;
    private volatile int startingJobNumber = 0;
    private volatile int numThreads = 0;
    private final int MAX_THREADS = 2;

    public synchronized boolean reportSuccessfulMokBlock(MokBlock mokBlock) {
        if (this.mokFound.compareAndSet(false, true)) {
            this.mokBlock = mokBlock;
            return true;
        }
        return false;
    }

    public boolean getJobAvailability() {
        return jobAvailable.get();
    }

    public boolean setJobAvailability(boolean expected, boolean availability) {
        return jobAvailable.compareAndSet(expected, availability);
    }

    public AtomicBoolean getMokFound() {
        return mokFound;
    }

    public AtomicBoolean getThreadsCompleted() {
        return threadsCompleted;
    }

    public void setStartingJobNumber(int startingJobNumber) {
        this.startingJobNumber = startingJobNumber;
    }

    public AtomicInteger getCurrentJobNumber() {
        return currentJobNumber;
    }

    public synchronized void setCurrentJobNumber(int newJobNumber) {
        this.currentJobNumber.set(newJobNumber);
    }

    public int getMoksMined() {
        return moksMined.get();
    }

    public int incrementMoksMined() {
        return moksMined.incrementAndGet();
    }

    public boolean newJob(JsonObject newJob) {
        JsonObject currentJob = job.get();
        int newJobNumber = newJob.get("jobNumber").getAsInt();
        System.out.println("Checking new job number: " + newJobNumber);

        if (currentJob == null) {
            job.set(newJob);
            this.startingJobNumber = newJobNumber;
            setCurrentJobNumber(newJobNumber);
            return true;
        }

        if (newJobNumber > this.currentJobNumber.get()) {
            boolean test = job.compareAndSet(currentJob, newJob);
            setCurrentJobNumber(newJobNumber);
            System.out.println("@@@@@ UPDATED NEW JOB: " + test + " @@@@@\n");
            return true;
        }
        return false;
    }

    public MokBlock getMokBlock() {
        return mokBlock;
    }

    public synchronized void updateThreadCount(boolean direction) {
        if (direction) {
            numThreads += 1;
        } else {
            numThreads -= 1;
        }
    }

    public boolean getGotNewJob() {
        return gotNewJob.get();
    }

    public void setGotNewJob(boolean gotNewJob) {
        this.gotNewJob.set(gotNewJob);
    }

    private void init(String hostName, int portNumber) {
        Thread poolHandler = new Thread(new PoolHandler(hostName, portNumber, this));
        poolHandler.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        int startingNonce = 0;
        int difficulty;

        String[] transactions;
        String txsArrayString, hashLastBlock, target;
        int jobNumber = 1;

        while (jobAvailable.get()) {
//            System.out.println("JobAvailable: " + mining.get() + " | MokFound: " + mokFound.get() + " | " + jobNumber + " | " + currentJobNumber.get());
            if (!mining.get() && !mokFound.get()) {
                JsonObject currentJob = job.get();

                jobNumber = currentJob.get("jobNumber").getAsInt();
                txsArrayString = currentJob.get("transactions").getAsString();
                transactions = gson.fromJson(txsArrayString, String[].class);
                hashLastBlock = currentJob.get("hashLastBlock").getAsString();
                target = currentJob.get("target").getAsString();
                difficulty = currentJob.get("difficulty").getAsInt();

//                System.out.println("Getting a new job number: " + jobNumber + " | " + currentJobNumber.get());
                if (jobNumber == currentJobNumber.get() && getGotNewJob()) {
                    System.out.println("jobNumber: " + jobNumber + " currentJobNumber: " + currentJobNumber.get());
                    while (numThreads < MAX_THREADS) {
                        MinerThread minerThread = new MinerThread(
                                this,
                                transactions,
                                hashLastBlock,
                                target,
                                difficulty,
                                startingNonce
                        );
                        Thread thread = new Thread(minerThread);
                        thread.start();
                        updateThreadCount(true);
                        startingNonce += 1_000_000;
                    }

                    if (numThreads == MAX_THREADS) {
                        mining.compareAndSet(false, true);
                        System.out.println("MAX THREADS REACHED: " + mining.get());
                    }

                    threadsCompleted.set(false);
                }
            } else if (numThreads < MAX_THREADS && !mokFound.get()) {
                boolean success = mining.compareAndSet(true, false);
                System.out.println("A miningThread finished running! Getting ready to mine: " + success);
            } else if (mokFound.get()) {
                if (startingNonce != 0) {
                    System.out.println("MokBlock mined or job completed! Resetting nonce..");
                    startingNonce = 0;
                }
                if (numThreads == 0) {
                    System.out.println("All threads completed! Awaiting a new job..");
                    threadsCompleted.compareAndSet(false, true);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
            } else {
                try {
                    System.out.println("Mining in progress..");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println("Usage: java Miner <host name> <port number>");
            System.err.println("Example: java Miner localhost 100");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        Miner miner = new Miner();
        miner.init(hostName, portNumber);
    }
}

class PoolHandler implements Runnable {

    String hostName;
    int portNumber;
    Miner miner;

    PoolHandler(String hostName, int portNumber, Miner miner) {
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.miner = miner;
    }

    @Override
    public void run() {
        System.out.print("Enter Miner name: ");
//        Scanner sc = new Scanner(System.in);
//        String minerName = sc.nextLine();
        String minerName = miner.toString();
        int moksToMine = 0;

        try (
                Socket minerSocket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(minerSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(minerSocket.getInputStream()));) {

            // Send minerName to MiningPool
            out.println(minerName);

//            this.moksMined.incrementAndGet();
            Gson gsonPP = new GsonBuilder().setPrettyPrinting().create();
            Gson gson = new Gson();
            JsonParser par = new JsonParser();
            JsonObject obj;
            JsonElement ele;
            String jsonObj;
            AtomicBoolean mokFound = miner.getMokFound();
            AtomicBoolean threadsCompleted = miner.getThreadsCompleted();

            System.out.println("Miner ready for work!");
            String poolInput = in.readLine();
            System.out.println("\n" + poolInput);
            poolInput = in.readLine();

            ele = par.parse(poolInput);
            obj = ele.getAsJsonObject();
            jsonObj = gsonPP.toJson(obj);
            System.out.println(jsonObj);
            poolInput = in.readLine();

            moksToMine = obj.get("totalMoksToMine").getAsInt();
            miner.setJobAvailability(true, obj.get("currentlyMining").getAsBoolean());

            // Get initial job if available
            if (miner.getJobAvailability()) {
                System.out.println("\n" + poolInput);
                poolInput = in.readLine();
                ele = par.parse(poolInput);
                obj = ele.getAsJsonObject();
                jsonObj = gsonPP.toJson(obj);

                miner.setStartingJobNumber(obj.get("jobNumber").getAsInt());
                miner.newJob(obj);
                miner.setGotNewJob(true);
                System.out.println(jsonObj + "\n*** MOKBLOCK POOL INITIAL JOB END ***\n");
                poolInput = "";
            }

            while (poolInput != null && miner.getJobAvailability()) { // will be null if server terminates 

                if (!poolInput.isEmpty()) {
                    try {
                        System.out.println("\n" + poolInput);
                        poolInput = in.readLine();
                        ele = par.parse(poolInput);
                        obj = ele.getAsJsonObject();
                        jsonObj = gsonPP.toJson(obj);

                        System.out.println(jsonObj + "\n*** MOKBLOCK POOL NEW JOB END ***\n");

                        String latestMiner = obj.get("minerLastBlock").getAsString();
                        if (latestMiner.equals(minerName)) {
                            miner.incrementMoksMined();
                        }
                        miner.newJob(obj);
                        miner.setGotNewJob(true);

                    } catch (JsonSyntaxException e) {
                        poolInput = "";
                    }
                }

                if (in.ready()) {
                    poolInput = in.readLine();
                    if (poolInput.equals("end")) {
                        System.out.println();
                        poolInput = in.readLine();
                        System.out.println(poolInput);

                        poolInput = in.readLine();
                        ele = par.parse(poolInput);
                        obj = ele.getAsJsonObject();
                        jsonObj = gsonPP.toJson(obj);
                        System.out.println(jsonObj);

                        String latestMiner = obj.get("minerLastBlock").getAsString();
                        if (latestMiner.equals(minerName)) {
                            miner.incrementMoksMined();
                        }

                        poolInput = null;
                    }
                } else if (mokFound.get()) {
                    System.out.println("Sending MokBlock to MokPool!");
                    MokBlock mok = miner.getMokBlock();
                    jsonObj = gson.toJson(mok);

                    obj = new JsonObject();
                    obj.addProperty("Miner", miner.toString());
                    obj.addProperty("MokBlock", jsonObj);

                    out.println(gson.toJson(obj));

                    boolean loop = true;
                    while (loop) {
                        if (threadsCompleted.get()) {
                            mokFound.compareAndSet(true, false);
                            miner.setGotNewJob(false);
                            loop = false;
                        }
                    }
                    poolInput = "";
                } else {
                    poolInput = "";
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }

            miner.setJobAvailability(true, false);
            System.out.println("Mining operations complete!");
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
        System.out.println("\nClosing connection! Mining Pool has successfully mined " + moksToMine + " MokBlocks"
                + "\nMokBlocks mined: " + miner.getMoksMined()
        );
    }
}
