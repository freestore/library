/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and
 * the authors indicated in the @author tags
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package quorum.core;

import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import quorum.communication.ServerCommunicationSystem;
import quorum.communication.system.RequestReceiver;
import quorum.view.ServerViewController;





public abstract class QuorumReplica implements RequestReceiver{

    
    // replica ID
    protected int id;
    // Server side comunication system
    private ServerCommunicationSystem cs = null;
    
    protected ServerViewController SVController;
    
  

    
    /**
     * ****************************************************
     */
    /**
     * Constructor
     *
     * @param id Replica ID
     */
    public QuorumReplica(int id) {
        this(id, "");
    }

    /**
     * Constructor
     *
     * @param id Process ID
     * @param configHome Configuration directory for JBP
     */
    public QuorumReplica(int id, String configHome) {
        this(id,configHome,null,-1);
    }
    
    
    public QuorumReplica(int id, String configHome,String host, int port) {
        this.id = id;
        this.SVController = new ServerViewController(id, configHome);
        
        if(this.SVController.getCurrentView().isMember(id)){
            this.init(host,port);
        }
        /*
        if(host == null){
            host = this.SVController.getStaticConf().getHost(id);
        }
        if(host != null){
            if(port == -1){
                port = SVController.getStaticConf().getPort(id);
            }
            this.init(host,port);
        }*/
    }


    /*public void setReplyController(Replier replier) {
        this.replier = replier;
    }*/
    public void requestReceived(QuorumMessage msg){
         
         if(msg.getType() == MessageType.QUORUM_REQUEST){
             QuorumMessage resp = executeRequest(msg);
             if(resp != null){
                cs.send(new int[]{msg.getSender()}, resp);
             }
         }else  if(msg.getType() == MessageType.RECONFIGURATION_MESSAGE){
             QuorumMessage resp = executeReconfigurationMessage(msg);
             if(resp != null){ 
                cs.send(new int[]{msg.getSender()}, resp);
             }
         }
             
    }

    
    public void sendReply(int[] targets, QuorumMessage msg){
       cs.send(targets, msg);
    }
    
    public int getId() {
        return id;
    }

     
     
    public ServerViewController getSVController() {
        return SVController;
    }

     
     
    
    public abstract QuorumMessage executeRequest(QuorumMessage msg);
    
    public abstract QuorumMessage executeReconfigurationMessage(QuorumMessage msg);
    
    
    /*public int getQuorumSize(){
        return this.SVController.currentView.getQuorum();
    }*/

    //******* EDUARDO END **************//
    // this method initializes the object
    public void init(String host, int port) {
        try {
            cs = new ServerCommunicationSystem(this.SVController,host,port);
            cs.setRequestReceiver(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unable to build a communication system.");
        }
        //cs.start();
    }


  
}
