package com.heavenbites.model;

public class Admin extends Person {
    public Admin(String id, String name, String email, String hash) { super(id, name, email, hash); }
    public Admin(String name, String email, String rawPassword) { super(name, email, rawPassword); }

    @Override public String getRole() { return "admin"; }
    @Override public String greeting() { return "Welcome back, Admin " + name; }
}