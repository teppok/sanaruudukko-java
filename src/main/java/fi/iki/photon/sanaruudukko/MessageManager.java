package fi.iki.photon.sanaruudukko;

import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

@Stateless
public class MessageManager {

    @Resource(lookup = "sanaruudukkoCF")
    private TopicConnectionFactory tcf;
    
    @Resource(lookup = "sanaruudukkoTopic")
    private Topic topic;
    

    public void sendNewWordsNotify(String playerName) {
        sendNotify(playerName);
    }

    public void sendNewRoundNotify(String playerName) {
        sendNotify(playerName);
    }


    public void sendNotify(String playerName) {
        System.out.println("Send notify to " + playerName);
        try {
 
            TopicConnection topicConnection = tcf.createTopicConnection();
            
            TopicSession ts = topicConnection.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            
            ObjectMessage message = ts.createObjectMessage();
            
            message.setStringProperty("player", playerName);
            
            ts.createProducer(topic).send(message);

            topicConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }        

    public Status trueWordWaiter(String name) {
        if (name == null) return null;
        try {
            TopicConnection topicConnection = tcf.createTopicConnection();
            
            TopicSession ts = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Create a consumer with selector player = 'thisplayer'
            MessageConsumer mc = ts.createConsumer(topic, "player = '" + name + "'");
            
            topicConnection.start();
            
            boolean waitNewMessages = true;

            Date waitExpires = new Date();
            waitExpires.setTime(waitExpires.getTime() + 15000);
            
            while (waitNewMessages) {
                Message m = mc.receive(1000);
                
                if (m != null) {
                    System.out.println("Got notify for " + m.getStringProperty("player"));
                    waitNewMessages = false;
                }
                if (waitExpires.before(new Date())) {
                    waitNewMessages = false;
                }
                
            }
            topicConnection.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        Status s = new Status();
        s.newChat = s.newWords = true;
        return s;
        /*
         * THE FOLLOWING CODE WAS REPLACED BY THE PRECEDING CODE ON 8.4.2013.
         * Kept here for a while if there are problems with the preceding code.
         * 
        final int MAX_LOOPED = 75;
        System.out.println("Waiting");
        boolean newWords = false, newChat = false, newRound = false;
        int looped = 0;
        while (looped < MAX_LOOPED && ! newWords && ! newChat && ! newRound) {
            try {
                tx.begin();

                Player p2 = em.find(Player.class, name);
                Room roomItem = rm.getRoom(p2);
                
                newWords = p2.isNewWords();
                if (getLastChat(roomItem) >= p2.getLastChat()) {
                    newChat = true;
                }
                
                newRound = p2.isNewRound();
                tx.commit();
            } catch (SecurityException | IllegalStateException
                    | RollbackException | HeuristicMixedException
                    | HeuristicRollbackException | SystemException | NotSupportedException e1) {
                e1.printStackTrace();
            }
            
            if (! newWords && ! newRound) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // If interrupted for some reason, exit
                    looped = MAX_LOOPED;
                }
            }
            looped++;
        }
        System.out.println("Waited " + newWords + " " + newRound + " " + newChat);

        Status s = new Status();
        s.newWords = newWords;
        s.newChat = newChat;

        if (looped == MAX_LOOPED) {
            s.newWords = s.newChat = true;
        }
        
        try {
            tx.begin();

            Player p2 = em.find(Player.class, name);

            if (newRound) {
                p2.setNewRound(false);
            }
            tx.commit();
        } catch (SecurityException | IllegalStateException
                | RollbackException | HeuristicMixedException
                | HeuristicRollbackException | SystemException | NotSupportedException e1) {
            e1.printStackTrace();
        }
        return s;
        */
    }



}
