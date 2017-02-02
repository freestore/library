/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dynastore.wso;

import dynastore.DynaStoreReplica;
import dynastore.messages.DynaStoreMessageType;
import dynastore.messages.WSOMessage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class WeakSnapshotObject {
    
    
    public View v;
    private Map<Integer,Register> swmr = new HashMap<Integer,Register>();
    
    private DynaStoreReplica replica; 
    
    private int ts = 0; //tb uso como id das mensagens
    
    public WeakSnapshotObject(View associatedView, DynaStoreReplica replica) {
        this.v = v;
        this.replica = replica;
        for(int i = 0; i < this.v.getMembership().length; i++){
            swmr.put(this.v.getMembership()[i], new Register(null,-1));
        }
    }
    
    
    public void update(View up){
        if(collect().isEmpty()){
            
            WSOMessage write = new WSOMessage(up, ts, DynaStoreMessageType.WSO_WRITE, v);
            ts++;
            this.replica.multicast(new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, write, replica.getId()), v.getMembership(),v);
        }
    }
    
    public List<View> scan(){
        List<View> ret = collect();
        if(ret.isEmpty()){
            return null;
        }
        ret = collect();
        return ret;
    }
    
    private List<View> collect(){
     
        WSOMessage collect = new WSOMessage(null, ts, DynaStoreMessageType.WSO_COLLECT, v);
        ts++;
        this.replica.multicast(new QuorumMessage(MessageType.RECONFIGURATION_MESSAGE, collect, replica.getId()), v.getMembership(),v);
        //espera por um quorum
        
        return null;
    }
    
    
    public WSOMessage execMessage(WSOMessage msg, int sender){
        if(msg.type == DynaStoreMessageType.WSO_WRITE){
            this.swmr.get(sender).write((View)msg.obj, msg.timestamp);
            //enviar o write_resp para sender
            return new WSOMessage(null, msg.timestamp, DynaStoreMessageType.WSO_WRITE_RESP, v);
        }else if (msg.type == DynaStoreMessageType.WSO_COLLECT){
            //enviar o collect_resp para o sender
            return new WSOMessage(this.swmr, 0, DynaStoreMessageType.WSO_COLLECT_RESP, v);
        }
        return null;
    }
    
}
