package com.heavenbites.model;

public class Customer extends Person {
    public Customer(String id, String name, String email, String hash) { super(id, name, email, hash); }
    public Customer(String name, String email, String rawPassword) { super(name, email, rawPassword); }

    @Override public String getRole() { return "customer"; }
}