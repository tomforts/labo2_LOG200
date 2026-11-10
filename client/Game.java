import java.util.ArrayList;

public class Game {

    //retourne tous les coups possibles incluant les cases a detruire
    public static ArrayList<String> getMoves(int[][] board, int player) {
        ArrayList<String> moves = new ArrayList<>();
        int px = -1, py = -1;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == player) {
                    px = i;
                    py = j;
                    break;
                }
            }
            if (px != -1) break;
        }

        if (px == -1) return moves;

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
                        String move = square(px, py, board[0].length) + "-" + square(newX, newY, board[0].length) + "-" + target;
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
        String[] positions = move.replaceAll("[\\s-]+", "").toUpperCase().split("(?=[A-Z])");

        int startX = positions[0].charAt(0) - 'A';
        int startY = board[0].length - Integer.parseInt(positions[0].substring(1));
        int endX = positions[1].charAt(0) - 'A';
        int endY = board[0].length - Integer.parseInt(positions[1].substring(1));
        int destroyX = positions[2].charAt(0) - 'A';
        int destroyY = board[0].length - Integer.parseInt(positions[2].substring(1));

        board[startX][startY] = 0;
        board[endX][endY] = player;
        board[destroyX][destroyY] = 1;
    }

    public static void undoMove(int[][] board, String move, int player) {
        String[] positions = move.replaceAll("[\\s-]+", "").toUpperCase().split("(?=[A-Z])");

        int startX = positions[0].charAt(0) - 'A';
        int startY = board[0].length - Integer.parseInt(positions[0].substring(1));
        int endX = positions[1].charAt(0) - 'A';
        int endY = board[0].length - Integer.parseInt(positions[1].substring(1));
        int destroyX = positions[2].charAt(0) - 'A';
        int destroyY = board[0].length - Integer.parseInt(positions[2].substring(1));

        board[destroyX][destroyY] = 0;
        board[endX][endY] = 0;
        board[startX][startY] = player;
    }

    //compte le nombre total de move possible, excluant les cases a detruire
    public static int countMoves(int[][] board, int player) {
        int px = -1, py = -1;

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y] == player) {
                    px = x;
                    py = y;
                    break;
                }
            }
            if (px != -1) break;
        }

        if (px == -1) return 0;

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

        // 0 = empty, 1 = destroyed, 3 = black, 5 = red

        int opposite = player == 5 ? 3 : 5;
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

        int sizeMod = 2;
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
