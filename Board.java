/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author Devyanshi Agarwal
 */
class Board {

    /** Default number of moves for each side that results in a draw. */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /** Pattern describing a valid square designator (cr). */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /** A Board whose initial contents are taken from INITIALCONTENTS
     *  and in which the player playing TURN is to move. The resulting
     *  Board has
     *        get(col, row) == INITIALCONTENTS[row][col]
     *  Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     *
     *  CAUTION: The natural written notation for arrays initializers puts
     *  the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /** A new board in the standard initial position. */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /** A Board whose initial contents and state are copied from
     *  BOARD. */
    Board(loa.Board board) {
        this();
        copyFrom(board);
    }

    /** Set my state to CONTENTS with SIDE to move. */
    void initialize(Piece[][] contents, Piece side) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Square newSquare = sq(col, row);
                set(newSquare, contents[row][col], side);
            }
        }
        _turn = side;
        _moveLimit = DEFAULT_MOVE_LIMIT;
    }

    /** Set me to the initial configuration. */
    void clear() {
        initialize(INITIAL_PIECES, BP);
        _winner = null;
        _winnerKnown = false;
        _subsetsInitialized = false;
        _moveLimit = DEFAULT_MOVE_LIMIT;
        _moves.clear();
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        _turn = BP;
        _replaced = new ArrayList<>();
    }

    /** Set my state to a copy of BOARD. */
    void copyFrom(loa.Board board) {
        if (board == this) {
            return;
        }
        this._turn = board._turn;
        this._moveLimit = board._moveLimit;
        this._moves.addAll(board._moves);
        this._winnerKnown = board._winnerKnown;
        this._blackRegionSizes.addAll(board._blackRegionSizes);
        this._whiteRegionSizes.addAll(board._whiteRegionSizes);
        this._winner = board._winner;
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Square newSquare = sq(col, row);
                set(newSquare, board.get(newSquare));
            }
        }
    }

    /** Return the contents of the square at SQ. */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /** Set the square at SQ to V and set the side that is to move next
     *  to NEXT, if NEXT is not null. */
    void set(Square sq, Piece v, Piece next) {
        _board[sq.index()] = v;
        if (next != null) {
            _turn = next;
        }
    }

    /** Set the square at SQ to V, without modifying the side that
     *  moves next. */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /** Set limit on number of moves by each side that results in a tie to
     *  LIMIT, where 2 * LIMIT > movesMade(). */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }
        _moveLimit = 2 * limit;
    }

    /** Assuming isLegal(MOVE), make MOVE. This function assumes that
     *  MOVE.isCapture() will return false.  If it saves the move for
     *  later retraction, makeMove itself uses MOVE.captureMove() to produce
     *  the capturing move. */
    void makeMove(Move move) {
        assert isLegal(move);
        _moves.add(move);
        Square from = move.getFrom();
        Square to = move.getTo();
        _replaced.add(get(to));
        set(to, get(from));
        set(from, EMP, _turn.opposite());
        winner();
        _winnerKnown = false;
        _subsetsInitialized = false;
    }

    /** Retract (unmake) one move, returning to the state immediately before
     *  that move.  Requires that movesMade () > 0. */
    void retract() {
        if (_moves.size() == 0) {
            return;
        }
        assert movesMade() > 0;
        Move lastMove = _moves.get(_moves.size() - 1);
        _moves.remove(_moves.size() - 1);
        Square from = lastMove.getFrom();
        Square to = lastMove.getTo();
        set(from, get(to));
        set(to, _replaced.get(_replaced.size() - 1), _turn.opposite());
        _replaced.remove(_replaced.size() - 1);
        _subsetsInitialized = true;
    }

    /** Return the Piece representing who is next to move. */
    Piece turn() {
        return _turn;
    }

    /** Return true iff FROM - TO is a legal move for the player currently on
     *  move. */
    boolean isLegal(Square from, Square to) {
        assert (from.isValidMove(to));
        if (get(from) != _turn) {
            return false;
        }
        int distance = from.distance(to);
        int dir = from.direction(to);
        int count = numPieces(dir, from);
        if (count != distance) {
            return false;
        }
        for (int steps = 1; steps < distance; steps++) {
            Square next = from.moveDest(dir, steps);
            if (next != null) {
                if (this.get(next) == _turn.opposite()) {
                    return false;
                }
            }
        } if (this.blocked(from, to) && this.get(to) == _turn) {
            return false;
        }
        return true;
    }

    /** Return true iff MOVE is legal for the player currently on move.
     *  The isCapture() property is ignored. */
    boolean isLegal(Move move) {
        if (move == null) {
            return false;
        }
        return isLegal(move.getFrom(), move.getTo());
    }

    /** Return a sequence of all legal moves from this position. */
    List<Move> legalMoves() {
        List<Move> moves = new ArrayList<Move>();
        for (Square from : ALL_SQUARES) {
            if (get(from) == this._turn) {
                for (int dir = 0; dir < 8; dir++) {
                    int steps = numPieces(dir, from);
                    Square to = from.moveDest(dir, steps);
                    Move mv = Move.mv(from, to);
                    if (this.isLegal(mv)) {
                        moves.add(mv);
                    }
                }
            }
        }
        return moves;
    }

    /** Return true iff the game is over (either player has all his
     *  pieces continguous or there is a tie). */
    boolean gameOver() {
        return winner() != null;
    }

    /** Return true iff SIDE's pieces are continguous. */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /** Return the winning side, if any.  If the game is not over, result is
     *  null.  If the game has ended in a tie, returns EMP. */
    Piece winner() {
        if (!_winnerKnown) {
            _winner = null;
            if (piecesContiguous(BP) && piecesContiguous(WP)) {
                _winner = _turn.opposite();
            } else if (piecesContiguous(BP)) {
                _winner = BP;
            } else if (piecesContiguous(WP)) {
                _winner = WP;
            } else if (_moves.size() == _moveLimit) {
                _winner = EMP;
            }
            _winnerKnown = true;
        }
        return _winner;
    }

    /** Return the total number of moves that have been made (and not
     *  retracted).  Each valid call to makeMove with a normal move increases
     *  this number by 1. */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        loa.Board b = (loa.Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /** Return true if a move from FROM to TO is blocked by an opposing
     *  piece or by a friendly piece on the target square. */
    private boolean blocked(Square from, Square to) {
        if (get(to) == get(from)) {
            return true;
        }
        int dir = from.direction(to);
        int steps = numPieces(dir, from);
        for (int i = 1; i < steps; i++) {
            Square sq = from.moveDest(dir, 1);
            if (sq != null && get(sq) == get(from).opposite()) {
                return true;
            }
        }
        return false;
    }

    /** Return the size of the as-yet unvisited cluster of squares
     *  containing P at and adjacent to SQ.  VISITED indicates squares that
     *  have already been processed or are in different clusters.  Update
     *  VISITED to reflect squares counted. */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        if (p == EMP || p != get(sq)) {
            return 0;
        }
        if (visited[sq.col()][sq.row()]) {
            return 0;
        }
        visited[sq.col()][sq.row()] = true;
        int count = 1;
        Square[] adjacent = sq.adjacent();
        for (Square adjSq : adjacent) {
            count += numContig(adjSq, visited, p);
        }
        return count;
    }

    /** Set the values of _whiteRegionSizes and _blackRegionSizes. */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        boolean[][] visited = new boolean[8][8];
        for (Square sq: ALL_SQUARES) {
            if (get(sq) == BP) {
                int count = numContig(sq, visited, get(sq));
                if (count != 0) {
                    _blackRegionSizes.add(count);
                }
            } else if (get(sq) == WP) {
                int count = numContig(sq, visited, get(sq));
                if (count != 0) {
                    _whiteRegionSizes.add(count);
                }
            }
        }
        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /** Return the sizes of all the regions in the current union-find
     *  structure for side S. */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }

    /** Puts the opposite direction by taking in DIRECTION. **/
    void oppositeDir(int direction) {
        _opposite.put(0, 4);
        _opposite.put(1, 5);
        _opposite.put(2, 6);
        _opposite.put(3, 7);
        _opposite.put(4, 0);
        _opposite.put(5, 1);
        _opposite.put(6, 2);
        _opposite.put(7, 3);
    }

    /** Find the number of pieces in the line of action by
     * taking in DIR and FROM, returning count. **/
    int numPieces(int dir, Square from) {
        int count = 1;
        oppositeDir(dir);
        int opp = _opposite.get(dir);
        for (Square thisSq : ALL_SQUARES) {
            if (from.isValidMove(thisSq)) {
                int currDir = from.direction(thisSq);
                if (currDir == dir || currDir == opp) {
                    if (get(thisSq) != EMP) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** The standard initial configuration for Lines of Action (bottom row
     *  first). */
    static final Piece[][] INITIAL_PIECES = {
            { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }

    };

    /** Current contents of the board.  Square S is at _board[S.index()]. */
    private final Piece[] _board = new Piece[BOARD_SIZE  * BOARD_SIZE];

    /** List of all unretracted moves on this board, in order. */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /** Current side on move. */
    private Piece _turn;
    /** Limit on number of moves before tie is declared.  */
    private int _moveLimit;
    /** True iff the value of _winner is known to be valid. */
    private boolean _winnerKnown;
    /** Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     *  in progress).  Use only if _winnerKnown. */
    private Piece _winner = null;

    /** True iff subsets computation is up-to-date. */
    private boolean _subsetsInitialized;

    /** List of the sizes of continguous clusters of pieces, by color. */
    private final ArrayList<Integer>
        _whiteRegionSizes = new ArrayList<>(),
        _blackRegionSizes = new ArrayList<>();

    /** HashMap of opposite directions. **/
    private Map<Integer, Integer> _opposite = new HashMap<>();

    /** Replaces piece in the last move. */
    private ArrayList<Piece> _replaced = new ArrayList<>();
}
