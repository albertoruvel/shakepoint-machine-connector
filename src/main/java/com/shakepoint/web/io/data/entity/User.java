package com.shakepoint.web.io.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "User")
@Table(name = "user")
public class User {

    @Id
    private String id;

    @Column(name = "email")
    private String email;
	
	@Column(name = "role")
	private String role; 

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
	
	public String getRole() {
		return role; 
	}
	
	public void setRole(String role) {
		this.role = role; 
	}
}
