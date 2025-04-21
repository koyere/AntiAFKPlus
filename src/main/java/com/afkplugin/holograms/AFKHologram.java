package com.afkplugin.holograms;

import org.bukkit.Location;
import java.util.Arrays;
import java.util.List;
import eu.decentsoftware.holograms.api.DHAPI;

public class AFKHologram {
    public void create(String name, Location loc) {
        List<String> lines = Arrays.asList("AFK!");
        DHAPI.createHologram(name, loc, lines);
    }
}
