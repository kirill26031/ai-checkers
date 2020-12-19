import java.util.*;

public class MinMaxTree {
	Board board;
	Piece[] current_pieces;
	MinMaxVertex root;
	private static final int DEFAULT_LEAVES_CAPACITY = 7;
	ArrayList<MinMaxVertex> leaves = new ArrayList<>(DEFAULT_LEAVES_CAPACITY);
	ArrayList<MinMaxVertex> deepest_leaves = new ArrayList<>(DEFAULT_LEAVES_CAPACITY);
	private int current_leaves_index = 0;
	boolean isFirst;

	public static final double KING_COST = 5;

	public MinMaxTree(Board board, boolean isFirst) {
		this.isFirst = isFirst;
		this.board = board;
		current_pieces = board.pieces;
		root = new MinMaxVertex(!isFirst, null, board.pieces, null);
		MinMaxVertex enemy_root = new MinMaxVertex(isFirst, root, board.pieces, null);
		root.addChild(enemy_root);
		leaves.add(enemy_root);
		addLayer(System.currentTimeMillis()+1000);
		addLayer(System.currentTimeMillis()+1000);
	}

	boolean addLayer(long deadline) throws OutOfMemoryError {
//		final int size_of_MinMaxVertex_object = 400;
//		long memory_used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//		int size_of_Piece_object = 64;
		for(; current_leaves_index<leaves.size(); ++current_leaves_index) {
			MinMaxVertex vertex = leaves.get(current_leaves_index);
			//omitting grow of deprecated branches
			if(vertex.getFather() == null) continue;
//            int size_of_HashMap_object = vertex.current_pieces.size()*(size_of_Piece_object+16)+32*24+4*32;
//            int size_of_MinMaxVertex_object = 1+4+4+8+1+4+size_of_HashMap_object+4+2*16;
//			if (Runtime.getRuntime().freeMemory() <= size_of_MinMaxVertex_object * 8)
//				throw new OutOfMemoryError("There's no more memory!");
			long time = System.currentTimeMillis();
			if(time >= deadline) return false;
			vertex.setAsNotEvaluated();
			ArrayList<Move> possible_moves = getAvailableMoves(vertex.current_pieces, vertex.isMax());
			if (possible_moves.isEmpty()) continue;
			for (Move move : possible_moves) {
				MinMaxVertex new_v = new MinMaxVertex(!vertex.isMax(), vertex, changeStateByMove(vertex.current_pieces, move), move);
				deepest_leaves.add(new_v);
				vertex.addChild(new_v);
			}
		}
		leaves = deepest_leaves;
		current_leaves_index = 0;
		deepest_leaves = new ArrayList<>(leaves.size()*8);
//		long current_memory_used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
//		System.out.println("Added memory: " + (current_memory_used - memory_used));
		return true;
	}

	private Piece[] changeStateByMove(Piece[] state, Move move) {
		Piece[] copy = state.clone();
		if (move.getClass() == JumpMove.class) {
			for (Piece beaten : ((JumpMove) move).beaten_pieces) {
				copy[beaten.occupied_tile.position_id] = null;
			}
		}
		if(copy[move.positions.getFirst()] == null){
			System.out.println("err");
		}
		Piece moved_piece = copy[move.positions.getFirst()].clone();
		copy[move.positions.getFirst()] = null;
		copy[move.positions.getLast()] = moved_piece;
		moved_piece.occupied_tile = board.tiles[move.positions.getLast()];
		int first_enemys_row = (allowedDirectionCoefficient(moved_piece)==1 ? 7 : 0);
		if(first_enemys_row == moved_piece.occupied_tile.position.row && !moved_piece.king) moved_piece.king = true;
		return copy;
	}

	ArrayList<Move> getAvailableMoves(Piece[] pieces, boolean side) {
		ArrayList<Move> available_moves = new ArrayList<>();
		ArrayList<Move> jump_moves = new ArrayList<>();
		for (Piece piece : pieces) {
			if (piece == null) continue;
			int a_dir_c = allowedDirectionCoefficient(piece);
			if(jump_moves.isEmpty()){
				for (BoardTile tile : piece.occupied_tile.neighbours) {
					if (piece.side == side && (piece.king || a_dir_c * (tile.position.row - piece.occupied_tile.position.row) > 0)) {
						if (pieces[tile.position_id] == null) {
							available_moves.add(new Move(piece.occupied_tile.position_id, tile.position_id));
						}
					}
				}
			}
			jump_moves.addAll(getAllAvailableJumpMovesFrom(piece, a_dir_c, pieces, side));
		}
		if(!jump_moves.isEmpty()) return jump_moves;
		return available_moves;
	}

