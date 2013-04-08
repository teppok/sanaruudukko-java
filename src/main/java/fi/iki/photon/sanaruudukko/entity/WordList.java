package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity implementation class for Entity: WordList
 *
 * Contains valid words in different languages. The table will be initialized
 * on deployment time externally using text files.
 * 
 * @author Teppo Kankaanp‰‰
 */
@IdClass(WordListPK.class)
@Entity
public class WordList implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
    @Column(nullable=false, length=16)
    private String word;
	
	@Id
    @Column(nullable=false, length=2)
	private String language;
    

	public WordList() {
		super();
	}
   
}
