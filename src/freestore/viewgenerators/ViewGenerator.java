/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.viewgenerators;

import freestore.messages.FreeStoreMessage;
import java.util.List;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public interface ViewGenerator {
    
    public void generateView(List<View> proposal);
    public void execMessage(FreeStoreMessage msg);
    public View getAssociatedView();
    
}
