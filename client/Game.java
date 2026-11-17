import java.util.ArrayList;

public class Game {
    // Constants
    private static final int RED = 5;
    private static final int BLACK = 3;
    private static final int EMPTY = 0;
    private static final int DESTROYED = 1;


    //retourne tous les coups possibles incluant les cases a detruire
    public static ArrayList<String> getMoves(int[][] board, int player) {
        ArrayList<String> moves = new ArrayList<>();
        int[] playerPos = findPlayer(board, player);
        if (playerPos == null) {
            return moves;
        }
        int px = playerPos[0], py = playerPos[1];

        int boardSize = board[0].length;
        String currentPoString = square(px, py, boardSize);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                for (int distance = 1; distance <= 3; distance++) {
                    int newX = px + dx * distance;
                    int newY = py + dy * distance;

                    if (!inside(board, newX, newY) || board[newX][newY] != 0) break;

                    board[px][py] = 0;
                    board[newX][newY] = player;

                    ArrayList<String> removableSquares = getRemovableSquares(board, player);

                    for (String target : removableSquares) {
                        String move = currentPoString + "-" + square(newX, newY, boardSize) + "-" + target;
                        moves.add(move);
                    }

                    board[newX][newY] = 0;
                    board[px][py] = player;
                }
            }
        }

        return moves;
    }

    public static void applyMove(int[][] board, String move, int player) {
        int boardSize = board[0].length;
        String[] positions = move.replaceAll("[\\s-]+", "").toUpperCase().split("(?=[A-Z])");

        int startX = positions[0].charAt(0) - 'A';
        int startY = boardSize - Integer.parseInt(positions[0].substring(1));
        int endX = positions[1].charAt(0) - 'A';
        int endY = boardSize - Integer.parseInt(positions[1].substring(1));
        int destroyX = positions[2].charAt(0) - 'A';
        int destroyY = boardSize - Integer.parseInt(positions[2].substring(1));

        board[startX][startY] = 0;
        board[endX][endY] = player;
        board[destroyX][destroyY] = 1;
    }

    public static void undoMove(int[][] board, String move, int player) {
        int boardSize = board[0].length;
        String[] positions = move.replaceAll("[\\s-]+", "").toUpperCase().split("(?=[A-Z])");

        int startX = positions[0].charAt(0) - 'A';
        int startY = boardSize - Integer.parseInt(positions[0].substring(1));
        int endX = positions[1].charAt(0) - 'A';
        int endY = boardSize - Integer.parseInt(positions[1].substring(1));
        int destroyX = positions[2].charAt(0) - 'A';
        int destroyY = boardSize - Integer.parseInt(positions[2].substring(1));

        board[destroyX][destroyY] = 0;
        board[endX][endY] = 0;
        board[startX][startY] = player;
    }

    public static int[] findPlayer(int[][] board, int player) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == player) {
                    return new int[]{i,j};
                }
            }
        }
        return null;
    }

    //compte le nombre total de move possible, excluant les cases a detruire
    public static int countMoves(int[][] board, int player) {
        int[] playerPos = findPlayer(board, player);
        if (playerPos == null) {
            return 0;
        }
        int px = playerPos[0], py = playerPos[1];

        int moves = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                for (int distance = 1; distance <= 3; distance++) {
                    int x = px + dx * distance;
                    int y = py + dy * distance;

                    if (!inside(board, x, y) || board[x][y] != 0) break;
                    moves++;
                }
            }
        }

        return moves;
    }

    private static ArrayList<String> getRemovableSquares(int[][] board, int player) {
        ArrayList<String> removableSquares = new ArrayList<>();

        int opposite = player == RED ? BLACK : RED;
        int px = -1;
        int py = -1;
        int destroyedSquares = 0;

        for(int x = 0; x < board.length; ++x) {
            for(int y = 0; y < board[0].length; ++y) {
                if (board[x][y] == opposite) {
                    px = x;
                    py = y;
                } else if (board[x][y] == 1) {
                    destroyedSquares++;
                }
            }
        }

        if (px == -1) {
            return removableSquares;
        }

        // 1 if 20 or more destroyed
        // 2 if between 10 and 19
        // 3 if 10 or less
        int sizeMod = 1;
        if (destroyedSquares < 10) {
            sizeMod = 3;
        } else if (destroyedSquares < 20) {
            sizeMod = 2;
        }

        for (int i = sizeMod; i > 0; i--) {
            for (int dx = -i; dx <= i; dx += i) {
                // -n 0 n
                for (int dy = -i; dy <= i; dy += i) {
                    // n
                    // 0
                    // -n
                    int x = px + dx;
                    int y = py + dy;
                    if ((dx == 0 && dy == 0) || !inside(board, x, y) || board[x][y] != 0) {
                        // if square is player, not inside or opponent/already destroyed, skip
                        continue;
                    }
                    removableSquares.add(square(x, y, board[0].length));
                }
            }
            if (destroyedSquares < 20 && removableSquares.size() > 0) {
                break;
            }
        }

        return removableSquares;
    }

    private static boolean inside(int[][] board, int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board[0].length;
    }

    private static String square(int x, int y, int rows) {
        return "" + (char)(65 + x) + (rows - y);
    }
}
