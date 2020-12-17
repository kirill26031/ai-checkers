import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Board {
    BoardTile[] tiles;
    Piece[] pieces;
    private String bot_color;

    public Board(Tile[] board, String bot_color) {
        if (board == null) board = generateDefaultBoard();
        this.bot_color = bot_color;
        tiles = new BoardTile[32];
        pieces = new Piece[32];
        for (Tile tile : board) {
            tiles[tile.position - 1] = new BoardTile(
                    new Point(tile.row, tile.column),
                    tile.position - 1);
            Piece new_piece = new Piece(tile.king, tile.position - 1, tile.color.equals(bot_color), tiles[tile.position - 1]);
            pieces[tile.position - 1] = new_piece;
        }
        for (int i = 12; i < 20; ++i)
            tiles[i] = new BoardTile(new Point(i / 4, i % 4), i);
        for (BoardTile boardTile : tiles) {
            boardTile.neighbours = generateNeighbours(boardTile);
            boardTile.jump_neighbours = generateJumpNeighbours(boardTile);
        }
    }

    private Tile[] generateDefaultBoard() {
        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(Paths.get("response.json"));
            GameInfo result = new Gson().fromJson(reader, GameInfo.class);
            return result.data.board;
        } catch (IOException e) {
            e.printStackTrace();
            String red = "RED";
            String white = "BLACK";
            Tile[] result = new Tile[24];
            for (int i = 0; i < 12; ++i) {
                result[i] = new Tile(red, i / 4, i % 4, false, i + 1);
            }
            for (int i = 12; i < 24; ++i) {
                result[i] = new Tile(red, i / 4, i % 4, false, i+9);
            }
            return result;
        }
    }

    private ArrayList<BoardTile> generateJumpNeighbours(BoardTile tile) {
        Point boardPos = Point.toBoardPos(tile.position);
        ArrayList<BoardTile> jump_neighbours = new ArrayList<>(4);
        for (int sign_row = -2, i = 0; sign_row <= 2; sign_row += 4) {
            for (int sign_col = -2; sign_col <= 2; sign_col += 4, ++i) {
                Point board_pos_neighbour = new Point(boardPos.row + sign_row, boardPos.column + sign_col);
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
                Point board_pos_neighbour = new Point(boardPos.row + sign_row, boardPos.column + sign_col);
                int neighbour_pos_id = Point.calculatePosIdFromBoardPos(board_pos_neighbour);
                if (neighbour_pos_id >= 0 && neighbour_pos_id < 32 && board_pos_neighbour.isValid(8))
                    nearest.add(tiles[neighbour_pos_id]);
            }
        }
        return nearest;
    }

    void updateColor(String new_color) {
        if (!new_color.equals(bot_color)) {
            for(Piece piece : pieces){
                if(piece!=null) piece.side = !piece.side;
            }
//            for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
//                if (entry.getValue() != null) entry.getValue().side = !entry.getValue().side;
//            }
        }
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
