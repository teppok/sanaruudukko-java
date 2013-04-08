package fi.iki.photon.sanaruudukko;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
//import org.eclipse.jdt.annotation.NonNull;
//import javax.validation.constraints.NotNull;
//import javax.validation.constraints.Null;

import fi.iki.photon.sanaruudukko.entity.ChatLine;
import fi.iki.photon.sanaruudukko.entity.Player;
import fi.iki.photon.sanaruudukko.entity.Room;
import fi.iki.photon.sanaruudukko.entity.Round;
import fi.iki.photon.sanaruudukko.entity.ValidWord;
import fi.iki.photon.sanaruudukko.entity.Word;

/**
 * RoomManager class for managing rooms.
 * 
 * @author Teppo Kankaanp��
 *
 */

@Stateless
public class RoomManager {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private MessageManager mm;

    /**
     * Returns the room the player is in.
     * @param player
     * @return A room object or null.
     */
    
    public @Nullable Room getRoom(@NonNull Player player) {
        List<Room> rooms = em.createQuery("Select r FROM Player p JOIN p.room r WHERE p = ?1", Room.class).setParameter(1, player).getResultList();
        if (rooms.size() > 0) {
            return rooms.get(0);
        }
        return null;
    }

    /**
     * Returns the room data as an XML.
     * @param r
     * @return Data as an XML string.
     */
    
    public static @NonNull String displayRoom(@Nullable Room r) {
        if (r == null) return "";
        return "<room><id>" + r.getId() + "</id><roomname>" + r.getName() + "</roomname></room>";
    }

    /**
     * Places the player into the given room and initializes the room specific
     * values.
     * @param p
     * @param joinedRoom
     */
    
    public void enterRoom(@NonNull Player p, @Nullable Room joinedRoom) {
        if (joinedRoom != null ) {
            p.setLastChat(getLastChat(joinedRoom));
        }
        
        p.setReady(false);
        p.setMoreTime(false);
        p.setRoom(joinedRoom);
        
        
    }

    /**
     * Given a player, remove him from the room he is in.
     * @param p
     */
    
    public static void leaveRoom(@NonNull Player p) {
        p.setRoom(null);
    }
    
    /**
     * Returns all the rooms in the system.
     * @return Room list as an XML string.
     */

    public @NonNull String displayRooms() {
        List<Room> rooms = em.createQuery("SELECT r FROM Room r ORDER BY r.id", Room.class).getResultList();

        StringBuilder result = new StringBuilder();
        result.append("<rooms>");
        for (Room r : rooms) {
            if (r != null) {
                int activePlayers = countActivePlayers(r, 20);
                if (activePlayers > 0) {
                    result.append("<room><id>" + r.getId() + "</id><roomname>" + r.getName() + "</roomname>");
                    result.append("<players>" + activePlayers + "</players></room>");
                }
            }
        }
        result.append("</rooms>");
        String returnVal = result.toString();
        return returnVal != null ? returnVal : "";
    }

    /**
     * Counts the number of players in the room that have been active during the last 'seconds' seconds.
     * 
     * @param r
     * @param seconds
     * @return Number of active players.
     */
    
    private int countActivePlayers(@NonNull Room r, int seconds) {
        List<Player> playersInRoom = em.createQuery("SELECT p FROM Player p WHERE p.room = ?1", Player.class).setParameter(1, r).getResultList();
        
        int players = 0;
        for (Player p : playersInRoom) {
            if (p.isActive(seconds)) {
                players++;
            }
        }
        return players;
    }
    
    /**
     * Returns the room by its id.
     * @param roomId
     * @return Room.
     */
    
    public @Nullable Room getRoom(String roomId) {
        return em.find(Room.class, Integer.valueOf(roomId));
    }
    
    /**
     * Creates a new room with the given name.
     * 
     * If the system has old rooms with no active players, delete such
     * a room and all its contents, kick the users, and reuse its id for the new room.
     * 
     * Otherwise just create a new instance of a room.
     * 
     * @param roomName
     * @return The newly created room.
     */

    public @NonNull Room newRoom(String roomName) {
        List<Room> rooms = em.createQuery("SELECT r FROM Room r", Room.class).getResultList();
    
        Room chosenRoom = null;
        
        for (Room r : rooms) {
            if (r != null)  {
                if (countActivePlayers(r, 60) == 0) {
                    chosenRoom = r;
                    break;
                }
            }
        }
    
        if (chosenRoom != null) {
            // reuse chosen room
            chosenRoom.setName(roomName);
            chosenRoom.setCurrentRound(null);

            List<Player> playersInRoom = em.createQuery("SELECT p FROM Player p WHERE p.room = ?1", Player.class).setParameter(1, chosenRoom).getResultList();

            for (Player p : playersInRoom) {
                p.setRoom(null);
            }

            List<ChatLine> chatsInRoom = em.createQuery("SELECT c FROM ChatLine c WHERE c.room = ?1", ChatLine.class).setParameter(1, chosenRoom).getResultList();

            for (ChatLine c : chatsInRoom) {
                em.remove(c);
            }
            
            List<Round> roundsInRoom = em.createQuery("SELECT ro FROM Round ro WHERE ro.room = ?1", Round.class).setParameter(1, chosenRoom).getResultList();
            
            for (Round ro : roundsInRoom) {
                List<Word> wordsInRound = em.createQuery("SELECT w FROM Word w WHERE w.round = ?1", Word.class).setParameter(1, ro).getResultList();

                for (Word w : wordsInRound) {
                    em.remove(w);
                }

                List<ValidWord> validWordsInRound = em.createQuery("SELECT vw FROM ValidWord vw WHERE vw.round = ?1", ValidWord.class).setParameter(1, ro).getResultList();

                for (ValidWord w : validWordsInRound) {
                    em.remove(w);
                }

                em.remove(ro);
            }

            
            
            return chosenRoom;
        }
        Room newRoom = new Room();
        newRoom.setName(roomName);
        em.persist(newRoom);
        return newRoom;
    }

    /**
     * Returns the last chat id in the given room.
     * @param roomItem
     * @return Last chat id.
     */
    
    private int getLastChat(@NonNull Room roomItem) {
        Integer lastChat = em.createQuery("Select max(c.id) From ChatLine c where c.room = ?1", Integer.class).setParameter(1, roomItem).getSingleResult();

        if (lastChat == null) return -1;
        return lastChat.intValue();
    }
    
    /**
     * Notifies the players in the given room that are not the given player, for new chat items.
     * @param playerItem
     * @param roomItem
     */
    
    public void notifyForNewChat(@NonNull Player playerItem, @NonNull Room roomItem) {
        notifyForNewWords(playerItem, roomItem);
    }

    /**
     * Notifies the players in the given room that are not the given player, for new word/player items.
     * @param playerItem
     * @param roomItem
     */

    public void notifyForNewWords(@NonNull Player playerItem, @NonNull Room roomItem) {
        List<Player> playersInRoom = em.createQuery("SELECT p FROM Player p WHERE p.room = ?1", Player.class).setParameter(1, roomItem).getResultList();
        
        for (Player p : playersInRoom) { 
            if (!p.equals(playerItem)) {
                mm.sendNotify(p.getName());
            }
        }
    }
}
