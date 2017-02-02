/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.abd;


import java.io.Serializable;

/**
 *
 * @author eduardo
 */
public class ABDFreeStoreMessage implements Serializable{
    
    
    public ABDFreeStoreMessageType type;
    public int timestamp;
    public Object value;
    
    public byte[] view;
    
    private int id;

    public ABDFreeStoreMessage() {
    }

    public ABDFreeStoreMessage(ABDFreeStoreMessageType type, int timestamp, Object value, byte[] view, int id) {
        this.type = type;
        this.timestamp = timestamp;
        this.value = value;
        this.view = view;
        this.id = id;
    }

    
    public ABDFreeStoreMessage(int timestamp, Object value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Object getValue() {
        return value;
    }

    public ABDFreeStoreMessageType getType() {
        return type;
    }


    public int getId() {
        return id;
    }
    
    

}
