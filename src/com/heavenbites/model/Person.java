package com.heavenbites.model;

import com.heavenbites.util.Text;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Abstract base class for every kind of user. */
public abstract class Person implements Jsonable {
    private final String id;
    protected String name;
    protected String email;
    private final String passwordHash;

    // Constructor 1: used when loading from file (password already hashed)
    public Person(String id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Constructor 2 (overloaded): used for a brand-new user
    public Person(String name, String email, String rawPassword) {
        this(UUID.randomUUID().toString().substring(0, 8), name, email, hash(rawPassword));
    }

    public abstract String getRole();

    public String greeting() {
        return "Welcome, " + name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public boolean checkPassword(String raw) {
        return raw != null && passwordHash.equals(hash(raw));
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(raw.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Person create(String id, String name, String email, String hash, String role) {
        switch (role) {
            case "admin":    return new Admin(id, name, email, hash);
            case "waiter":   return new Waiter(id, name, email, hash);
            case "cashier":  return new Cashier(id, name, email, hash);
            case "customer": return new Customer(id, name, email, hash);
            default: throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    public static Person createNew(String name, String email, String rawPassword, String role) {
        switch (role) {
            case "admin":    return new Admin(name, email, rawPassword);
            case "waiter":   return new Waiter(name, email, rawPassword);
            case "cashier":  return new Cashier(name, email, rawPassword);
            case "customer": return new Customer(name, email, rawPassword);
            default: throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    public static Person fromLine(String line) {
        String[] f = line.split("\\|", -1);
        return create(f[0], f[1], f[2], f[3], f[4]);
    }

    public String toLine() {
        return String.join("|", id, name, email, passwordHash, getRole());
    }

    @Override
    public String toJson() {
        return "{\"id\":" + Text.q(id) + ",\"name\":" + Text.q(name)
                + ",\"email\":" + Text.q(email) + ",\"role\":" + Text.q(getRole()) + "}";
    }
}