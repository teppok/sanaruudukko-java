package fi.iki.photon.sanaruudukko;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.LockModeType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import fi.iki.photon.sanaruudukko.entity.Player;
import fi.iki.photon.sanaruudukko.entity.Room;
import fi.iki.photon.sanaruudukko.entity.Round;
import fi.iki.photon.sanaruudukko.entity.ValidWord;

@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class AsyncManager {

    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @EJB
    private WordManager wm;

    @EJB
    private MessageManager mm;

    /**
     * Notifies the users in the given room that the round has started.
     * 
     * @param roomId
     */
    
    private void roundStarted(final int roomId) {
        try {
            tx.begin();

            final Room roomItem = em.find(Room.class, Integer.valueOf(roomId));

            final List<Player> players = em
                    .createQuery("Select p From Player p where p.room = ?1",
                            Player.class).setParameter(1, roomItem)
                    .getResultList();

            for (final Player p : players) {
                mm.sendNewRoundNotify(p.getName());
            }

            tx.commit();
        } catch (SecurityException | IllegalStateException | RollbackException
                | HeuristicMixedException | HeuristicRollbackException
                | SystemException | NotSupportedException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Given a roomid and round id, polls the database for up to 20 seconds and
     * when the round has actually started (when roundstarted is not in the future), 
     * notifies the players in the room.
     * 
     * @param roomId
     * @param roundId
     */
    
    @Asynchronous
    public void newRoundWaiter(final int roomId, final int roundId) {
        System.out.println("Waiting for round start");

        boolean newRoundStarted = false;
        int looped = 0;
        while (!newRoundStarted && looped < 100) {
            looped++;
            try {
                tx.begin();

                final Round ro = em.find(Round.class, Integer.valueOf(roundId), LockModeType.OPTIMISTIC);

                if (ro.getRoundStart().before(new Date())) {
                    newRoundStarted = true;
                }
                tx.commit();
            } catch (NotSupportedException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SystemException | SecurityException | IllegalStateException e1 ) {
                try {
                    tx.rollback();
                } catch (IllegalStateException | SecurityException
                        | SystemException e) {
                    e.printStackTrace();
                }
                e1.printStackTrace();
            }

            try {
                Thread.sleep(200);
            } catch (final InterruptedException e) {
                // If interrupted for some reason, exit
                newRoundStarted = true;
            }
        }
        System.out.println("Round started!");

        roundStarted(roomId);

    }

    @Asynchronous
    public void calculateValidWords(int roundId) {
        try {
            tx.begin();

            Round ro = em.find(Round.class, Integer.valueOf(roundId));
            if (ro == null) return;
            
            final List<String> allWords = em
                    .createQuery(
                            "Select wl.word From WordList wl WHERE wl.language = ?1",
                            String.class).setParameter(1, "FI").getResultList();

            for (final String s : allWords) {
                if (s != null) {
                    if (WordManager.checkWord(ro, s)) {
                        final ValidWord vw = new ValidWord();
                        vw.setWord(s);
                        vw.setRound(ro);
                        em.persist(vw);
                        System.out.println("VALID " + s);
                    }
                }
            }

            tx.commit();
        } catch (SecurityException | IllegalStateException | RollbackException
                | HeuristicMixedException | HeuristicRollbackException
                | SystemException | NotSupportedException e1) {
            e1.printStackTrace();
        }

    }

}
