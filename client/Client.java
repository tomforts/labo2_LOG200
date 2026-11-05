import java.io.*;
import java.util.ArrayList;

class Client {
    private static int numExploredNodes = 0;
    private static int myPiece = 5;
    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8888;
        int[][] board = null;

        
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

                        // TODO : sauvegarder le temps max pour repondre

                        break;

                    case GameConnection.PLAY_RED:
                         readBoard(message.getData(), board, game.getRows(), game.getCols());

                        System.out.println("Nouvelle partie! Vous jouez rouge.");

                        System.out.println( "Entrez votre premier coup : ");
                        // TODO : Appeler getNextMoveAB(board) pour choisir le meilleur coup
                        // TODO : Selectionner un coup parmi les meilleurs coups retournes
                        // TODO : Envoyer automatiquement ce coup au serveur
                        // TODO : Memoriser le coup envoye pour mettre a jour le plateau lorsqu'il est accepte
                        connection.sendMove(console.readLine());                       
                        break;

                    case GameConnection.PLAY_BLACK:
                        readBoard(
                                message.getData(),
                                board,
                                game.getRows(),
                                game.getCols());

                        System.out.println("Nouvelle partie! Vous jouez noir.");

                        System.out.println("Attendez le coup des rouges.");
                        // TODO : Attendre le message PLAY contenant le coup des rouges
                        // TODO : Verifier la validite du coup adverse lors de sa reception
                        break;

                    case GameConnection.PLAY:
                        System.out.println("Dernier coup : "+ message.getData());

                        System.out.println("Entrez votre coup : ");

                        connection.sendMove(console.readLine());
                        // TODO : Gerer le coup special A8-A8 envoye au premier tour des rouges
                        // TODO : updater le plateau avec notre dernier coup accepte
                        // TODO : applique le coup adverse sur le plateau avec applyMove()
                        // TODO : appeler getNextMoveAB(board) pour calculer notre prochain coup
                        // TODO : selectionner un coup et l'envoyer au serveur
                        // TODO : memoriser le coup envoye en attendant sa validation
                        break;

                    case GameConnection.INVALID_MOVE:
                        System.out.println("Coup invalide, entrez un nouveau coup : ");

                        connection.sendMove(console.readLine());
                        // TODO : identifier le coup refuse par le serveur
                        // TODO : pas renvoyer le meme coup invalide
                        // TODO : selectionner un autre coup valide
                        break;

                    case GameConnection.GAME_OVER:
                        if (message.getData().isEmpty()) {
                            System.out.println("Partie terminée.");
                        }
                        else {
                            System.out.println("Partie terminée. Dernier coup : "+ message.getData());
                        }

                        // TODO : analyser le resultat WIN LOSS DRAW ou TIMEOUT
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

    private static int scoreAlphaBeta(int[][] board, int player, int alpha, int beta, int depth) {
        int opposite = player == 5 ? 3 : 5;
        numExploredNodes++;

        if (depth == 0) return evaluateBoard(board);

        ArrayList<String> moves = getMoves(board, player);

        if (moves.isEmpty()) return player == myPiece ? -100000 : 100000;

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

            int value = scoreAlphaBeta(board, opposite, alpha, beta, 2);

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
        // TODO : generer les deplacements et les cases a detruire
        return new ArrayList<>();
    }

    private static void applyMove(int[][] board, String move, int player) {
        // TODO : appliquer le deplacement et la destruction
    }

    private static void undoMove(int[][] board, String move, int player) {
        // TODO : annuler le coup
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
