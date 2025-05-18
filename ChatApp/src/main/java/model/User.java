package model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;
    private String profilePic;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_chat_subscriptions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "chat_id")
    )
    private Set<Chat> subscribedChats = new HashSet<>();

    
    public int getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public Set<Chat> getSubscribedChats() {
        return subscribedChats;
    }

    public void setSubscribedChats(Set<Chat> subscribedChats) {
        this.subscribedChats = subscribedChats;
    }

    
    public void addSubscription(Chat chat) {
        this.subscribedChats.add(chat);
        chat.getSubscribedUsers().add(this);
    }

    public void removeSubscription(Chat chat) {
        this.subscribedChats.remove(chat);
        chat.getSubscribedUsers().remove(this);
    }
}
