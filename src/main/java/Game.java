import java.io.IOException;
import java.util.*;

import static java.util.Objects.isNull;

class Game {
    private static final int MAX_LAYERS_DEPTH = 9;
    private final long timeBetweenRequests = 150;
    private boolean turnState = false;
    static Move next_move;
    GameInfo.GIData state;
    SCResponse.SCRData connection_data;
    Network network;
    boolean isMoveSent = false;
    long deadline_to_send_move = Long.MAX_VALUE;
    static LinkedList<Move> qmoves = new LinkedList<>();
    static int current_depth = 3;

    private TimerTask sendBestMoveToServer;

    Game(String team_name) {
        Board board = new Board(null, "RED");
        network = new Network(team_name);
        try {
            connection_data = null;
            while (connection_data == null) {
                try {
                    connection_data = network.connectToServer(team_name);
                } catch (IOException e) {
//					e.printStackTrace();
                }
                Thread.sleep(100);
            }
            board.updateColor(connection_data.color);

            state = network.getInfo();
            MinMaxTree.initializeMinMaxTree(board, state.whose_turn.equals(connection_data.color));

            turnState = false;
            long time_to_send = 400;

            while (shouldContinueFetching()) {
                try {
                    turnState = !state.whose_turn.equals(connection_data.color);
                    if (turnState) {
                        if(MinMaxTree.lastSentMove != null) Runtime.getRuntime().gc();
                        MinMaxTree.lastSentMove = null;
                        System.out.println("Waiting for enemy to move");
                    } else {
                        System.out.println("It's my turn now");

                        if (!qmoves.isEmpty()) {
                            network.sendMove(connection_data.token, qmoves.pollFirst());
                            System.out.println("I sent a move");
                            state = network.getInfo();
                            continue;
                        }

                        if (state.last_move != null){
                            MinMaxTree.updateByMove(new Move(state.last_move), false);
                            current_depth--;
                        }

                        Timer mTimer = new Timer();
                        SendMoveToServer task = new SendMoveToServer(network, connection_data.token);
                        mTimer.schedule(task,
                                (long) state.available_time*1000 - time_to_send);


                        next_move = MinMaxTree.evaluate(current_depth);

                        while (isNull(MinMaxTree.lastSentMove) && current_depth < MAX_LAYERS_DEPTH) {
                            try {
                                if (!MinMaxTree.leaves.isEmpty() && current_depth < MAX_LAYERS_DEPTH) {
//                                    System.out.println("I'm growing tree");
                                    boolean finished = MinMaxTree.addLayer();
                                    if(finished) {
                                        current_depth++;
                                        next_move = MinMaxTree.evaluate(MAX_LAYERS_DEPTH);
                                    }
                                } else break;
                            } catch (OutOfMemoryError error) {
                                System.out.println("We need more resources!");
                                break;
                            }
                        }

                        if(!task.isStarted) {
                            mTimer.cancel();
                            task.run();
                        }


//                        System.out.println("Whose turn: " + state.whose_turn);
//                        System.out.println("Last move: " + state.last_move);
//                        StringBuilder str = new StringBuilder();
//                        for (MinMaxVertex v : MinMaxTree.root.getChildren().get(0).getChildren())
//                            str.append(v.move.toString()).append(", ");
//                        System.out.println("Available enemy moves" + str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (state.is_finished) {
                    break;
                }
                System.out.println("Current depth: "+current_depth);
                state = network.getInfo();
                Thread.sleep(timeBetweenRequests);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Game finished! Winner: " + state.winner);
        System.out.println("Average calc func time in nanoseconds: " + MinMaxVertex.average_1000);
        System.out.println("Stat from " + MinMaxVertex.amount_of_1000_av * 1000 + " calculations");
    }


    private LinkedList<Move> analyzeAndSendMove(Move next_move) throws IOException {
        if (next_move.positions.size() > 2) {
            LinkedList<Move> queue_of_moves = generateQueue(next_move);
            MinMaxTree.updateByMove(next_move, true);
            network.sendMove(connection_data.token, queue_of_moves.pollFirst());
            return queue_of_moves;
        } else sendMoveAndUpdateTree(next_move);
        return new LinkedList<>();
    }

    private boolean isGameFinished(MinMaxTree MinMaxTree) {
        boolean exist_e_piece = false;
        for (Piece p : MinMaxTree.current_pieces) {
            if (p == null) continue;
            if (!p.side) exist_e_piece = true;
        }
        if (!exist_e_piece) return true;
        ArrayList<Move> available_enemy_moves = MinMaxTree.getAvailableMoves(MinMaxTree.current_pieces, false);
        return available_enemy_moves.isEmpty();
    }

    private boolean shouldContinueFetching() {
        return !state.is_finished && isNull(state.winner);
    }

    private void sendMoveAndUpdateTree(Move move) throws IOException {
        network.sendMove(connection_data.token, move);
        MinMaxTree.updateByMove(move, true);
    }

    private LinkedList<Move> generateQueue(Move move) {
        LinkedList<Move> res = new LinkedList<>();
        for (int i = 1; i < move.positions.size(); ++i) {
            res.add(new Move(move.positions.get(i - 1), move.positions.get(i)));
        }
        return res;
    }


}

class SendMoveToServer extends TimerTask {
    //Timer task of sending info to server
    private final String myToken;
    private final Network myNet;
    public boolean isStarted = false;

    public SendMoveToServer(Network net, String token) {
        myToken = token;
        myNet = net;
    }

    @Override
    public void run() {
        try {
            isStarted=true;
            Game.current_depth--;
            if (Game.next_move.positions.size() > 2) {
                Game.qmoves = generateQueue(Game.next_move);
                MinMaxTree.lastSentMove = Game.next_move;
                MinMaxTree.updateByMove(Game.next_move, true);
                myNet.sendMove(myToken, Game.qmoves.pollFirst());
            } else {
                sendMoveAndUpdateTree(Game.next_move);
                Game.qmoves = new LinkedList<>();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMoveAndUpdateTree(Move move) throws IOException {
        myNet.sendMove(myToken, move);
        MinMaxTree.lastSentMove = move;
        MinMaxTree.updateByMove(move, true);
    }

    private LinkedList<Move> generateQueue(Move move) {
        LinkedList<Move> res = new LinkedList<>();
        for (int i = 1; i < move.positions.size(); ++i) {
            res.add(new Move(move.positions.get(i - 1), move.positions.get(i)));
        }
        return res;
    }
}

class SCResponse {
    String status;
    SCRData data;

    class SCRData {
        String color;
        String token;
    }
}

class GameInfo {
    String status;
    GIData data;

    class GIData {
        String status;
        String whose_turn;
        String winner;
        Tile[] board;
        double available_time;
        LastMove last_move;
        boolean is_started;
        boolean is_finished;
    }

    class LastMove {
        String player;
        int[][] last_moves;

        @Override
        public String toString() {
            StringBuffer moves_p = new StringBuffer();
            for (int[] arr : last_moves) {
                moves_p.append(Arrays.toString(arr) + ", ");
            }
            return player + ": " + moves_p.toString();
        }
    }
}

class MoveResponse {
    String status;
    String data;
}

class Tile {
    String color;
    Integer row;
    Integer column;
    Boolean king;
    Integer position;

    public Tile(String color,
                Integer row,
                Integer column,
                Boolean king,
                Integer position) {
        this.color = color;
        this.row = row;
        this.column = column;
        this.king = king;
        this.position = position;
    }
}