/*
 * The MiningPool is the "brain" of the network.
 * 
 * It handles connections from Miners concurrently and delegates
 * jobs for every client connected. The MiningPool maintains a single copy 
 * of the blockchain, in this case the MokChain and ensures that all
 * miners connected to it are given the latest details and job.
 * 
 */
package aa.project.mining;

import aa.project.blockchain.MokBlock;
import aa.project.blockchain.MokChain;
import aa.project.utility.Utility;
import aa.StopWatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Martius
 */
public class MiningPool {

    private final ConcurrentHashMap<String, Integer> minerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean mining = new AtomicBoolean(true);
    private final AtomicBoolean mokFound = new AtomicBoolean();
    private final AtomicInteger moksFound = new AtomicInteger(0);
    private final int moksToMine;
    private volatile MokChain mokChain;
    private volatile String latestMiner;
    private int numMiners;

    public MiningPool(MokChain mokChain, int moksToMine) {
        this.mokChain = mokChain;
        this.moksToMine = moksToMine;
    }

    // Initializes 
    public void init(int portNumber, MiningPool pool) {
        MokChain mokChain = pool.getMokChain();
        StopWatch watch = new StopWatch();
        watch.start();

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            serverSocket.setSoTimeout(10000);

            System.out.println("\n*** MokBlock Mining Pool started! ***");
            System.out.println("> Number of MokBlocks to mine: " + pool.moksToMine);
            System.out.println("> Initial difficulty: " + mokChain.getDifficulty().get());

            while (moksFound.get() < moksToMine) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    numMiners++;
                    System.out.println("New miner accepted!");
                    Thread minerHandler = new Thread(new MinerHandler(clientSocket, pool));
                    minerHandler.start();
                    String minerStats = "\n";
                    for (String miner : minerMap.keySet()) {
                        int numMokBlocksMined = minerMap.get(miner);
                        String concat = "| " + miner + " :           " + numMokBlocksMined + "\n";
                        minerStats += concat;
                    }
                    System.out.println(
                            "> Waiting for miners to join the pool.. "
                            + "\n> MINERS CONNECTED:"
                            + "\n>      MINER NAME     :     NUMBER OF MOKBLOCKS MINED"
                            + minerStats
                            + "\n=============================="
                    );
                } catch (SocketTimeoutException e) {
                    System.out.println("\nChecking if number of MokBlocks to mine has been reached: " + (moksFound.get() == moksToMine));
                }
            }
            System.out.println("\nTarget MokBlocks reached! MokPool stopping operations..");

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }

        System.out.println(
                "\n==============================\n"
                + "\n> MOKPOOL MOKBLOCK MINING OPERATIONS REPORT:\n"
                + "\n> MOKBLOCKS TO MINE: " + moksToMine
                + "\n> MINERS CONNECTED: " + numMiners
                + "\n\n> VERIFYING MOKCHAIN INTEGRITY.."
                + "\n> MOKCHAIN VALID: " + mokChain.isChainValid()
        );

        String minerStats = "\n";
        for (String miner : minerMap.keySet()) {
            int numMokBlocksMined = minerMap.get(miner);
            String concat = "| " + miner + " :           " + numMokBlocksMined + "\n";
            minerStats += concat;
        }

        System.out.println(
                "\n\n> MOKMINER STATS:\n"
                + ">      MINER NAME     :     NUMBER OF MOKBLOCKS MINED"
                + minerStats
                + "\n=============================="
        );

        System.out.println("\nTime elapsed: " + watch.toString());

