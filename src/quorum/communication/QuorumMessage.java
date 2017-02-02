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


import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * This is the super-class for all other kinds of messages created by JBP
 * 
 */

public class QuorumMessage  implements Externalizable {

    private int sender;
    
    private MessageType msgType;
    
    private Object msg;
    
    /**
     * Creates a new instance of SystemMessage
     */
    public QuorumMessage(){}

    public QuorumMessage(MessageType msgType, Object msg, int sender) {
        this.sender = sender;
        this.msgType = msgType;
        this.msg = msg;
    }
    
    /**
     * Creates a new instance of SystemMessage
     * @param sender ID of the process which sent the message
     */
    
    
    
    public QuorumMessage(int sender, MessageType type){
        this.sender = sender;
        this.msgType = type;
    }

    public void setMsg(Object msg) {
        this.msg = msg;
    }

    public Object getMsg() {
        return msg;
    }
    
    
        
    public MessageType getType(){
        return msgType;
    }

    public int getSender() {
        return sender;
    }

    
    
   

    
    
    // This methods implement the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(sender);
        out.writeInt(msgType.toInt());
        out.writeObject(msg);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender = in.readInt();
        msgType = MessageType.fromInt(in.readInt());
        msg = in.readObject();
    }
    
    
     public byte[] messageToBytes() {
        try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream dos = new ObjectOutputStream(baos);
       
            this.writeExternal(dos);
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        
    }
    
}
