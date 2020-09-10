package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.*;
import com.loxon.javachallenge.memory.api.communication.commands.ResponseScan;
import com.loxon.javachallenge.memory.api.communication.commands.ResponseStats;
import com.loxon.javachallenge.memory.api.communication.commands.ResponseSuccessList;
import com.loxon.javachallenge.memory.api.communication.general.Command;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.*;

import static com.loxon.javachallenge.memory.api.MemoryState.*;

public class GameImplementationFactory {
    private static ArrayList<Player> players;
    private static int roundCount;
    private static ArrayList<MemoryState> memoryState;
    private static ArrayList<PlayerScore> playerScores;
    //private static ArrayList<Cell> cells;
    private static Cell[] cells;
    private static MemoryState[] memoryStates;

    public static Game get() {
        players = new ArrayList<>();
        roundCount = -1;
        memoryState = new ArrayList<>();
        playerScores = new ArrayList<>();

        return new Game() {
            @Override
            public Player registerPlayer(String name) {
                Player player = new Player(name);
                players.add(player);
                playerScores.add(new PlayerScore(player, true));
                return player;
            }

            @Override
            public void startGame(List<MemoryState> initialMemory, int rounds) {
                memoryState.addAll(initialMemory);
                roundCount = rounds;
                memoryStates = new MemoryState[memoryState.size()];
                cells = new Cell[memoryState.size()];
                for (int i = 0; i < memoryState.size(); i++) {
                    memoryStates[i] = memoryState.get(i);
                    cells[i] = new Cell(null, memoryState.get(i));
                }
            }

            @Override
            public List<Response> nextRound(Command... requests) {
                roundCount--;

                ArrayList<Command> reqs = new ArrayList<>();
                ArrayList<Player> registeredPlayers = new ArrayList<>();

                for (Command req : requests) {
                    if(players.contains(req.getPlayer()) && !registeredPlayers.contains(req.getPlayer())){
                        reqs.add(req);
                        registeredPlayers.add(req.getPlayer());
                    }
                }

                ArrayList<Response> responses = new ArrayList<>();
                ArrayList<Integer> overloadedCells = new ArrayList<>();
                ArrayList<Integer> modifiedCells = new ArrayList<>();

                for (int i = 0; i < requests.length; i++) {
                    if (!requests[i].getType().equals("scan") && !requests[i].getType().equals("stats")) {
                        for (Integer a : requests[i].getCells()) {
                            if (modifiedCells.contains(a)) {
                                overloadedCells.add(a);
                            }
                            modifiedCells.add(a);
                        }
                    }
                }

                for (Command request : reqs) {
                    switch (request.getType()) {
                        case "scan":
                            int index = request.getCell() - request.getCell() % 4;
                            if (request.getCell() >= 0 && request.getCell() < memoryStates.length) {
                                updateMemoryState(request);

                                responses.add(new ResponseScan(request.getPlayer(), index,
                                        memoryState.subList(index, index + 4)));
                            } else {
                                responses.add(new ResponseScan(request.getPlayer(), -1,
                                        Collections.emptyList()));
                                return responses;
                            }
                            break;

                        case "stats":
                            ResponseStats rs = new ResponseStats(request.getPlayer());
                            rs.setCellCount(cells.length);

                            int owned = 0;
                            int free = 0;
                            int allocated = 0;
                            int corrupt = 0;
                            int fortified = 0;
                            int system = 0;
                            for (Cell cell : cells) {
                                switch (cell.type) {
                                    case FREE:
                                        free++;
                                        break;

                                    case CORRUPT:
                                        corrupt++;
                                        break;

                                    case ALLOCATED:
                                        allocated++;
                                        if (cell.player == request.getPlayer()) owned++;
                                        break;

                                    case FORTIFIED:
                                        fortified++;
                                        if (cell.player == request.getPlayer()) owned++;
                                        break;

                                    case SYSTEM:
                                        system++;
                                }
                            }
                            rs.setAllocatedCells(allocated);
                            rs.setCorruptCells(corrupt);
                            rs.setFortifiedCells(fortified);
                            rs.setFreeCells(free);
                            rs.setOwnedCells(owned);
                            rs.setSystemCells(system);

                            rs.setRemainingRounds(roundCount);

                            responses.add(rs);
                            break;

                        default:
                            responses.add(new ResponseSuccessList(request.getPlayer(),
                                    successCells(request, overloadedCells)));

                    }
                }


                return responses;
            }

            @Override
            public List<PlayerScore> getScores() {
                playerScores = new ArrayList<>();
                for (Player player : players) {
                    PlayerScore ps = new PlayerScore(player);

                    int owned = 0;
                    int fortified = 0;
                    int blocks = 0;
                    boolean isFullBlock = false;
                    for (int i = 0; i < cells.length; i++) {
                        Cell cell = cells[i];
                        if (i % 4 == 0) isFullBlock = true;
                        switch (cell.type) {
                            case ALLOCATED:
                                if (cell.player.equals(player)) owned++;
                                else isFullBlock = false;
                                break;

                            case FORTIFIED:
                                if (cell.player.equals(player)) {
                                    owned++;
                                    fortified++;
                                } else isFullBlock = false;
                                break;

                            default:
                                isFullBlock = false;
                        }
                        if (i % 4 == 3 && isFullBlock) blocks++;
                    }

                    ps.setFortifiedCells(fortified);
                    ps.setOwnedCells(owned);
                    ps.setOwnedBlocks(blocks);
                    ps.setTotalScore(owned + 4 * blocks);

                    playerScores.add(ps);
                }

                return playerScores;
            }

            @Override
            public String visualize() {
                return null;
            }
        };
    }

