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
package quorum.communication.netty;

import quorum.view.ServerViewController;
import quorum.communication.QuorumMessage;
import quorum.communication.system.CommunicationSystemServerSide;
import quorum.communication.system.RequestReceiver;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;






/**
 *
 * @author Paulo
 */
@ChannelPipelineCoverage("all")
public class NettyClientServerCommunicationSystemServerSide extends SimpleChannelHandler implements CommunicationSystemServerSide {

    private RequestReceiver requestReceiver;
    private HashMap sessionTable;
    //private ReentrantReadWriteLock rl;
   // private ServerViewController controller;

    public NettyClientServerCommunicationSystemServerSide(ServerViewController controller, String host, int port) {
       

            //this.controller = controller;
            sessionTable = new HashMap();
           // rl = new ReentrantReadWriteLock();

            //Configure the server.
            /* Cached thread pool */
            ServerBootstrap bootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool(),100));

            //******* EDUARDO BEGIN **************//
            //Mac macDummy = Mac.getInstance(controller.getStaticConf().getHmacAlgorithm());

            bootstrap.setOption("tcpNoDelay", true);
            bootstrap.setOption("keepAlive", true);

            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.keepAlive", true);

            //Set up the default event pipeline.
            bootstrap.setPipelineFactory(new NettyServerPipelineFactory(this,sessionTable));

            if(host == null){
                //Bind and start to accept incoming connections.
                
                
               /* bootstrap.bind(new InetSocketAddress(controller.getStaticConf().getHost(
                    controller.getStaticConf().getProcessId()),
                    controller.getStaticConf().getPort(controller.getStaticConf().getProcessId())));*/
                bootstrap.bind(controller.getRemoteAddress(controller.getStaticConf().getProcessId()));
                
                
            }else{
                bootstrap.bind(new InetSocketAddress(host,port));
            }
            
            System.out.println("#Bound to port " + controller.getStaticConf().getPort(controller.getStaticConf().getProcessId()));
            System.out.println("#myId " + controller.getStaticConf().getProcessId());
            System.out.println("#current view " + controller.getCurrentView());
            System.out.println("#n " + controller.getCurrentView().getN());
            System.out.println("#f " + controller.getCurrentView().getF());
            System.out.println("#quorum " + controller.getCurrentView().getQuorum());
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e) {
    	if(e.getCause() instanceof ClosedChannelException)
            System.out.println("Connection with client closed.");
    	else if(e.getCause() instanceof ConnectException) {
            System.out.println("Impossible to connect to client.");
        } else {
            e.getCause().printStackTrace(System.err);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        //delivers message
        if (requestReceiver == null) System.out.println("RECEIVER NULO!!!!!!!!!!!!");
        else requestReceiver.requestReceived((QuorumMessage) e.getMessage());
    }

    @Override
    public void channelConnected( ChannelHandlerContext ctx, ChannelStateEvent e) {
        //Logger.println("Session Created, active clients=" + sessionTable.size());
        
        
        
        
        System.out.println("Session Created, active clients=" + sessionTable.size());
    }

    @Override
    public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e) {
        //rl.writeLock().lock();
        try {
            Set s = sessionTable.entrySet();
            Iterator i = s.iterator();
            while (i.hasNext()) {
                Entry m = (Entry) i.next();
                NettyClientServerSession value = (NettyClientServerSession) m.getValue();
                if (e.getChannel().equals(value.getChannel())) {
                    int key = (Integer) m.getKey();
                    System.out.println("#Removing client channel with ID= " + key);
                    sessionTable.remove(key);
                    System.out.println("#active clients=" + sessionTable.size());
                    break;
                }
            }
        } finally {
           // rl.writeLock().unlock();
        }
       // Logger.println("Session Closed, active clients=" + sessionTable.size());
    }

    @Override
    public void setRequestReceiver(RequestReceiver tl) {
        this.requestReceiver = tl;
    }

    //private ReentrantLock sendLock = new ReentrantLock();
    
    @Override
    public void send(int[] targets, QuorumMessage sm, boolean serializeClassHeaders) {
        
       

        

        for (int i = 0; i < targets.length; i++) {
           // rl.readLock().lock();
            //sendLock.lock();
            try {
	            NettyClientServerSession ncss = (NettyClientServerSession) sessionTable.get(targets[i]);
	            if (ncss != null) {
                         //System.out.println("NCSS NÃO NULL "+targets[i]);
	                Channel session = ncss.getChannel();
	                //sm.destination = targets[i];
	                //send message
	                session.write(sm); // This used to invoke "await". Removed to avoid blockage and race condition.
	            }else{
                        System.out.println("NCSS É NULL "+targets[i]);
                    }
            } finally {
               // sendLock.unlock();
               // rl.readLock().unlock();
            }
        }
    }
}
