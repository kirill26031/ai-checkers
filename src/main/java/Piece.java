public class Piece implements Cloneable {
    boolean king;
    int start_position; // seems redundant
    boolean side;
    BoardTile occupied_tile;
    boolean alive; // seems redundant

    public Piece(boolean king, int start_position, boolean side, BoardTile occupied_tile) {
        this.alive = true;
        this.king = king;
        this.start_position = start_position;
        this.side = side;
        this.occupied_tile = occupied_tile;
    }

    @Override
    public Piece clone(){
        return new Piece(king, start_position, side, occupied_tile);
    }
}
