package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/**
 * Entity implementation class for Entity: ChatLine
 *
 * @author Teppo Kankaanp‰‰
 */

@Entity
@Table(name = "CHATLINES")
@Access(AccessType.FIELD)
public class ChatLine implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable=false)
	private int id;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
	private Date chatTime;
	
    @Column(nullable = false, length = 256)
	private String chatRow;
    
    @ManyToOne
	private Room room;
	
    @ManyToOne
	private Player player;
	
	
	public ChatLine() {
		super();
	}


    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public Date getChatTime() {
        return chatTime == null ? null : (Date) chatTime.clone();
    }


    public void setChatTime(Date chatTime) {
        this.chatTime = (chatTime == null ? null : (Date) chatTime.clone());
    }


    public String getChatRow() {
        return chatRow;
    }


    public void setChatRow(String chatRow) {
        this.chatRow = chatRow;
    }


    public Room getRoom() {
        return room;
    }


    public void setRoom(Room room) {
        this.room = room;
    }


    public Player getPlayer() {
        return player;
    }


    public void setPlayer(Player player) {
        this.player = player;
    }
   
}
