package Server;

import java.util.Base64;
import Enums.*;

public class User {

    private String userId;

    private Integer numberOfActiveSessions;


    private String userPassword;

    private UserState userStatus;

    public User(String id, String password, UserState status) {
        this.userId = id;
        this.userPassword = password;
        this.userStatus = status;
        this.numberOfActiveSessions = 1;
    }

    public Integer getNumberOfSessions() {
        return this.numberOfActiveSessions;
    }

    public void addNewSession() {
        this.numberOfActiveSessions++;
    }

    public UserState getUserState() {
        return this.userStatus;
    }
    public String getUserId() {
        return this.userId;
    }

    public String getEncodedPassword() {
        return Base64.getEncoder().encodeToString(this.userPassword.getBytes());
    }
}
