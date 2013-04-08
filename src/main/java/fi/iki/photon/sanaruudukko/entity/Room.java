package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Entity implementation class for Entity: Room
 *
 * Contains a nullable reference to current round in this room.
 * This is basically a duplicate attribute for 
 * Round r WHERE r.id = ( SELECT MAX(id) FROM Round r2 WHERE r2.roomid = ? )
 * 
 * @author Teppo Kankaanp‰‰
 */

@Entity
@Table(name = "ROOMS")
@Access(AccessType.FIELD)
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable=false)
    private int id;

    @Column(nullable=false, length=16)
    private String name;

    @OneToOne
    @JoinColumn(nullable = true)
    private Round currentRound;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Round currentRound) {
        this.currentRound = currentRound;
    }
    
}
