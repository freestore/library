/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore;

import freestore.messages.FreeStoreMessage;
import freestore.messages.FreeStoreMessageType;
import freestore.messages.InstallSeqMessage;
import freestore.messages.RECVMessage;
import freestore.messages.StateUpdateMessage;
import freestore.viewgenerators.LiveViewGenerator;
import freestore.viewgenerators.SafeViewGenerator;
import freestore.viewgenerators.ViewGenerator;
import freestore.viewgenerators.ViewGeneratorType;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
public abstract class FreeStoreReplica extends QuorumReplica implements ReplyReceiver {

    private List<RECVMessage> recv = new LinkedList<RECVMessage>();

    private CommunicationSystemClientSide cs;

    ///FALTA LIMPAR DADOS ANTIGOS DESTAS ESTRUTURAS
    private Map<String, List<StateUpdateMessage>> stateMsgs = new HashMap<String, List<StateUpdateMessage>>();
    private Map<String, List<FreeStoreMessage>> viewUpdatedMsgs = new HashMap<String, List<FreeStoreMessage>>();
    private Map<String, ViewGenerator> vg = new HashMap<String, ViewGenerator>();
    private List<InstallSeqMessage> installed = new LinkedList<InstallSeqMessage>();
    ///////////////////////////////////////////////////

    private List<FreeStoreMessage> responses = new LinkedList<FreeStoreMessage>();

    private boolean waitingRecConfirm = false;

    private final ReentrantLock canExecuteLock = new ReentrantLock();
    private final ReentrantLock replyLock = new ReentrantLock();
    private final ReentrantLock recvLock = new ReentrantLock();

    private Semaphore clientOperationLock = new Semaphore(1);
    private boolean alreadyLocked = false;
    private Semaphore joinLeave = new Semaphore(0);

    //Queues used to store requests to be processed by a pool of threads
    private LinkedBlockingQueue<FreeStoreMessage> reconfigQueue = new LinkedBlockingQueue<FreeStoreMessage>();
    private LinkedBlockingQueue<QuorumMessage> clientOperationsQueue = new LinkedBlockingQueue<QuorumMessage>();

    
    private ViewGeneratorType vgType;
    
    public FreeStoreReplica(int id, ViewGeneratorType type) {
        this(id, "", 10000, type);
    }

    public FreeStoreReplica(int id, long recPeriod, ViewGeneratorType type) {
        this(id, "", recPeriod, type);
    }

