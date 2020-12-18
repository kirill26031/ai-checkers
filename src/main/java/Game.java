import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.*;

class Game {
	private final String url = "http://localhost:8081/";
	private String team_name = "Ants";
	private final String charset = "UTF-8";
	GameInfo.GIData state;
	Move next_move;
	Timer timer;

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

			SCResponse.SCRData connection_data = null;
			while (connection_data == null){
				try {
					connection_data = connectToServer(team_name);
				}
				catch (IOException e){
//					e.printStackTrace();
				}
			}
			board.updateColor(connection_data.color);

			state = getInfo();
			MinMaxTree minMaxTree = new MinMaxTree(board, state.whose_turn.equals(connection_data.color));
			minMaxTree.addLayer(1000);
			minMaxTree.addLayer(1000);
			TimerTask repeatedRequest = new TimerTask() {
				public void run() {
					try {
						state = getInfo();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			timer = new Timer("Timer");
			long request_period = 500;
			timer.schedule(repeatedRequest, request_period, request_period);

			long turn_deadline;
			while (!state.is_finished && state.winner == null) {
				try {
					if (!state.whose_turn.equals(connection_data.color)) {
						try {
							minMaxTree.addLayer(System.currentTimeMillis() + request_period);
						} catch (OutOfMemoryError error) {
							System.out.println("We need more resources!");
						}
					} else {
						if (state.last_move != null) minMaxTree.updateByEnemysMove(new Move(state.last_move));
						if (!queue_of_moves.isEmpty()) {
							sendMove(connection_data.token, queue_of_moves.pollFirst());
							break;
						}
						long time_to_send = 800;
//					Timer response_timer = new Timer("Response Timer");
//					response_timer.schedule(new TimerTask() {
//						@Override
//						public void run() {
//							try {
//								sendMove(connection_data.token, next_move);
//							} catch (IOException e) {
//								e.printStackTrace();
//							}
//						}
//					}, (int) (1000 * state.available_time) - time_to_send);
//					next_move = minMaxTree.root.best_child.best_child.move;
						turn_deadline = System.currentTimeMillis() + (int) (1000 * state.available_time);
						long time_to_eval = ((int) (1000 * state.available_time) - time_to_send) / 2;
						while (System.currentTimeMillis() < turn_deadline - time_to_eval - time_to_send) {
							try {
								minMaxTree.addLayer(
										Math.min(turn_deadline - time_to_eval - time_to_send,
												System.currentTimeMillis() + 500));
							} catch (OutOfMemoryError error) {
								System.out.println("We need more resources!");
							}
						}
						next_move = minMaxTree.evaluate();
						if (next_move.positions.size() > 2) {
							queue_of_moves = generateQueue(next_move);
							sendMove(connection_data.token, queue_of_moves.pollFirst());
						} else sendMove(connection_data.token, next_move);
					}
				}
				catch (IOException e){
					e.printStackTrace();
				}
			}
			timer.cancel();
			System.out.println(state.winner);
		} catch (Exception e) {
			timer.cancel();
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

		System.out.println(result.data.status);
		System.out.println("Whose turn: " + result.data.whose_turn);
		System.out.println("Winner: " + result.data.winner);
		System.out.println("Time: " + result.data.available_time);
		System.out.println(result.data.last_move);
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