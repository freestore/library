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

import java.io.ByteArrayInputStream;
import java.util.Map;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import quorum.communication.QuorumMessage;
import java.io.ObjectInputStream;

/**
 *
 * @author Eduardo Alchieri
 */
@ChannelPipelineCoverage("one")
public class NettyMessageDecoder extends FrameDecoder {

    /**
     * number of measures used to calculate statistics
     */
    //private final int BENCHMARK_PERIOD = 10000;
    //private boolean isClient;
    private Map sessionTable;
    //private Storage st;
    //private int macSize;
    //private int signatureSize;
   // private ViewController controller;
   // private boolean firstTime;
    //private ReentrantReadWriteLock rl;
    //******* EDUARDO BEGIN: commented out some unused variables **************//
    //private long numReceivedMsgs = 0;
    //private long lastMeasurementStart = 0;
    //private long max=0;
    //private Storage st;
    //private int count = 0;
   
    //private Signature signatureEngine;
    
    
     //******* EDUARDO END **************//
    
    //private boolean useMAC;

    public NettyMessageDecoder(Map sessionTable) {
       // this.isClient = isClient;
        this.sessionTable = sessionTable;
        //this.macSize = macLength;
       // this.controller = controller;
        //this.firstTime = true;
       // this.rl = rl;
        //this.signatureSize = signatureLength;
        //this.useMAC = useMAC;
       
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {

        // Wait until the length prefix is available.
        if (buffer.readableBytes() < 4) {
            return null;
        }

        int dataLength = buffer.getInt(buffer.readerIndex());

        //Logger.println("Receiving message with "+dataLength+" bytes.");

        // Wait until the whole data is available.
        if (buffer.readableBytes() < dataLength + 4) {
            return null;
        }

        // Skip the length field because we know it already.
        buffer.skipBytes(4);

        int totalLength = dataLength;

       


        byte[] data = new byte[totalLength];
        buffer.readBytes(data);


       // DataInputStream dis = null;
        QuorumMessage sm = null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            //dis = new DataInputStream(bais);
            sm = new QuorumMessage();
            sm.readExternal(ois);
            //sm.serializedMessage = data;

            if (!sessionTable.containsKey(sm.getSender())) {
                NettyClientServerSession cs = new NettyClientServerSession(channel, sm.getSender());
                sessionTable.put(sm.getSender(), cs);
                
            }

            
  
            return sm;
        } catch (Exception ex) {
            //bftsmart.tom.util.Logger.println("Impossible to decode message: "+
                  //  ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

   
}
