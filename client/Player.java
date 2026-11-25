import java.util.ArrayList;

public class Player {
    private static final int RED = 5;
    private static final int BLACK = 3;

    private final int myPiece;
    private int timeLimit;
    private long searchDeadline;
    private boolean stopSearch;
    private int numExploredNodes = 0;

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

    // Improvements:
    // 1. evaluate board and explore in order of score
    // 2. score moves with before and after of score of board
    public ArrayList<String> getNextMovesAB2() {
        numExploredNodes = 0;

        ArrayList<String> bestMoves = new ArrayList<>();
        ArrayList<Integer> possibleMovements = currentGame.getMovements(myPiece);

        if (possibleMovements.isEmpty()) return bestMoves;

        int opposite = myPiece == RED ? BLACK : RED;
        double alpha = Integer.MIN_VALUE;
        double beta = Integer.MAX_VALUE;
        double bestScore = Integer.MIN_VALUE;

        int currentPosition = currentGame.getPosition(myPiece);
        for (int destination : possibleMovements) {
            currentGame.applyMovement(currentPosition, destination, myPiece);
            // we could evaluate the board post-move and check the destructions only of the best post-move boards ?
            ArrayList<Integer> destroyables = currentGame.getDestroyables(myPiece);
            for (Integer destroySquare : destroyables) {
                currentGame.toggleDestroyed(destroySquare);

                double value = scoreAlphaBeta(opposite, alpha, beta, 2);

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

    private double scoreAlphaBeta2(int player, double alpha, double beta, int depth) {
        int opposite = player == RED ? BLACK : RED;
        numExploredNodes++;
        int defaultGameValue = player == myPiece ? LOSE_VALUE : WIN_VALUE;

        ArrayList<Integer> possibleMovements = currentGame.getMovements(myPiece);
        if (possibleMovements.isEmpty()) return defaultGameValue;
        if (depth == 0) return evaluateBoard();

        int currentPosition = currentGame.getPosition(player);
        if (player == myPiece) {
            double value = Integer.MIN_VALUE;

            for (int destination : possibleMovements) {
                currentGame.applyMovement(currentPosition, destination, player);
                // we could evaluate the board post-move and check the destructions only of the best post-move boards ?
                ArrayList<Integer> destroyables = currentGame.getDestroyables(player);
                for (Integer destroySquare : destroyables) {
                    currentGame.toggleDestroyed(destroySquare);

                    double score = scoreAlphaBeta(opposite, alpha, beta, depth - 1);

                    currentGame.toggleDestroyed(destroySquare);

                    value = Math.max(value, score);
                    alpha = Math.max(alpha, value);

                    if (alpha >= beta) break;
                }
                currentGame.undoMovement(currentPosition, destination, player);
            }

            return value;
        } else {
            double value = Integer.MAX_VALUE;

            for (int destination : possibleMovements) {
                currentGame.applyMovement(currentPosition, destination, player);
                // we could evaluate the board post-move and check the destructions only of the best post-move boards ?
                ArrayList<Integer> destroyables = currentGame.getDestroyables(player);
                for (Integer destroySquare : destroyables) {
                    currentGame.toggleDestroyed(destroySquare);

                    double score = scoreAlphaBeta(opposite, alpha, beta, depth - 1);

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

        int currentPosition = currentGame.getPosition(myPiece);
        ArrayList<Integer> possibleMovements = currentGame.getMovements(myPiece);

        if (possibleMovements.isEmpty()) {
            return bestMoves;
        }

        // Keep a legal fallback in case the time limit is extremely short.
        for (int destination : possibleMovements) {
            currentGame.applyMovement(currentPosition, destination, myPiece);

            try {
                ArrayList<Integer> destroyables = currentGame.getDestroyables(myPiece);

                if (!destroyables.isEmpty()) {
                    bestMoves.add(currentGame.moveToString(
                            currentPosition, destination, destroyables.get(0)
                    ));
                    break;
                }
            } finally {
                // use finally since it allows to be executed even when breaking
                // This means if we stop several level deep, the board is not stuck with a move half-done
                currentGame.undoMovement(currentPosition, destination, myPiece);
            }
        }

        // no destroyable squares possible
        if (bestMoves.isEmpty()) {
            return bestMoves;
        }

        // Iterative deepening.
        for (int depth = 1; depth <= 20; depth++) {
            if (System.currentTimeMillis() >= searchDeadline) {
                break;
            }

            ArrayList<String> iterationBestMoves = searchRoot(depth);

            // Do not keep a partially completed search.
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
        int currentPosition = currentGame.getPosition(myPiece);

        ArrayList<Integer> possibleMovements = currentGame.getMovements(myPiece);

        double bestScore = Integer.MIN_VALUE;

        for (int destination : possibleMovements) {
            if (stopSearch) break;

            currentGame.applyMovement(currentPosition, destination, myPiece);

            try {
                ArrayList<Integer> destroyables = currentGame.getDestroyables(myPiece);

                for (int destroySquare : destroyables) {
                    if (stopSearch) break;

                    currentGame.toggleDestroyed(destroySquare);

                    double value;

                    try {
                        // The root move has already consumed one depth.
                        // Search starts with the opponent's turn.
                        value = scoreAlphaBeta(
                                opposite,
                                Integer.MIN_VALUE,
                                Integer.MAX_VALUE,
                                depth - 1
                        );
                    } finally {
                        currentGame.toggleDestroyed(destroySquare);
                    }

                    if (stopSearch) break;

                    if (value > bestScore) {
                        bestScore = value;
                        bestMoves.clear();
                        bestMoves.add(currentGame.moveToString(
                                currentPosition, destination, destroySquare
                        ));
                    } else if (value == bestScore) {
                        bestMoves.add(currentGame.moveToString(
                                currentPosition, destination, destroySquare
                        ));
                    }
                }
            } finally {
                currentGame.undoMovement(currentPosition, destination, myPiece);
            }
        }

        return bestMoves;
    }

    private double scoreAlphaBeta(
            int player,
            double alpha,
            double beta,
            int depth
    ) {
        numExploredNodes++;

        // Avoid checking the clock at every node.
        if ((numExploredNodes & 1023) == 0 && System.currentTimeMillis() >= searchDeadline) {
            stopSearch = true;
            return evaluateBoard();
        }

        int opposite = player == RED ? BLACK : RED;

        // IMPORTANT: Generate movements for the actual player.
        ArrayList<Integer> possibleMovements = currentGame.getMovements(player);

        double defaultGameValue = player == myPiece ? LOSE_VALUE : WIN_VALUE;

        // Check for a terminal position before evaluating the depth limit.
        if (possibleMovements.isEmpty()) {
            return defaultGameValue;
        }

        if (depth == 0) {
            return evaluateBoard();
        }

        int currentPosition = currentGame.getPosition(player);

        boolean maximizing = player == myPiece;
        double value = maximizing
                ? Integer.MIN_VALUE
                : Integer.MAX_VALUE;

        boolean foundLegalTurn = false;

        for (int destination : possibleMovements) {
            if (stopSearch) break;

            currentGame.applyMovement(
                    currentPosition, destination, player
            );

            try {
                ArrayList<Integer> destroyables =
                        currentGame.getDestroyables(player);

                for (int destroySquare : destroyables) {
                    if (stopSearch) break;

                    foundLegalTurn = true;

                    currentGame.toggleDestroyed(destroySquare);

                    double score;

                    try {
                        score = scoreAlphaBeta(
                                opposite, alpha, beta, depth - 1
                        );
                    } finally {
                        currentGame.toggleDestroyed(destroySquare);
                    }

                    if (stopSearch) break;

                    if (maximizing) {
                        value = Math.max(value, score);
                        alpha = Math.max(alpha, value);
                    } else {
                        value = Math.min(value, score);
                        beta = Math.min(beta, value);
                    }

                    if (alpha >= beta) {
                        break;
                    }
                }
            } finally {
                currentGame.undoMovement(
                        currentPosition, destination, player
                );
            }

            if (alpha >= beta) {
                break;
            }
        }

        // A turn requires both a movement and a destruction.
        if (!foundLegalTurn) {
            return defaultGameValue;
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
        ArrayList<Integer> myMoves = currentGame.getMovements(myPiece);
        ArrayList<Integer> opponentMoves = currentGame.getMovements(opposite);
        int myMoveCount = myMoves.size();
        int opponentMoveCount = opponentMoves.size();

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
}