//        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
//            serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
//            serverSocketChannel.configureBlocking(false);
//            MokChain mokChain = pool.getMokChain();
//
//            System.out.println("\n*** MokBlock Mining Pool started! ***");
//            System.out.println("> Number of MokBlocks to mine: " + pool.moksToMine);
//            System.out.println("> Initial difficulty: " + mokChain.getDifficulty().get());
//            System.out.println("> Waiting for miners to join the pool.. \n");
//
//            while (moksFound.get() < moksToMine) {
//                SocketChannel clientSocketChannel = serverSocketChannel.accept();
//
//                if (clientSocketChannel != null) {
//                    Socket clientSocket = clientSocketChannel.socket();
//                    System.out.println("New miner accepted!");
//                    Thread minerHandler = new Thread(new MinerHandler(clientSocket, pool));
//                    minerHandler.start();
//                    minerMap.put(clientSocket.getLocalPort(), true);
//                } 
//            }
//            System.out.println("Success! Completed MokBlock mining operations =)");
//
//        } catch (IOException e) {
//            System.out.println("Exception caught when trying to listen on port "
//                    + portNumber + " or listening for a connection");
//            System.out.println(e.getMessage());
//        }
    }

    public ConcurrentHashMap<String, Integer> getMinerMap() {
        return minerMap;
    }

    public synchronized void updateMinerMap(String minerName, int mokBlocksMined) {
        minerMap.put(minerName, mokBlocksMined);
    }

    public AtomicBoolean getMining() {
        return mining;
    }

    public AtomicBoolean getMokFound() {
        return mokFound;
    }

    public AtomicInteger getMoksFound() {
        return moksFound;
    }

    public int getMoksToMine() {
        return moksToMine;
    }

    public MokChain getMokChain() {
        return mokChain;
    }

    public synchronized String getLatestMiner() {
        return latestMiner;
    }

    public synchronized void setLatestMiner(String latestMiner) {
        this.latestMiner = latestMiner;
    }

    public static void main(String[] args) {
        // ensures that the required arguments are passed in when starting the program
        if (args.length != 3) {
            System.err.println("Usage: java MiningPool <port number> <number of blocks to mine> <initial difficulty>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        int moksToMine = Integer.parseInt(args[1]);
        int difficulty = Integer.parseInt(args[2]);

        // initializes a mining pool and starts a thread to listen for incoming connections
        MiningPool pool = new MiningPool(new MokChain(difficulty), moksToMine);
        pool.init(portNumber, pool);
    }
}

// Thread for handling incoming clients (miners)
// Responsible for receiving blocks from the connected miner 
// Responsible for updating the connected miner with the latest job
class MinerHandler implements Runnable {

    Socket client;
    MiningPool pool;

    //So that we can identify each thread's owner
    MinerHandler(Socket client, MiningPool pool) {
        this.client = client;
        this.pool = pool;
    }

    @Override
    public void run() {

        // there are two Gson initialized here
        // gsonPP is for pretty printing for easier display to the screen
        // gson is to handle the incoming json data as gson is unable
        // to parse the pretty printing directly 
        Gson gsonPP = new GsonBuilder().setPrettyPrinting().create();
        Gson gson = new Gson();
        JsonObject obj = new JsonObject();
        JsonParser par = new JsonParser();
        JsonElement ele;
        String jsonObj;

        int moksMined = 0;
        int moksToMine = pool.getMoksToMine();
        MokChain mokChain = pool.getMokChain();
        AtomicBoolean mining = pool.getMining();
        AtomicBoolean mokFound = pool.getMokFound();
        AtomicInteger totalMoksMined = pool.getMoksFound();
        AtomicInteger difficulty = mokChain.getDifficulty();
        String minerName;

        //Each thread is owned by 1 client so we execute the following code for each client
        try (
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));) {

            // gets the name of the miner that connected
            String inputLine = in.readLine();
            minerName = inputLine;
            pool.updateMinerMap(minerName, 0);

            // sends welcome message and initial details to the miner
            String responseLine = "*** Welcome " + inputLine + " to the MokBlock Mining Pool ***";
            obj.addProperty("currentMokchainSize", mokChain.getSize());
            obj.addProperty("totalMoksToMine", moksToMine);
            obj.addProperty("currentlyMining", mining.get());
            obj.addProperty("currentDifficulty", difficulty.get());
            jsonObj = gson.toJson(obj);

            out.println(responseLine);
            out.println(jsonObj);
            inputLine = "";

            int currentJobNumber = totalMoksMined.get() + 1;
            String[] transactions;
            String target;

            // if the pool has not yet reached the target number of blocks to mine
            // send the first job to the client in json
            if (mining.get() && currentJobNumber <= moksToMine) {
                responseLine = "*** MOKPOOL MOKBLOCK MINING INITIAL JOB START ***";
                transactions = Utility.generateTransactions(1);
                target = new String(new char[difficulty.get()]).replace('\0', '0');
                obj = new JsonObject();
                obj.addProperty("jobNumber", totalMoksMined.get() + 1);
                obj.addProperty("transactions", Arrays.toString(transactions));
                obj.addProperty("hashLastBlock", mokChain.getLatestBlockHash());
                obj.addProperty("target", target);
                obj.addProperty("difficulty", difficulty.get());
                jsonObj = gson.toJson(obj);

                out.println(responseLine);
                out.println(jsonObj);
            }

            // if the pool has not yet reached the target number of blocks to mine
            // start listening for block updates from the miner
            // and also publish updates to the miner when a new block is found 
            // presumably by other miners
            while (mining.get() && inputLine != null) {  // will be null if client terminates
                // publish latest job
                if (totalMoksMined.get() == moksToMine) {
                    mining.compareAndSet(true, false);
                }

                if (mining.get()) {

                    if (totalMoksMined.get() == currentJobNumber) {
                        responseLine = "*** MOKBLOCK POOL NEW JOB START ***";
                        currentJobNumber++;

                        transactions = Utility.generateTransactions(1);
                        target = new String(new char[difficulty.get()]).replace('\0', '0');
                        obj = new JsonObject();
                        obj.addProperty("jobNumber", currentJobNumber);
                        obj.addProperty("transactions", Arrays.toString(transactions));
                        obj.addProperty("hashLastBlock", mokChain.getLatestBlockHash());
                        obj.addProperty("target", target);
                        obj.addProperty("difficulty", difficulty.get());
                        obj.addProperty("minerLastBlock", pool.getLatestMiner());
                        jsonObj = gson.toJson(obj);

                        out.println(responseLine);
                        out.println(jsonObj);
                    }

                    if (!inputLine.isEmpty()) {
                        obj = par.parse(inputLine).getAsJsonObject();
                        jsonObj = gsonPP.toJson(obj);
                        System.out.println("New MokBlock received!");
                        System.out.println(jsonObj);

                        MokBlock mokBlock = gson.fromJson(obj.get("MokBlock").getAsString(), MokBlock.class);
                        System.out.println("Attempting to add MokBlock to MokChain..");
                        if (mokChain.addBlock(mokBlock)) {
                            totalMoksMined.incrementAndGet();
                            pool.updateMinerMap(minerName, ++moksMined);
                            pool.setLatestMiner(minerName);
                            System.out.println("MokBlock successfully added! New MokChain length: " + mokChain.getSize());
                            System.out.println(
                                    "> VERIFYING MOKCHAIN INTEGRITY.."
                                    + "\n> MOKCHAIN VALID: " + mokChain.isChainValid()
                            );
                        } else {
                            System.out.println("MokBlock was not able to be added! It may have been orphaned..");
                        }
                        inputLine = "";
                    }
                }

                if (in.ready()) {
                    inputLine = in.readLine();
                } else {
                    inputLine = "";
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // when target number of blocks has been reached
            // update miner to stop mining
            // send final report to miner
            out.println("end");

            responseLine = "*** MOKPOOL MOKBLOCK MINING REPORT:";
            obj = new JsonObject();
            obj.addProperty("currentMokchainSize", mokChain.getSize());
            obj.addProperty("totalMoksToMine", moksToMine);
            obj.addProperty("currentlyMining", mining.get());
            obj.addProperty("currentDifficulty", difficulty.get());
            obj.addProperty("hashLastBlock", mokChain.getLatestBlockHash());
            obj.addProperty("minerLastBlock", pool.getLatestMiner());
            jsonObj = gson.toJson(obj);

            out.println(responseLine);
            out.println(jsonObj);

            System.out.println("Mining target reached! Closing connection with " + minerName);
        } catch (IOException e) {
            System.err.println("Exception caught: client disconnected.");
        }
    }
}
