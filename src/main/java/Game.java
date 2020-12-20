import java.io.IOException;
import java.util.*;

import static java.util.Objects.isNull;

class Game {
    private static final int MAX_LAYERS_DEPTH = 8;
    private final long timeBetweenRequests = 150;
    private boolean turnState = false;
    GameInfo.GIData state;
    SCResponse.SCRData connection_data;
    Network network;
    boolean isMoveSent = false;
    long deadline_to_send_move = Long.MAX_VALUE;
    static LinkedList<Move> qmoves = new LinkedList<>();

    private TimerTask sendBestMoveToServer;

    Game(String team_name) {
        Board board = new Board(null, "RED");
        network = new Network(team_name);
        try {
            LinkedList<Move> queue_of_moves = new LinkedList<>();

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
            long time_to_send = 200;

            while (shouldContinueFetching()) {
                try {
                    if (turnState) {
                        state = network.getInfo();
                        turnState = !state.whose_turn.equals(connection_data.color);
                        System.out.println("Waiting for enemy to move");
                        Thread.sleep(timeBetweenRequests);
                    } else {

                        System.out.println("It's my turn now");

                        Timer mTimer = new Timer();
                        mTimer.schedule(new SendMoveToServer(network, connection_data.token, queue_of_moves.pollFirst()),
                                (long) state.available_time - time_to_send);

                        if (state.last_move != null) MinMaxTree.updateByMove(new Move(state.last_move), false);
//                        System.out.println("Updated enemy's move");

                        if (!queue_of_moves.isEmpty()) {
                            network.sendMove(connection_data.token, queue_of_moves.pollFirst());
                            System.out.println("I sent a move");
                            mTimer.cancel();
                            continue;
                        }
                        Move next_move;
                        next_move = MinMaxTree.evaluate(1000);

                        while (isNull(MinMaxTree.lastSentMove)) {
                            try {
                                if (!MinMaxTree.leaves.isEmpty() && MinMaxTree.getDeepestBest().calculateLength() < MAX_LAYERS_DEPTH) {
//                                    System.out.println("I'm growing tree");
                                    MinMaxTree.addLayer();
                                    next_move = MinMaxTree.evaluate(7);
                                } else break;
                            } catch (OutOfMemoryError error) {
                                System.out.println("We need more resources!");
                                break;
                            }
                        }

                        // check if it is last move
                        queue_of_moves = analyzeAndSendMove(next_move);
                        MinMaxTree.lastSentMove = null;

                        state = network.getInfo();
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
    private final Move myMove;
    private final Network myNet;

    public SendMoveToServer(Network net, String token, Move move) {
        myToken = token;
        myMove = move;
        myNet = net;
    }

    @Override
    public void run() {
        try {
            if (myMove.positions.size() > 2) {
                LinkedList<Move> queue_of_moves = generateQueue(myMove);
                MinMaxTree.updateByMove(myMove, true);
                myNet.sendMove(myToken, queue_of_moves.pollFirst());
                Game.qmoves = queue_of_moves;
            } else sendMoveAndUpdateTree(myMove);
            Game.qmoves = new LinkedList<>();

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