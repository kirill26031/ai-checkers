import java.util.*;

public class MinMaxTree {
    Board board;
    MinMaxVertex root;
    ArrayList<MinMaxVertex> leaves = new ArrayList<>(1024);
    boolean isFirst;

    public MinMaxTree(Board board, boolean isFirst) {
        this.isFirst = isFirst;
        this.board = board;
        root = new MinMaxVertex(!isFirst, null, board.pieces, null);
        MinMaxVertex enemy_root = new MinMaxVertex(isFirst, root, board.pieces, null);
        root.addChild(enemy_root);
        leaves.add(enemy_root);
    }

    void addLayer() throws OutOfMemoryError {
        long memory_used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int size_of_Piece_object = 64;
        ArrayList<MinMaxVertex> new_layer = new ArrayList<>(leaves.size() * 2);
        for (MinMaxVertex vertex : leaves) {
//            int size_of_HashMap_object = vertex.current_pieces.size()*(size_of_Piece_object+16)+32*24+4*32;
//            int size_of_MinMaxVertex_object = 1+4+4+8+1+4+size_of_HashMap_object+4+2*16;
            int size_of_MinMaxVertex_object = 400;
            if(Runtime.getRuntime().freeMemory() <= size_of_MinMaxVertex_object*8) throw new OutOfMemoryError("There's no more memory!");
            ArrayList<Move> possible_moves = getAvailableMoves(vertex.current_pieces, !vertex.getFather().isMax());
            for (Move move : possible_moves) {
                MinMaxVertex new_v = new MinMaxVertex(vertex.getFather().isMax(), vertex, changeStateByMove(vertex.current_pieces, move), move);
                new_layer.add(new_v);
                vertex.addChild(new_v);
            }
            vertex.current_pieces=null;
        }
        leaves = new_layer;
        long current_memory_used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Free memory: "+Runtime.getRuntime().freeMemory());
        System.out.println("Added memory: "+(current_memory_used - memory_used)+
                " \nFor each vertex: "+(current_memory_used - memory_used)/leaves.size());
    }

//    Move bestMove(){
//
//    }

    private Piece[] changeStateByMove(Piece[] state, Move move) {
        Piece[] copy = state.clone();
        if (move.getClass() == JumpMove.class) {
            for (Piece beaten : ((JumpMove) move).beaten_pieces) {
                copy[beaten.occupied_tile.position_id]=null;
            }
        }
        Piece moved_piece = copy[move.positions.getFirst()].clone();
        copy[move.positions.getFirst()] = null;
        copy[move.positions.getLast()] = moved_piece;
        moved_piece.occupied_tile = board.tiles[move.positions.getLast()];
        return copy;
    }

    private ArrayList<Move> getAvailableMoves(Piece[] pieces, boolean side) {
        ArrayList<Move> available_moves = new ArrayList<>();
        for (Piece piece : pieces) {
            if(piece == null) continue;
            int a_dir_c = allowedDirectionCoefficient(piece);
            for (BoardTile tile : piece.occupied_tile.neighbours) {
                if (piece.side == side && (piece.king || a_dir_c * (tile.position.row - piece.occupied_tile.position.row) > 0)) {
                    if (pieces[tile.position_id] == null) {
                        available_moves.add(new Move(piece.occupied_tile.position_id, tile.position_id));
                    }
                }
            }
            available_moves.addAll(getAllAvailableJumpMovesFrom(piece, a_dir_c, pieces, side));
        }
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
                    // I need recursion to support possible jump-chains
//                    // Also, in order to omit copying entire pieces HashMap (with small changes), I change pieces before recursion, and return it back after.
//                    pieces.replace(piece.occupied_tile.position_id, changed_piece);
//                    pieces.remove(possibly_beaten.occupied_tile.position_id);
                    ArrayList<JumpMove> possible_next_moves =
                            getAllAvailableJumpMovesFrom(changed_piece, a_dir_c, changeStateByMove(pieces, new_move), side);
                    if (possible_next_moves.isEmpty()) possible_moves.add(new_move);
                    for (JumpMove next_move : possible_next_moves) {
                        next_move.positions.addFirst(new_move.positions.getFirst());
                        new_move.beaten_pieces.addFirst(new_move.beaten_pieces.getFirst());
                        possible_moves.add(next_move);
                    }
//                    // return pieces to current state
//                    pieces.put(possibly_beaten.occupied_tile.position_id, possibly_beaten);
//                    pieces.replace(piece.occupied_tile.position_id, piece);
                }
            }
        }
        return possible_moves;
    }

    private int allowedDirectionCoefficient(Piece piece) {
        // coefficient which declares in which direction piece may move (according to initial game coordinates)
        // 1 - for RED
        // -1 - for BLACK
//        // 0 - for kings
//        if(piece.king) return 0;
        return isFirst == piece.side ? 1 : -1;
    }
}
