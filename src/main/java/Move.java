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

	public Move(GameInfo.LastMove last_move) {
		positions = new LinkedList<>();
		positions.add(last_move.last_moves[0][0]-1);
		for(int[] m : last_move.last_moves){
			positions.add(m[1]-1);
		}
	}

	@Override
	public String toString(){
		StringBuilder start = new StringBuilder("[");
		start.append(positions.get(0)+1);
		for(int i=1; i<positions.size(); ++i) start.append(", ").append(positions.get(i)+1);
		start.append(']');
		return start.toString();
	}

	@Override
	public boolean equals(Object move){
		if(move.getClass() != Move.class) return false;
		if(positions.size() != ((Move)move).positions.size()) return false;
		boolean equal = true;
		for(int i=0; i<positions.size(); ++i){
			equal &= positions.get(i).equals(((Move) move).positions.get(i));
		}
		return equal;
	}
}

class MoveAdapter implements JsonSerializer<Move> {

	public JsonElement serialize(Move move, Type typeOfSrc,
								 JsonSerializationContext context) {

		JsonObject obj = new JsonObject();

		obj.addProperty("move", String.format("{\n    \"move\": %s\n}", move.toString()));
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