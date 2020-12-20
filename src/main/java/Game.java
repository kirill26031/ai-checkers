import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.*;

class Game {
    private static final int MAX_LAYERS_DEPTH = 9;
    GameInfo.GIData state;
    SCResponse.SCRData connection_data;
    Network network;
    boolean isMoveSent = false;
    long deadline_to_send_move = Long.MAX_VALUE;

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
            }
            board.updateColor(connection_data.color);

            state = network.getInfo();
            MinMaxTree minMaxTree = new MinMaxTree(board, state.whose_turn.equals(connection_data.color));
            System.out.println("I built basic tree");
//            TimerTask repeatedRequest = new TimerTask() {
//                public void run() {
//                    try {
//                        state = getInfo();
//
//						System.out.println("-------------------------Whose turn: " + state.whose_turn);
//						System.out.println("-------------------------Time: " + state.available_time);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            };
//            timer = new Timer("Timer");
//            long request_period = 500;
//            timer.schedule(repeatedRequest, request_period, request_period);

            long turn_deadline;
            long time_to_send = 200;

//			Timer response_timer = new Timer("Response Timer");
//			TimerTask send_move = new TimerTask() {
//				@Override
//				public void run() {
//					try {
//						if(state.whose_turn.equals(connection_data.color) && !isMoveSent &&
//								System.currentTimeMillis() >= deadline_to_send_move){
//							isMoveSent = true;
//							sendMove(connection_data.token, next_move);
//							minMaxTree.updateByMove(next_move, true);
//							System.out.println("Second move sent ");
//							System.out.println("Whose turn: " + state.whose_turn);
//							System.out.println("Time: " + state.available_time);
//						}
//						state = getInfo();
//                        System.out.println("Is my turn: "+state.whose_turn.equals(connection_data.color));
//                        System.out.println("Is move sent: "+isMoveSent);
//                        System.out.println("Time to deadline "+(deadline_to_send_move - System.currentTimeMillis()));
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			};
//			response_timer.schedule(send_move, 0, 300);

            System.out.println("I'm near loop");
            while (!state.is_finished && state.winner == null) {
                try {
                    state = network.getInfo();
                    if (!state.whose_turn.equals(connection_data.color)) {
                        isMoveSent = false;
                        try{
                        	Thread.sleep(100);
						}
						catch(Exception e){
                        	e.printStackTrace();
						}
                        try {
//							if(minMaxTree.leaves.get(0).calculateLength()<MAX_LAYERS_DEPTH){
//                                minMaxTree.addLayer(System.currentTimeMillis() + 200);
//                                System.out.println("Growing tree while enemy's move");
//                            }

                        } catch (OutOfMemoryError error) {
                            System.out.println("We need more resources!");
                        }
                    } else {
                        System.out.println("It's my turn now");
                        deadline_to_send_move = System.currentTimeMillis()+(int) (1000 * state.available_time) - time_to_send;
                        if (state.last_move != null) minMaxTree.updateByMove(new Move(state.last_move), false);
                        System.out.println("Updated enemy's move");
                        if (!queue_of_moves.isEmpty()) {
                            if(isGameFinished(minMaxTree)) continue;
                            network.sendMove(connection_data.token, queue_of_moves.pollFirst());
                            continue;
                        }
                        Move next_move;
//                        next_move = minMaxTree.evaluate(1000);
                        next_move = minMaxTree.evaluate(1000);
//                        for(int i=3; i<=minMaxTree.leaves.get(0).calculateLength(); ++i){
//                            if(System.currentTimeMillis() >= deadline_to_send_move){
//								queue_of_moves = analyzeAndSendMove(next_move, minMaxTree);
//								isMoveSent=true;
//                            	continue;
//							}
//                            next_move = minMaxTree.evaluate(i);
//                        }
                        while (System.currentTimeMillis() < deadline_to_send_move) {
                            try {
                                if (!minMaxTree.leaves.isEmpty() && minMaxTree.leaves.get(0).calculateLength() < MAX_LAYERS_DEPTH) {
                                    System.out.println("I'm growing tree");
                                    long deadline = deadline_to_send_move;
                                    minMaxTree.addLayer(deadline);
                                    System.out.println("Difference between planned stop time and actual:" + (deadline - System.currentTimeMillis()));
                                    next_move = minMaxTree.evaluate(getMaxDepth(deadline_to_send_move-System.currentTimeMillis()));
                                } else break;
                            } catch (OutOfMemoryError error) {
                                System.out.println("We need more resources!");
                                break;
                            }
                        }
                        // check if it is last move
                        queue_of_moves = analyzeAndSendMove(next_move, minMaxTree);
						isMoveSent=true;

                        state = network.getInfo();
                        System.out.println("Whose turn: " + state.whose_turn);
                        System.out.println("Winner: " + state.winner);
                        System.out.println("Time: " + state.available_time);
                        System.out.println(state.last_move);
                        System.out.println("I just sent: " + next_move);
                        StringBuilder str = new StringBuilder();
                        for (MinMaxVertex v : minMaxTree.root.getChildren().get(0).getChildren())
                            str.append(v.move.toString()).append(", ");
                        System.out.println(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (state.is_finished) {
                    System.out.println("Game finished! Winner: " + state.winner);
                    break;
                }
				try{
//					Thread.sleep(200);
				}
				catch(Exception e){
					e.printStackTrace();
				}
            }
//			timer.cancel();
        } catch (Exception e) {
//			timer.cancel();
            e.printStackTrace();
        }
    }

    private int getMaxDepth(long l) {
        if(l <= 500) return 4;
        if(l<= 1000) return 6;
        if(l <= 2000) return 7;
        return 8;
    }

    private LinkedList<Move> analyzeAndSendMove(Move next_move, MinMaxTree minMaxTree) throws IOException{
		if (next_move.positions.size() > 2) {
			LinkedList<Move> queue_of_moves = generateQueue(next_move);
            minMaxTree.updateByMove(next_move, true);
			network.sendMove(connection_data.token, queue_of_moves.pollFirst());
			return queue_of_moves;
		} else sendMoveAndUpdateTree(next_move, minMaxTree);
		return new LinkedList<>();
	}

    private boolean isGameFinished(MinMaxTree minMaxTree) {
        boolean exist_e_piece = false;
        for(Piece p : minMaxTree.current_pieces){
            if(p == null) continue;
            if(!p.side) exist_e_piece = true;
        }
        if(!exist_e_piece) return true;
        ArrayList<Move> available_enemy_moves = minMaxTree.getAvailableMoves(minMaxTree.current_pieces, false);
        return available_enemy_moves.isEmpty();
    }

    private void sendMoveAndUpdateTree(Move move, MinMaxTree minMaxTree) throws IOException{
        network.sendMove(connection_data.token, move);
        minMaxTree.updateByMove(move, true);
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