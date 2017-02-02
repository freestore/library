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
package quorum.communication.netty;

import freestore.messages.FreeStoreMessage;
import freestore.messages.FreeStoreMessageType;
import quorum.view.ClientViewController;
import quorum.communication.QuorumMessage;
import quorum.communication.system.CommunicationSystemClientSide;
import quorum.communication.system.ReplyReceiver;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import quorum.view.View;
import quorum.view.ViewController;

/**
 *
 * @author Paulo
 */
@ChannelPipelineCoverage("all")
public class NettyClientServerCommunicationSystemClientSide extends SimpleChannelUpstreamHandler implements CommunicationSystemClientSide {

    private int clientId;
    protected ReplyReceiver trr;
    private ClientViewController controller;
    private boolean closed = false;
    private Map sessionTable = new HashMap();

    public NettyClientServerCommunicationSystemClientSide(int clientId, ClientViewController controller) {
        super();

        this.clientId = clientId;
        this.controller = controller;

        /*int[] currV = controller.getCurrentView().getMembership();
         for (int i = 0; i < currV.length; i++) {
         connectTo(currV[i]);
         }*/
        //connectToView(controller.getCurrentView());
    }

    private NettyClientServerSession connectTo(int id, View v) {
        NettyClientServerSession cs = null;
        try {
            // Configure the client.                                        
            ClientBootstrap bootstrap = new ClientBootstrap(
                    new NioClientSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool()));

            bootstrap.setOption("tcpNoDelay", true);
            bootstrap.setOption("keepAlive", true);
            bootstrap.setOption("connectTimeoutMillis", 10000);

            // Set up the default event pipeline.
            bootstrap.setPipelineFactory(new NettyClientPipelineFactory(this, sessionTable));

            SocketAddress addr = null;
            if(v == null){
                addr = controller.getRemoteAddress(id);
            }else{
                addr = v.getAddress(id);
            }
            
            System.out.println("Connecting to replica " + id + " at " + addr);
            
            // Start the connection attempt.
            ChannelFuture future = bootstrap.connect(addr);

            //future.awaitUninterruptibly();
            final CountDownLatch channelLatch = new CountDownLatch(1);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture cf) throws Exception {
                    if (cf.isSuccess()) {
                        //channel = cf.getChannel();
                        channelLatch.countDown();
                    } else {
                        bootstrap.releaseExternalResources();
                        throw new Exception("Something bad happened...");
                    }
                }
            });

            try {
                channelLatch.await();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            cs = new NettyClientServerSession(future.getChannel(), id);
            sessionTable.put(id, cs);
            
            if (!future.isSuccess()) {
                System.err.println("Impossible to connect to " + id);
            }

        } catch (java.lang.NullPointerException ex) {
            //What the fuck is this??? This is not possible!!!
            System.err.println("Should fix the problem, and I think it has no other implications :-), "
                    + "but we must make the servers store the view in a different place.");
        }
        System.out.println("Connected to replica " + id);
        return cs;
    }

    public ViewController getController() {
        return controller;
    }

    @Override
    public void connectToView(View v) {
        

        //open connections with new servers
        for (int i = 0; i < v.getMembership().length; i++) {

            if (sessionTable.get(v.getMembership()[i]) == null) {
                connectTo(v.getMembership()[i],v);
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (e.getCause() instanceof ClosedChannelException) {
            System.out.println("Connection with replica closed.");
        } else if (e.getCause() instanceof ConnectException) {
            System.out.println("Impossible to connect to replica.");
        } else {
            System.out.println("Replica disconnected.");
            e.getCause().printStackTrace();
        }
    }

    @Override
    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) {
        QuorumMessage sm = (QuorumMessage) e.getMessage();
        //delivers message to replyReceived callback
        trr.replyReceived(sm);
    }

    @Override
    public void channelConnected(
            ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.out.println("Channel connected");
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
       /* if (this.closed) {
            return;
        }

        try {
            //sleeps 10 seconds before trying to reconnect
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        //rl.writeLock().lock();
        //Iterator sessions = sessionTable.values().iterator();
        ArrayList<NettyClientServerSession> sessions = new ArrayList<NettyClientServerSession>(sessionTable.values());
        for (NettyClientServerSession ncss : sessions) {
            if (ncss.getChannel() == ctx.getChannel()) {
                System.out.println("RE-Connecting to replica " + ncss.getReplicaId() + " at " + controller.getRemoteAddress(ncss.getReplicaId()));
                // Configure the client.
                ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
                // Set up the default event pipeline.
                bootstrap.setPipelineFactory(new NettyClientPipelineFactory(this, sessionTable));
                // Start the connection attempt.
                if (controller.getRemoteAddress(ncss.getReplicaId()) != null) {
                    ChannelFuture future = bootstrap.connect(controller.getRemoteAddress(ncss.getReplicaId()));

                    NettyClientServerSession cs = new NettyClientServerSession(future.getChannel(), ncss.getReplicaId());
                    sessionTable.remove(ncss.getReplicaId());
                    sessionTable.put(ncss.getReplicaId(), cs);
                    //System.out.println("RE-Connecting to replica "+ncss.getReplicaId()+" at " + conf.getRemoteAddress(ncss.getReplicaId()));
                } else {
                    // This cleans an olde server from the session table
                    sessionTable.remove(ncss.getReplicaId());
                }

            }
        }*/

        //closes all other channels to avoid messages being sent to only a subset of the replicas
        /*Enumeration sessionElements = sessionTable.elements();
         while (sessionElements.hasMoreElements()){
         ((NettyClientServerSession) sessionElements.nextElement()).getChannel().close();
         }*/
        //rl.writeLock().unlock();
    }

    @Override
    public void setReplyReceiver(ReplyReceiver trr) {
        this.trr = trr;
    }

    @Override
    public void send(int[] targets, QuorumMessage sm, View v) {

        
        
        //Logger.println("Sending message with "+sm.serializedMessage.length+" bytes of content.");
        // int sent = 0;
        for (int i = targets.length - 1; i >= 0; i--) {
          //  sm.destination = targets[i];

           
            
            //rl.readLock().lock();
            NettyClientServerSession ncss = (NettyClientServerSession) sessionTable.get(targets[i]);

            if (ncss == null) {
                //System.out.println("NCSS to " + targets[i] + " is null");
                ncss = connectTo(targets[i],null);
            }

            Channel channel = ncss.getChannel();
            //rl.readLock().unlock();

            if (!channel.isConnected()) {
                //System.out.println("Channel to " + targets[i] + " is not connected");
                connectTo(targets[i],v);
            }
            
            /*if(sm.getMsg() instanceof FreeStoreMessage){
                 FreeStoreMessage recMsg = (FreeStoreMessage) sm.getMsg();
                 
                 if(recMsg.type == FreeStoreMessageType.STATE_UPDATE){
                     System.out.println("Sending state to "+targets[i]);
                 }
            }*/
            
            channel.write(sm);
            
            
        }

    }

    @Override
    public void close() {
        this.closed = true;
        //Iterator sessions = sessionTable.values().iterator();
        //rl.readLock().lock();
        ArrayList<NettyClientServerSession> sessions = new ArrayList<NettyClientServerSession>(sessionTable.values());
        //rl.readLock().unlock();
        for (NettyClientServerSession ncss : sessions) {
            ncss.getChannel().close();
        }
    }
}
