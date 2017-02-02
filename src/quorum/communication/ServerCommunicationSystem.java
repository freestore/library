/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package quorum.communication;


/*import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.communication.client.CommunicationSystemServerSideFactory;
import bftsmart.communication.client.RequestReceiver;
import bftsmart.communication.server.ServersCommunicationLayer;

import bftsmart.consensus.roles.Acceptor;

import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;
*/

import quorum.view.ServerViewController;
import quorum.communication.system.CommunicationSystemServerSide;
import quorum.communication.system.CommunicationSystemServerSideFactory;
import quorum.communication.system.RequestReceiver;
/**
 *
 * @author alysson
 */
public class ServerCommunicationSystem {

  
    private CommunicationSystemServerSide clientsConn;
    private ServerViewController controller;

    /**
     * Creates a new instance of ServerCommunicationSystem
     * @param controller
     * @param host
     * @param port
     * @throws java.lang.Exception
     */
    public ServerCommunicationSystem(ServerViewController controller, String host, int port) throws Exception {
        this.controller = controller;
        clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller,host,port);
    }

   
    public void setRequestReceiver(RequestReceiver requestReceiver) {
       /* if (clientsConn == null) {
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
        }*/
        clientsConn.setRequestReceiver(requestReceiver);
    }

    public void send(int[] targets, QuorumMessage sm) {
            clientsConn.send(targets,  sm, false);
    }
    
    public CommunicationSystemServerSide getClientsConn() {
        return clientsConn;
    }
    
}
