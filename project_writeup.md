# IS303 Architectural Analysis Project Writeup
> Martius Lim 
  
## Brief description of project
`MokPool` is a cryptocurrency mining pool designed to mine SMU’s most valued crypto-asset. The project comprises two main packages, `aa.project.mining` and `aa.project.blockchain`. 

`aa.project.mining` comprises the `MiningPool` to coordinate mining efforts of many `Miners` that delegate the mining tasks to various `MiningThreads`. 

`aa.project.blockchain` comprises the `MokChain` which is a blockchain data structure that consists of `MokBlocks` chained together. 

## Justification for multi-threading
Mining (with regards to cryptocurrency) is in itself a CPU intensive activity. Mining is essentially a race between mining nodes to calculate hashes that satisfy certain requirements such as the current difficulty level of the blockchain. 

Building a mining node alone already has strong grounds for multi-threading due to the repetitive nature of the task involved. Calculating hashes is done on the order of millions and billions and this task can be delegated to threads perform the computation.

However, in many blockchains (in real life), solo-mining is practically infeasible due to the difficulty of the blockchain, requiring massive computing power. In fact, if you were a solo miner, it could take you up to 10 years or more to even successfully “mine” a block of bitcoin. As such, Mining Pools were developed to collectively pool the resources of many miners that contribute computing power to the pool. This way, if anyone in the pool mines a block, the rewards are shared across the pool according to their contribution, meaning that miners could get a portion of the rewards regularly for contributing to the pool rather than hope to gain a reward once every 10 years (on average).

## Transactional Integrity
In a Mining Pool application, there are many opportunities for race conditions, for example:
1.	Multiple miners submitting mined blocks to the pool. For the purpose of this project, the pool takes the first block that is submitted (that satisfies the blockchain requirements) and rejects the rest.
2.	MinerThreads for each Miner need to coordinate so that they do not waste unnecessary computations on older jobs. 
For (1), the method to add blocks to the blockchain is synchronized so that only one thread tries to add a block at a time.
For (2), there are AtomicBoolean variables which are checked periodically to update the thread on whether they should continue working on the current task or not.

## Performance
Performance is greatly improved if the program is started with sufficiently high difficulty (more than 5-10). However, stats are not provided due to the physical cpu core limitations that won’t be able to show the improvement on a single machine. The pool is designed for multiple users to connect to the pool to contribute their computing power. 

## Evidence of exploration
Most of the project required significant additional research. For example, building a multi-threaded server that is non-blocking (to receive new connections) and also able to take in input when received rather than waiting for it endlessly. I also explored the use of java.util.concurrent.Atomic to make use of the packages already written to ensure data consistency. It was also a struggle to transmit data across the server-client using JSON as it was far from intuitive as compared to other languages. Finally, implementing blockchain as a data structure also required extensive time due to the requirement to synchronize the chain across the threads that access and use it.

## Innovation
My project is innovative mainly due to exploring “something out of the norm” and a technology that is quite a hot buzzword nowadays. Studying how mining pools, mining, and blockchain work together has greatly increased my understanding of concurrency programming and how difficult it can be to ensure integrity and consistency.

## Adherence to coding conventions and good practices
The code is packaged properly, well indented and follows standard coding conventions.

## References / acknowledgement
- [My First Bitcoin Miner (Python)](https://github.com/philipperemy/my-first-bitcoin-miner)
- [Creating Your First Blockchain (Java)](https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa)
- [Bitcoin mining the hard way](http://www.righto.com/2014/02/bitcoin-mining-hard-way-algorithms.html
)