    public FreeStoreReplica(int id, String configHome, long recPeriod, ViewGeneratorType type) {
        super(id, configHome);
        if (!configHome.equals("")) {
            this.cs = CommunicationSystemClientSideFactory.getCommunicationSystemClientSide(this.id, new ClientViewController(id));
        } else {
            this.cs = CommunicationSystemClientSideFactory.getCommunicationSystemClientSide(this.id, new ClientViewController(id, configHome));
        }
        this.cs.setReplyReceiver(this);
        
        this.vgType = type;
        
        int t = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < t; i++) {
            new ReconfigurationWorker(reconfigQueue).start();
            new ClientOperationWorker(clientOperationsQueue).start();
        }
        if (!this.SVController.getCurrentView().isMember(id)) {
            try {
                this.clientOperationLock.acquire(); //disable the execution of client operations
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            join();
            do {
                try {
                    this.joinLeave.acquire(); //wait until the reconfiguration occurs
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } while (!this.SVController.getCurrentView().isMember(id));
        }
        (new Timer()).scheduleAtFixedRate(new TimerTask() {
            public void run() {
                startReconfiguration();
            }
        }, recPeriod, recPeriod);

        /*(new Timer()).scheduleAtFixedRate(new TimerTask() {
         public void run() {
         if(id != 1){
         System.out.println("vai enviar o leave");
         leave();
          }
         }
         }, recPeriod*2, recPeriod*50);*/
    }

    public abstract int getTimestamp();

    public abstract Object getValue();

    public abstract void updateState(int timestamp, Object value);

    public abstract QuorumMessage executeProtocolRequest(QuorumMessage msg);

    
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
        waitingRecConfirm = true;
        do {
            QuorumMessage joinRequest = new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE,
                    new RECVMessage(new InetSocketAddress(host, port),
                            FreeStoreMessageType.RECONFIG, id, this.getSVController().getCurrentView()), id);
            responses.clear();
            multicast(joinRequest, this.cs.getController().getCurrentView().getMembership(), this.cs.getController().getCurrentView());
            try {
                if (!this.joinLeave.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                    System.out.println("TIMEOUT FOR THE JOIN REQUEST");
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        } while (responses.size() < this.cs.getController().getCurrentView().getQuorum());
    }

    /**
     * The leave method
     */
    public void leave() {
        waitingRecConfirm = true;
        do {
            QuorumMessage leaveRequest = new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE,
                    new RECVMessage(null, FreeStoreMessageType.RECONFIG, -id, this.getSVController().getCurrentView()), id);
            responses.clear();
            multicast(leaveRequest, this.cs.getController().getCurrentView().getMembership(), this.cs.getController().getCurrentView());
            try {
                if (!this.joinLeave.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                    System.out.println("TIMEOUT FOR THE LEAVE REQUEST");
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        } while (responses.size() < cs.getController().getCurrentView().getQuorum());
    }

    /**
     * Called by the netty system to execute a client operation
     *
     * @param msg
     * @return
     */
    @Override
    public QuorumMessage executeRequest(QuorumMessage msg) {
        try {
            this.clientOperationsQueue.put(msg);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
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

        FreeStoreMessage recMsg = (FreeStoreMessage) msg.getMsg();
        if (recMsg.type == FreeStoreMessageType.RECONFIG) {
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
        }
    }

    /**
     * Called by the client netty system to inform that a reply (from a
     * join/leave request) was received
     *
     * @param reply
     */
    @Override
    public void replyReceived(QuorumMessage reply) {//Processamento de respostas para Join e Leave
        replyLock.lock();
        FreeStoreMessage recMsg = (FreeStoreMessage) reply.getMsg();
        if (recMsg.type == FreeStoreMessageType.REC_CONFIRM && waitingRecConfirm) {//conta mais um rec confirm como nas operações de clientes
            if (recMsg.getValue() != null) { //view is outdated
                View v = (View) recMsg.getValue();
                this.SVController.reconfigureTo(v);
                cs.getController().reconfigureTo(v);
                responses.clear(); //sinaliza a reexecução da fase
                this.joinLeave.release();
            } else { // view is the most up-to-date
                responses.add(recMsg);
                if (responses.size() == cs.getController().getCurrentView().getQuorum()) {
                    waitingRecConfirm = false;
                    this.joinLeave.release();
                }
            }
        }
        replyLock.unlock();
    }
    
    
    /**
     * Periodically, starts a reconfiguration periodically if there are updates
     * to be processed
     */
    private void startReconfiguration() {
        recvLock.lock();
        if (recv.size() > 0) { //começar uma reconfiguração para current view
            //System.out.println("Vai iniciar uma reconfiguração: " + recv.get(0));
            int[] updates = new int[getSVController().getCurrentView().getUpdates().length + recv.size()];
            for (int i = 0; i < updates.length; i++) {
                if (i < getSVController().getCurrentView().getUpdates().length) {
                    updates[i] = getSVController().getCurrentView().getUpdates()[i];
                } else {
                    updates[i] = Integer.parseInt(recv.get(i - getSVController().getCurrentView().getUpdates().length).value.toString());
                }
            }
            View vp = new View(updates, null);
            Map<Integer, InetSocketAddress> addr = new HashMap<Integer, InetSocketAddress>();
            for (int i = 0; i < vp.getMembership().length; i++) {
                if (getSVController().getCurrentView().isMember(vp.getMembership()[i])) {
                    addr.put(vp.getMembership()[i], getSVController().getCurrentView().getAddress(vp.getMembership()[i]));
                } else {//esta em rcev
                    for (int j = 0; j < recv.size(); j++) {
                        if (Integer.parseInt(recv.get(j).value.toString()) == vp.getMembership()[i]) {
                            addr.put(vp.getMembership()[i], recv.get(j).addr);
                        }
                    }
                }
            }
            vp.setAddresses(addr);
            List<View> proposal = new LinkedList<View>();
            proposal.add(vp);
            //System.out.println("Vai propor view " + proposal.get(0));
            this.getVG(SVController.getCurrentView()).generateView(proposal);

        } else {
            System.out.println("NAO Vai iniciar uma reconfiguração.");
        }
        recvLock.unlock();
    }

    /**
     * A callback from the view generator to inform that a sequence was
     * generated
     *
     * @param seq
     * @param associatedView
     */
    public void newView(List<View> seq, View associatedView) {
        View w = getLeastUpdated(seq);
        StringBuffer b = new StringBuffer("");
        for (int i = 0; i < seq.size() - 1; i++) {
            b.append(seq.get(i) + ",");
        }
        b.append(seq.get(seq.size() - 1));
        System.out.println("Sequencia gerada: " + b.toString());
        InstallSeqMessage inst = new InstallSeqMessage(w, FreeStoreMessageType.INSTALL_SEQ, seq, associatedView);
        QuorumMessage qm = new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, inst, id);
        this.cs.send(associatedView.getMembership(), qm, associatedView);
    }


    /**
     * Processing of INSTALL_SEQ message
     *
     * @param m
     */
    private void installSeq(FreeStoreMessage m) {//execução da reconfiguração propriamente dita!!! Não estou implementando o R_Multicast!!! Tem que verificar se ja enviou e cano não enviou enviar também antes de executar
        InstallSeqMessage msg = (InstallSeqMessage) m;
        if (!alreadyInstalled(msg)) {
            this.installed.add(msg);
            View ov = msg.view;
            View w = msg.getW();
            System.out.println("Installing view " + w + " to update " + ov);
            if (ov.isMember(id)) {
                if (w.isMostUpToDateThan(this.SVController.getCurrentView())) {
                    
                    if (!alreadyLocked) {
                        alreadyLocked = true;
                        try {
                            //System.out.println("vai bloquear"); //usar um atomicboolean para ver se já esta locked
                            this.clientOperationLock.acquire();
                            //System.out.println("passou");
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                List<View> genSeq = (List<View>) msg.getValue();
                StateUpdateMessage su = null;
                if (genSeq.size() > 1) { //there are more views to propose
                    genSeq.remove(w);
                    su = new StateUpdateMessage(recv, getTimestamp(), FreeStoreMessageType.STATE_UPDATE, getValue(), ov, w, genSeq);
                } else {
                    su = new StateUpdateMessage(recv, getTimestamp(), FreeStoreMessageType.STATE_UPDATE, getValue(), ov, w, null);
                }
                if (w.getMembership().length > 0) {
                    this.cs.connectToView(w);
                    this.cs.send(w.getMembership(),
                            new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, su, id), w);//enviar state_update para servidores em w
                } else {//Última réplica sainda (não precisa fazer mais nada)
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Processing of STATE_UPDATE message
     *
     * @param msg
     */
    private void stateUpdateMessage(FreeStoreMessage msg) {
        StateUpdateMessage sum = (StateUpdateMessage) msg;
        View ov = sum.view;
        List<StateUpdateMessage> msgs = this.getStateMessages(ov);
        //System.out.println(sum.w+" SM: " + sum.view + " TM: " + msgs.size());
        int count = 1;
        for (int i = 0; i < msgs.size(); i++) {
            if (msgs.get(i).w.equals(sum.w)) {
                count++;
            }
        }
        msgs.add(sum);
        if (count == sum.view.getQuorum()) {
            // System.out.println("ENTROU SM: " + sum.view + " TM: " + msgs.size());
            if (sum.w.isMostUpToDateThan(this.SVController.getCurrentView())) {
                if (sum.w.isMember(id)) {
                    //System.out.println("UPDATE STATE SM: " + sum.view + " TM: " + msgs.size());
                    updateState(getStateMessages(ov), sum.w);//atualiza value, timestamp e RECV
                    this.SVController.reconfigureTo(sum.w);
                    cs.getController().reconfigureTo(sum.w);
                    if (!ov.isMember(id)) { //esperando o processamento do JOIN (da pra fazer diferente tb)
                        joinLeave.release();//enable operations? libera a thread que criou e executou join!!!
                    }

                    FreeStoreMessage wu = new FreeStoreMessage(FreeStoreMessageType.VIEW_UPDATED, null, sum.w);//enviar view_updated
                    for (int i = 0; i < ov.getMembership().length; i++) {
                        if (!sum.w.isMember(ov.getMembership()[i])) { //leave
                            //System.out.println("ENVIANDO VIEW UPDATED SM: " + sum.view + " TM: " + msgs.size());
                            this.cs.send(new int[]{ov.getMembership()[i]}, new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, wu, id), ov);
                        }
                    }
                    List<View> newProposal = new LinkedList<View>();
                    if (sum.generatedSeq != null) {
                        for (int i = 0; i < sum.generatedSeq.size(); i++) {
                            if (sum.generatedSeq.get(i).isMostUpToDateThan(sum.w)) {
                                newProposal.add(sum.generatedSeq.get(i));
                            }
                        }
                    }
                    if (newProposal.size() > 0) {
                        //System.out.println(sum.w+" propondo novamente");
                        this.getVG(sum.w).generateView(newProposal);
                    } else {
                        //System.out.println(sum.w+" liberando lock"); 
                        boolean reconfigInProcess = false;
                        for(int i = 0; i < this.installed.size();i++){
                            if(this.installed.get(i).getW().isMostUpToDateThan(sum.w)){
                                reconfigInProcess = true;
                            }
                        }
                        if(!reconfigInProcess){
                            this.alreadyLocked = false;
                            this.clientOperationLock.release(); //unlok the execution of client operations
                        }
                    }
                }
            }
        }
    }

    /**
     * A method to update the replica's state (register value and timestamp) and
     * recv
     *
     * @param msgs
     * @param w
     */
    private void updateState(List<StateUpdateMessage> msgs, View w) {
        StateUpdateMessage max = msgs.get(0);
        for (int i = 1; i < msgs.size(); i++) {
            if (msgs.get(i).timestamp > max.timestamp) {
                max = msgs.get(i);
            }
        }
        this.updateState(max.timestamp, max.getValue());
        List<RECVMessage> newUpdates = new LinkedList<RECVMessage>();
        for (int i = 0; i < msgs.size(); i++) {
            for (int j = 0; j < msgs.get(i).rec.size(); j++) {
                if (!w.containsUpdate(Integer.parseInt(msgs.get(i).rec.get(j).value.toString()))) {
                    if (!newUpdates.contains(msgs.get(i).rec.get(j))) {
                        newUpdates.add(msgs.get(i).rec.get(j));
                    }
                }
            }

        }
        recvLock.lock();
        for (int j = 0; j < recv.size(); j++) {
            if (!w.containsUpdate(Integer.parseInt(recv.get(j).value.toString()))) {
                if (!newUpdates.contains(recv.get(j))) {
                    newUpdates.add(recv.get(j));
                }
            }
        }
        this.recv = newUpdates;
        recvLock.unlock();
    }

    /**
     * Processing of VIEW_UPDATED message
     *
     * @param msg
     */
    private void viewUpdatedMessage(FreeStoreMessage msg) {
        List<FreeStoreMessage> msgs = getVUMessages(msg.view);
        msgs.add(msg);
        if (msgs.size() == msg.view.getQuorum()) {
            //HALT
            this.cs.close();

            System.exit(0);
        }
    }

    /**
     * Aux method to verify if a INSTALL_SEQ message already was processed
     *
     * @param m
     * @return
     */
    private boolean alreadyInstalled(InstallSeqMessage m) {
        for (int i = 0; i < this.installed.size(); i++) {
            if (this.installed.get(i).equals(m)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aux method to get the least updated view of some sequence
     *
     * @param s
     * @return
     */
    private View getLeastUpdated(List<View> s) {
        View ret = s.get(0);
        for (int i = 1; i < s.size(); i++) {
            if (ret.isMostUpToDateThan(s.get(i))) {
                ret = s.get(i);
            }
        }

        return ret;
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
    private ViewGenerator getVG(View view) {
        ViewGenerator ret = this.vg.get(view.toString());
        if (ret == null) {
            if(this.vgType == ViewGeneratorType.LIVE){
                ret = new LiveViewGenerator(view, this);
            }else if (this.vgType == ViewGeneratorType.SAFE){
                ret = new SafeViewGenerator(view, this);
            }
            this.vg.put(view.toString(), ret);
        }
        return ret;
    }

    //Falta: remover as das visões passadas (depois de instalar uma visão)
    private List<StateUpdateMessage> getStateMessages(View view) {
        List<StateUpdateMessage> ret = this.stateMsgs.get(view.toString());
        if (ret == null) {
            ret = new LinkedList<StateUpdateMessage>();
            this.stateMsgs.put(view.toString(), ret);
        }
        return ret;
    }

    //Falta: remover as das visões passadas (depois de instalar uma visão)
    private List<FreeStoreMessage> getVUMessages(View view) {
        List<FreeStoreMessage> ret = this.viewUpdatedMsgs.get(view.toString());
        if (ret == null) {
            ret = new LinkedList<FreeStoreMessage>();
            this.viewUpdatedMsgs.put(view.toString(), ret);
        }
        return ret;
    }

    /**
     * Pool of threads used to process reconfiguration requests
     */
    private class ReconfigurationWorker extends Thread {

        private LinkedBlockingQueue<FreeStoreMessage> requests;

        public ReconfigurationWorker(LinkedBlockingQueue<FreeStoreMessage> requests) {
            this.requests = requests;
        }

        public void run() {
            FreeStoreMessage msg = null;
            while (true) {
                try {
                    msg = this.requests.take();
                    //System.out.println("recebeu uma msg do tipo " + msg.type + " view: " + msg.view);
                    if (msg.type == FreeStoreMessageType.INSTALL_SEQ) {
                        canExecuteLock.lock();
                        installSeq(msg);
                        canExecuteLock.unlock();
                    } /*else if (msg.type == FreeStoreMessageType.L_SEQ_CONV || msg.type == FreeStoreMessageType.L_SEQ_VIEW ||
                            msg.type == FreeStoreMessageType.S_ACCEPT || msg.type == FreeStoreMessageType.S_PREPARE ||
                            msg.type == FreeStoreMessageType.S_PREPARE_REPLY) {
                        getVG(msg.view).execMessage(msg);
                    } */else if (msg.type == FreeStoreMessageType.STATE_UPDATE) {
                        canExecuteLock.lock();
                        stateUpdateMessage(msg);
                        canExecuteLock.unlock();
                    } else if (msg.type == FreeStoreMessageType.VIEW_UPDATED) {
                        canExecuteLock.lock();
                        viewUpdatedMessage(msg);
                        canExecuteLock.unlock();
                    } else {
                        //System.out.println("MENSAGEM DE TIPO NENHUM!!!");
                        getVG(msg.view).execMessage(msg);
                    }

                } catch (Exception ie) {
                    ie.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * Pool of threads used to process client operations
     */
    private class ClientOperationWorker extends Thread {

        private LinkedBlockingQueue<QuorumMessage> requests;

        public ClientOperationWorker(LinkedBlockingQueue<QuorumMessage> requests) {
            this.requests = requests;
        }

        public void run() {
            QuorumMessage msg = null;
            while (true) {
                try {
                    msg = this.requests.take();
                    clientOperationLock.acquire();
                    QuorumMessage resp = executeProtocolRequest(msg);
                    clientOperationLock.release();
                    sendReply(new int[]{msg.getSender()}, resp);
                } catch (Exception ie) {
                    ie.printStackTrace();
                    continue;
                }
            }
        }
    }

}
