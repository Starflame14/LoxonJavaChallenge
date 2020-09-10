package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.Command;

/**
 * Scan a block in the memory.
 */
public class CommandScan extends Command {
    private Integer cell;

    public CommandScan(final Player player, final Integer cell) {
        super(player);
        this.cell = cell;
        this.resolveCommand();
    }

    @Override
    public Integer getCell() {
        return cell;
    }

    @Override
    public String getType(){
        return "scan";
    }

    void resolveCommand(){
        
    }

}
