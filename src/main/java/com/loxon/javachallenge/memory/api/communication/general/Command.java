package com.loxon.javachallenge.memory.api.communication.general;

import com.loxon.javachallenge.memory.api.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of all handled commands.
 */
public abstract class Command {
    private Player player;

    public Command( final Player player ) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Integer getCell(){
        return null;
    }

    public List<Integer> getCells(){
        return null;
    }

    public String getType(){
        return null;
    }
}
