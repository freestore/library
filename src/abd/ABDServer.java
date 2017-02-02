/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package abd;

import abd.messages.ABDMessageType;
import abd.messages.ABDMessage;
import quorum.core.QuorumReplica;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author eduardo
 */
public class ABDServer extends QuorumReplica{

    
    private Object value = null;
    private int timestamp = -1;
    
    private ReentrantLock canExecuteLock = new ReentrantLock();
    
    public ABDServer(int id) {
         super(id);
     }
    
    
    public QuorumMessage executeRequest(QuorumMessage msg) {
        canExecuteLock.lock();
        ABDMessage req = ((ABDMessage) msg.getMsg());
        ABDMessage resp = null;
        if(req.type == ABDMessageType.READ){
            resp = new ABDMessage(ABDMessageType.READ_RESP, timestamp, value,req.id);
        }else if (req.type == ABDMessageType.READ_TS){
            resp = new ABDMessage(ABDMessageType.READ_TS_RESP, timestamp, null,req.id);
        }else if (req.type == ABDMessageType.WRITE){
            if(req.timestamp > this.timestamp){
                value = req.value;
                timestamp = req.timestamp;
                System.out.println("Stored value: "+value+" with timestamp: "+timestamp);
            }else{
               // System.out.println("Value "+ req.value +" not stored (lower timestamp): "+req.timestamp);
            }
            resp = new ABDMessage(ABDMessageType.WRITE_RESP, 0, null, req.id);
        }        
        canExecuteLock.unlock();
        return new QuorumMessage(MessageType.QUORUM_RESPONSE, resp, id);
    }

    @Override
    public QuorumMessage executeReconfigurationMessage(QuorumMessage msg) {
        throw new UnsupportedOperationException("Not supported at ABDServer."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public static void main(String[] args) {
        
        
        if (args.length < 1) {
            System.out.println("Usage: ... ABDServer <process id>");
            System.exit(-1);
        }
        
        new ABDServer(Integer.parseInt(args[0]));
    }


}
