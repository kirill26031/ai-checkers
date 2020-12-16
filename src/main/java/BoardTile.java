import java.util.ArrayList;

public class BoardTile {
    ArrayList<BoardTile> neighbours;
    ArrayList<BoardTile> jump_neighbours;
    Point position;
    Piece piece;

    public BoardTile(Point position, Piece piece) {
        this.piece = piece;
        this.position = position;
    }

    public boolean isConnectedTo(BoardTile tile) {
        for(BoardTile n_tile : neighbours){
            if (n_tile.equals(tile)) return true;
        }
        return false;
    }

    public boolean isJumpConnectedTo(BoardTile tile) {
        for(BoardTile jn_tile : jump_neighbours){
            if (jn_tile.equals(tile)) return true;
        }
        return false;
    }

    public BoardTile getTileBetween(BoardTile tile) {
        for(BoardTile n_tile : neighbours){
            if(n_tile.isConnectedTo(tile)) return n_tile;
        }
        return null;
    }
}
