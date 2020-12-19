import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.util.Base64;

public class Network {
    private final String url = "http://localhost:8081/";
    private final String charset = "UTF-8";
    String team_name;

    Network(String team_name){
        this.team_name = team_name;
    }

    public MoveResponse sendMove(String token, Move move) throws IOException {
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

    public SCResponse.SCRData connectToServer(String team_name) throws IOException, ConnectException {
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

    public GameInfo.GIData getInfo() throws IOException {
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
