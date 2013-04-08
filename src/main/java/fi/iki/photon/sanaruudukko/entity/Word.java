package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;
import javax.persistence.*;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Entity implementation class for Entity: Word
 *
 * languageCheck will be true if the word matches a word in WordList.
 * 
 * @author Teppo Kankaanp‰‰
 */
@Entity
@Table(name = "WORDS")
@Access(AccessType.FIELD)
public class Word implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private Round round;
    
    @ManyToOne
    @JoinColumn(nullable=false)
    private Player player;
    
    @Column(nullable=false, length = 16)
    private String word;
    
    @Column(nullable=false)
    private boolean languageCheck;
	
    @Column(nullable=false)
    private boolean disabled;

	public Word() {
		super();
	}

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public @Nullable String getWord() {
        return word;
    }

    public void setWord(@NonNull String word) {
        this.word = word;
    }

    public boolean isLanguageCheck() {
        return languageCheck;
    }

    public void setLanguageCheck(boolean languageCheck) {
        this.languageCheck = languageCheck;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }
   
}
