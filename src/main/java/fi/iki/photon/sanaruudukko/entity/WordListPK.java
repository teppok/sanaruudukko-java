package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;

/** 
 * Primary key class for WordList 
 * 
 * @author Teppo Kankaanp��
 */

public class WordListPK implements Serializable {
    private static final long serialVersionUID = 1L;
    public String word;
    public String language;
    
    @Override
    public int hashCode() {
        return word.hashCode()*3 + language.hashCode()*7;
    }

    private static boolean equalStrings(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof WordListPK) {
            return equalStrings(((WordListPK) o).word, word) && equalStrings(((WordListPK) o).language, language);
        }
        return false;
    }
}
