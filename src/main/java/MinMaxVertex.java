import java.util.ArrayList;

public class MinMaxVertex implements Cloneable{
    private boolean max;
    private MinMaxVertex father;
    private ArrayList<MinMaxVertex> children;
    private double value;
    boolean isCalculated;
    public int depth;
    MinMaxVertex best_child = null;
    Piece[] current_pieces;
    Move move;

    public MinMaxVertex(boolean max,
                        MinMaxVertex father,
                        Piece[] current_pieces,
                        Move move
    ){
        this.move = move;
        this.max = max;
        this.father = father;
        this.children = new ArrayList<>();
        this.current_pieces = current_pieces;
        this.value = Double.NaN;
        depth = (father==null) ? 0 : father.depth +1;
    }

    void setValue(double value){this.value = value;}

    public boolean isMax() {
        return max;
    }

    public void setMax(boolean max) {
        this.max = max;
    }

    public double getValue() {
        return value;
    }

    public ArrayList<MinMaxVertex> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<MinMaxVertex> children) {
        this.children = children;
    }

    public MinMaxVertex getFather() {
        return father;
    }

    public void setFather(MinMaxVertex father) {
        this.father = father;
    }

    @Override
    public String toString(){
        int length = calculateLength();
        return ((max) ? "MAX " : "MIN ")+" curr_l: "+calculateLength()+" length: "+length+" Move: "+move.toString();
    }

    public int calculateLength() {
        int length = 0;
        MinMaxVertex c_father = getFather();
        while(c_father!=null){
            c_father = c_father.getFather();
            ++length;
        }
        return length;
    }

    public boolean isEvaluated() {
        return isCalculated;
    }

    public void addChild(MinMaxVertex enemy_root) {
        if(children==null) children = new ArrayList<>();
        children.add(enemy_root);
    }

    public void invertMax() {
        max = !max;
        for(MinMaxVertex v : children) v.invertMax();
    }

    public double evaluate() {
        isCalculated = true;
        if(children.isEmpty()){
            value = calculateFitness(current_pieces, max);
        }
        else{
            value = (max) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            best_child = null;
            for(MinMaxVertex child : children){
                double current_result = child.evaluate();
                if(max ? (value < current_result) : (value > current_result)){
                    value = current_result;
                    best_child = child;
                }
            }
        }
        return value;
    }

    private double calculateFitness(Piece[] pieces, boolean side) {
        int our_sum = 0;
        int enemy_sum = 0;
        for(Piece piece : pieces){
            if(piece != null){
                if(piece.side == side) our_sum += (piece.king ? MinMaxTree.KING_COST : 1);
                else enemy_sum += (piece.king ? MinMaxTree.KING_COST : 1);
            }
        }
        return our_sum - enemy_sum;
    }

    public void markLeavesAsDeprecated() {
        if(!children.isEmpty()){
            for(MinMaxVertex child : children) child.markLeavesAsDeprecated();
        }
        else{
            father = null;
        }
    }
}
