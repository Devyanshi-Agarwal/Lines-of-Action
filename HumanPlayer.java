/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

/** A Player that prompts for moves and reads them from its Game.
 *  @author Devyanshi Agarwal
 */
class HumanPlayer extends Player {

    /** A new HumanPlayer with no piece or controller (intended to produce
     *  a template). */
    HumanPlayer() {
        this(null, null);
    }

    /** A HumanPlayer that plays the SIDE pieces in GAME.  It uses
     *  GAME.getMove() as a source of moves.  */
    HumanPlayer(Piece side, Game game) {
        super(side, game);
        _side = side;
        _game = game;
    }

    @Override
    String getMove() {
        return _game.readLine(false);
    }

    @Override
    Player create(Piece piece, Game game) {
        return new loa.HumanPlayer(piece, game);
    }

    @Override
    boolean isManual() {
        return true;
    }

    /** Piece indicating the side human is playing. */
    private Piece _side;

    /** Game indicating the current game. */
    private Game _game;

}
