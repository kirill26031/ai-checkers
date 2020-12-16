public class Piece {
    boolean king;
    int start_position;
    boolean side;
    BoardTile occupied_tile;

    public Piece(boolean king, int start_position, boolean side, BoardTile occupied_tile) {
        this.king = king;
        this.start_position = start_position;
        this.side = side;
        this.occupied_tile = occupied_tile;
    }
}
