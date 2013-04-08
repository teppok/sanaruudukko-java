package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/**
 * Entity implementation class for Entity: Round
 *
 * @author Teppo Kankaanp‰‰
 */
@Entity
@Table(name = "ROUNDS")
@Access(AccessType.FIELD)
public class Round implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date roundStart;

    @Column(nullable = false, length=16)
    private String board;

    @ManyToOne
    private Room room;
    
	private static final long serialVersionUID = 1L;

	public Round() {
		super();
	}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getRoundStart() {
        return roundStart == null ? null : (Date) roundStart.clone();
    }

    public void setRoundStart(Date roundStart) {
        this.roundStart = (roundStart == null ? null : (Date) roundStart.clone());
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
