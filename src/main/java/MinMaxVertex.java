import java.util.ArrayList;

public class MinMaxVertex implements Cloneable{
    private boolean max;
    private MinMaxVertex father;
    private ArrayList<MinMaxVertex> children;
    private double value;
    boolean isCalculated;
    public int depth;
    ArrayList<Piece> current_pieces;

    public MinMaxVertex(boolean max,
                        MinMaxVertex father,
                        ArrayList<MinMaxVertex> children,
                        ArrayList<Piece> current_pieces
    ){
        this.max = max;
        this.father = father;
        this.children = children;
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

//    public MapTile getLocation() {
//        return location;
//    }
//
//    public void setLocation(MapTile location) {
//        this.location = location;
//    }

    public MinMaxVertex getFather() {
        return father;
    }

    public void setFather(MinMaxVertex father) {
        this.father = father;
    }

    @Override
    public String toString(){
        int length = calculateLength();
        return ((max) ? "MAX " : "MIN ")+" length: "+length;
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
        return !(Double.isNaN(value));
    }

    public void addChild(MinMaxVertex enemy_root) {
        if(children==null) children = new ArrayList<>();
        children.add(enemy_root);
    }
}
