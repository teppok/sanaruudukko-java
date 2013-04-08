package fi.iki.photon.sanaruudukko.entity;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity implementation class for Entity: Player
 *
 * moreTime and ready are toggles for players requesting more time or another round.
 * 
 * lastSeen is the time when the player was last seen by the server. It will be far in the
 * past if the player disconnects.

 * @author Teppo Kankaanp��
 */

@Entity
@Table(name = "PLAYERS")
@Access(AccessType.FIELD)
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(nullable = false, length = 16)
    private String name;

    @Column(nullable = false, length = 16)
    private String passcode;

    @ManyToOne
    private Room room;

    @Column(nullable = false)
    private boolean ready;

    @Column(nullable = false)
    private boolean moreTime;

    @Column(nullable = true)
    private int lastChat;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date lastSeen;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isMoreTime() {
        return moreTime;
    }

    public void setMoreTime(boolean moreTime) {
        this.moreTime = moreTime;
    }

    public int getLastChat() {
        return lastChat;
    }

    public void setLastChat(int lastChat) {
        this.lastChat = lastChat;
    }

    public Date getLastSeen() {
        return lastSeen == null ? null : (Date) lastSeen.clone();
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = (lastSeen == null ? null : (Date) lastSeen.clone());
    }
    
    public boolean isActive(int seconds) {
        Calendar c = Calendar.getInstance();
        c.setTime(lastSeen);
        c.add(Calendar.SECOND, seconds);
        return c.getTime().after(new Date());
    }
}