    public static boolean isCommand(String type) {
        return type.equals("allocate") || type.equals("fortify") || type.equals("free")
                || type.equals("recover") || type.equals("scan") || type.equals("stats")
                || type.equals("swap");
    }

    public static ArrayList<Integer> successCells(Command cmd, ArrayList<Integer> overloadedCells) {
        ArrayList<Integer> successCells = new ArrayList<>();

        if (cmd.getType().equals("swap")) {
            if (cmd.getCells().size() == 2 &&
                    cmd.getCells().get(0) != null && cmd.getCells().get(1) != null &&
                    cmd.getCells().get(0) >= 0 && cmd.getCells().get(0) < memoryStates.length &&
                    cmd.getCells().get(1) >= 0 && cmd.getCells().get(1) < memoryStates.length) {
                boolean isCorrupt = false;

                int i0 = cmd.getCells().get(0);
                int i1 = cmd.getCells().get(1);

                if (overloadedCells.size() > 0 && overloadedCells.contains(i0) &&
                        memoryStates[i0] != FORTIFIED && memoryStates[i0] != OWNED_FORTIFIED
                        && memoryStates[i0] != SYSTEM) {

                    memoryStates[i0] = CORRUPT;
                    memoryStates[i1] = CORRUPT;
                    cells[i0].player = null;
                    cells[i1].player = null;
                    cells[i0].type = CORRUPT;
                    cells[i1].type = CORRUPT;

                    isCorrupt = true;

                } else if (overloadedCells.size() > 0 && overloadedCells.contains(i1) &&
                        memoryStates[i1] != FORTIFIED && memoryStates[i1] != OWNED_FORTIFIED
                        && memoryStates[i1] != SYSTEM) {

                    memoryStates[i0] = CORRUPT;
                    memoryStates[i1] = CORRUPT;
                    cells[i0].player = null;
                    cells[i1].player = null;
                    cells[i0].type = CORRUPT;
                    cells[i1].type = CORRUPT;

                    isCorrupt = true;

                } else if (i0 == i1) {
                    memoryStates[i0] = CORRUPT;
                    memoryStates[i1] = CORRUPT;
                    cells[i0].player = null;
                    cells[i1].player = null;
                    cells[i0].type = CORRUPT;
                    cells[i1].type = CORRUPT;

                    isCorrupt = true;

                }

                if (!isCorrupt) {
                    Cell cSwap = cells[cmd.getCells().get(0)];
                    cells[cmd.getCells().get(0)] = cells[cmd.getCells().get(1)];
                    cells[cmd.getCells().get(1)] = cSwap;

                    MemoryState mSwap = memoryStates[cmd.getCells().get(0)];
                    memoryStates[cmd.getCells().get(0)] = memoryStates[cmd.getCells().get(1)];
                    memoryStates[cmd.getCells().get(1)] = mSwap;

                    successCells = new ArrayList<>(cmd.getCells());
                    return successCells;
                }

            } else {
                successCells = new ArrayList<>();
                return successCells;
            }

            return successCells;
        }

        int block1 = -1;
        if (cmd.getCells().get(0) != null) block1 = cmd.getCells().get(0) - cmd.getCells().get(0) % 4;
        int block2 = -1;
        if (cmd.getCells().size() > 1 && cmd.getCells().get(1) != null) {
            block2 = cmd.getCells().get(1) - cmd.getCells().get(1) % 4;
        }
        boolean isValid = true;
        if (block1 != -1 && block2 != -1) isValid = block1 == block2;

        if (cmd.getCells().size() < 3 && isValid) {
            for (Integer i : cmd.getCells()) {
                if (i == null) continue;
                if (i >= 0 && i < memoryStates.length) {
                    if (overloadedCells.size() > 0 && overloadedCells.contains(i) &&
                            memoryStates[i] != FORTIFIED && memoryStates[i] != SYSTEM) {
                        memoryStates[i] = CORRUPT;
                        cells[i].player = null;
                        cells[i].type = CORRUPT;
                    } else {
                        updateMemoryState(cmd);
                        switch (cmd.getType()) {
                            case "allocate":
                                switch (memoryState.get(i)) {
                                    case FREE:
                                        successCells.add(i);
                                        memoryStates[i] = ALLOCATED;
                                        cells[i].player = cmd.getPlayer();
                                        cells[i].type = ALLOCATED;
                                        break;

                                    case ALLOCATED:
                                    case OWNED_ALLOCATED:
                                        memoryStates[i] = CORRUPT;
                                        cells[i].player = null;
                                        cells[i].type = CORRUPT;
                                }
                                break;

                            case "fortify":
                                if (memoryState.get(i) == ALLOCATED || memoryState.get(i) == OWNED_ALLOCATED) {
                                    successCells.add(i);
                                    memoryStates[i] = FORTIFIED;
                                    cells[i].type = FORTIFIED;
                                }
                                break;

                            case "free":
                                switch (memoryState.get(i)) {
                                    case ALLOCATED:
                                    case OWNED_ALLOCATED:
                                    case CORRUPT:
                                    case FREE:
                                        successCells.add(i);
                                        memoryStates[i] = FREE;
                                        cells[i].player = null;
                                        cells[i].type = FREE;
                                }
                                break;

                            case "recover":
                                switch (memoryState.get(i)) {
                                    case CORRUPT:
                                        successCells.add(i);
                                        memoryStates[i] = ALLOCATED;
                                        cells[i].player = cmd.getPlayer();
                                        cells[i].type = ALLOCATED;
                                        break;

                                    case ALLOCATED:
                                    case OWNED_ALLOCATED:
                                    case FREE:
                                        memoryStates[i] = CORRUPT;
                                        cells[i].player = null;
                                        cells[i].type = CORRUPT;
                                }
                                break;

                        }
                    }
                } else {
                    System.out.println("Too long");
                    successCells = new ArrayList<>();
                    return successCells;
                }

            }
        } else {
            System.out.println("Invalid request");
            successCells = new ArrayList<>();
            return successCells;
        }

        return successCells;
    }

    public static void updateMemoryState(Command request) {
        memoryState = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.type == ALLOCATED && cell.player.equals(request.getPlayer()))
                memoryState.add(OWNED_ALLOCATED);
            else if (cell.type == FORTIFIED && cell.player.equals(request.getPlayer()))
                memoryState.add(OWNED_FORTIFIED);
            else memoryState.add(cell.type);
        }
    }

    public static boolean isBlocked(MemoryState ms) {
        return ms == FORTIFIED || ms == OWNED_FORTIFIED || ms == SYSTEM;
    }
}
