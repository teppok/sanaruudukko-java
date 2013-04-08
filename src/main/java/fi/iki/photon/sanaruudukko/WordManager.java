package fi.iki.photon.sanaruudukko;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import fi.iki.photon.sanaruudukko.entity.Player;
import fi.iki.photon.sanaruudukko.entity.Room;
import fi.iki.photon.sanaruudukko.entity.Round;
import fi.iki.photon.sanaruudukko.entity.Word;
import fi.iki.photon.sanaruudukko.entity.WordList;
import fi.iki.photon.sanaruudukko.entity.WordListPK;

/**
 * WordManager class for managing submitted words.
 * 
 * @author Teppo Kankaanp��
 *
 */

@Stateless
public class WordManager {
    @PersistenceContext
    private EntityManager em;

    @EJB
    private RoundManager rom;
    
    /**
     * Lists all the players and words in the given room for the given round. 
     * If the round is still continuing, only list the summary data for other players. In any case, don't
     * show disabled words for other players than the calling player.
     * 
     * @param playerItem
     * @param roomItem
     * @param roundItem
     * @return XML string containing the data.
     */
    
    public @NonNull String displayWords(@NonNull Player playerItem, @Nullable Room roomItem, @Nullable Round roundItem)  {
        if (roomItem == null) return "";
        
        final boolean roundC = RoundManager.roundContinues(roundItem);
        
        List<Player> playersInRoom = em.createQuery("SELECT p FROM Player p WHERE p.room = ?1 ORDER BY p.name", Player.class).setParameter(1, roomItem).getResultList();
    
        StringBuilder result = new StringBuilder();
        result.append("<players>");
        for (Player p : playersInRoom) {
            if (p != null) {
                if (! p.isActive(40)) continue;
                
                buildPlayer(result, p, roomItem, roundItem, roundC, p.equals(playerItem));
            }
        }
        result.append("</players>");
        String returnValue = result.toString();
        return returnValue != null ? returnValue : "";
    }

    /**
     * Builds a player XML element, given the parameters.
     * 
     * @param result
     * @param p
     * @param roomItem
     * @param roundItem
     * @param roundC
     * @param isCurrentPlayer
     */
    
    private void buildPlayer(@NonNull StringBuilder result, @NonNull Player p, @NonNull Room roomItem, @Nullable Round roundItem, boolean roundC, boolean isCurrentPlayer) {
        int totalScore = 0;
        if (roundItem != null) {
            List<Round> previousRounds = em.createQuery("SELECT ro FROM Round ro WHERE ro.room = ?1 AND ro.id < ?2", Round.class).setParameter(1, roomItem).setParameter(2, Integer.valueOf(roundItem.getId())).getResultList();

            
            for (Round prevRo : previousRounds) {
                System.out.println("prev " + prevRo.getId());
                
                List<String> singularWords = em.createQuery("SELECT w.word FROM Word w WHERE w.round = ?1 GROUP BY w.word HAVING Count(w.player) = 1", String.class).setParameter(1, prevRo).getResultList();

                System.out.println(singularWords.toString());
                
                List<Word> playerWords = em.createQuery("SELECT w FROM Word w WHERE w.player = ?1 AND w.round = ?2", Word.class).setParameter(1, p).setParameter(2, prevRo).getResultList();

                System.out.println(playerWords.toString());
                
                for (Word pw : playerWords) {
                    if (! pw.isDisabled() && singularWords.contains(pw.getWord())) {
                        totalScore += score(pw.getWord());
                    }
                }
            }
        }
        
        result.append("<player>");
        result.append("<name>" + p.getName() + "</name>");
        result.append("<active>" + (p.isActive(20)?"t":"f") + "</active>");
        result.append("<ready>" + (p.isReady()?"t":"f") + "</ready>");
        result.append("<moretime>" + (p.isMoreTime()?"t":"f") + "</moretime>");
        result.append("<totalscore>" + totalScore + "</totalscore>");

   
        if (roundC && ! isCurrentPlayer) {
            buildWordCount(result, p, roundItem);
        } else {
            buildAllWords(result, p, roundItem, roundC, isCurrentPlayer);
        }
        result.append("</player>");
    }

    /**
     * Builds the words element in the case of summary.
     * 
     * @param result
     * @param p
     * @param roundItem
     */
    
    private void buildWordCount(@NonNull StringBuilder result, @NonNull Player p, @Nullable Round roundItem) {
        if (roundItem == null) {
            result.append("<mode>0</mode><wordcount>0</wordcount>");
        } else {
            Object count = em.createQuery("SELECT count(w) FROM Word w WHERE w.player = ?1 AND w.round = ?2 AND w.disabled = ?3").setParameter(1, p).setParameter(2, roundItem).setParameter(3, new Boolean(false)).getSingleResult();
            if (count != null) {
                result.append("<mode>0</mode><wordcount>" + count + "</wordcount>");
            }
        }
    }

    /**
     * Builds all the words for the given player according to parameters.
     * 
     * @param result
     * @param p
     * @param roundItem
     * @param roundC
     * @param isCurrentPlayer
     */
    
