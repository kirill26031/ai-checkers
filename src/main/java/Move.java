import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;

public class Move {
	LinkedList<Integer> positions;

	Move (int from, int to){
		positions = new LinkedList<>();
		positions.add(from);
		positions.add(to);
	}

	Move(LinkedList<Integer> positions){
		this.positions=positions;
	}

	@Override
	public String toString(){
		StringBuilder start = new StringBuilder("[");
		start.append(positions.get(0));
		for(int i=1; i<positions.size(); ++i) start.append(", ").append(positions.get(i)+1);
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

class JumpMove extends Move{
	LinkedList<Piece> beaten_pieces = new LinkedList<>();

	JumpMove(int from, int to, Piece beaten) {
		super(from, to);
		beaten_pieces.add(beaten);
	}

	JumpMove(LinkedList<Integer> positions, ArrayList<Piece> pieces) {
		super(positions);
		beaten_pieces.addAll(pieces);
	}
}