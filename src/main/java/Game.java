import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Base64;

class Game {
	private final String url = "http://localhost:8081/";
	private final String team_name = "Ants";
	private final String charset = "UTF-8";

	Game(){
		Board board = new Board(null, "RED");
		MinMaxTree minMaxTree = new MinMaxTree(board, true);
		try{
			for(int i=0; i<4; ++i){
				System.out.println(i+"");
				minMaxTree.addLayer();
			}
			Move next = minMaxTree.evaluate();
			System.out.println(next);
//			minMaxTree.changeRootMaxMode(false);
		}
		catch (OutOfMemoryError error){
			System.out.println("We need more resources");
		}
//		try {
//			SCResponse.SCRData connection_data = connectToServer(team_name);
//			board.updateColor(connection_data.color);
//
//			GameInfo.GIData state = getInfo();
//			while(!state.whose_turn.equals(connection_data.color)){
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				state = getInfo();
//			}
//			sendMove(connection_data.token, new Move(9, 13));
//			getInfo();
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private MoveResponse sendMove(String token, Move move) throws IOException {
		HttpURLConnection connection = ((HttpURLConnection) new URL(url+"move").openConnection());
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Content-Type", "application/json; "+charset);
		connection.setDoOutput(true);
//		String encoded_token = Base64.getEncoder().encodeToString(token.getBytes());
		String auth_header_value = Base64.getEncoder().encodeToString(("Token "+token).getBytes());
		connection.setRequestProperty("Authorization", "Token "+token);

		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Move.class, MoveAdapter.class)
				.setPrettyPrinting()
				.create();
//		String serialized_move = String.format("{\n    \"move\": %s\n}", move.toString());
//		System.out.println(serialized_move);
		try(OutputStream os = connection.getOutputStream()) {
			byte[] input = gson.toJson(move).getBytes(charset);
			os.write(input, 0, input.length);
		}

		InputStream response = connection.getInputStream();
		Reader reader = new InputStreamReader(response, charset);
		MoveResponse result  = new Gson().fromJson(reader, MoveResponse.class);

		System.out.println(result.status);
		System.out.println(result.data);
		return result;
	}

	private SCResponse.SCRData connectToServer(String team_name) throws IOException {
		String query = "team_name="+URLEncoder.encode(team_name, charset);
		URLConnection connection = new URL(url+"game?"+query).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

		OutputStream output = connection.getOutputStream();
		output.write(query.getBytes(charset));

		InputStream response = connection.getInputStream();
		Reader reader = new InputStreamReader(response, charset);
		SCResponse result  = new Gson().fromJson(reader, SCResponse.class);
		System.out.println("Status: "+result.status);
		System.out.println("Color: "+result.data.color);
		System.out.println("Token: "+result.data.token);
		return result.data;
	}

	private GameInfo.GIData getInfo() throws IOException {
		String query = "game";
		URLConnection connection = new URL(url+query).openConnection();
		connection.setRequestProperty("Accept-Charset", charset);
		InputStream response = connection.getInputStream();
		Reader reader = new InputStreamReader(response, charset);
		GameInfo result  = new Gson().fromJson(reader, GameInfo.class);

		System.out.println(result.data.status);
		System.out.println("Whose turn: "+result.data.whose_turn);
		System.out.println("Winner: "+result.data.winner);
		System.out.println("Time: "+result.data.available_time);
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
		public String toString(){
			StringBuffer moves_p = new StringBuffer();
			for(int[] arr : last_moves){
				moves_p.append(Arrays.toString(arr)+", ");
			}
			return player+": "+moves_p.toString();
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
				Integer position){
		this.color = color;
		this.row = row;
		this.column = column;
		this.king = king;
		this.position = position;
	}
}