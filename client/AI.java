import java.util.ArrayList;

public class AI {
    private final int myPiece;
    private int numExploredNodes = 0;

    public AI(int myPiece) {
        this.myPiece = myPiece;
    }
    private int scoreAlphaBeta(int[][] board, int player, int alpha, int beta, int depth) {
        int opposite = player == 5 ? 3 : 5;
        numExploredNodes++;

        if (Game.countMoves(board, player) == 0) return player == myPiece ? -100000 : 100000;
        if (depth == 0) return evaluateBoard(board);

        ArrayList<String> moves = Game.getMoves(board, player);
        if (moves.isEmpty()) return player == myPiece ? -100000 : 100000;

        if (player == myPiece) {
            int value = Integer.MIN_VALUE;

            for (String move : moves) {
                Game.applyMove(board, move, player);
                int score = scoreAlphaBeta(board, opposite, alpha, beta, depth - 1);
                Game.undoMove(board, move, player);

                value = Math.max(value, score);
                alpha = Math.max(alpha, value);

                if (alpha >= beta) break;
            }

            return value;
        } else {
            int value = Integer.MAX_VALUE;

            for (String move : moves) {
                Game.applyMove(board, move, player);
                int score = scoreAlphaBeta(board, opposite, alpha, beta, depth - 1);
                Game.undoMove(board, move, player);

                value = Math.min(value, score);
                beta = Math.min(beta, value);

                if (alpha >= beta) break;
            }

            return value;
        }
    }
    public ArrayList<String> getNextMoveAB(int[][] board) {
        numExploredNodes = 0;

        ArrayList<String> bestMoves = new ArrayList<>();
        ArrayList<String> possibleMoves = Game.getMoves(board, myPiece);

        if (possibleMoves.isEmpty()) return bestMoves;

        int opposite = myPiece == 5 ? 3 : 5;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int bestScore = Integer.MIN_VALUE;

        for (String move : possibleMoves) {
            Game.applyMove(board, move, myPiece);

            int value = scoreAlphaBeta(board, opposite, alpha, beta, 2);

            Game.undoMove(board, move, myPiece);

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

    //donne un poids au coups qui avantage notre joueur, mais seulement par le nombre de coup totaux qu'on a vs l'adversaire
    //donne un bonus au poids si l'adverse a moins ou egale a 3 coups possible
    //faudrait ameliorer ca, car avoir 3 moves veut pas dire qu'on est proche de gagner
    private int evaluateBoard(int[][] board) {
        int opposite = myPiece == 5 ? 3 : 5;
        int myMoves = Game.countMoves(board, myPiece);
        int opponentMoves = Game.countMoves(board, opposite);

        if (myMoves == 0) return -100000;
        if (opponentMoves == 0) return 100000;

        int score = myMoves * 10 - opponentMoves * 10;

        if (myMoves <= 3) score -= 100;
        if (opponentMoves <= 3) score += 100;

        return score;
    }
}
