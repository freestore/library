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
public class DynaStoreMessage implements Serializable{
    
    
    public DynaStoreMessageType type;
   
    public View view;

    public DynaStoreMessage() {
    }

    public DynaStoreMessage(DynaStoreMessageType type, View view) {
        this.type = type;
        
   
        this.view = view;
    }

   

}
