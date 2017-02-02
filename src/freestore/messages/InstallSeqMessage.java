/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.messages;

import java.io.Serializable;
import java.util.List;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class InstallSeqMessage extends FreeStoreMessage implements Serializable{
    
    public View w;

    public InstallSeqMessage(View w, FreeStoreMessageType type, Object value, View view) {
        super(type, value, view);
        this.w = w;
    }

    public View getW() {
        return w;
    }
    
    @Override
    public boolean equals(Object o){
        if(o instanceof InstallSeqMessage){
            InstallSeqMessage other = (InstallSeqMessage) o;
            if (other.view.equals(this.view) && other.w.equals(this.w)){
                
                List<View> s1 = (List<View>) getValue();
                List<View> s2 = (List<View>) other.getValue();
                if(equals(s1,s2)){
                    return true;
                }
            }
        }
        return false;
    }
    
    
     private boolean equals(List<View> s1, List<View> s2){
        if( s1 == s2){
            return true;
        }
        
        if(s1.size() != s2.size()){
            return false;           
        }
        for(int i = 0; i < s1.size();i++){
            if(!contains(s2,s1.get(i))){
                return false;
            }
        }
         
        for(int i = 0; i < s2.size();i++){
            if(!contains(s1,s2.get(i))){
                return false;
            }
        }
        
        return true;
    }
     
     private boolean contains(List<View> s, View v){
        for(int i = 0; i < s.size();i++){
            if(s.get(i).equals(v)){
                return true;
            }
        }
        return false;
    }
    
}
