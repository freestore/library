/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package freestore.abd;

import quorum.QuorumSystem;
import quorum.core.QuorumSender;
import quorum.communication.MessageType;
import quorum.communication.QuorumMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import quorum.view.View;

/**
 *
 * @author eduardo
 */
public class ABDFreeStoreClient extends QuorumSender implements QuorumSystem {

    //private int waitPoint = -1;
    private List<ABDFreeStoreMessage> responses = new LinkedList<ABDFreeStoreMessage>();
    private final ReentrantLock canReceiveLock = new ReentrantLock();

    private int expectedId = 0;

    public ABDFreeStoreClient(int processId) {
        super(processId);

    }

    @Override
    public Object read() {
        //First Phase
        do {
            responses.clear();
            ABDFreeStoreMessage read = new ABDFreeStoreMessage(
                    ABDFreeStoreMessageType.READ, 0, null, getViewManager().getCurrentView().getHash(), expectedId);
            multicast(new QuorumMessage(MessageType.QUORUM_REQUEST, read, id));
            waitReplies();
        } while (responses.size() < this.getQuorumSize());
        ABDFreeStoreMessage readValue = responses.get(0);
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
            //waitPoint = 1;
            
            do {
                responses.clear();
                multicast(new QuorumMessage(MessageType.QUORUM_REQUEST,
                        new ABDFreeStoreMessage(ABDFreeStoreMessageType.WRITE,
                                readValue.timestamp, readValue.value,
                                getViewManager().getCurrentView().getHash(), expectedId), id));
                waitReplies();
            } while (responses.size() < this.getQuorumSize());
        }

        return readValue.value;
    }

    @Override
    public void write(Object value) {
        //First Phase
        do {
            responses.clear();
            ABDFreeStoreMessage read_ts = new ABDFreeStoreMessage(
                    ABDFreeStoreMessageType.READ_TS, 0, null, getViewManager().getCurrentView().getHash(), expectedId);
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

        do {
            responses.clear();
            ABDFreeStoreMessage write = new ABDFreeStoreMessage(
                    ABDFreeStoreMessageType.WRITE, ts + 1, value, getViewManager().getCurrentView().getHash(), expectedId);
            multicast(new QuorumMessage(MessageType.QUORUM_REQUEST, write, id));
            //System.out.println("vai aguardar replies para ts: "+(ts+1));
            waitReplies();
            //System.out.println("vai desbloquear replies para ts: "+(ts+1));
        } while (responses.size() < this.getQuorumSize());

    }

    public void replyReceived(QuorumMessage msg) {
        canReceiveLock.lock();
        ABDFreeStoreMessage resp = ((ABDFreeStoreMessage) msg.getMsg());
       // if (resp.getId() == expectedId && (resp.type == ABDFreeStoreMessageType.READ_TS_RESP && waitPoint == 0)
        //       || (resp.type == ABDFreeStoreMessageType.WRITE_RESP && waitPoint == 1)
        //     || (resp.type == ABDFreeStoreMessageType.READ_RESP && waitPoint == 2)) {
        if (resp.getId() == expectedId) {

            if (resp.timestamp < 0) { //view is outdated
                View v = (View) resp.getValue();
                this.getViewManager().reconfigureTo(v); //verificar se precisa atualizar conexões ou se é automático
                responses.clear(); //sinaliza a reexecução da fase
                repliesReceived();
            } else { // view is the most up-to-date
                responses.add(resp);

                if (responses.size() == this.getQuorumSize()) {
                    //Processar
                    //System.out.println("recebeu um : "+resp.type+" e vai desbloquear"+" com ts: "+resp.timestamp);
                    //waitPoint = -1;
                    expectedId++;
                    repliesReceived();

                }/*else{
                 System.out.println("recebeu um : "+resp.type+" com ts: "+resp.timestamp);
                 }*/

            }
        }
        canReceiveLock.unlock();

    }
}
