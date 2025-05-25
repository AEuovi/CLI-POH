package com.aeuovi.poh;

public class Passport {
    private final int id;
    private final String username;
    private final String type;

    public Passport(int id, String username, String type) {
        this.id = id;
        this.username = username;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Passport{#" + id + ", username='" + username + "', type='" + type + "'}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Passport)) return false;
        Passport other = (Passport) obj;
        return this.id == other.id && this.username.equals(other.username) && this.type.equals(other.type);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(id);
        result = 31 * result + username.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
