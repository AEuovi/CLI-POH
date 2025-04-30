package com.aeuovi.poh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print
        try {
            String json = mapper.writeValueAsString(this);
            if ("[]".equals(json)) {
                return "Unknown username";
            } else {
                return json;
            }
        } catch (JsonProcessingException e) {
            return "Error converting User to JSON: " + e.getMessage();
        }
    }
}
