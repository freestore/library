FreeStore V0
----------

This package contains the FreeStore source code (src/), binary file (build/ and dist/), libraries needed (lib/), running scripts (run.sh, runABDFreeStoreServer.sh and runABDFreeStoreClientMicrobenckmarks.sh), netbeans project configuration (nbproject/) and configuration files (config/).


------ FreeStore Overview-----------------------

Quorum systems are useful tools for implementing consistent and available storage in the presence of failures. These systems usually comprise a static set of servers that provide a fault-tolerant read/write register accessed by a set of clients. 

FreeStore is a dynamic variant of these systems and consists of a set of fault-tolerant protocols that emulates a register in dynamic asynchronous systems in which processes are able to join/leave the servers set at runtime. These protocols use a new abstraction called view generators, that captures the agreement requirements of reconfiguration and can be implemented in different system models with different properties, i.e., Freestore protocols are tunable to execute consensus-based or consensus-free reconfigurations. Consequently, the reconfiguration protocol that is modular, efficient, tunable (consensus-free or consesus-based) and loosely coupled with read/write protocols, improving the overall system performance.


------ Implementing/Adapting a static quorum system protocol to dynamic execution -----------------------

FreeStore reconfiguration protocols work with any existing static quorum system protocol, i.e., it could be used to adapt these protocols for dynamic execution. In the current version, we provide the implementation/adaptation of the static ABD quorum system protocol (src/freestore/abd/).

  - Server-side: it is necessary to extend the freestore.FreeStoreReplica class and to supply the replica id and the type of view generator to be used (the safe consensus-based or the live consensus-free).
  - Client-side: it is necessary to extend the quorum.core.QuorumSender class (suplies the methods to multicast a message and to receive the replies from the servers in the current view) and to implement the  quorum.QuorumSystem interface (suplies the methods to write and read the register value that must be implemented according with the static protocol).

------ How to run FreeStore -----------------------


To run any demonstration you first need to configure FreeStore to define the protocol behavior and the location of each replica.

1.) The servers of the initial view must be specified in the configuration file (see 'config/hosts.config').

Important tip #1: Always provide IP addresses instead of hostnames. If a machine running a replica is not correctly configured, FreeStore may fail to obtain the proper IP address and use the loopback address instead (127.0.0.1). This phenomenom may prevent clients and/or replicas from successfully establishing a connection among them.

2.) The system initial view must be specified in the file 'config/system.config'. A server that is not in the current view will ask for a join in the system. Afterward, a server could leave the system by executing the method "leave" provided by the freestore.FreeStoreReplica class that it must extends. 

Important tip #2: Clients requests should not be issued before all replicas in the initial view have been properly initialized (or at least a quorum of them). 

You can run the microbenckmarks demonstration by executing the following commands, from within the main folder:

- Start the servers (3 replicas to tolerate up to 1 crash failure)
  - For consensus-based reconfigurations:
    - ./runABDFreeStoreServer.sh 1 
    - ./runABDFreeStoreServer.sh 2
    - ./runABDFreeStoreServer.sh 3
   - For consensus-free reconfigurations:
    - ./runABDFreeStoreServer.sh 1 live
    - ./runABDFreeStoreServer.sh 2 live
    - ./runABDFreeStoreServer.sh 3 live
    
- Start the client
  - ./runABDFreeStoreClientMicrobenckmarks.sh <num. threads/clients> <init process id> <number of operations> <value size> <interval>
      - e.g.: ./runABDFreeStoreClientMicrobenckmarks.sh 5 7001 10 50 0 will start 5 clients (7001, 7002, 7003, 7004 and 7005) that will execute 5 operations to write/read (the client 7001 executes write operations while the others read the value from the register) a value of 50 bytes from the registers without any delay between the invocations.




------ Additional information and publications ------

  - Papers describing FreeStore protocols: 
      - https://arxiv.org/pdf/1607.05344 (published at Arxiv 2016)
      - http://link.springer.com/chapter/10.1007%2F978-3-642-33651-5_49 (Brief Announcement at DISC 2012)
      - http://ieeexplore.ieee.org/document/6927130/ (portuguese -- published at SBRC 2014)
   
  - Papers describing Mateus Braga GoLang implementation of FreeStore: 
      - http://sbrc2014.ufsc.br/anais/files/wtf/ST2-4.pdf (portuguese -- published at WTF 2014)
      - http://bdm.unb.br/bitstream/10483/8130/1/2014_MateusAntunesBraga.pdf (portuguese -- Mateus Braga's Bachelor's thesis 2014)


------ Other Implementations ------

  - Mateus Braga GoLang implementation of FreeStore: https://github.com/mateusbraga/freestore 

Feel free to contact us if you have any questions!
