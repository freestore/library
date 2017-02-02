/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dynastore;


import dynastore.messages.DynaStoreMessage;
import dynastore.wso.WeakSnapshotObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import quorum.communication.system.CommunicationSystemClientSide;
import quorum.communication.system.CommunicationSystemClientSideFactory;
import quorum.communication.system.ReplyReceiver;
import quorum.core.QuorumReplica;
import quorum.view.ClientViewController;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class DynaStoreReplica extends QuorumReplica implements ReplyReceiver {

   

    private CommunicationSystemClientSide cs;

    private LinkedBlockingQueue<DynaStoreMessage> processingQueue = new LinkedBlockingQueue<DynaStoreMessage>();
    
    
    private Map<String, WeakSnapshotObject> wso = new HashMap<String, WeakSnapshotObject>();
    
    public DynaStoreReplica(int id) {
        this(id, "");
    }


    public DynaStoreReplica(int id, String configHome) {
        super(id, configHome);
        if (!configHome.equals("")) {
            this.cs = CommunicationSystemClientSideFactory.getCommunicationSystemClientSide(this.id, new ClientViewController(id));
        } else {
            this.cs = CommunicationSystemClientSideFactory.getCommunicationSystemClientSide(this.id, new ClientViewController(id, configHome));
        }
        this.cs.setReplyReceiver(this);
        
        
        
        int t = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < t; i++) {
            new Worker(processingQueue).start();
        }
        if (!this.SVController.getCurrentView().isMember(id)) {
            
            
            join(); //aguardar pelo join
            
            
            
        }
       
       /* (new Timer()).scheduleAtFixedRate(new TimerTask() {
         public void run() {
         if(id != 1){
         System.out.println("vai enviar o leave");
         leave();
          }
         }
         }, recPeriod*2, recPeriod*50);*/
    }

  /*  public abstract int getTimestamp();

    public abstract Object getValue();

    public abstract void updateState(int timestamp, Object value);

    public abstract QuorumMessage executeProtocolRequest(QuorumMessage msg);*/

    
    /**
     * The join method (used in the constructor when a replica is not in the
     * current view)
     */
    public void join() {
        String host = "";
        int port = 12000;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
            boolean free = false;
            ServerSocket ss = null;
            do {
                try {
                    ss = new ServerSocket(port);
                    free = true;
                } catch (IOException ex) {
                    port++;
                } finally {
                    try {
                        if (ss != null) {
                            ss.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } while (!free);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
        this.init(host, port);
        
        //enviar o join
    }

    /**
     * The leave method
     */
    public void leave() {
        
    }

    /**
     * Called by the netty system to execute a client operation
     *
     * @param msg
     * @return
     */
    @Override
    public QuorumMessage executeRequest(QuorumMessage msg) {
        //ver se é read ou write (precisa de locks?)
        
        return null;
    }

    /**
     * Called by the netty system to execute a reconfiguration request
     *
     * @param msg
     * @return
     */
    @Override
    public QuorumMessage executeReconfigurationMessage(QuorumMessage msg) {//Processamento de Mensagens de Reconfiguração do FreeStore

        DynaStoreMessage recMsg = (DynaStoreMessage) msg.getMsg();
        /*if (recMsg.type == FreeStoreMessageType.RECONFIG) {
            //System.out.println("recebeu reconfig");
            if (this.getSVController().getCurrentView().equalsByHash(recMsg.view.getHash())) {//view up-to-date
                // System.out.println("vai pegar o lock");
                recvLock.lock();
                // System.out.println("passou pelo lock");
                if (!this.getSVController().getCurrentView().containsUpdate(Integer.parseInt(recMsg.getValue().toString()))) {

                    if (!recv.contains((RECVMessage) recMsg)) {
                        // System.out.println("Adicionou "+recMsg.value);
                        recv.add((RECVMessage) recMsg);
                    }//else{
                    // System.out.println("Não adicionou "+recMsg.value);
                    //}
                }//else{
                // System.out.println("Não adicionou pq current view contém update "+recMsg.value);
                // }
                recvLock.unlock();
                return new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE,
                        new FreeStoreMessage(FreeStoreMessageType.REC_CONFIRM, null, this.getSVController().getCurrentView()), id);
            } else {//view outdated
                //recvLock.unlock();
                // System.out.println("visão desatualizada");
                return new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE,
                        new FreeStoreMessage(FreeStoreMessageType.REC_CONFIRM, this.getSVController().getCurrentView(), null), id);
            }
        } else {
            try {
                //System.out.println("recebeu uma msg do tipo " + msg.getType() + " from: " + msg.getSender());
                this.reconfigQueue.put(recMsg);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            return null;
        }*/
        return null;
    }

    /**
     * Called by the client netty system to inform that a reply (from a
     * join/leave request) was received
     *
     * @param reply
     */
    @Override
    public void replyReceived(QuorumMessage reply) {//Processamento de respostas para Join e Leave
        //será que precisa???
    }
    
   


    /**
     * Aux method to multicast a message to servers using the client
     * communication system
     *
     * @param sm
     * @param servers
     */
    public void multicast(QuorumMessage sm, int[] servers, View v) {
        cs.send(servers, sm, v);
    }

    //Falta: remover as reconfigurações passadas (testar pela current view quando da instalação)
    private WeakSnapshotObject getVG(View view) {
        WeakSnapshotObject ret = this.wso.get(view.toString());
        if (ret == null) {
            ret = new WeakSnapshotObject(view, this);
            this.wso.put(view.toString(), ret);
        }
        return ret;
    }


    /**
     * Pool of threads used to process msgs
     */
    private class Worker extends Thread {

        private LinkedBlockingQueue<DynaStoreMessage> requests;

        public Worker(LinkedBlockingQueue<DynaStoreMessage> requests) {
            this.requests = requests;
        }

        public void run() {
            DynaStoreMessage msg = null;
            while (true) {
                try {
                    msg = this.requests.take();
                    

                } catch (Exception ie) {
                    ie.printStackTrace();
                    continue;
                }
            }
        }
    }

   

}
