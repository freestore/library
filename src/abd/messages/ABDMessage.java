/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package abd.messages;


import java.io.Serializable;

/**
 *
 * @author eduardo
 */
public class ABDMessage implements Serializable{
    
    
    public ABDMessageType type;
    public int timestamp;
    public Object value;

    public int id;
    
    public ABDMessage() {
    }

    public ABDMessage(ABDMessageType type, int timestamp, Object value, int id) {
        this.type = type;
        this.timestamp = timestamp;
        this.value = value;
        this.id = id;
    }

    
    public ABDMessage(int timestamp, Object value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Object getValue() {
        return value;
    }

    public ABDMessageType getType() {
        return type;
    }
    
    
    
}
