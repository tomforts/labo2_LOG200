import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Partie {
    // Constants
    private static final int RED = 5;
    private static final int BLACK = 3;
    private static final int EMPTY = 0;
    private static final int DESTROYED = 1;
    private static final int[] DY = {
        -1, -1, -1,
        0,       0,
        1,   1,  1
    };
    private static final int[] DX = {
        -1, 0, 1,
        -1,    1,
        -1, 0, 1
    };

    private final int[] territoryQueue;
    private final int[] territoryDepth;
    private final int[] territoryVisited;

    // Board data
    private int[] board;
    private int rows;
    private int cols;

    // Players Position
    private int redPos;
    private int blackPos;

    // Boardstate data
    public int destroyedCount = 0;

    /**
     * Initializes a Partie object representing a game.
     * Creates an array of rows*cols size for the board
     * @param rowCount amount of rows
     * @param colCount amount of cols
     */
    public Partie(int rowCount, int colCount) {
        rows = rowCount;
        cols = colCount;
        board = new int[rows * cols];

        territoryQueue = new int[rows * cols];
        territoryDepth = new int[rows * cols];
        territoryVisited = new int[rows * cols];
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

    /**
     * Gets the amount of rows
     * @return amount of rows
     */
    public int getRowCount() {
        return rows;
    }

    /**
     * Gets the amount of columns
     * @return amount of colums
     */
    public int getColCount() {
        return cols;
    }

    /**
     * Gets the currently stored position of the player
     * @param player player to get the position of
     * @return index in the board array where the player is
     */
    public int getPosition(int player) {
        return player == RED ? redPos : blackPos;
    }

    /**
     * Sets the position of the player, does not interact with the board
     * @param player player to set position
     * @param position index of the position
     */
    public void setPosition(int player, int position) {
        if (player == RED) {
            redPos = position;
        } else {
            blackPos = position;
        }
        board[position] = player;
    }

    /**
     * Toggles wether a square is destroyed or empty. Interacts with the board
     * @param position index of the sqyare to destroy
     */
    public void toggleDestroyed(int position) {
        if (board[position] == DESTROYED) {
            board[position] = EMPTY;
            destroyedCount--;
        } else {
            board[position] = DESTROYED;
            destroyedCount++;
        }
    }


    // --- Utilities --- //


    /**
     * Gets the index matching the x and y coordinate
     * @param x
     * @param y
     * @return index in the board array of the position
     */
    public int getIndex(int x, int y) {
        return y * cols + x;
    }

    /**
     * Gets the x coordinate of the position
     * @param position
     * @return x coordinate
     */
    public int getX(int position) {
        return position % cols;
    }

    /**
     * Gets the y coordinate of the position
     * @param position
     * @return y coordinate
     */
    public int getY(int position) {
        return position / cols;
    }

    /**
     * Checks wether the square at the x and y coordinates is outside the board
     * @param x
     * @param y
     * @return
     */
    public boolean inside(int x, int y) {
        return x >= 0 && x < cols
            && y >= 0 && y < rows;
    }

    /**
     * Checks wether the given position is outside the board
     * @param position
     * @return
     */
    public boolean inside(int position) {
        return position >= 0 && position < rows * cols;
    }


    // --- String conversions --- //


    /**
     * Turns a position to a string representing it on the board
     * @param position
     * @return
     */
    public String squareToString(int position) {
        int x = getX(position);
        int y = getY(position);
        return "" + (char)('A' + x) + (rows - y);
    }

    /**
     * Gets the position from a string representation of a square
     * @param square
     * @return
     */
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

    /**
     * Turns a move string to its 3 position components (from, to, destroy)
     * @param move
     * @return
     */
    public int[] stringToMove(String move) {
        String[] parts = move.replaceAll("[\\s-]+", "").toUpperCase().split("(?=[A-Z])");

        int from = stringToSquare(parts[0]);
        int to = stringToSquare(parts[1]);
        int destroy = stringToSquare(parts[2]);

        return new int[]{from,to,destroy};
    }

    /**
     * Turns the from, to and destroyed positions to a move string
     * @param from
     * @param to
     * @param destroyed
     * @return
     */
    public String moveToString(int from, int to, int destroyed) {
        return squareToString(from) + "-" + squareToString(to) + "-" + squareToString(destroyed);
    }


    // --- Board Interactions --- //

    /**
     * Moves the player from->to. Affects the board
     * @param from
     * @param to
     * @param player
     */
    public void applyMovement(int from, int to, int player) {
        board[from] = EMPTY;
        setPosition(player, to);
    }

    /**
     * Moves the player to->from. Affects the board
     * @param from
     * @param to
     * @param player
     */
    public void undoMovement(int from, int to, int player) {
        board[to] = EMPTY;
        setPosition(player, from);
    }

    /**
     * Applies a move to the board. Uses applyMovement. Affects the board
     * @param from
     * @param to
     * @param destroyed
     * @param player
     */
    public void applyMove(int from, int to, int destroyed, int player) {
        applyMovement(from, to, player);
        toggleDestroyed(destroyed);
    }

    /**
     * Applies a move to the board. Uses applyMovement. Affects the board
     * @param move
     * @param player
     */
    public void applyMove(String move, int player) {
        int[] moveSquares = stringToMove(move);
        applyMove(moveSquares[0], moveSquares[1], moveSquares[2], player);
    }

    /**
     * Undoes a move, actually applies the move in reverse.
     * @param move
     * @param player
     */
    public void undoMove(String move, int player) {
        int[] moveSquares = stringToMove(move);
        undoMove(moveSquares[0], moveSquares[1], moveSquares[2], player);
    }

    /**
     * Undoes a move, actually applies the move in reverse.
     * @param from
     * @param to
     * @param destroyed
     * @param player
     */
    public void undoMove(int from, int to, int destroyed, int player) {
        toggleDestroyed(destroyed);
        undoMovement(from, to, player);
    }

    public void applyEncoded(int move, int player) {
        int from = move & 255;//on lit les 8 premier bits
        int to = (move >>> 8) & 255;//on lit les 8 bits suivants
        int destroyed = (move >>> 16) & 255;//on lit les 8 derniers bits
        applyMove(from, to, destroyed, player);
    }


    public void undoEncoded(int move, int player) {
        int from = move & 255;
        int to = (move >>> 8) & 255;
        int destroyed = (move >>> 16) & 255;
        undoMove(from, to, destroyed, player);
    }

    // convertit le coup pour le serveur
    public String encodedToString(int move) {
        int from = move & 255;
        int to = (move >>> 8) & 255;
        int destroyed = (move >>> 16) & 255;
        return moveToString(from, to, destroyed);
    }


    // --- Move Generation --- //

    // Génère les coups complets dans output sans faire les string qu'on avait avant
    public int generateMoves(int player, int[] output) {
        int from = getPosition(player);
        int px = getX(from);
        int py = getY(from);
        int[] targets = new int[24];
        int count = 0;

        for (int d = 0; d < 8; d++) {
            for (int distance = 1; distance <= 3; distance++) {
                int x = px + DX[d] * distance;
                int y = py + DY[d] * distance;

                if (!inside(x, y)) break;

                int to = getIndex(x, y);
                if (board[to] != EMPTY) break;

                applyMovement(from, to, player);

                int n = fillDestroyables(player, targets);

                for (int i = 0; i < n; i++) {
                    // Exemple : from = 5 , to = 3 , targets[i] = 7
                    // en binaire : 5 = 00000101, 3 = 00000011, 7 = 00000111
                    // resultat = 00000111 00000011 00000101
                    output[count++] = from | (to << 8) | (targets[i] << 16);
                }

                undoMovement(from, to, player);
            }
        }

        return count;
    }
    /**
     * Gets an arraylist of legal positions for the player to move on the current board
     * Can be improved for speed
     * @param player
     * @return
     */
    public ArrayList<Integer> getMovements(int player) {
        ArrayList<Integer> possibleMovements = new ArrayList<>();

        int playerPos = getPosition(player);
        int px = getX(playerPos);
        int py = getY(playerPos);

        for (int d = 0; d < 8; d++) {
            for (int distance = 1; distance <= 3; distance++) {
                int newX = px + DX[d] * distance;
                int newY = py + DY[d] * distance;

                if (!inside(newX, newY)) break;

                int newPos = getIndex(newX, newY);
                if (board[newPos] != EMPTY) break;

                possibleMovements.add(newPos);
            }
        }

        return possibleMovements;
    }

    /**
     * Gets an arrayList of the positions that can be destroyed by the player
     * @param player
     * @return
     */

    public ArrayList<Integer> getDestroyables(int player) {
        int[] targets = new int[24];
        int count = fillDestroyables(player, targets);
        ArrayList<Integer> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) result.add(targets[i]);

        return result;
    }

    // les indices des cases destructibles vont dans targets
    // retourne le nombre de cases trouver
    private int fillDestroyables(int player, int[] targets) {
        int opposite = player == RED ? BLACK : RED;
        int oppositePos = getPosition(opposite);
        int px = getX(oppositePos);
        int py = getY(oppositePos);

        int maxDistance = destroyedCount < 10 ? 3 : 2;
        int count = 0;

        for (int distance = maxDistance; distance >= 1; distance--) {
            int previousCount = count;

            for (int dx = -distance; dx <= distance; dx += distance) {
                for (int dy = -distance; dy <= distance; dy += distance) {
                    if (dx == 0 && dy == 0) continue;

                    int x = px + dx;
                    int y = py + dy;

                    if (!inside(x, y)) continue;

                    int position = getIndex(x, y);
                    if (board[position] == EMPTY) targets[count++] = position;
                }
            }

            if (destroyedCount < 20 && count > previousCount) break;
        }

        return count;
    }

    // compte les déplacements sans créer de liste a chaque fois comme avant
    public int countMovements(int player) {
        int playerPos = getPosition(player);
        int px = getX(playerPos);
        int py = getY(playerPos);
        int count = 0;

        for (int d = 0; d < 8; d++) {
            for (int distance = 1; distance <= 3; distance++) {
                int x = px + DX[d] * distance;
                int y = py + DY[d] * distance;

                if (!inside(x, y) || board[getIndex(x, y)] != EMPTY) break;
                count++;
            }
        }

        return count;
    }


    // --- Score Calculation --- //
    private int territoryStamp = 0;

    public long getTerritory(int player) {
        int opposite = player == RED ? BLACK : RED;
        int start = getPosition(player);
        int opponent = getPosition(opposite);

        // Generate a new visitation stamp.
        if (++territoryStamp == Integer.MAX_VALUE) {
            Arrays.fill(territoryVisited, 0);
            territoryStamp = 1;
        }

        int head = 0;
        int tail = 0;

        territoryQueue[tail] = start;
        territoryDepth[tail] = 0;
        tail++;

        territoryVisited[start] = territoryStamp;

        int immediate = 0;
        int future = 0;

        while (head < tail) {

            int current = territoryQueue[head];
            int depth = territoryDepth[head];
            head++;

            if (depth == 2) {
                continue;
            }

            //on prend colonne au lieu d'hardcoder le nombre
            int x = current % cols;
            int y = current / cols;

            for (int d = 0; d < 8; d++) {

                for (int distance = 1; distance <= 3; distance++) {

                    int nx = x + DX[d] * distance;
                    int ny = y + DY[d] * distance;

                    // Outside the board.
                    if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) break;

                    int next = ny * cols + nx;

                    // Opponent or obstacle.
                    if (next == opponent ||
                        (next != start && board[next] != EMPTY)) {
                        break;
                    }

                    if (territoryVisited[next] == territoryStamp) {
                        continue;
                    }

                    territoryVisited[next] = territoryStamp;

                    territoryQueue[tail] = next;
                    territoryDepth[tail] = depth + 1;
                    tail++;

                    if (depth == 0) {
                        immediate++;
                    } else {
                        future++;
                    }
                }
            }
        }

        // Pack both values into one long to avoid creating an array.
        return ((long) immediate << 32) | (future & 0xFFFFFFFFL);
    }

    /**
     * Gets the amount of directions where the player can go.
     * can be improved, almost a duplicate of getMovements
     * @param player
     * @return
     */
    public int getDirectionCount(int player) {
        int playerPos = getPosition(player);
        int px = getX(playerPos);
        int py = getY(playerPos);

        int directionCount = 0;
        for (int d = 0; d < 8; d++) {
            int newX = px + DX[d];
            int newY = py + DY[d];

            if (!inside(newX, newY)) continue;

            int newPos = getIndex(newX, newY);
            if (board[newPos] == EMPTY) {
                directionCount++;
            }
        }
        return directionCount;
    }
}
