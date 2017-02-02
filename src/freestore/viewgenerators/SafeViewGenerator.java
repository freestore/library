/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.viewgenerators;

import freestore.FreeStoreReplica;
import freestore.messages.FreeStoreMessage;
import freestore.messages.FreeStoreMessageType;
import freestore.messages.PaxosMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class SafeViewGenerator implements ViewGenerator{

    
    private FreeStoreReplica replica;
    private int leader = 1; //a réplica 1 sempre é a lider (não foi implementado os casos de falha do lider)
    private View v; //associated view
    private ReentrantLock lock = new ReentrantLock();
    private PaxosMessage lastPrepared = null; 
    
    private List<PaxosMessage> prepareReplies = new LinkedList<PaxosMessage>();
    private boolean decided = false;
    
    public SafeViewGenerator(View associatedView, FreeStoreReplica replica) {
        this.v = associatedView;
        this.replica = replica;
        System.out.println("******************* Criou um novo SAFE gerador **************** "+associatedView.getQuorum());
    }
    
    @Override
    public void generateView(List<View> proposal) {
        if(replica.getId() == leader){
            System.out.println("Vai propor view " + proposal.get(0));
            boolean updated = true;
            for(int i = 0; i < proposal.size();i++){
                if(v.isMostUpToDateThan(proposal.get(i))){
                    updated = false;
                }
            }
            if(updated){
                PaxosMessage prepare = new PaxosMessage(FreeStoreMessageType.S_PREPARE, proposal, v, 0);
                //System.out.println("Vai fazer o multicast para "+v);
                this.replica.multicast(new QuorumMessage(
                        MessageType.RECONFIGURATION_MESSAGE, prepare, replica.getId()), v.getMembership(),v);//enviar prepare
                
            }
        }
    }

    

    @Override
    public View getAssociatedView() {
        return this.v;
    }

    
    
    @Override
    public void execMessage(FreeStoreMessage msg) {
        lock.lock();
        PaxosMessage m = (PaxosMessage) msg;
        if(m.type == FreeStoreMessageType.S_PREPARE){
            if(lastPrepared == null || m.n > lastPrepared.n){
                lastPrepared = m;
            }
            PaxosMessage prepareReply = new PaxosMessage(FreeStoreMessageType.S_PREPARE_REPLY, lastPrepared.value, lastPrepared.view, lastPrepared.n);
            this.replica.multicast(new QuorumMessage(
                        MessageType.RECONFIGURATION_MESSAGE, prepareReply, replica.getId()), v.getMembership(),v); //prepare_reply
        }else if(m.type == FreeStoreMessageType.S_PREPARE_REPLY){
            this.prepareReplies.add(m);
            if(this.prepareReplies.size() == v.getQuorum()){
                //preparar e enviar o accept
                PaxosMessage max = this.prepareReplies.get(0);
                for(int i = 1; i < this.prepareReplies.size(); i++){
                    if(max.n < this.prepareReplies.get(i).n){
                        max = this.prepareReplies.get(i);
                    }
                }
                
                PaxosMessage accept = new PaxosMessage(FreeStoreMessageType.S_ACCEPT, max.value, v, max.n);
                //System.out.println("Vai fazer o multicast para "+v);
                this.replica.multicast(new QuorumMessage(
                        MessageType.RECONFIGURATION_MESSAGE, accept, replica.getId()), v.getMembership(),v);//enviar prepare
                
                
            }
            
        }else if(m.type == FreeStoreMessageType.S_ACCEPT){
            if(!decided){
                decided = true;
                this.replica.newView((List<View>) m.getValue(), v);
            }
        }
        lock.unlock();
    }

    
    
    
    
}
