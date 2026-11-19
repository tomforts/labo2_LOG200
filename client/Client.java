import java.io.*;
import java.util.ArrayList;

class Client {
    private static final int RED = 5;
    private static final int BLACK = 3;

    // @TODO: REMOVE THIS ONCE TESTING HAS BEEN FINISHED
    private static final boolean useImprovedVersion = true;

    private static Partie currentGame;
    private static Player playerAI;

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
                        int rows = game.getRows();
                        int cols = game.getCols();
                        System.out.println(
                            "Nouvelle partie :"
                            + "\n  Plateau : " + rows + " x " + cols
                            + "\n  Temps maximal par coup : "
                            + game.getMaxMoveTime() + " ms");


                        board = new int[rows][cols];
                        myPiece = game.getSide() == GameConnection.RED ? RED : BLACK;
                        lastMove = null;
                        rejectedMoves.clear();

                        currentGame = new Partie(rows, cols);
                        break;

                    case GameConnection.PLAY_RED:
                        System.out.println("Nouvelle partie! Vous jouez rouge.");
                        if (useImprovedVersion) {
                            currentGame.readBoard(message.getData());
                            playerAI = new Player(myPiece, currentGame);
                            lastMove = playMove2(connection, rejectedMoves);
                        } else {
                            readBoard(message.getData(), board, game.getRows(), game.getCols());
                            lastMove = playMove(connection, board, rejectedMoves);
                        }

                        break;

                    case GameConnection.PLAY_BLACK:
                        if (useImprovedVersion) {
                            currentGame.readBoard(message.getData());
                            playerAI = new Player(myPiece, currentGame);
                        } else {
                            readBoard(message.getData(), board, game.getRows(), game.getCols());
                        }

                        System.out.println("Nouvelle partie! Vous jouez noir.");

                        System.out.println("Attendez le coup des rouges.");
                        break;

                    case GameConnection.PLAY:
                        String opponentMove = message.getData().trim().toUpperCase();

                        // Ulric: cette ligne sert à quoi ???
                        if (opponentMove.equals("A8-A8")) {
                            if (myPiece == RED && lastMove == null) lastMove = playMove(connection, board, rejectedMoves);
                            break;
                        }

                        if (lastMove != null) {
                            Game.applyMove(board, lastMove, myPiece);
                            lastMove = null;
                        }

                        int opposite = myPiece == RED ? BLACK : RED;
                        System.out.println("Coup adverse : " + opponentMove);

                        if (useImprovedVersion) {
                            currentGame.applyMove(opponentMove, opposite);
                            rejectedMoves.clear();
                            lastMove = playMove2(connection, rejectedMoves);
                        } else {
                            Game.applyMove(board, opponentMove, opposite);
                            rejectedMoves.clear();
                            lastMove = playMove(connection, board, rejectedMoves);
                        }

                        break;

                    case GameConnection.INVALID_MOVE:
                        System.out.println("Coup refuse : " + lastMove);

                        if (lastMove != null) rejectedMoves.add(lastMove);

                        if (useImprovedVersion) {
                            // undo last move
                            lastMove = playMove2(connection, rejectedMoves);
                        } else {
                            lastMove = playMove(connection, board, rejectedMoves);
                        }
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
        ArrayList<String> moves = Game.getMoves(board, myPiece);
        moves.removeAll(rejectedMoves);

        if (moves.isEmpty()) {
            System.out.println("Aucun coup possible.");
            return null;
        }

        String move;

        if (rejectedMoves.isEmpty()) {
            long start = System.currentTimeMillis();

            ArrayList<String> bestMoves = new AI(myPiece).getNextMoveAB(board);
            move = bestMoves.isEmpty() ? moves.get(0) : bestMoves.get(0);
            
            //print pour tester la rapidité de notre algo. permet de voir le temps quand on fait des changements..
            System.out.println("Temps Alpha-Beta : " + (System.currentTimeMillis() - start) + " ms");

            // 596ms
            // 595ms
            // 592ms
        } else {
            move = moves.get(0);
        }

        connection.sendMove(move);
        System.out.println("Coup envoye : " + move);
        return move;
    }

    private static String playMove2(GameConnection connection, ArrayList<String> rejectedMoves) throws IOException {
        String move;

        if (rejectedMoves.isEmpty()) {
            long start = System.currentTimeMillis();

            ArrayList<String> bestMoves = playerAI.getNextMovesAB();
            move = bestMoves.get(0);
            
            //print pour tester la rapidité de notre algo. permet de voir le temps quand on fait des changements..
            System.out.println("Temps Alpha-Beta v2 : " + (System.currentTimeMillis() - start) + " ms");

            System.out.println("Temps total getMov : " + (Partie.testGetMove) + " ms");
            System.out.println("Temps total territory : " + (Partie.testTerritory) + " ms");
            Partie.testGetMove = 0;
            Partie.testTerritory = 0;
        } else {
            // get next best thing ?
            throw new IllegalArgumentException("Invalid playMove2");
            // move = "";
        }

        connection.sendMove(move);
        currentGame.applyMove(move, myPiece);
        System.out.println("Coup envoye : " + move);
        return move;
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


}
