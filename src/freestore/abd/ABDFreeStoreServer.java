/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.abd;

import freestore.FreeStoreReplica;
import freestore.viewgenerators.ViewGeneratorType;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;


/**
 *
 * @author eduardo
 */
public class ABDFreeStoreServer extends FreeStoreReplica {

    private Object value = null;
    private int timestamp = 0;

   

    public ABDFreeStoreServer(int id) {
        super(id,ViewGeneratorType.SAFE);
    }

    public QuorumMessage executeProtocolRequest(QuorumMessage msg) {
        
        ABDFreeStoreMessage req = ((ABDFreeStoreMessage) msg.getMsg());
        ABDFreeStoreMessage resp = null;
        if (this.getSVController().getCurrentView().equalsByHash(req.view)) {
            if (req.type == ABDFreeStoreMessageType.READ) {
                resp = new ABDFreeStoreMessage(ABDFreeStoreMessageType.READ_RESP, timestamp, value, null,req.getId());
            } else if (req.type == ABDFreeStoreMessageType.READ_TS) {
                resp = new ABDFreeStoreMessage(ABDFreeStoreMessageType.READ_TS_RESP, timestamp, null, null,req.getId());
            } else if (req.type == ABDFreeStoreMessageType.WRITE) {
                if (req.timestamp > this.timestamp) {
                    value = req.value;
                    timestamp = req.timestamp;
                    System.out.println("Stored value: " + value + " with timestamp: " + timestamp);
                } else {
                     System.out.println("Value "+ req.value +" not stored (lower timestamp): "+req.timestamp);
                }
                resp = new ABDFreeStoreMessage(ABDFreeStoreMessageType.WRITE_RESP, 0, null, null,req.getId());
            }
        } else {
            if (req.type == ABDFreeStoreMessageType.READ) {
                resp = new ABDFreeStoreMessage(ABDFreeStoreMessageType.READ_RESP, -1, this.getSVController().getCurrentView(), null,req.getId());
            } else if (req.type == ABDFreeStoreMessageType.READ_TS) {
                resp = new ABDFreeStoreMessage(ABDFreeStoreMessageType.READ_TS_RESP, -1, this.getSVController().getCurrentView(), null,req.getId());
            } else if (req.type == ABDFreeStoreMessageType.WRITE) {
                resp = new ABDFreeStoreMessage(ABDFreeStoreMessageType.WRITE_RESP, -1, this.getSVController().getCurrentView(), null,req.getId());
            }
        }
        
        return new QuorumMessage(MessageType.QUORUM_RESPONSE, resp, id);
    }

    
    public void updateState(int timestamp, Object value) {
        if (timestamp > this.timestamp) {
            this.value = value;
            this.timestamp = timestamp;
            System.out.println("Value updated from a reconfiguration. Stored value: " + value + " with timestamp: " + timestamp);
        } 
    }

    
    
    public int getTimestamp() {
        return this.timestamp;
    }

    public Object getValue() {
        return this.value;
    }

    
    
    
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: ... ABDFreeStoreServer <process id>");
            System.exit(-1);
        }

        new ABDFreeStoreServer(Integer.parseInt(args[0]));
    }

}
