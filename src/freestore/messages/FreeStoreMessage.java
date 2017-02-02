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
public class FreeStoreMessage implements Serializable{
    
    
    public FreeStoreMessageType type;
    public Object value;
    
    public View view;

    public FreeStoreMessage() {
    }

    public FreeStoreMessage(FreeStoreMessageType type, Object value, View view) {
        this.type = type;
        
        this.value = value;
        this.view = view;
    }

    
    public Object getValue() {
        return value;
    }

    public FreeStoreMessageType getType() {
        return type;
    }

}
