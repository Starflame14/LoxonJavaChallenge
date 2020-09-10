package com.loxon.javachallenge.memory.api.communication.general;

import com.loxon.javachallenge.memory.api.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract command affecting number of cells.
 */
public abstract class CommandGeneral extends Command {
    private List<Integer> cells;

    public CommandGeneral(final Player player, final List<Integer> cells) {
        super(player);
        this.cells = cells;
    }

    @Override
    public List<Integer> getCells() {
        return cells;
    }

}
