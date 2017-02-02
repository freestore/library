/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dynastore.messages;



import java.io.Serializable;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class WSOMessage extends DynaStoreMessage implements Serializable{
    
    
    public Object obj;
    public int timestamp;

    public WSOMessage(Object obj, int timestamp, DynaStoreMessageType type, View view) {
        super(type, view);
        this.obj = obj;
        this.timestamp = timestamp;
    }

    

   

}
