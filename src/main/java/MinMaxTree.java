import java.util.*;

public class MinMaxTree {
    Board board;
    MinMaxVertex root;
    ArrayList<MinMaxVertex> leaves = new ArrayList<>(1024);
    boolean isFirst;

    public MinMaxTree(Board board, boolean isFirst) {
        this.isFirst=isFirst;
        this.board = board;
        root = new MinMaxVertex(!isFirst, null, new ArrayList<>(), board.pieces);
        MinMaxVertex enemy_root = new MinMaxVertex(isFirst, root, new ArrayList<>(), board.pieces);
        root.addChild(enemy_root);
        leaves.add(enemy_root);
    }

    void addLayer() {
        ArrayList<MinMaxVertex> new_layer = new ArrayList<>(leaves.size()*2);
        for(MinMaxVertex vertex : leaves){
            ArrayList<Move> possible_moves = getAvailableMoves(vertex.getFather().current_pieces, !isFirst);

        }
    }

    ArrayList<Move> getAvailableMoves(HashMap<Integer, Piece> pieces, boolean side){
        ArrayList<Move> available_moves = new ArrayList<>();
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            Piece piece = entry.getValue();
            int a_dir_c = allowedDirectionCoefficient(piece);
            for(BoardTile tile : piece.occupied_tile.neighbours){
                if(piece.side == side && (piece.king || a_dir_c*(tile.position.row - piece.occupied_tile.position.row) > 0)){
                    if(pieces.get(tile.position_id) == null){
                        available_moves.add(new Move(piece.occupied_tile.position_id, tile.position_id));
                    }
                }
            }
            available_moves.addAll(getAllAvailableJumpMovesFrom(piece, a_dir_c, pieces, side));
        }
        return available_moves;
    }

    private ArrayList<JumpMove> getAllAvailableJumpMovesFrom(Piece piece, int a_dir_c, HashMap<Integer, Piece> pieces, boolean side) {
        ArrayList<JumpMove> possible_moves = new ArrayList<>();
        for(BoardTile tile : piece.occupied_tile.jump_neighbours){
            if(piece.side == side && (piece.king || a_dir_c*(tile.position.row - piece.occupied_tile.position.row) > 0)){
                Piece possibly_beaten = pieces.get(piece.occupied_tile.getTileBetween(tile).position_id);
                if(pieces.get(tile.position_id) == null && possibly_beaten != null && possibly_beaten.side != piece.side){
                    JumpMove new_move = new JumpMove(piece.occupied_tile.position_id, tile.position_id, possibly_beaten);
                    Piece changed_piece = new Piece(piece.king, piece.start_position, piece.side, tile);
                    // I need recursion to support possible jump-chains
                    // Also, in order to omit copying entire pieces HashMap (with small changes), I change pieces before recursion, and return it back after.
                    pieces.replace(piece.occupied_tile.position_id, changed_piece);
                    pieces.remove(possibly_beaten.occupied_tile.position_id);
                    ArrayList<JumpMove> possible_next_moves = getAllAvailableJumpMovesFrom(changed_piece, a_dir_c, pieces, side);
                    if(possible_next_moves.isEmpty()) possible_moves.add(new_move);
                    for(JumpMove next_move : possible_next_moves){
                        next_move.positions.addFirst(new_move.positions.getFirst());
                        new_move.beaten_pieces.addFirst(new_move.beaten_pieces.getFirst());
                        possible_moves.add(next_move);
                    }
                    // return pieces to current state
                    pieces.put(possibly_beaten.occupied_tile.position_id, possibly_beaten);
                    pieces.replace(piece.occupied_tile.position_id, piece);
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
