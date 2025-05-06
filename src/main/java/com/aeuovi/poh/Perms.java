package com.aeuovi.poh;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Perms {
    public List<Boolean> build;
    public List<Boolean> destroy;
    
    @JsonProperty("switch")
    public List<Boolean> switchPerms;
    
    public List<Boolean> itemUse;
    public Flags flags;
}