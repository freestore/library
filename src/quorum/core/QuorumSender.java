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

import quorum.communication.QuorumMessage;
import quorum.communication.system.CommunicationSystemClientSide;
import quorum.communication.system.CommunicationSystemClientSideFactory;
import quorum.communication.system.ReplyReceiver;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import quorum.view.ClientViewController;

/**
 * This class is used to multicast messages to replicas and receive replies.
 */
public abstract class QuorumSender implements ReplyReceiver {

    protected int id; // process id

    private ClientViewController viewController;

    private CommunicationSystemClientSide cs; // Client side comunication system

    private Semaphore sm = new Semaphore(0);

    public QuorumSender(int processId) {
        this(processId, null);
    }

    public QuorumSender(int processId, String configHome) {
        if (configHome == null) {
            init(processId);
        } else {
            init(processId, configHome);
        }
    }

    protected void waitReplies() {

        try {
            if (!this.sm.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                System.out.println("TIMEOUT FOR THE REPLIES");
                
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    
    protected void repliesReceived(){
        this.sm.release();
    }

    public void close() {
        cs.close();
    }

    public CommunicationSystemClientSide getCommunicationSystem() {
        return this.cs;
    }

    public int getQuorumSize() {
        return this.viewController.getCurrentView().getQuorum();
    }

    public ClientViewController getViewManager() {
        return this.viewController;
    }

    /**
     * @param processId ID of the process
     */
    private void init(int processId) {
        this.viewController = new ClientViewController(processId);
        startsCS(processId);
    }

    private void init(int processId, String configHome) {
        this.viewController = new ClientViewController(processId, configHome);
        startsCS(processId);
    }

    private void startsCS(int clientId) {
        this.cs = CommunicationSystemClientSideFactory.getCommunicationSystemClientSide(clientId, this.viewController);
        this.cs.setReplyReceiver(this); // This object itself shall be a reply receiver
        this.id = this.viewController.getStaticConf().getProcessId();
        //this.useSignatures = this.viewController.getStaticConf().getUseSignatures()==1?true:false;
        //this.session = new Random().nextInt();
    }
    //******* EDUARDO END **************//

    public int getProcessId() {
        return id;
    }

  
    /**
     * Multicast a QuorumMessage to the group of replicas
     *
     * @param sm Message to be multicast
     */
    public void multicast(QuorumMessage sm) {
        cs.send(this.viewController.getCurrentView().getMembership(), sm, this.viewController.getCurrentView());
    }

   
}
