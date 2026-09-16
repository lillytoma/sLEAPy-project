package com.sleapy.project.models;

;

public class ClientDTO {



    public ClientDTO(Long id, String email, String name, double currentBalance) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.currentBalance = currentBalance;
    }

    public ClientDTO() {

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    private Long id;
    private String email;
    private String name;
    private double currentBalance;
    
}
