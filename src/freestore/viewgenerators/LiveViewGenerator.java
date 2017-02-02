/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.viewgenerators;

import freestore.FreeStoreReplica;
import freestore.messages.FreeStoreMessage;
import freestore.messages.FreeStoreMessageType;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class LiveViewGenerator implements ViewGenerator{

    private List<View> seq = new LinkedList<View>();
    private List<View> lcseq = new LinkedList<View>();

    private ReentrantLock lock = new ReentrantLock();
    
    private int seqViewCount = 0;
    private Map<List<View>,Integer> seqConvCont = new HashMap<List<View>, Integer>();
    
    private FreeStoreReplica replica;
    
    private View v; //associated view
    
    public LiveViewGenerator(View associatedView, FreeStoreReplica replica) {
        this.v = associatedView;
        this.replica = replica;
        //System.out.println("******************* Criou um novo gerador **************** "+associatedView.getQuorum());
    }
    
    public void generateView(List<View> proposal) {
        lock.lock();
        if(seq.size() == 0){
            boolean updated = true;
            for(int i = 0; i < proposal.size();i++){
                if(v.isMostUpToDateThan(proposal.get(i))){
                    updated = false;
                }
            }
            if(updated){
                seq.addAll(proposal);
                FreeStoreMessage seqView = new FreeStoreMessage(FreeStoreMessageType.L_SEQ_VIEW, seq, v);
                //System.out.println("Vai fazer o multicast para "+v);
                this.replica.multicast(new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, seqView, replica.getId()), v.getMembership(),v);//enviar seq_view
                
            }/*else{
                System.out.println("PROPOSTA NAO UPDATED");
            }*/
        }
        lock.unlock();
    }


    @Override
    public View getAssociatedView() {
        return v;
    }

   
    
    public void execMessage(FreeStoreMessage msg){
        //System.out.println("VG: vai processar " + msg.type + " view: " + msg.view);
        lock.lock();
        //System.out.println("VG: processando " + msg.type + " view: " + msg.view);
          
        if (msg.type == FreeStoreMessageType.L_SEQ_CONV) {
                List<View> recSeq = (List<View>)msg.getValue();
                Iterator<List<View>> it = seqConvCont.keySet().iterator();
                boolean found = false;
                while(it.hasNext() && !found){
                    List<View> l = it.next();
                    if(equals(l,recSeq)){
                        found = true;
                        int old = seqConvCont.get(l).intValue();
                        seqConvCont.replace(l, old+1);
                        if(old+1 == v.getQuorum()){
                            replica.newView(recSeq,v); //sequencia recSeq foi gerada
                        }
                    }
                }
                if(!found){
                    this.seqConvCont.put(recSeq, 1);
                    if(v.getQuorum() == 1){ // :-)
                        replica.newView(recSeq,v); //sequencia recSeq foi gerada
                    }
                }
        } else if (msg.type == FreeStoreMessageType.L_SEQ_VIEW) {
            List<View> recSeq = (List<View>)msg.getValue();
            boolean restart = false;
            for(int i = 0; i < recSeq.size();i++){
                if(!contains(seq,recSeq.get(i))){
                    restart = true;
                    break;
                }
            }
            if(restart){
                if(areConflicting(seq, recSeq)){
                    View v1 = getMostUpdated(seq);
                    View v2 = getMostUpdated(recSeq);
                    int[] updates1 = v1.getUpdates();
                    int[] updates2 = v2.getUpdates();
                    List<Integer> up = new LinkedList<Integer>();
                    for(int i = 0; i < updates1.length; i++){
                        up.add(updates1[i]);
                    }
                    for(int i = 0; i < updates2.length; i++){
                        boolean add = true;
                        for(int j = 0; j < updates1.length; j++){
                            if(updates2[i] == updates1[j]){
                                add = false;
                            }
                            
                        }
                        if(add){
                            up.add(updates2[i]);
                        }
                    }
                    int[] updates = new int[up.size()];
                    for(int i = 0; i < up.size(); i++){
                        updates[i] = up.get(i).intValue();
                    }
                    View union = new View(updates,null);
                     Map<Integer,InetSocketAddress> addr = new HashMap<Integer,InetSocketAddress>();
                    for(int i = 0; i < union.getMembership().length;i++){
                        if(v1.isMember(union.getMembership()[i])){
                            addr.put(union.getMembership()[i], v1.getAddress((union.getMembership()[i])));
                        }else{
                            addr.put(union.getMembership()[i], v2.getAddress((union.getMembership()[i])));
                        }
                        
                    }
                    union.setAddresses(addr);
                    seq.clear();
                    seq.addAll(lcseq);
                    seq.add(union);
                    seqViewCount = 0;
                }else{
                    if(seq.size() == 0){ //Ainda não tinha proposto e vai propor a mesma sequencia
                        seqViewCount = 1; //conta o seq_view recebido
                    }else{ //Já tinha proposto algo e vai modificar a proposta
                        seqViewCount = 0; //zera o contador
                    }
                    for(int i = 0; i < recSeq.size(); i++){
                        if(!contains(seq,recSeq.get(i))){
                            seq.add(recSeq.get(i));
                        }
                    }
                }
                
                FreeStoreMessage seqView = new FreeStoreMessage(FreeStoreMessageType.L_SEQ_VIEW, seq, v);
                this.replica.multicast(new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, seqView, replica.getId()), v.getMembership(), v);//enviar seq_view
            }else if (equals(seq,recSeq)){
                seqViewCount++;
                if(seqViewCount == v.getQuorum()){
                     //System.out.println(v+" enviando seqCONV "+seqViewCount);
                    lcseq.clear();
                    lcseq.addAll(seq);
                    FreeStoreMessage seqConv = new FreeStoreMessage(FreeStoreMessageType.L_SEQ_CONV, seq, v);
                    this.replica.multicast(new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, seqConv, replica.getId()), v.getMembership(), v);//enviar seq_conv
                }//else{
                   // System.out.println(v+" Não enviando seqCONV "+seqViewCount);
               // }
            }//else{
              //    System.out.println(v+" Desconsiderando uma seqView"+seqViewCount);
            //}
        }
        lock.unlock();
    }
    
    private View getMostUpdated(List<View> s){
        View ret = s.get(0);
        for(int i = 1; i < s.size();i++){
            if(s.get(i).isMostUpToDateThan(ret)){
                ret = s.get(i);
            }
        }
        
        return ret;
    }
    
    private boolean areConflicting(List<View> s1, List<View> s2){
         for(int i = 0; i < s1.size();i++){
             View v1 = s1.get(i);
             for(int j = 0; j < s2.size();j++){
                 View v2 = s2.get(j);
                 if(!v2.isComparable(v1)){
                     return true;
                 }
             }
         }
        
        
        return false;
    }
    
    
     private boolean equals(List<View> s1, List<View> s2){
        if( s1 == s2){
            return true;
        }
         
         if(s1.size() != s2.size()){
            return false;           
        }
        for(int i = 0; i < s1.size();i++){
            if(!contains(s2,s1.get(i))){
                return false;
            }
        }
         
        for(int i = 0; i < s2.size();i++){
            if(!contains(s1,s2.get(i))){
                return false;
            }
        }
        
        return true;
    }
    
    private boolean contains(List<View> s, View v){
        for(int i = 0; i < s.size();i++){
            if(s.get(i).equals(v)){
                return true;
            }
        }
        return false;
    }
    
}
