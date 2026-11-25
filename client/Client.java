
import java.io.*;
import java.util.ArrayList;

class Client {
    private static final int RED = 5;
    private static final int BLACK = 3;

    private static Partie currentGame;
    private static Player playerAI;

    private static int myPiece = 5;

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8888;
        String lastMove = null;
        ArrayList<String> rejectedMoves = new ArrayList<>();

        try (GameConnection connection = new GameConnection(host, port);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

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
                        int rows = game.getRows();
                        int cols = game.getCols();

                        System.out.println(
                                "Nouvelle partie :"
                                        + "\n  Plateau : " + rows + " x " + cols
                                        + "\n  Temps maximal par coup : "
                                        + game.getMaxMoveTime() + " ms");

                        myPiece = game.getSide() == GameConnection.RED ? RED : BLACK;
                        lastMove = null;
                        rejectedMoves.clear();

                        currentGame = new Partie(rows, cols);
                        playerAI = null;
                        break;

                    case GameConnection.PLAY_RED:
                        System.out.println("Nouvelle partie! Vous jouez rouge.");

                        currentGame.readBoard(message.getData());
                        playerAI = new Player(myPiece, currentGame, game.getMaxMoveTime());
                        lastMove = playMove2(connection, rejectedMoves);
                        break;

                    case GameConnection.PLAY_BLACK:
                        currentGame.readBoard(message.getData());
                        playerAI = new Player(myPiece, currentGame, game.getMaxMoveTime());

                        System.out.println("Nouvelle partie! Vous jouez noir.");
                        System.out.println("Attendez le coup des rouges.");
                        break;

                    case GameConnection.PLAY:
                        String opponentMove = message.getData().trim().toUpperCase();

                        // Message special du serveur au premier tour des rouges.
                        if (opponentMove.equals("A8-A8")) {
                            if (myPiece == RED && lastMove == null) lastMove = playMove2(connection, rejectedMoves);
                            break;
                        }

                        // Notre dernier coup est deja applique dans Partie.
                        lastMove = null;

                        int opposite = myPiece == RED ? BLACK : RED;
                        System.out.println("Coup adverse : " + opponentMove);

                        currentGame.applyMove(opponentMove, opposite);
                        rejectedMoves.clear();
                        lastMove = playMove2(connection, rejectedMoves);
                        break;

                    case GameConnection.INVALID_MOVE:
                        System.out.println("Coup refuse : " + lastMove);

                        if (lastMove != null) {
                            currentGame.undoMove(lastMove, myPiece);
                            rejectedMoves.add(lastMove);
                        }

                        lastMove = playMove2(connection, rejectedMoves);
                        break;

                    case GameConnection.GAME_OVER:
                        if (message.getData().isEmpty()) {
                            System.out.println("Partie terminée.");
                        } else {
                            System.out.println("Partie terminée. Dernier coup : " + message.getData());
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

    private static String playMove2(GameConnection connection, ArrayList<String> rejectedMoves) throws IOException {
        String move = null;

        if (rejectedMoves.isEmpty()) {
            long start = System.currentTimeMillis();

            ArrayList<String> bestMoves = playerAI.getNextMovesAB();
            if (!bestMoves.isEmpty()) move = bestMoves.get(0);

            //print pour tester la rapidité de notre algo. permet de voir le temps quand on fait des changements..
            System.out.println("Temps Alpha-Beta v2 : " + (System.currentTimeMillis() - start) + " ms");

            // System.out.println("Temps total territory : " + (Partie.testTerritory) + " ms");
            // Partie.testTerritory = 0;
        } else {
            // get next best thing ?
            move = getAlternativeMove(rejectedMoves);
        }

        if (move == null) {
            System.out.println("Aucun coup possible.");
            return null;
        }

        connection.sendMove(move);
        currentGame.applyMove(move, myPiece);
        System.out.println("Coup envoye : " + move);
        return move;
    }

    // Trouve un autre coup legal si le serveur refuse le premier.
    private static String getAlternativeMove(ArrayList<String> rejectedMoves) {
        int from = currentGame.getPosition(myPiece);
        ArrayList<Integer> movements = currentGame.getMovements(myPiece);

        for (int to : movements) {
            currentGame.applyMovement(from, to, myPiece);

            try {
                ArrayList<Integer> destroyables = currentGame.getDestroyables(myPiece);

                for (int destroy : destroyables) {
                    String move = currentGame.moveToString(from, to, destroy);
                    if (!rejectedMoves.contains(move)) return move;
                }
            } finally {
                currentGame.undoMovement(from, to, myPiece);
            }
        }

        return null;
    }
}
