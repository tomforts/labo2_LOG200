import java.io.*;
import java.util.ArrayList;

class Client {
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
                            Game.applyMove(board, lastMove, myPiece);
                            lastMove = null;
                        }

                        int opposite = myPiece == 5 ? 3 : 5;
                        Game.applyMove(board, opponentMove, opposite);
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
            bestMoves.removeAll(rejectedMoves);
            move = bestMoves.isEmpty() ? moves.get(0) : bestMoves.get(0);
            //print pour tester la rapidité de notre algo. permet de voir le temps quand on fait des changements..
            System.out.println("Temps Alpha-Beta : " + (System.currentTimeMillis() - start) + " ms");
        } else {
            move = moves.get(0);
        }

        connection.sendMove(move);
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
