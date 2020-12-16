public class Point {
    int row;
    int column;
    int position;

    public Point(int row, int column, int position) {
        this.row = row;
        this.column = column;
        this.position = position;
    }

    int squaredDistance(Point target) {
        return (target.row - row) * (target.row - row) + (target.column) * (target.column);
    }

    public static Point toBoardPos(Point point) {
        return new Point(point.row, (point.row % 2 == 0) ? (2 * (point.column + 1) - 1) : (2 * (point.column + 1) - 2), point.position);
    }

    public static Point toGamePos(Point point) {
        int row = point.row;
        int column = (row % 2 == 0) ? ((point.column + 1) / 2 - 1) : (point.column / 2);
        return new Point(row, column, point.position);
    }

    public static int calculatePosIdFromBoardPos(Point point) {
        Point g_pos = Point.toGamePos(point);
        return g_pos.row * 4 + g_pos.column;
    }

    public boolean isValid(int board_size) {
        return row >= 0 && row < board_size && column >= 0 && column < board_size;
    }
}
