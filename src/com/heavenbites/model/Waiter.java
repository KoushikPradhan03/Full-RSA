package com.heavenbites.model;

public class Waiter extends Person {
    public Waiter(String id, String name, String email, String hash) { super(id, name, email, hash); }
    public Waiter(String name, String email, String rawPassword) { super(name, email, rawPassword); }

    @Override public String getRole() { return "waiter"; }
    @Override public String greeting() { return "Hello waiter " + name; }
}