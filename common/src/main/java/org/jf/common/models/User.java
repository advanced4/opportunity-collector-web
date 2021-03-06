package org.jf.common.models;

import java.util.UUID;

public class User {

    private UUID id;
    private String username;
    private String email;
    private String password;
    private boolean enabled;
    private boolean admin;

    public boolean isAdmin() {return admin;}
    public void setAdmin(boolean admin) {this.admin = admin;}
    public boolean isEnabled() {return enabled;}
    public void setEnabled(boolean enabled) {this.enabled = enabled;}
    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}
    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
}
