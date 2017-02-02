/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import abd.ABDClient;
import freestore.abd.ABDFreeStoreClient;
import quorum.QuorumSystem;

/**
 *
 * @author eduardo
 */
public class Microbenckmarks {
    
    public static int initId = 0;
    
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: ... Microbenckmark <num. threads> <process id> <number of operations> <value size> <interval>");
            System.exit(-1);
        }

        int numThreads = Integer.parseInt(args[0]);
        initId = Integer.parseInt(args[1]);

        int numberOfOps = Integer.parseInt(args[2]);
        int valueSize = Integer.parseInt(args[3]);
        int interval = Integer.parseInt(args[4]);
        
        Client[] c = new Client[numThreads];
        
        for(int i=0; i<numThreads; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
               ex.printStackTrace();
            }
            
            System.out.println("Launching client " + (initId+i));
            c[i] = new Client(initId+i,numberOfOps,valueSize,interval);
            //c[i].start();
        }

        for(int i=0; i<numThreads; i++) {

            
            c[i].start();
        }
        
        
        for(int i=0; i<numThreads; i++) {

            try {
                c[i].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }

        
        //System.exit(0);
        
        
       
        
        
    }
    
    
     private static class Client extends Thread {

        int id;
        int numberOfOps;
        int valueSize;
        int interval;
        QuorumSystem quorum;
        byte[] request;
        
        public Client(int id, int numberOfOps, int valueSize, int interval) {
            super("Client "+id);
        
            this.id = id;
            this.numberOfOps = numberOfOps;
            this.valueSize = valueSize;
            this.interval = interval;
            //this.quorum = new ABDClient(id);
            this.quorum = new ABDFreeStoreClient(id);
            
            this.request = new byte[this.valueSize];
            
        }

        public void run() {
            System.out.println("Warm up...");
            for (int i = 0; i < numberOfOps / 2; i++) {
            
            }

            Storage st = new Storage(numberOfOps / 2);

            System.out.println("Executing experiment for " + numberOfOps / 2 + " ops");

            for (int i = 0; i < numberOfOps / 2; i++) {
                long last_send_instant = System.nanoTime();
                
                if(this.id == initId){
                    quorum.write(i+id);
                }else{
                    // System.out.println("Vai ler");
                    quorum.read();
                }
                
                st.store(System.nanoTime() - last_send_instant);

                if (interval > 0) {
                    try {
                        //sleeps interval ms before sending next request
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                    }
                }
                                
                
            }

            if(id == initId) {
                System.out.println(this.id + " // Average time for " + numberOfOps / 2 + " executions (-10%) = " + st.getAverage(true) / 1000 + " us ");
                System.out.println(this.id + " // Standard desviation for " + numberOfOps / 2 + " executions (-10%) = " + st.getDP(true) / 1000 + " us ");
                System.out.println(this.id + " // Average time for " + numberOfOps / 2 + " executions (all samples) = " + st.getAverage(false) / 1000 + " us ");
                System.out.println(this.id + " // Standard desviation for " + numberOfOps / 2 + " executions (all samples) = " + st.getDP(false) / 1000 + " us ");
                System.out.println(this.id + " // Maximum time for " + numberOfOps / 2 + " executions (all samples) = " + st.getMax(false) / 1000 + " us ");
                System.out.println("Read value: "+quorum.read());
            }
            
            //proxy.close();
        }
    }
    
}
