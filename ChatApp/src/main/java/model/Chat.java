package model;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToMany(mappedBy = "subscribedChats", fetch = FetchType.LAZY)
    private Set<User> subscribedUsers = new HashSet<>();
    
    private String name;
    
    
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

    public Set<User> getSubscribedUsers() {
        return subscribedUsers;
    }

    public void setSubscribedUsers(Set<User> subscribedUsers) {
        this.subscribedUsers = subscribedUsers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