	private ArrayList<JumpMove> getAllAvailableJumpMovesFrom(Piece piece, int a_dir_c, Piece[] pieces, boolean side) {
		ArrayList<JumpMove> possible_moves = new ArrayList<>();
		for (BoardTile tile : piece.occupied_tile.jump_neighbours) {
			if (piece.side == side && (piece.king || a_dir_c * (tile.position.row - piece.occupied_tile.position.row) > 0)) {
				Piece possibly_beaten = pieces[piece.occupied_tile.getTileBetween(tile).position_id];
				if (pieces[tile.position_id] == null && possibly_beaten != null && possibly_beaten.side != piece.side) {
					JumpMove new_move = new JumpMove(piece.occupied_tile.position_id, tile.position_id, possibly_beaten);
					Piece changed_piece = new Piece(piece.king, piece.start_position, piece.side, tile);
					if (!piece.king && tile.position.row == ((a_dir_c == 1) ? 7 : 0)) {
						// if next tile will be the first enemy's row, stop jump-chain
						possible_moves.add(new_move);
						break;
					}
					// I need recursion to support possible jump-chains
					ArrayList<JumpMove> possible_next_moves =
							getAllAvailableJumpMovesFrom(changed_piece, a_dir_c, changeStateByMove(pieces, new_move), side);
					if (possible_next_moves.isEmpty()) possible_moves.add(new_move);
					for (JumpMove next_move : possible_next_moves) {
						next_move.positions.addFirst(new_move.positions.getFirst());
						next_move.beaten_pieces.addFirst(new_move.beaten_pieces.getFirst());
						possible_moves.add(next_move);
					}
				}
			}
		}
		return possible_moves;
	}

	private int allowedDirectionCoefficient(Piece piece) {
		// coefficient which declares in which direction piece may move (according to initial game coordinates)
		// 1 - for RED
		// -1 - for BLACK
		return isFirst == piece.side ? 1 : -1;
	}

	public void changeRootMaxMode(boolean isMax) {
		if (isFirst != isMax) {
			root.invertMax();
		}
	}

	public Move evaluate(int max_depth) {
		long start_time = System.currentTimeMillis();
		root.evaluate(max_depth
//				,deadline
		);
		long time_diff = System.currentTimeMillis()-start_time;
		System.out.println("Evaluation time: "+time_diff+"(ms)");
		if(!leaves.isEmpty()) {
			System.out.println("Level: " + Math.min(leaves.get(0).calculateLength(), max_depth));
			System.out.println(
					(!leaves.isEmpty() ? ("Leaves of depth " + leaves.get(0).calculateLength() + ": " + leaves.size()) : ("")) +
							(!deepest_leaves.isEmpty() ? ("Leaves of depth " + deepest_leaves.get(0).calculateLength() + ": " + deepest_leaves.size()) : (""))
			);
		}
		return root.best_child.best_child.move;
	}

	public void updateByMove(Move move, boolean side) throws IllegalStateException{
		if(root.getChildren().get(0).getChildren().isEmpty() || root.getChildren().get(0).getChildren().get(0).isMax() == side) return;
		if(current_pieces[move.positions.get(0)] == null || current_pieces[move.positions.get(0)].side != side) return;
		MinMaxVertex vertex_of_move = null;
		for(MinMaxVertex v : root.getChildren().get(0).getChildren()){
			if(move.equals(v.move)){
				vertex_of_move = v;
				continue;
			}
			v.markLeavesAsDeprecated();
		}
		if(vertex_of_move == null) {
			throw new IllegalStateException(move.toString());
		}
		current_pieces = vertex_of_move.current_pieces;
		root.getChildren().get(0).setMax(vertex_of_move.isMax());
		root.getChildren().get(0).setChildren(vertex_of_move.getChildren());
		root.getChildren().get(0).evaluate(1);
		for(MinMaxVertex child_of_moved : vertex_of_move.getChildren()){
			child_of_moved.setFather(root.getChildren().get(0));
		}
	}
}
