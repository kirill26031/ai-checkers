import com.google.gson.Gson;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

class Game {
	private final String url = "http://localhost:8081/";
	private final String team_name = "Ants";
	private final String charset = "UTF-8";

	Game(){
		try {
			connectToServer(team_name);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void connectToServer(String team_name) throws IOException {
		String query = "team_name="+URLEncoder.encode(team_name, charset);
		URLConnection connection = new URL(url+"game?"+query).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

		OutputStream output = connection.getOutputStream();
		output.write(query.getBytes(charset));

		InputStream response = connection.getInputStream();
		Reader reader = new InputStreamReader(response, charset);
		SuccessfulConnectionJsonResponse result  = new Gson().fromJson(reader, SuccessfulConnectionJsonResponse.class);
		System.out.println("Status: "+result.status);
		System.out.println("Color: "+result.data.color);
		System.out.println("Token: "+result.data.token);
//		{
//			"status": "success",
//				"data": {
//			"color": "BLACK",
//					"token": "94ce212a5cc2ecbc3fdbd1229a415628"
//		}
//		}
	}
}

class SuccessfulConnectionJsonResponse{
	String status;
	SuccessfulConnectionJsonResponseData data;

	class SuccessfulConnectionJsonResponseData {
		String color;
		String token;
	}
}
