/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.messages;

import java.io.Serializable;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class PaxosMessage extends FreeStoreMessage implements Serializable{

    public int n;
    
    public PaxosMessage(FreeStoreMessageType type, Object value, View view, int n) {
        super(type, value, view);
        this.n = n;
    }
    
    
    
    
}
