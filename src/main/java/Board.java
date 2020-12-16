import java.util.ArrayList;
import java.util.HashMap;

public class Board {
    BoardTile[] tiles;
    Piece[] pieces;

    public Board(GameInfo.Tile[] board, String bot_color) {
        tiles = new BoardTile[32];
        pieces = new Piece[32];
        for (GameInfo.Tile tile : board) {
            pieces[tile.position - 1] = new Piece(tile.king, tile.position, tile.color.equals(bot_color), null);
            tiles[tile.position - 1] = new BoardTile(
                    new Point(tile.row, tile.column, tile.position - 1),
                    pieces[tile.position - 1]);
            tiles[tile.position - 1].piece.occupied_tile = tiles[tile.position - 1];
        }
        for (GameInfo.Tile tile : board) {
            tiles[tile.position - 1].neighbours = generateNeighbours(tiles[tile.position - 1]);
            tiles[tile.position - 1].jump_neighbours = generateJumpNeighbours(tiles[tile.position - 1]);
        }
    }

    private ArrayList<BoardTile> generateJumpNeighbours(BoardTile tile) {
        Point boardPos = Point.toBoardPos(tile.position);
        ArrayList<BoardTile> jump_neighbours = new ArrayList<>(4);
        for (int sign_row = -2, i = 0; sign_row <= 2; sign_row += 4) {
            for (int sign_col = -2; sign_col <= 2; sign_col += 4, ++i) {
                Point board_pos_neighbour = new Point(boardPos.row + sign_row, boardPos.column + sign_col, -1);
                int neighbour_pos_id = Point.calculatePosIdFromBoardPos(board_pos_neighbour);
                if (neighbour_pos_id >= 0 && neighbour_pos_id < 32 && board_pos_neighbour.isValid(8))
                    jump_neighbours.add(tiles[neighbour_pos_id]);
            }
        }
        return jump_neighbours;
    }

    private ArrayList<BoardTile> generateNeighbours(BoardTile tile) {
        Point boardPos = Point.toBoardPos(tile.position);
        ArrayList<BoardTile> nearest = new ArrayList<>(4);
        for (int sign_row = -1, i = 0; sign_row <= 1; sign_row += 2) {
            for (int sign_col = -1; sign_col <= 1; sign_col += 2, ++i) {
                Point board_pos_neighbour = new Point(boardPos.row + sign_row, boardPos.column + sign_col, -1);
                int neighbour_pos_id = Point.calculatePosIdFromBoardPos(board_pos_neighbour);
                if (neighbour_pos_id >= 0 && neighbour_pos_id < 32 && board_pos_neighbour.isValid(8))
                    nearest.add(tiles[neighbour_pos_id]);
            }
        }
        return nearest;
    }

//    void updateBoard(GameInfo.LastMove lastMove){
//        int[] last = lastMove.last_moves[0];
//        if(tiles[last[0]].isConnectedTo(tiles[last[1]])){
//
//        }
//        else {
//            Piece beaten = tiles[last[0]].getTileBetween(tiles[last[1]]).piece;
//        }
//    }
}
