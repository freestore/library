/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.messages;

import java.io.Serializable;
import java.net.InetSocketAddress;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class RECVMessage  extends FreeStoreMessage implements Serializable{
     
    public InetSocketAddress addr;

    public RECVMessage(InetSocketAddress addr, FreeStoreMessageType type, Object value, View view) {
        super(type, value, view);
        this.addr = addr;
    }

    public boolean equals(Object o){
        if(o instanceof RECVMessage){
            RECVMessage other = (RECVMessage)o;
            if(this.value.toString().equals(other.value.toString())){
                return true;
            }
        }
        return false;
    } 
     
     
     
}
