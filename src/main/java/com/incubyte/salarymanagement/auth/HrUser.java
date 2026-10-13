package com.incubyte.salarymanagement.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hr_user")
public class HrUser {
    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    protected HrUser() {
    }

    public HrUser(String email, String passwordHash) {
        this.id = 1;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
    }

    public Short getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
}
