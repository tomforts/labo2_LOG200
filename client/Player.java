import java.util.ArrayList;

public class Player {
    private static final int RED = 5;
    private static final int BLACK = 3;

    private final int myPiece;
    // @TODO: change these values to something more accurate
    private final int LOSE_VALUE = -100000;
    private final int WIN_VALUE = 100000;
    private int numExploredNodes = 0;

    private Partie currentGame;

    public Player(int myPiece, Partie game) {
        this.myPiece = myPiece;
        this.currentGame = game;
    }

    public ArrayList<String> getNextMovesAB() {
        numExploredNodes = 0;

        ArrayList<String> bestMoves = new ArrayList<>();
        ArrayList<Integer> possibleMovements = currentGame.getMovements(myPiece);

        if (possibleMovements.isEmpty()) return bestMoves;

        int opposite = myPiece == RED ? BLACK : RED;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int bestScore = Integer.MIN_VALUE;

        int currentPosition = currentGame.getPosition(myPiece);
        for (int destination : possibleMovements) {
            currentGame.applyMovement(currentPosition, destination, myPiece);
            // we could evaluate the board post-move and check the destructions only of the best post-move boards ?
            ArrayList<Integer> destroyables = currentGame.getDestroyables(myPiece);
            for (Integer destroySquare : destroyables) {
                currentGame.toggleDestroyed(destroySquare);

                int value = scoreAlphaBeta(opposite, alpha, beta, 2);

                currentGame.toggleDestroyed(destroySquare);

                if (value > bestScore) {
                    bestScore = value;
                    bestMoves.clear();
                    bestMoves.add(currentGame.moveToString(currentPosition, destination, destroySquare));
                } else if (value == bestScore) {
                    bestMoves.add(currentGame.moveToString(currentPosition, destination, destroySquare));
                }

                alpha = Math.max(alpha, bestScore);
            }
            currentGame.undoMovement(currentPosition, destination, myPiece);
        }

        return bestMoves;
    }

    private int scoreAlphaBeta(int player, int alpha, int beta, int depth) {
        int opposite = player == RED ? BLACK : RED;
        numExploredNodes++;
        int defaultGameValue = player == myPiece ? LOSE_VALUE : WIN_VALUE;

        ArrayList<Integer> possibleMovements = currentGame.getMovements(myPiece);
        if (possibleMovements.isEmpty()) return defaultGameValue;
        if (depth == 0) return evaluateBoard();

        int currentPosition = currentGame.getPosition(player);
        if (player == myPiece) {
            int value = Integer.MIN_VALUE;

            for (int destination : possibleMovements) {
                currentGame.applyMovement(currentPosition, destination, player);
                // we could evaluate the board post-move and check the destructions only of the best post-move boards ?
                ArrayList<Integer> destroyables = currentGame.getDestroyables(player);
                for (Integer destroySquare : destroyables) {
                    currentGame.toggleDestroyed(destroySquare);

                    int score = scoreAlphaBeta(opposite, alpha, beta, depth - 1);

                    currentGame.toggleDestroyed(destroySquare);

                    value = Math.max(value, score);
                    alpha = Math.max(alpha, value);

                    if (alpha >= beta) break;
                }
                currentGame.undoMovement(currentPosition, destination, player);
            }

            return value;
        } else {
            int value = Integer.MAX_VALUE;

            for (int destination : possibleMovements) {
                currentGame.applyMovement(currentPosition, destination, player);
                // we could evaluate the board post-move and check the destructions only of the best post-move boards ?
                ArrayList<Integer> destroyables = currentGame.getDestroyables(player);
                for (Integer destroySquare : destroyables) {
                    currentGame.toggleDestroyed(destroySquare);

                    int score = scoreAlphaBeta(opposite, alpha, beta, depth - 1);

                    currentGame.toggleDestroyed(destroySquare);

                    value = Math.min(value, score);
                    beta = Math.min(beta, value);

                    if (alpha >= beta) break;
                }
                currentGame.undoMovement(currentPosition, destination, player);
            }

            return value;
        }
    }

    //donne un poids au coups qui avantage notre joueur, mais seulement par le nombre de coup totaux qu'on a vs l'adversaire
    //donne un bonus au poids si l'adverse a moins ou egale a 3 coups possible
    //faudrait ameliorer ca, car avoir 3 moves veut pas dire qu'on est proche de gagner
    // @TODO: improve this to better represent how close to winning a board is.
    // Ideas to improve: 
    // - take into account the destroyable squares, 
    // - how deep we currently are, 
    // - the amount of possible outcomes overall(ex: a path has 1 win and 1 loss, another has 2 wins and 2 losses, which is better ?)
    // - are ties possible ? If so, do we give them a value other than 0 ?
    // - which stage of the game we are (destroyed squares > 20, etc.)
    // - check if destination can leave us boxed in without losing
    private int evaluateBoard() {
        int opposite = myPiece == RED ? BLACK : RED;
        int myMoves = currentGame.getMovements(myPiece).size();
        int opponentMoves = currentGame.getMovements(opposite).size();

        if (myMoves == 0) return LOSE_VALUE;
        if (opponentMoves == 0) return WIN_VALUE;

        int score = myMoves * 10 - opponentMoves * 10;

        if (myMoves <= 3) score -= 100;
        if (opponentMoves <= 3) score += 100;

        return score;
    }
}
