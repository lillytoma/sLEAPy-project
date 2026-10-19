package com.sleapy.project.models.dtos;

public class LoginRequestDTO {
    
    private String email; // User's email (unique identifier)
    private String password; // User's password (to be verified against hash)

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