    private void buildAllWords(@NonNull StringBuilder result, @NonNull Player p, @Nullable Round roundItem, boolean roundC, boolean isCurrentPlayer) {
        if (roundItem == null) {
            result.append("<mode>1</mode><thisroundscore>0</thisroundscore>");
        } else {
            List<String> duplicateWords = null;
            if (! roundC) {
                duplicateWords = em.createQuery("SELECT w.word FROM Word w WHERE w.round = ?1 GROUP BY w.word HAVING Count(w.player) > 1", String.class).setParameter(1, roundItem).getResultList();
            }
            
            
            List<Word> playerWords = em.createQuery("SELECT w FROM Word w WHERE w.player = ?1 AND w.round = ?2 ORDER BY w.word", Word.class).setParameter(1, p).setParameter(2, roundItem).getResultList();
    
            int thisRoundScore = 0;
            result.append("<mode>1</mode>");
            for (Word w : playerWords) {
                if (! w.isDisabled() || isCurrentPlayer) {
                    
                    int score = score(w.getWord());
                    boolean isDuplicate = false;
                    result.append("<item>");
                    result.append("<disabled>" + (w.isDisabled()?"t":"f") + "</disabled>");
                    result.append("<word>" + w.getWord() + "</word>");
                    result.append("<languagecheck>" + (w.isLanguageCheck()?"t":"f") + "</languagecheck>");
                    if (duplicateWords != null) {
                        isDuplicate = duplicateWords.contains(w.getWord());
                        result.append("<duplicate>" + (isDuplicate?"t":"f") + "</duplicate>");
                    } else {
                        result.append("<duplicate>f</duplicate>");
                    }
                    
                    if (w.isDisabled() || isDuplicate) {
                        score = 0;
                    }
                    
                    result.append("<score>" + score + "</score>");
                    result.append("</item>");
                    
                    thisRoundScore += score;
                }
            }
            result.append("<thisroundscore>" + thisRoundScore + "</thisroundscore>");
        }
    }

    /**
     * Returns the score value for the given word.
     * 
     * @param word
     * @return score
     */
    
    private static int score(@Nullable String word) {
        if (word == null) return 0;
        if (word.length() < 3) return 0;
        if (word.length() == 3 || word.length() == 4) return 1;
        if (word.length() == 5) return 2;
        if (word.length() == 6) return 3;
        if (word.length() == 7) return 5;
        return 11;
    }

    /**
     * Disables or enables the given word.
     * 
     * @param word
     * @param ro
     * @param p
     */
    
    public void removeWord(@NonNull String word, @NonNull Round ro, @NonNull Player p) {
        List<Word> words = em.createQuery("SELECT w FROM Word w WHERE w.round = ?1 AND w.player = ?2 AND w.word LIKE ?3", Word.class).setParameter(1, ro).setParameter(2, p).setParameter(3, word).getResultList();
        
        if (words.size() > 0) {
            Word w = words.get(0);
            w.setDisabled(! w.isDisabled());
        }
    }

    /**
     * Adds a word to the player. Checks if the word has already been submitted and if it actually
     * exists in the grid. If the word is ok, check it against WordList to determine if it's valid,
     * and flag it appropriately. Add it to the database.
     * @param word
     * @param ro
     * @param p
     */
    
    public void submitWord(@NonNull String word, @NonNull Round ro, @NonNull Player p) {
        System.out.println("Submitted: " + word);
        if (! RoundManager.roundContinues(ro)) return;

        // Don't allow duplicate words for the same player and round.
        List<Word> words = em.createQuery("SELECT w FROM Word w WHERE w.round = ?1 AND w.player = ?2 AND w.word LIKE ?3", Word.class).setParameter(1, ro).setParameter(2, p).setParameter(3, word).getResultList();
        
        System.out.println("Duplicate?");
        
        if (words.size() > 0) {
            System.out.println("Duplicate!");
            return;
        }
        
        if (! checkWord(ro, word)) {
            return;
        }

        WordListPK wlpk = new WordListPK();
        wlpk.word = word;
        wlpk.language = "FI";
        WordList wl = em.find(WordList.class, wlpk);
        
        Word w = new Word();
        w.setWord(word);
        w.setDisabled(false);
        if (wl != null) {
            w.setLanguageCheck(true);
        } else {
            w.setLanguageCheck(false);
        }
        w.setPlayer(p);
        w.setRound(ro);
        
        em.persist(w);
        
    }

    /**
     * Checks if the given word exists on the board for the given round.
     * @param ro
     * @param word
     * @return true if the word is on the board.
     */
    
    public static boolean checkWord(@NonNull Round ro, @NonNull String word) {
        int tmpx = 0, tmpy = 0;
        int matchLength = 0;

        char[] board = ro.getBoard().toCharArray();
        char[] wordArray = word.toCharArray();
        
        int[] x = new int[16];
        int[] y = new int[16];
        
        int looping = 0;
        while (looping == 0) {
            boolean allow = false;
            // Only allow locations that are close to the previous matched character and are not already in x,y table.
            if (matchLength > 0) {
                if (Math.abs(x[matchLength - 1] - tmpx) < 2 && Math.abs(y[matchLength - 1] - tmpy) < 2) {
                    allow = true;
                }
                for (int i = 0; i < matchLength; i++) {
                    if (x[i] == tmpx && y[i] == tmpy) {
                        allow = false;
                    }
                }
            } else { allow = true; }
            
            if (allow && wordArray[matchLength] == board[tmpy * 4 + tmpx]) {
                matchLength++;
                x[matchLength-1] = tmpx;
                y[matchLength-1] = tmpy;
                tmpx = tmpy = 0;
                if (matchLength == wordArray.length) {
                    looping = 1;
                }
            } else {
                if (tmpx == 3 && tmpy == 3) {
                    if (matchLength == 0) {
                        looping = 2;
                    } else {
                        tmpx = x[matchLength-1];
                        tmpy = y[matchLength-1];
                        matchLength--;
                    }
                }
                if (tmpx == 3 && tmpy == 3) {
                    if (matchLength == 0) {
                        looping = 2;
                    } else {
                        tmpx = x[matchLength-1];
                        tmpy = y[matchLength-1];
                        matchLength--;
                    }
                }
                
                tmpx++;
                if (tmpx > 3) {
                    tmpy++;
                    tmpx = 0;
                }
            }
        }

        return looping == 1;
    }
}
