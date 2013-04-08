package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity implementation class for Entity: ValidWord
 *
 * Contains the valid words checked against the words in wordlist for
 * each round in the database. 
 *
 * @author Teppo Kankaanp‰‰
 */
@IdClass(ValidWordPK.class)
@Entity
@Table(name = "VALIDWORDS")
@Access(AccessType.FIELD)
public class ValidWord implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
    @Column(nullable=false, length=16)
	private String word;
	
	@Id
	@ManyToOne
    @JoinColumn(nullable=false)
	private Round round;
	
	public ValidWord() {
		super();
	}

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

}

