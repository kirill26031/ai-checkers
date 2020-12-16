import java.util.ArrayList;

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

    }


}
