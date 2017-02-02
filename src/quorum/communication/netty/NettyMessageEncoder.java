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

import quorum.communication.QuorumMessage;
import static org.jboss.netty.buffer.ChannelBuffers.buffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;



@ChannelPipelineCoverage("all")
public class NettyMessageEncoder extends SimpleChannelHandler {
    
    //private boolean isClient;
    //private Map sessionTable;
   // private int macLength;
   // private int signatureLength;
   // private ReentrantReadWriteLock rl;
   // private boolean useMAC;

    public NettyMessageEncoder(){
       // this.isClient = isClient;
       // this.sessionTable = sessionTable;
       // this.macLength = macLength;
      //  this.rl = rl;
       // this.signatureLength = signatureLength;
       // this.useMAC = useMAC;
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        QuorumMessage sm = (QuorumMessage) e.getMessage();
        byte[] msgData = sm.messageToBytes();

        int dataLength = msgData.length;

        //Logger.println("Sending message with "+dataLength+" bytes.");

        ChannelBuffer buf = buffer(4+dataLength);
        /* msg size */
        buf.writeInt(dataLength);
        
        /* data to be sent */
        buf.writeBytes(msgData);
        
        Channels.write(ctx, e.getFuture(), buf);
    }

   

}
