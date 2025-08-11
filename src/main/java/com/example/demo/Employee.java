package com.example.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "DPS_USER")
public class Employee {
    @Id
    @Column(name = "ID")
    private int id;
    
    @Column(name = "LOGIN")
    private String login;
    
    @Column(name = "FIRST_NAME")
    private String firstName;
    
    @Column(name = "LAST_NAME")
    private String lastName;
    
    @Column(name = "EMAIL")
    private String email;
    
 
 
    public Employee() {}
    
    // Constructor to initialize transient fields with defaults
    public Employee(int id, String login, String firstName, String lastName, String email) {
        this.id = id;
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
       
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
 
    
   
}
