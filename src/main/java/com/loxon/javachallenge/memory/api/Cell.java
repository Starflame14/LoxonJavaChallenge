package com.loxon.javachallenge.memory.api;

public class Cell {
    public Player player;
    public MemoryState type;

    public Cell(Player owner, MemoryState state){
        player = owner;
        type = state;
    }

}
