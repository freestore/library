/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package abd;

import abd.messages.ABDMessageType;
import abd.messages.ABDMessage;
import quorum.QuorumSystem;
import quorum.core.QuorumSender;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author eduardo
 */
public class ABDClient extends QuorumSender implements QuorumSystem {

    //private int waitPoint = -1;
    private List<ABDMessage> responses = new LinkedList<ABDMessage>();
    private final ReentrantLock canReceiveLock = new ReentrantLock();

    private int expectedId = 0;

    public ABDClient(int processId) {
        super(processId);

    }

    @Override
    public Object read() {
        //First Phase
        ABDMessage read = new ABDMessage(ABDMessageType.READ, 0, null, expectedId);
        do {
            responses.clear();
            multicast(new QuorumMessage(MessageType.QUORUM_REQUEST, read, id));
            waitReplies();
        } while (responses.size() < this.getQuorumSize());

        ABDMessage readValue = responses.get(0);
        boolean need2Phase = false;
        for (int i = 1; i < responses.size(); i++) {
            if (readValue.timestamp != responses.get(i).timestamp) {
                need2Phase = true;
                if (readValue.timestamp < responses.get(i).timestamp) {
                    readValue = responses.get(i);
                }
            }

        }

        //Second Phase
        if (need2Phase) {
            //System.out.println("PRECISOU DE 2 FASE PARA: "+ readValue.timestamp);
            do {
                responses.clear();
                multicast(new QuorumMessage(MessageType.QUORUM_REQUEST,
                        new ABDMessage(ABDMessageType.WRITE, readValue.timestamp, readValue.value, expectedId), id));
                waitReplies();
            } while (responses.size() < this.getQuorumSize());
        }

        return readValue.value;
    }

    @Override
    public void write(Object value) {
        //First Phase
        ABDMessage read_ts = new ABDMessage(ABDMessageType.READ_TS, 0, null, expectedId);
        do {
            responses.clear();
            multicast(new QuorumMessage(MessageType.QUORUM_REQUEST, read_ts, id));
            waitReplies();
        } while (responses.size() < this.getQuorumSize());
        int ts = responses.get(0).timestamp;
        for (int i = 1; i < responses.size(); i++) {
            if (ts < responses.get(i).timestamp) {
                ts = responses.get(i).timestamp;
            }
        }

        //Second Phase
        ABDMessage write = new ABDMessage(ABDMessageType.WRITE, ts + 1, value, expectedId);
        do {
            responses.clear();
            multicast(new QuorumMessage(MessageType.QUORUM_REQUEST, write, id));
            waitReplies();
        } while (responses.size() < this.getQuorumSize());

    }

    public void replyReceived(QuorumMessage msg) {
        canReceiveLock.lock();
        ABDMessage resp = ((ABDMessage) msg.getMsg());

        //System.out.println("RESP: "+resp);
        /*if ((resp.type == ABDMessageType.READ_TS_RESP && waitPoint == 0)
         || (resp.type == ABDMessageType.WRITE_RESP && waitPoint == 1)
         || (resp.type == ABDMessageType.READ_RESP && waitPoint == 2)) {*/
        if (resp.id == expectedId) {
            responses.add(resp);
            if (responses.size() == this.getQuorumSize()) {
                //Processar
                //waitPoint = -1;
                expectedId++;
                repliesReceived();

            }
        }
        canReceiveLock.unlock();
    }
}
