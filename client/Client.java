import java.io.*;
import java.util.ArrayList;

class Client {
    private static int numExploredNodes = 0;
    private static int myPiece = 5;
    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8888;
        int[][] board = null;
        String lastMove = null;
        ArrayList<String> rejectedMoves = new ArrayList<>();

        try (GameConnection connection = new GameConnection(host, port);
             BufferedReader console =new BufferedReader(new InputStreamReader(System.in))
            ) {

            GameConnection.GameInfo game = null;
           
            System.out.println("Connecté à " + host + ":" + port);

            while (true) {
                GameConnection.Message message = connection.readMessage();

                if (message == null) {
                    System.out.println("Connexion fermée.");
                    break;
                }

                switch (message.getCommand()) {

                    case GameConnection.GAME_INFO:
                        game = connection.parseGameInfo(message);
                        System.out.println(
                            "Nouvelle partie :"
                            + "\n  Plateau : " + game.getRows() + " x " + game.getCols()
                            + "\n  Temps maximal par coup : "
                            + game.getMaxMoveTime() + " ms");


                        board = new int[game.getCols()][game.getRows()];
                        myPiece = game.getSide() == GameConnection.RED ? 5 : 3;
                        lastMove = null;
                        rejectedMoves.clear();
                        break;

                    case GameConnection.PLAY_RED:
                        readBoard(message.getData(), board, game.getRows(), game.getCols());
                        System.out.println("Nouvelle partie! Vous jouez rouge.");
                        lastMove = playMove(connection, board, rejectedMoves);
                        break;

                    case GameConnection.PLAY_BLACK:
                        readBoard( message.getData(), board,  game.getRows(), game.getCols());

                        System.out.println("Nouvelle partie! Vous jouez noir.");

                        System.out.println("Attendez le coup des rouges.");
                        break;

                    case GameConnection.PLAY:
                        String opponentMove = message.getData().trim().toUpperCase();

                        if (opponentMove.equals("A8-A8")) {
                            if (myPiece == 5 && lastMove == null) lastMove = playMove(connection, board, rejectedMoves);
                            break;
                        }

                        if (lastMove != null) {
                            applyMove(board, lastMove, myPiece);
                            lastMove = null;
                        }

                        int opposite = myPiece == 5 ? 3 : 5;
                        String normalizedMove = opponentMove.replaceAll("[\\s-]+", "");
                        boolean validMove = false;

                        for (String move : getMoves(board, opposite)) {
                            if (move.replace("-", "").equals(normalizedMove)) {
                                validMove = true;
                                break;
                            }
                        }

                        if (!validMove) {
                            System.out.println("Coup adverse invalide : " + opponentMove);
                            break;
                        }

                        applyMove(board, opponentMove, opposite);
                        System.out.println("Coup adverse : " + opponentMove);

                        rejectedMoves.clear();
                        lastMove = playMove(connection, board, rejectedMoves);
                        break;

                    case GameConnection.INVALID_MOVE:
                        System.out.println("Coup refuse : " + lastMove);

                        if (lastMove != null) rejectedMoves.add(lastMove);

                        lastMove = playMove(connection, board, rejectedMoves);
                        break;

                    case GameConnection.GAME_OVER:
                        if (message.getData().isEmpty()) {
                            System.out.println("Partie terminée.");
                        }
                        else {
                            System.out.println("Partie terminée. Dernier coup : "+ message.getData());
                        }

                        lastMove = null;
                        rejectedMoves.clear();
                        break;

                    default:
                        System.out.println("Commande inconnue : " + message.getCommand());
                        break;
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Erreur réseau : " + e.getMessage());
        }
    }

    private static String playMove(GameConnection connection, int[][] board, ArrayList<String> rejectedMoves) throws IOException {
        ArrayList<String> moves = getMoves(board, myPiece);
        moves.removeAll(rejectedMoves);

        if (moves.isEmpty()) {
            System.out.println("Aucun coup possible.");
            return null;
        }

        String move;

        if (rejectedMoves.isEmpty()) {
            long start = System.currentTimeMillis();
            ArrayList<String> bestMoves = getNextMoveAB(board);
            bestMoves.removeAll(rejectedMoves);
            move = bestMoves.isEmpty() ? moves.get(0) : bestMoves.get(0);
            System.out.println("Temps Alpha-Beta : " + (System.currentTimeMillis() - start) + " ms");
        } else {
            move = moves.get(0);
        }

        connection.sendMove(move);
        System.out.println("Coup envoye : " + move);
        return move;
    }

    private static int scoreAlphaBeta(int[][] board, int player, int alpha, int beta, int depth) {
        int opposite = player == 5 ? 3 : 5;
        numExploredNodes++;

        ArrayList<String> moves = getMoves(board, player);

        if (moves.isEmpty()) return player == myPiece ? -100000 : 100000;
        if (depth == 0) return evaluateBoard(board);

        if (player == myPiece) {
            int value = Integer.MIN_VALUE;

            for (String move : moves) {
                applyMove(board, move, player);
                int score = scoreAlphaBeta(board, opposite, alpha, beta, depth - 1);
                undoMove(board, move, player);

                value = Math.max(value, score);
                alpha = Math.max(alpha, value);

                if (alpha >= beta) break;
            }

            return value;
        } else {
            int value = Integer.MAX_VALUE;

            for (String move : moves) {
                applyMove(board, move, player);
                int score = scoreAlphaBeta(board, opposite, alpha, beta, depth - 1);
                undoMove(board, move, player);

                value = Math.min(value, score);
                beta = Math.min(beta, value);

                if (alpha >= beta) break;
            }

            return value;
        }
    }
    private static ArrayList<String> getNextMoveAB(int[][] board) {
        numExploredNodes = 0;

        ArrayList<String> bestMoves = new ArrayList<>();
        ArrayList<String> possibleMoves = getMoves(board, myPiece);

        if (possibleMoves.isEmpty()) return bestMoves;

        int opposite = myPiece == 5 ? 3 : 5;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int bestScore = Integer.MIN_VALUE;

        for (String move : possibleMoves) {
            applyMove(board, move, myPiece);

            int value = scoreAlphaBeta(board, opposite, alpha, beta, 1);

            undoMove(board, move, myPiece);

            if (value > bestScore) {
                bestScore = value;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (value == bestScore) {
                bestMoves.add(move);
            }

            alpha = Math.max(alpha, bestScore);
        }

        return bestMoves;
    }

    private static ArrayList<String> getMoves(int[][] board, int player) {
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

    private static void applyMove(int[][] board, String move, int player) {
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

    private static void undoMove(int[][] board, String move, int player) {
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

    private static int evaluateBoard(int[][] board) {
        // TODO :   evaluation statique du board
        return 0;
    }

    private static void readBoard(
            String data,
            int[][] board,
            int rows,
            int cols) {

        String[] values = data.trim().split("\\s+");

        if (values.length != rows * cols)
            throw new IllegalArgumentException("Invalid board received.");

        int index = 0;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                board[x][y] = Integer.parseInt(values[index++]);
            }
        }
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
