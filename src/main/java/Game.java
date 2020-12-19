import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.*;

class Game {
    private static final int MAX_LAYERS_DEPTH = 7;
    private final String url = "http://localhost:8081/";
    private String team_name = "Ants";
    private final String charset = "UTF-8";
    GameInfo.GIData state;
    Move next_move;
//    Timer timer;
    SCResponse.SCRData connection_data;
    boolean isMoveSent = false;

    Game(String team_name) {
        this.team_name = team_name;
        Board board = new Board(null, "RED");
//		try{
//			for(int i=0; i<5; ++i){
//				System.out.println(i+"");
//				minMaxTree.addLayer();
//			}
//			Move next = minMaxTree.evaluate();
//			System.out.println(next);
////			minMaxTree.changeRootMaxMode(false);
//		}
//		catch (OutOfMemoryError error){
//			System.out.println("We need more resources");
//		}
        try {
            LinkedList<Move> queue_of_moves = new LinkedList<>();

            connection_data = null;
            while (connection_data == null) {
                try {
                    connection_data = connectToServer(team_name);
                } catch (IOException e) {
//					e.printStackTrace();
                }
            }
            board.updateColor(connection_data.color);

            state = getInfo();
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
            long time_to_send = 800;

			Timer response_timer = new Timer("Response Timer");
			TimerTask send_move = new TimerTask() {
				@Override
				public void run() {
					try {
						if(state.whose_turn.equals(connection_data.color) && !isMoveSent &&
								(int)(state.available_time*1000) < time_to_send){
							isMoveSent = true;
							sendMove(connection_data.token, next_move);
							minMaxTree.updateByMove(next_move, true);
							System.out.println("Second move sent ");
							System.out.println("Whose turn: " + state.whose_turn);
							System.out.println("Time: " + state.available_time);
						}
						state = getInfo();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			response_timer.schedule(send_move, 0, 300);

            System.out.println("I'm near loop");
            while (!state.is_finished || state.winner == null) {
                try {
                    if (!state.whose_turn.equals(connection_data.color)) {
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
                        isMoveSent = false;
                        int response_delay = (int) (1000 * state.available_time) - 800;
                        if (state.last_move != null) minMaxTree.updateByMove(new Move(state.last_move), false);
                        System.out.println("Updated enemy's move");
                        if (!queue_of_moves.isEmpty()) {
                            sendMove(connection_data.token, queue_of_moves.pollFirst());
                            continue;
                        }

                        for(int i=2; i<minMaxTree.leaves.get(0).calculateLength(); ++i){
                            next_move = minMaxTree.evaluate(i);
                        }
                        turn_deadline = System.currentTimeMillis() + (int) (1000 * state.available_time);
                        while (System.currentTimeMillis() < turn_deadline - time_to_send) {
                            try {
                                if (minMaxTree.leaves.get(0).calculateLength() < MAX_LAYERS_DEPTH) {
                                    System.out.println("I'm growing tree");
                                    long deadline = turn_deadline - time_to_send;
                                    minMaxTree.addLayer(deadline);
                                    System.out.println("Difference between planned stop time and actual:" + (System.currentTimeMillis() - deadline));
                                    next_move = minMaxTree.evaluate(Integer.MAX_VALUE);
                                } else break;
                            } catch (OutOfMemoryError error) {
                                System.out.println("We need more resources!");
                                break;
                            }
                        }

                        if (!isMoveSent) response_timer.cancel();
                        else continue;
                        if (next_move.positions.size() > 2) {
                            queue_of_moves = generateQueue(next_move);
                            sendMove(connection_data.token, queue_of_moves.pollFirst());
                        } else sendMove(connection_data.token, next_move);
                        minMaxTree.updateByMove(next_move, true);
                        state = getInfo();
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
                    response_timer.cancel();
                    System.out.println("Game finished! Winner: " + state.winner);
                    break;
                }
            }
//			timer.cancel();
        } catch (Exception e) {
//			timer.cancel();
            e.printStackTrace();
        }
    }

    private LinkedList<Move> generateQueue(Move move) {
        LinkedList<Move> res = new LinkedList<>();
        for (int i = 1; i < move.positions.size(); ++i) {
            res.add(new Move(move.positions.get(i - 1), move.positions.get(i)));
        }
        return res;
    }

    private MoveResponse sendMove(String token, Move move) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) new URL(url + "move").openConnection());
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; " + charset);
        connection.setDoOutput(true);
//		String encoded_token = Base64.getEncoder().encodeToString(token.getBytes());
        String auth_header_value = Base64.getEncoder().encodeToString(("Token " + token).getBytes());
        connection.setRequestProperty("Authorization", "Token " + token);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Move.class, new MoveAdapter())
                .create();
//		String serialized_move = String.format("{\n    \"move\": %s\n}", move.toString());
//		System.out.println(serialized_move);
        String stringified = String.format("{\n    \"move\": %s\n}", move.toString());
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = stringified.getBytes(charset);
            os.write(input, 0, input.length);
        }

        if (connection.getResponseCode() == 400) {
            System.out.println("Error 400");
        }

        InputStream response = connection.getInputStream();
        Reader reader = new InputStreamReader(response, charset);
        MoveResponse result = new Gson().fromJson(reader, MoveResponse.class);

        System.out.println(result.status);
        System.out.println(result.data);
        return result;
    }

    private SCResponse.SCRData connectToServer(String team_name) throws IOException, ConnectException {
        String query = "team_name=" + URLEncoder.encode(team_name, charset);
        URLConnection connection = new URL(url + "game?" + query).openConnection();
        connection.setDoOutput(true); // Triggers POST.
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

        OutputStream output = connection.getOutputStream();
        output.write(query.getBytes(charset));

        InputStream response = connection.getInputStream();
        Reader reader = new InputStreamReader(response, charset);
        SCResponse result = new Gson().fromJson(reader, SCResponse.class);
        System.out.println("Status: " + result.status);
        System.out.println("Color: " + result.data.color);
        System.out.println("Token: " + result.data.token);
        return result.data;
    }

    private GameInfo.GIData getInfo() throws IOException {
        String query = "game";
        URLConnection connection = new URL(url + query).openConnection();
        connection.setRequestProperty("Accept-Charset", charset);
        InputStream response = connection.getInputStream();
        Reader reader = new InputStreamReader(response, charset);
        GameInfo result = new Gson().fromJson(reader, GameInfo.class);

//		System.out.println(result.data.status);
//		System.out.println("Whose turn: " + result.data.whose_turn);
//		System.out.println("Winner: " + result.data.winner);
//		System.out.println("Time: " + result.data.available_time);
//		System.out.println(result.data.last_move);
//		for(GameInfo.Tile tile : result.data.board){
//			System.out.println("(Pos: "+tile.position+", Color: "+tile.color+")");
//		}

        return result.data;
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