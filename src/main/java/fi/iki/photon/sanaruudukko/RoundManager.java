package fi.iki.photon.sanaruudukko;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import fi.iki.photon.sanaruudukko.entity.Player;
import fi.iki.photon.sanaruudukko.entity.Room;
import fi.iki.photon.sanaruudukko.entity.Round;

/**
 * Performs round related actions.
 * 
 * @author Teppo Kankaanp‰‰
 *
 */

@Stateless
public class RoundManager {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private WordManager wm;
    
    @EJB
    private AsyncManager am;

    @EJB
    private MessageManager mm;

    /**
     * Given a round, returns its data as XML.
     * @param ro
     * @return An XML string with the data.
     */
    
    public static @NonNull String displayRound(@Nullable Round ro) {
        int roundStatus = 0;
        String timeResult = "", boardResult = "";
        
        if (ro == null) { return "<round><board>????????????????</board></round>"; }
        Calendar c = Calendar.getInstance();
        c.setTime(ro.getRoundStart());
        c.add(Calendar.SECOND, Sanaruudukko.ROUNDLENGTH);
        // c contains end round time.
        
        if (ro.getRoundStart().after(new Date())) {
            roundStatus = 1; // Round starting
        }
        
        if (c.getTime().before(new Date())) {
            roundStatus = 2; // Round ended
        }
    
        // round continues, show the time until the end of the round
        if (roundStatus == 0) {
            long timeDiff = (c.getTimeInMillis() - new Date().getTime()) / 10; // time difference in hundreths of a second
            timeResult = "<time>" + timeDiff + "</time>";
        }
    
        // round starting, show the time until the start of the round
        if (roundStatus == 1) {
            long timeDiff = (ro.getRoundStart().getTime() - new Date().getTime()) / 10; // time difference in hundreths of a second
            timeResult = "<time>" + timeDiff + "</time><starting>1</starting><board>????????????????</board>";
        }
    
        
        if (roundStatus == 0 || roundStatus == 2) {
            boardResult = "<board>" + ro.getBoard() + "</board>";
        }
        
        return "<round>" + timeResult + boardResult + "</round>";
    }

    /**
     * Given a room, returns its current round.
     * 
     * @param room
     * @return Current round.
     */
    
    public static @Nullable Round getRound(@Nullable Room room) {
        if (room == null) return null;
        
        return room.getCurrentRound();
        /*
        List<Integer> roundIds = em.createQuery("Select max(ro.roundId) from Round ro WHERE ro.room = ?1", Integer.class).setParameter(1, room).getResultList();
        
        if (roundIds.size() > 0) {
            if (roundIds.get(0) == null) return null;
            return em.find(Round.class, roundIds.get(0));
        }
        */
    }

    /**
     * The given player requests for more time in the room for the given round.
     * If all active players in the room have requested it, add more time and reset the
     * moretime toggles for all players.
     * 
     * @param playerItem
     * @param roomItem
     * @param roundItem
     * @return true if more time was completed.
     */
    
    public boolean moreTime(@NonNull Player playerItem, @NonNull Room roomItem, @NonNull Round roundItem) {
        System.out.println("MT1");
        if (! roundContinues(roundItem))  return false;
        
        System.out.println("MT2");
        
        Calendar c = Calendar.getInstance();
        c.setTime(roundItem.getRoundStart());
        c.add(Calendar.SECOND, Sanaruudukko.MORETIMELIMIT);
        if (c.getTime().after(new Date())) return false;
        
        playerItem.setMoreTime(! playerItem.isMoreTime());
        
        List<Player> playersInRoom = em.createQuery("SELECT p FROM Player p WHERE p.room = ?1", Player.class).setParameter(1, roomItem).getResultList();
    
        boolean totalMoreTime = true;
        
        for (Player p : playersInRoom) {
            if (! p.isMoreTime() && p.isActive(20)) totalMoreTime = false;
        }
        
        if (totalMoreTime) {
            c.setTime(roundItem.getRoundStart());
            c.add(Calendar.SECOND, Sanaruudukko.MORETIMELIMIT);
            roundItem.setRoundStart(c.getTime());
            
            for (Player p : playersInRoom) {
                p.setMoreTime(false);
            }
            return true;
        }
        return false;
    }

    /**
     * The given player requests a new round to be started. If all active players in the room
     * are requesting it, start a new round with a random board.
     * 
     * @param roomItem
     * @param roundItem
     * @param playerItem
     * @return true if a new round was started.
     */
    
    public synchronized boolean newRound(@NonNull Room roomItem, @Nullable Round roundItem, @NonNull Player playerItem) {
        System.out.println("newround");
        if (roundContinues(roundItem)) return false;
        
        playerItem.setReady(! playerItem.isReady());

        List<Player> players = em.createQuery("Select p From Player p where p.room = ?1", Player.class).setParameter(1, roomItem).getResultList();
        
        System.out.println("Checking players");
        boolean ready = true;
        int playerNum = 0;
        
        for (Player p : players) {
            if (p.isActive(20)) {
                if (! p.isReady()) {
                    ready = false;
                }
                
                playerNum++;
            }
        }
        if (! ready) return false;
        
        Round r = new Round();
        r.setRoom(roomItem);
        
        Calendar c = Calendar.getInstance();
        if (playerNum == 1) {
            c.add(Calendar.SECOND, Sanaruudukko.PREROUNDTIME_SINGLE);
        } else {
            c.add(Calendar.SECOND, Sanaruudukko.PREROUNDTIME_FULL);
        }
            
        r.setRoundStart(c.getTime());
        r.setBoard(randomizeBoard());
        em.persist(r);
        
        roomItem.setCurrentRound(r);
        
        System.out.println("Created new round");
    
        
        for (Player p : players) {
            p.setReady(false);
            p.setMoreTime(false);
        }
        return true;
    }

    /**
     * Does the given round still continue,
     * 
     * @param roundItem
     * @return true if the round continues.
     */
    
    public static boolean roundContinues(@Nullable Round roundItem) {
        if (roundItem == null) return false;
        
        Calendar c = Calendar.getInstance();
        c.setTime(roundItem.getRoundStart());
        c.add(Calendar.SECOND, Sanaruudukko.ROUNDLENGTH);
        return c.getTime().after(new Date());

    }
    
    static char[][] dices = { 
            { 'A','I','S','B','U','J' },
            { 'A','E','E','N','E','A' },
            { 'a','I','o','N','S','T' },
            { 'A','N','P','F','S','K' },
            { 'A','P','H','S','K','O' },
            { 'D','E','S','R','I','L' },
            { 'E','I','E','N','U','S' },
            { 'H','I','K','N','M','U' },

            { 'A','G','A','a','L','a' },
            { 'C','I','O','T','M','U' },
            { 'A','J','T','O','T','O' },
            { 'E','I','T','O','S','S' },
            { 'E','L','Y','T','T','R' },
            { 'A','K','I','T','M','V' },
            { 'A','I','L','K','V','Y' },
            { 'A','L','R','N','N','U' } };

    /**
     * Returns a random board using the given dices.
     * @return Random board.
     */
    
    private static @NonNull String randomizeBoard() {
        int[] array = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        
        Random random = new Random();
        
        for (int i = 15; i>=0; i--) {
            int moveLoc = random.nextInt(i + 1);
            int tmp = array[moveLoc];
            array[moveLoc] = array[i];
            array[i] = tmp;
        }
        
        char[] result = new char[16];
        
        for (int i = 0; i < 16; i++) {
            result[i] = dices[array[i]][random.nextInt(6)];
        }
        System.out.println(new String(result));
        
        return new String(result);
    }

    
}
