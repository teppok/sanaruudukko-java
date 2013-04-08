package fi.iki.photon.sanaruudukko;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import fi.iki.photon.sanaruudukko.entity.ChatLine;
import fi.iki.photon.sanaruudukko.entity.Player;
import fi.iki.photon.sanaruudukko.entity.Room;
import fi.iki.photon.sanaruudukko.entity.Round;

/**
 * Sanaruudukko class, the main REST handler class.
 * 
 * The class has 3 main REST interfaces: process, wordwaiter and submitchat.
 * 
 * Process is the main processing interface for AJAX GET requests that perform
 * short and small actions.
 * 
 * WordWaiter is the interface for the Reverse AJAX polling, which returns data
 * only if new data is encountered.
 * 
 * SubmitChat is a POST interface so that long chat lines can be sent over POST.
 * 
 * Transaction management is done manually, mainly because wordwaiter requires
 * transactions to be released while it's waiting for new data.
 * Otherwise almost everything is completed in a single transaction.
 * 
 * There is no password security. Everything is in plain text. This is communicated
 * to the user by words and using a html field inputtext instead of password.
 * 
 * Return data for the queries can be either a status code, special reply such as
 * allwords or getrooms reply, or a set containing any of the following:
 * Round info (time until round ends or starts, the current board)
 * Room info (room name and id)
 * Word info (list of players and the words they have submitted)
 * Chat info (list of chat lines)
 * 
 * The chosen items are relevant to the processed function.
 */

