# MokCoin Mining Suite
This is my project submission for IS303 Architectural Analysis. The goal of the project was to explore concurrent programming in Java. This is a toy project for educational purposes and simplifies real-world cryptocurrency mining software.

## Instructions
The project contains 4 main batch scripts

1. compile.bat (compiles all files)
2. run.bat (runs both compile.bat then run_server.bat)
3. run_server.bat (runs the server with certain arguments)
4. run_client.bat (runs the client with certain arguments)

#### run_server.bat
Takes in 3 arguments  
  1. port number
  2. Number of MokBlocks to mine (can be any number you want, it's just the target limit for the mining activity)
  3. Difficulty of mining (don't set this above 8 - 10 as it may take too long to mine the blocks)
	
#### run_client.bat  
Takes in 2 arguments  
  1. hostname of server
  2. port number of server 

### Running the Mining Pool  
  1. Open a powershell/cmd window in the project directory for the server
  2. Run `run.bat` (compiles all files and starts the server)
  3. Open as many powershell/cmd windows for as many clients as you want
  4. Run `run_client.bat` for every client window
  5. Wait until all blocks have been mined

If it takes too long to mine blocks then you may want to lower the `difficulty`. The `difficulty` refers to how long it takes to find a hash that satisfies the mining requirement that is the hash needs to start with *difficulty* number of zeroes.
