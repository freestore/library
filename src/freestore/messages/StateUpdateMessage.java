/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.messages;

import freestore.FreeStoreReplica;
import java.io.Serializable;
import java.util.List;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class StateUpdateMessage extends FreeStoreMessage implements Serializable{
 
    public List<RECVMessage> rec;
    public int timestamp;
    public View w;
    public List<View> generatedSeq;
    
    public StateUpdateMessage(List<RECVMessage> rec, int timestamp, FreeStoreMessageType type, Object value, View view, View w, List<View> generatedSeq) {
        super(type, value, view);
        this.rec = rec;
        this.timestamp = timestamp;
        this.w = w;
        this.generatedSeq = generatedSeq;
    }
    
    
    
}
