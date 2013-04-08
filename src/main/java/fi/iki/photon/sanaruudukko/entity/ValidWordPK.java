package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;

/** 
 * Primary key class for ValidWord 
 * 
 * @author Teppo Kankaanp��
 */

public class ValidWordPK implements Serializable {
    private static final long serialVersionUID = 1L;
    public String word = null;
    public int round = 0;

    @Override
    public int hashCode() {
        if (word != null) return word.hashCode()*3 + round*7;
        return 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (! (o instanceof ValidWordPK)) return false;
        if (((ValidWordPK) o).word == null) {
            return word == null && ((ValidWordPK) o).round == round;
        }
        return ((ValidWordPK) o).word.equals(word) && ((ValidWordPK) o).round == round;
    }
}
