package model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    
    private String logFile;
    
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin;
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    
    public Date getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public String getLogFile() {
        return logFile;
    }
    
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }
    
    public User getAdmin() {
        return admin;
    }
    
    public void setAdmin(User admin) {
        this.admin = admin;
    }
}