@Path("/sr")
@Produces(MediaType.TEXT_PLAIN)
@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class Sanaruudukko {
    
    public static final int ROUNDLENGTH = 180;
    // Allow request of more time this many seconds from the beginning of round.
    public static final int MORETIMELIMIT = 120;
    // MAX VALUE ~20 sec
    public static final int PREROUNDTIME_FULL = 10;
    public static final int PREROUNDTIME_SINGLE = 5;
    
    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction tx;
    
    @EJB
    private RoomManager rm;

    @EJB
    private RoundManager rom;
    
    @EJB
    private WordManager wm;
    
    @EJB
    private AsyncManager am;
    
    @EJB
    private MessageManager mm;
    
    /**
     * Default constructor.
     */
    public Sanaruudukko() {
    }

    /**
     * Main processing method. Receives the player name and passcode (in plaintext) and a function to perform.
     * If the func is registerp, creates a new player if it doesn't already exist.
     * 
     * Other parameters, room, roomname and word are used on need basis.
     * 
     * Returns an xml string representing the state of the database, containing some data, depending on the
     * chosen function.
     * 
     * The following error statuses can be sent:
     * 100: no function
     * 1: password mismatch
     * 2: roomname is empty when creating a new room
     * 3: word is empty or too long when submitting or removing a word. 
     * 4: room id is empty when joining an existing room
     * 5: weird error.
     * 
     * @param player
     * @param passcode
     * @param func
     * @param room
     * @param roomName
     * @param word
     * @return XML string for AJAX processing
     */
    
    @GET
    @Path("process")
    public String process(@QueryParam(value = "player") final String player,
            @QueryParam(value = "passcode") final String passcode,
            @QueryParam(value = "func") final String func,
            @QueryParam(value = "room") final String room,
            @QueryParam(value = "roomname") final String roomName,
            @QueryParam(value = "word") final String word) {
        
        System.out.println("Started processing");

        // Some checks for clearly invalid data.
        
        if (func == null || "".equals(func)) {
            return xmlReply("<data><status>100</status></data>");
        }

        if (player == null || "".equals(player) || passcode == null || "".equals(passcode)) {
            return xmlReply("<data><status>1</status></data>");
        }
        
        if ("newroom".equals(func) && ( roomName == null || "".equals(roomName))) {
            return xmlReply("<data><status>2</status></data>");
        }
        
        if ("submitword".equals(func) || "removeword".equals(func)) {
            if (word == null || "".equals(word) || word.length() > 16) {
                return xmlReply("<data><status>3</status></data>");
            }
        }

        if ("joinroom".equals(func) && ( room == null || "".equals(room))) {
            return xmlReply("<data><status>4</status></data>");
        }
        
        try {
            tx.begin();
        
            
            if ("registerp".equals(func)) {
                // Create a new player if it doesn't already exist.
                registerPlayer(player, passcode);
            }

            String result;

            Player playerItem = checkPassword(player, passcode);
            if (playerItem == null) {
                // Name and passcode don't match to an existing player
                tx.commit();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Do nothing.
                }
                return xmlReply("<data><status>1</status></data>");
            }

            playerItem.setLastSeen(new Date());
            
            Room roomItem = rm.getRoom(playerItem);
            Round roundItem = RoundManager.getRound(roomItem);

            Status s = new Status();
            
            result = dispatchFunction(func, playerItem, roomItem, roundItem, roomName, room, word, s);
            
            if (s.roundStarted) {
                roundItem = RoundManager.getRound(roomItem);

                tx.commit();
                if (roundItem != null && roomItem != null) {
                    am.calculateValidWords(roundItem.getId());
                    
                    am.newRoundWaiter(roomItem.getId(), roundItem.getId());
                }
            } else {
                tx.commit();
            }

            System.out.println(result);
            return xmlReply("<data>" + result + "</data>");

        } catch (NotSupportedException | RollbackException | HeuristicMixedException | HeuristicRollbackException e1) {
            e1.printStackTrace();
        } catch (SystemException e1) {
            e1.printStackTrace();
        } catch (SecurityException | IllegalStateException e) {
            e.printStackTrace();
        }
        return xmlReply("");
    }

    /**
     * Takes the function and arguments inferred from GET parameters.
     * Checks if the arguments are valid for the given function and if they are not, returns an error status.
     * If they are valid, dispatches the processing to the given function.
     *  
     * @param func
     * @param playerItem
     * @param roomItem
     * @param roundItem
     * @param roomName
     * @param room
     * @param word
     * @return XML containing the data.
     * @throws NotSupportedException 
     * @throws SystemException 
     * @throws HeuristicRollbackException 
     * @throws HeuristicMixedException 
     * @throws RollbackException 
     * @throws IllegalStateException 
     * @throws SecurityException 
     */
    
    private @NonNull String dispatchFunction(@NonNull String func, @NonNull Player playerItem, @Nullable Room roomItem, @Nullable Round roundItem, 
            @Nullable String roomName, @Nullable String room, @Nullable String word, @NonNull Status s) {
        String result;
        
        if ("getrooms".equals(func)) {
            result = rm.displayRooms();
        
        } else if ("allwords".equals(func)) {
            result = getValidWords(roundItem); 
        
        } else if("moretime".equals(func)) { 
            if (roomItem == null || roundItem == null) {
                result = "<status>5</status>";
            } else {
                result = moreTime(playerItem, roomItem, roundItem);
            }

        } else if("newround".equals(func)) {
            if (roomItem == null) {
                result = "<status>5</status>";
            } else {
                result = newRound(playerItem, roomItem, roundItem, s);
            }

        } else if ("joinroom".equals(func)) {
            if (room == null) {
                result = "<status>5</status>";
            } else {
                result = joinRoom(playerItem, room);
            }

        } else if("submitword".equals(func)) {
            if (roomItem == null || roundItem == null || word == null) {
                result = "<status>5</status>";
            } else {
                result = submitWord(playerItem, roomItem, roundItem, word);
            }
        } else if("removeword".equals(func)) {
            if (roomItem == null || roundItem == null || word == null) {
                result = "<status>5</status>";
            } else {
                result = removeWord(playerItem, roomItem, roundItem, word);
            }

        } else if ("leaveroom".equals(func)) {
            if (roomItem == null) {
                result = "<status>5</status>";
            } else {
                result = leaveRoom(playerItem, roomItem);
            }

        } else if ("newroom".equals(func)) {
            if (roomName == null) {
                result = "<status>5</status>";
            } else {
                result = newRoom(playerItem, roomName);
            }

        } else if ("registerp".equals(func)) {
            if (roomItem == null) {
                result = "<status>10</status>";
            } else {
                result = initializeLoggedToRoom(playerItem, roomItem, roundItem);
            }

        } else if ("displayround".equals(func)) {
            result = RoundManager.displayRound(roundItem); 

        } else if ("getwords".equals(func)) {
            result = wm.displayWords(playerItem, roomItem, roundItem);
        } else {
            result = "";
        }
        
        return result != null ? result : "";
    }

    /**
     * Given player, room and round, initializes a newly logged in user for the given room.
     * 
     * @param playerItem
     * @param roomItem
     * @param roundItem
     * @return
     */
    
    private @NonNull String initializeLoggedToRoom(@NonNull Player playerItem, @NonNull Room roomItem, @Nullable Round roundItem) {
        rm.enterRoom(playerItem, roomItem);
        rm.notifyForNewWords(playerItem, roomItem);
        return RoomManager.displayRoom(roomItem) + 
               RoundManager.displayRound(roundItem) + 
               wm.displayWords(playerItem, roomItem, roundItem);
    }
    
    /**
     * Leaves the room where the player is and notifies the users.
     * 
     * @param playerItem
     * @param roomItem
     * @return
     */
    
    private @NonNull String leaveRoom(@NonNull Player playerItem, @NonNull Room roomItem) {
        RoomManager.leaveRoom(playerItem);
        rm.notifyForNewWords(playerItem, roomItem);
        return "<status>10</status>";
    }
    
    private @NonNull String removeWord(@NonNull Player playerItem, @NonNull Room roomItem, @NonNull Round roundItem, @NonNull String word) {
        wm.removeWord(word, roundItem, playerItem);
        rm.notifyForNewWords(playerItem, roomItem); 
        return wm.displayWords(playerItem, roomItem, roundItem);
    }
    
    private @NonNull String submitWord(@NonNull Player playerItem, @NonNull Room roomItem, @NonNull Round roundItem, @NonNull String word) {
        wm.submitWord(word, roundItem, playerItem); 
        rm.notifyForNewWords(playerItem, roomItem); 
        return wm.displayWords(playerItem, roomItem, roundItem);
    }
    
    private @NonNull String newRoom(@NonNull Player playerItem, @NonNull String roomName) {
        Room newRoom = rm.newRoom(roomName);
        rm.enterRoom(playerItem, newRoom);
        Round newRound = RoundManager.getRound(newRoom); 
        rm.notifyForNewWords(playerItem, newRoom); 
        return RoomManager.displayRoom(newRoom) +
               RoundManager.displayRound(newRound) +
               wm.displayWords(playerItem, newRoom, newRound);
    }
    
    private @NonNull String moreTime(@NonNull Player playerItem, @NonNull Room roomItem, @NonNull Round roundItem) {
        rom.moreTime(playerItem, roomItem, roundItem);
        rm.notifyForNewWords(playerItem, roomItem);
        // NewWords reveals the round info too, so we don't have to notify other players about that. 
        return RoundManager.displayRound(roundItem) +
                wm.displayWords(playerItem, roomItem, roundItem);
    }
    
    private @NonNull String joinRoom(@NonNull Player playerItem, @NonNull String room) {
        Room newRoom = rm.getRoom(room);
        if (newRoom == null) {
            return "<status>5</status>";
        }
        
        rm.enterRoom(playerItem, newRoom);
        Round newRound = RoundManager.getRound(newRoom);
        rm.notifyForNewWords(playerItem, newRoom); 
        return   RoomManager.displayRoom(newRoom) + 
                 RoundManager.displayRound(newRound) + 
                 wm.displayWords(playerItem, newRoom, newRound);
    }

    private @NonNull String newRound(@NonNull Player playerItem, @NonNull Room roomItem, @Nullable Round roundItem, Status s) {
        s.roundStarted = rom.newRound(roomItem, roundItem, playerItem); 
        Round newRound = RoundManager.getRound(roomItem);
        rm.notifyForNewWords(playerItem, roomItem); 
    
        return RoundManager.displayRound(newRound) +
               wm.displayWords(playerItem, roomItem, newRound);
    }
    
    /**
     * A reverse-AJAX query that will wait until new data is available, or when about 15 seconds have
     * passed, whichever comes first.
     * 
     * Returns at least the new data (possibly also some other redundant data).
     * 
     * @param player
     * @param passcode
     * @return New data as an XML
     */
    
    @GET
    @Path("wordwaiter")
    public String wordwaiter(@QueryParam(value = "player") final String player,
            @QueryParam(value = "passcode") final String passcode) {

        if (player == null || "".equals(player) || passcode == null || "".equals(passcode)) {
            return xmlReply("<data><status>1</status></data>");
        }

        try {
            tx.begin();
        
            Player playerItem = checkPassword(player, passcode);
            if (playerItem == null) {
                // Name and passcode don't match to an existing player
                tx.commit();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Do nothing.
                }
                return xmlReply("<data><status>1</status></data>");
            }

            playerItem.setLastSeen(new Date());
    
            tx.commit();

            // Do the actual waiting.
            
            Status status = mm.trueWordWaiter(playerItem.getName());
    
            tx.begin();
            playerItem = em.find(Player.class, player);
            
            if (playerItem == null) {
                tx.commit();
                return xmlReply("<data><status>5</status></data>");
            }
            
            Room roomItem = rm.getRoom(playerItem);
            Round roundItem = RoundManager.getRound(roomItem);

            // Based on the results of waiting, compile the result.
            
            String roundInfo = RoundManager.displayRound(roundItem);
            String wordInfo = "";
            String chatInfo = "";
            if (status != null) {
                if (status.newWords) {
                    wordInfo = wm.displayWords(playerItem, roomItem, roundItem);
                }
                if (status.newChat) {
                    chatInfo = displayChat(playerItem, roomItem);
                }
            }
    
            tx.commit();

            String result = "<data>" + roundInfo + wordInfo + chatInfo + "</data>";
            System.out.println(result);
            return xmlReply(result);
        } catch (NotSupportedException e1) {
            e1.printStackTrace();
        } catch (SystemException e1) {
            e1.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RollbackException e) {
            e.printStackTrace();
        } catch (HeuristicMixedException e) {
            e.printStackTrace();
        } catch (HeuristicRollbackException e) {
            e.printStackTrace();
        }
        
        return xmlReply("");
    }

    /**
     * A method for processing a POST request, which submits a new chat line.
     * 
     * 
     * 
     * @param   player
     * @param passcode
     * @param chatLine
     * @return New chat lines as an XML
     */
    
    @POST
    @Path("submitchat")
    public String submitChat(@FormParam(value = "player") final String player,
            @FormParam(value = "passcode") final String passcode,
            @FormParam(value="chat") final String chatLine) {

        if (player == null || "".equals(player) || passcode == null || "".equals(passcode)) {
            return xmlReply("<data><status>1</status></data>");
        }

        try {
            tx.begin();
    
            Player playerItem = checkPassword(player, passcode);
            if (playerItem == null) {
                // Name and passcode don't match to an existing player
                tx.commit();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Do nothing.
                }
                return xmlReply("<data><status>1</status></data>");
            }
    
            Room roomItem = rm.getRoom(playerItem);
            
            if (roomItem == null) {
                tx.commit();
                return xmlReply("<data><status>5</status></data>");
            }
            ChatLine c = new ChatLine();
            c.setPlayer(playerItem);
            c.setRoom(roomItem);
            c.setChatRow(chatLine);
            c.setChatTime(new Date());
            em.persist(c);

            rm.notifyForNewChat(playerItem, roomItem);
            
            String result = "<data>" + displayChat(playerItem, roomItem) + "</data>";
            
            tx.commit();
            
            return xmlReply(result);

        } catch (NotSupportedException e1) {
            e1.printStackTrace();
        } catch (SystemException e1) {
            e1.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RollbackException e) {
            e.printStackTrace();
        } catch (HeuristicMixedException e) {
            e.printStackTrace();
        } catch (HeuristicRollbackException e) {
            e.printStackTrace();
        }

        return xmlReply("");
    }
    
    /**
     * Returns all the chat lines that have been submitted in the room that haven't been sent
     * to the player yet.
     * 
     * @param playerItem
     * @param roomItem
     * @return
     */
    
    private String displayChat(Player playerItem, Room roomItem) {

        List<ChatLine> chatLines = em.createQuery("Select c From ChatLine c where c.room = ?1 AND c.id >= ?2 ORDER BY c.id", ChatLine.class).setParameter(1, roomItem).setParameter(2, Integer.valueOf(playerItem.getLastChat())).getResultList();
        
        StringBuilder sb = new StringBuilder();
        for (ChatLine c : chatLines) {
            sb.append("<chatrecord>");
            sb.append("<id>" + c.getId() + "</id>");
            sb.append("<player>" + c.getPlayer().getName() + "</player>");
            sb.append("<line>" + c.getChatRow() + "</line>");
            sb.append("</chatrecord>");
        }
        
        if (chatLines.size() > 0) {
            playerItem.setLastChat(chatLines.get(chatLines.size()-1).getId() + 1);
        }
        
        return sb.toString();
    }

    /**
     * Returns all the valid words for the given round. It is assumed that the validword table has already
     * been initialized by AsyncManager.calculateValidWords.
     * 
     * @param ro
     * @return
     */

    private String getValidWords(Round ro) {
        List<String> validWords = em.createQuery("Select vw.word From ValidWord vw WHERE vw.round = ?1 ORDER BY vw.word", String.class).setParameter(1, ro).getResultList();
        
        StringBuilder sb = new StringBuilder();
        for (String s : validWords) {
            sb.append("<word>" + s + "</word>");
        }

        return sb.toString();
    }


    /**
     * If a player with the given name doesn't exist, create a new player.
     * 
     * @param player
     * @param passcode
     * @return true if a new player was created.
     */
    
    private boolean registerPlayer(String player, String passcode) {
        Player p = em.find(Player.class, player);
        
        if (p != null) {
            // Already exists
            return false;
        }
        
        p = new Player();
        p.setName(player);
        p.setLastSeen(new Date());
        p.setMoreTime(false);
        p.setPasscode(passcode);
        p.setRoom(null);
        p.setReady(false);
        em.persist(p);

        System.out.println("Player created.");
        
        return true;
    }

    /**
     * Checks if the given player name and passcode match the player names and passcodes in the database.
     * 
     * @param player
     * @param passcode
     * @return true if the player with this password exists.
     */
    
    private @Nullable Player checkPassword(String player, String passcode) {
        List<Player> players = em.createQuery("Select p From Player p where p.name LIKE ?1 AND p.passcode LIKE ?2", Player.class).setParameter(1, player).setParameter(2, passcode).getResultList();
        if (players.size() == 0) {
            return null;
        }
        return players.get(0);
    }
    
    private static String xmlReply(String reply) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + reply;
    }
    
}
