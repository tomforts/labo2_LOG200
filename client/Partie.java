import java.util.ArrayList;

public class Partie {
    // Constants
    private static final int RED = 5;
    private static final int BLACK = 3;
    private static final int EMPTY = 0;
    private static final int DESTROYED = 1;

    // Board data
    private int[] board;
    private int rows;
    private int cols;

    // Players Position
    private int redPos;
    private int blackPos;

    // Boardstate data
    public int destroyedCount = 0;


    public Partie(int rowCount, int colCount) {
        rows = rowCount;
        cols = colCount;
        board = new int[rows * cols];
    }

    /**
     * Inits the board of the game from the data string
     * @param data
     */
    public void readBoard(String data) {
        String[] values = data.trim().split("\\s+");

        int totalSize = rows * cols;
        if (values.length != totalSize)
            throw new IllegalArgumentException("Invalid board received.");

        redPos = -1;
        blackPos = -1;
        destroyedCount = 0;

        for (int i = 0; i < totalSize; i++) {
            int value = Integer.parseInt(values[i]);
            board[i] = value;

            switch (value) {
                case RED:
                    redPos = i;
                    break;
                case BLACK:
                    blackPos = i;
                    break;
                case DESTROYED:
                    destroyedCount++;
                    break;
                default:
                    // should never happen
                    break;
            }
        }
    }

    public int getRowCount() {
        return rows;
    }

    public int getColCount() {
        return cols;
    }

    public int getPosition(int player) {
        return player == RED ? redPos : blackPos;
    }
    
    public void setPosition(int player, int position) {
        if (player == RED) {
            redPos = position;
        } else {
            blackPos = position;
        }
        board[position] = player;
    }

    public void toggleDestroyed(int position) {
        if (board[position] == DESTROYED) {
            board[position] = EMPTY;
            destroyedCount--;
        } else {
            board[position] = DESTROYED;
            destroyedCount++;
        }
    }


    private int getIndex(int x, int y) {
        return y * rows + x;
    }

    private int getX(int position) {
        return position % cols;
    }

    private int getY(int position) {
        return position / rows;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && x < cols
            && y >= 0 && y < rows;
    }
    

    public void applyMovement(int from, int to, int player) {
        board[from] = EMPTY;
        setPosition(player, to);
    }

    public void undoMovement(int from, int to, int player) {
        board[to] = EMPTY;
        setPosition(player, from);
    }



    public void applyMove(int from, int to, int destroyed, int player) {
        board[from] = EMPTY;
        setPosition(player, to);
        toggleDestroyed(destroyed);
    }

    public void applyMove(String move, int player) {
        int[] moveSquares = stringToMove(move);
        applyMove(moveSquares[0], moveSquares[1], moveSquares[2], player);
    }

    public void undoMove(String move, int player) {
        int[] moveSquares = stringToMove(move);
        applyMove(moveSquares[1], moveSquares[0], moveSquares[2], player);
    }

    public void undoMove(int from, int to, int destroyed, int player) {
        applyMove(from, to, destroyed, player);
    }

    public ArrayList<Integer> getMovements(int player) {
        ArrayList<Integer> possibleMovements = new ArrayList<>();

        int playerPos = getPosition(player);
        int px = getX(playerPos);
        int py = getY(playerPos);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                for (int distance = 1; distance <= 3; distance++) {
                    int newX = px + dx * distance;
                    int newY = py + dy * distance;

                    if (!inside(newX, newY)) break;

                    int newPos = getIndex(newX, newY);
                    if (board[newPos] != EMPTY) break;

                    possibleMovements.add(newPos);
                }
            }
        }
        return possibleMovements;
    }

    public ArrayList<Integer> getDestroyables(int player) {
        // get the cases that can be destroyed according to the rules
        int opposite = player == RED ? BLACK : RED;
        ArrayList<Integer> possibleDestroy = new ArrayList<>();

        int playerPos = getPosition(opposite);
        int px = getX(playerPos);
        int py = getY(playerPos);

        // 1 if 20 or more destroyed
        // 2 if between 10 and 19
        // 3 if 10 or less
        int sizeMod = 1;
        if (destroyedCount < 10) {
            sizeMod = 3;
        } else if (destroyedCount < 20) {
            sizeMod = 2;
        }

        for (int i = sizeMod; i > 0; i--) {
            for (int dx = -i; dx <= i; dx+=i) {
                // -n 0 n
                for (int dy = -i; dy <= i; dy+=i) {
                    // n
                    // 0
                    // -n
                    int newX = px + dx;
                    int newY = py + dy;
                    int newPos = getIndex(newX, newY);
                    if ((dx == 0 && dy == 0) || !inside(newX, newY) || board[newPos] != EMPTY) {
                        // if square is player, not inside or opponent/already destroyed, skip
                        continue;
                    }
                    // if none of these, add as possible
                    possibleDestroy.add(newPos);
                }
            }
            if (!possibleDestroy.isEmpty()) {
                // Only allowed to destroy lower distance when there are NO possible at the distance we should destroy.
                break;
            }
        }

        return possibleDestroy;
    }


    public String squareToString(int position) {
        int x = getX(position);
        int y = getY(position);
        return "" + (char)('A' + x) + (rows - y);
    }

    public int stringToSquare(String square) {
        if (square.length() < 2) {
            throw new IllegalArgumentException("Invalid square code: " + square);
        }

        char colChar = square.charAt(0);
        int col = colChar - 'A';
        int row = Integer.parseInt(square.substring(1));

        if (col < 0 || col >= cols || row < 1 || row > rows) {
            throw new IllegalArgumentException("Square out of bounds: " + square);
        }

        return getIndex(col, rows - row);
    }

    public int[] stringToMove(String move) {
        String[] parts = move.replaceAll("[\\s-]+", "").toUpperCase().split("(?=[A-Z])");

        int from = stringToSquare(parts[0]);
        int to = stringToSquare(parts[1]);
        int destroy = stringToSquare(parts[2]);

        return new int[]{from,to,destroy};
    }

    public String moveToString(int from, int to, int destroyed) {
        return squareToString(from) + "-" + squareToString(to) + "-" + squareToString(destroyed);
    }
}
