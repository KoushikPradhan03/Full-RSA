package com.heavenbites.model;

public class Cashier extends Person {
    public Cashier(String id, String name, String email, String hash) { super(id, name, email, hash); }
    public Cashier(String name, String email, String rawPassword) { super(name, email, rawPassword); }

    @Override public String getRole() { return "cashier"; }
    @Override public String greeting() { return "Counter open, " + name; }
}