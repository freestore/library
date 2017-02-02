/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dynastore.wso;


import java.io.Serializable;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class Register implements Serializable{
    
    public View obj;
    public int timestamp;
    
    
    public Register(View obj, int timestamp) {
        this.obj = obj;
        this.timestamp = timestamp;
        
    }
    
    public void write(View obj, int timestamp){
        if(timestamp > this.timestamp){
            this.timestamp = timestamp;
            this.obj = obj;
        }
    
    }
   
    
}
