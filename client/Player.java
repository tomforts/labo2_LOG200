import java.util.ArrayList;

public class Player {
    private static final int RED = 5;
    private static final int BLACK = 3;

    private final int myPiece;
    private int timeLimit;
    private long searchDeadline;
    private boolean stopSearch;
    private int numExploredNodes = 0;
    private static final int MAX_MOVES = 576;
    // chaque ligne correspond a un niveau de l'arbre
    // chaque colonne contient un coup possible
    // on reutilise le  tableau pour eviter de recreer des listes
    private final int[][] moves = new int[22][MAX_MOVES];
    // @TODO: change these values to something more accurate
    private final int LOSE_VALUE = -100000;
    private final int WIN_VALUE = 100000;
    private final int MOBILITY_WEIGHT = 100;
    private final int TERRITORY_WEIGHT = 100;
    private final int TERRITORY_FUTURE_WEIGHT = 5;
    private final int DISTANCE_WEIGHT = 2;
    private final int POSITION_WEIGHT = 20;

    private Partie currentGame;

    public Player(int myPiece, Partie game, int timeLimit) {
        this.myPiece = myPiece;
        this.currentGame = game;
        this.timeLimit = timeLimit;
    }

    /**
     * Gets the next best moves using alpha-beta
     * @return arraylist of move strings
     */
    public ArrayList<String> getNextMovesAB() {
        return getNextMovesAB(timeLimit);
    }

    /**
     * Gets the next best moves by calculating the most it can in the time it has available
     * @param timeLimitMs the time limit to play a move
     * @return arraylist of move strings
     */
    public ArrayList<String> getNextMovesAB(long timeLimitMs) {
        numExploredNodes = 0;
        stopSearch = false;

        searchDeadline = System.currentTimeMillis() + timeLimitMs;

        ArrayList<String> bestMoves = new ArrayList<>();

        // génere les coups possible à la racine
        int count = currentGame.generateMoves(myPiece, moves[0]);

        if (count == 0) return bestMoves;

        // premier coup légal si manque de temps
        bestMoves.add(currentGame.encodedToString(moves[0][0]));

        for (int depth = 1; depth <= 20; depth++) {
            if (System.currentTimeMillis() >= searchDeadline) {
                break;
            }

            ArrayList<String> iterationBestMoves = searchRoot(depth);

            if (stopSearch || iterationBestMoves.isEmpty()) {
                break;
            }

            bestMoves = iterationBestMoves;
        }
        return bestMoves;
    }

    /**
     *
     * @param depth
     * @return
     */
    private ArrayList<String> searchRoot(int depth) {
        ArrayList<String> bestMoves = new ArrayList<>();

        int opposite = myPiece == RED ? BLACK : RED;
        int count = currentGame.generateMoves(myPiece, moves[0]);

        double bestScore = Integer.MIN_VALUE;
        double alpha = Integer.MIN_VALUE;
        double beta = Integer.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            if (stopSearch) break;

            int move = moves[0][i];
            currentGame.applyEncoded(move, myPiece);

            double value;

            try {
                value = scoreAlphaBeta(opposite, alpha, beta, depth - 1, 1);
            } finally {
                currentGame.undoEncoded(move, myPiece);
            }

            if (stopSearch) break;

            if (value > bestScore) {
                bestScore = value;
                bestMoves.clear();
                bestMoves.add(currentGame.encodedToString(move));
            } else if (value == bestScore) {
                bestMoves.add(currentGame.encodedToString(move));
            }

            alpha = Math.max(alpha, bestScore);
        }

