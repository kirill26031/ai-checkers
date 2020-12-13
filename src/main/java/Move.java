import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class Move {
	int from;
	int to;

	Move (int from, int to){
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString(){
		return "M{"+from+", "+to+"}";
	}
}

class MoveAdapter implements JsonSerializer<Move> {

	public JsonElement serialize(Move move, Type typeOfSrc,
								 JsonSerializationContext context) {

		JsonObject obj = new JsonObject();

		obj.addProperty("move", "["+move.from+ "," +move.to+"]");
		return obj;
	}
}
