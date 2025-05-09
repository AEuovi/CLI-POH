package com.aeuovi.poh;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class User {

    public String name;
    public String uuid;
    public String title;
    public String surname;
    public String formattedName;
    public String about;
    public Town town;
    public Nation nation;
    public Timestamps timestamps;
    public Status status;
    public Stats stats;
    public Perms perms;
    public Ranks ranks;
    public List<Friend> friends;

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

        String registered = timestamps != null ? formatter.format(Instant.ofEpochMilli(timestamps.registered)) : "N/A";
        String joinedTownAt = timestamps != null ? formatter.format(Instant.ofEpochMilli(timestamps.joinedTownAt)) : "N/A";
        String lastOnline = timestamps != null ? formatter.format(Instant.ofEpochMilli(timestamps.lastOnline)) : "N/A";

        return String.format(
            "Name: %s%nUUID: %s%nTitle: %s%nSurname: %s%nAbout: %s%n" +
            "Town: %s%nNation: %s%nRegistered: %s%nJoinedTownAt: %s%nLast Online: %s",
            name,
            uuid,
            title != null ? title : "N/A",
            surname != null ? surname : "N/A",
            about != null ? about : "N/A",
            town != null ? town.name : "N/A",
            nation != null ? nation.name : "N/A",
            registered,
            joinedTownAt,
            lastOnline
        );
    }
}