        return bestMoves;
    }


    private double scoreAlphaBeta(int player, double alpha, double beta, int depth, int level) {
        numExploredNodes++;

        if (System.currentTimeMillis() >= searchDeadline) {
            stopSearch = true;
            return 0;
        }

        int opposite = player == RED ? BLACK : RED;
        double defaultGameValue = player == myPiece ? LOSE_VALUE : WIN_VALUE;

        if (currentGame.countMovements(player) == 0) return defaultGameValue;

        if (depth == 0) return evaluateBoard();

        boolean maximizing = player == myPiece;
        double value = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        // chaque level possède son propre tableau de coups.
        int count = currentGame.generateMoves(player, moves[level]);
        if (count == 0) return defaultGameValue;

        for (int i = 0; i < count; i++) {
            if (stopSearch) break;

            int move = moves[level][i];

            currentGame.applyEncoded(move, player);

            double score;

            try {
                score = scoreAlphaBeta(opposite, alpha, beta, depth - 1, level + 1);
            } finally {
                currentGame.undoEncoded(move, player);
            }

            if (stopSearch) break;

            if (maximizing) {
                value = Math.max(value, score);
                alpha = Math.max(alpha, value);
            } else {
                value = Math.min(value, score);
                beta = Math.min(beta, value);
            }

            if (alpha >= beta) break;
        }

        return value;
    }




    // @TODO: improve this to better represent how close to winning a board is.
    // Ideas to improve:
    // - take into account the destroyable squares,
    // - how deep we currently are,
    // - the amount of possible outcomes overall(ex: a path has 1 win and 1 loss, another has 2 wins and 2 losses, which is better ?)
    // - which stage of the game we are (destroyed squares > 20, etc.)
    // -x check if destination can leave us boxed in without losing
    private double evaluateBoard() {
        int opposite = myPiece == RED ? BLACK : RED;
        int myMoveCount = currentGame.countMovements(myPiece);
        int opponentMoveCount = currentGame.countMovements(opposite);

        if (myMoveCount == 0) return LOSE_VALUE;
        if (opponentMoveCount == 0) return WIN_VALUE;

        // Mobility (HIGH)
        double myMobility = Math.log1p(1 + myMoveCount);
        double opponentMobility = Math.log1p(1 + opponentMoveCount);
        double mobility = MOBILITY_WEIGHT * (myMobility - opponentMobility);

        // Territory (VERY HIGH)
        long myTerritory = currentGame.getTerritory(myPiece);
        long opponentTerritory = currentGame.getTerritory(opposite);
        int myImmediate = (int) (myTerritory >>> 32);
        int myFuture = (int) myTerritory;
        int opponentImmediate = (int) (opponentTerritory >>> 32);
        int opponentFuture = (int) opponentTerritory;
        // int territory = TERRITORY_WEIGHT * (myTerritory - opponentTerritory);
        double territory = TERRITORY_WEIGHT * (Math.log1p(1 + myImmediate) - Math.log1p(1 + opponentImmediate)) + TERRITORY_FUTURE_WEIGHT * (myFuture - opponentFuture);


        // Distance (VERY LOW)
        /*
        int myPos = currentGame.getPosition(myPiece);
        int myPosX = currentGame.getX(myPos);
        int myPosY = currentGame.getY(myPos);
        int opponentPos = currentGame.getPosition(opposite);
        int opponentPosX = currentGame.getX(opponentPos);
        int opponentPosY = currentGame.getY(opponentPos);
        // distance between players ? optimal distance ?
        // does not represent that much, needs to be improved
        int distance = DISTANCE_WEIGHT * Math.max(Math.abs(myPosX - opponentPosX), Math.abs(myPosY - opponentPosY));
        */

        // Flexibility (LOW)
        // could divide the board into quadrants based on the directions
        // the amount and size of the quadrants could be their value
        int myPositionFlex = currentGame.getDirectionCount(myPiece);
        int opponentPositionFlex = currentGame.getDirectionCount(opposite);
        int positionFlex = POSITION_WEIGHT * (myPositionFlex - opponentPositionFlex);

        // should take into account depth
        return mobility + territory + positionFlex;
    }

    public int getExploredNodes() {
        return numExploredNodes;
    }
}
