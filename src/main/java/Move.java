import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Move {
	ArrayList<Integer> positions;

	Move (int from, int to){
		positions = new ArrayList<>(2);
		positions.add(from);
		positions.add(to);
	}

	Move(ArrayList<Integer> positions){
		this.positions=positions;
	}

	@Override
	public String toString(){
		StringBuilder start = new StringBuilder("[");
		start.append(positions.get(0));
		for(int i=1; i<positions.size(); ++i) start.append(", ").append(positions.get(i));
		start.append(']');
		return start.toString();
	}
}

class MoveAdapter implements JsonSerializer<Move> {

	public JsonElement serialize(Move move, Type typeOfSrc,
								 JsonSerializationContext context) {

		JsonObject obj = new JsonObject();

		obj.addProperty("move", move.toString());
		return obj;
	}
}
