package com.fs.backend.domain;

import com.fs.backend.domain.pieces.Bishop;
import com.fs.backend.domain.pieces.King;
import com.fs.backend.domain.pieces.Knight;
import com.fs.backend.domain.pieces.Pawn;
import com.fs.backend.domain.pieces.PieceFactory;
import com.fs.backend.domain.pieces.Queen;
import com.fs.backend.domain.pieces.Rook;
import com.fs.backend.domain.pieces.common.Pair;
import com.fs.backend.domain.pieces.common.Piece;
import com.fs.backend.domain.pieces.common.PieceColor;
import com.fs.backend.exceptions.GameException;
import com.fs.backend.exceptions.GameInconsistencyException;
import com.fs.backend.exceptions.IllegalMovementException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    private String id;
    private LocalDateTime createdAt;
    private MatchStatus status;
    private boolean isWhiteTurn;
    private Player whitePlayer;
    private Player blackPlayer;
    private Player winner;
    private List<Piece> whitePieces;
    private List<Piece> blackPieces;
    @Setter(AccessLevel.NONE)
    private final int BOARD_LENGHT = 8;

    public Match(String whitePlayerName, String blackPlayerName) {
        this.whitePlayer = new Player(whitePlayerName);
        this.blackPlayer = new Player(blackPlayerName);
        this.isWhiteTurn = true;
        status = MatchStatus.IN_PROGRESS;
        createdAt = LocalDateTime.now();
        createPieces();
    }

    public Match(Player hostPlayerName) {
        if (new Random().nextBoolean()) {
            this.whitePlayer = hostPlayerName;
            this.blackPlayer = null;
        } else {
            this.whitePlayer = null;
            this.blackPlayer = hostPlayerName;
        }
        this.isWhiteTurn = true;
        status = MatchStatus.NEW;
        createdAt = LocalDateTime.now();
        createPieces();
    }

    public void setWhitePlayer(Player whitePlayer) {
        this.whitePlayer = whitePlayer;
        if (this.blackPlayer != null && whitePlayer != null) {
            this.status = MatchStatus.IN_PROGRESS;
        }
    }

    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
        if (this.whitePlayer != null && blackPlayer != null) {
            this.status = MatchStatus.IN_PROGRESS;
        }
    }

    private void createPieces() {
        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();

        whitePieces.add(PieceFactory.create('R', PieceColor.WHITE, new Pair(1, 1)));
        whitePieces.add(PieceFactory.create('N', PieceColor.WHITE, new Pair(2, 1)));
        whitePieces.add(PieceFactory.create('B', PieceColor.WHITE, new Pair(3, 1)));
        whitePieces.add(PieceFactory.create('Q', PieceColor.WHITE, new Pair(4, 1)));
        whitePieces.add(PieceFactory.create('K', PieceColor.WHITE, new Pair(5, 1)));
        whitePieces.add(PieceFactory.create('B', PieceColor.WHITE, new Pair(6, 1)));
        whitePieces.add(PieceFactory.create('N', PieceColor.WHITE, new Pair(7, 1)));
        whitePieces.add(PieceFactory.create('R', PieceColor.WHITE, new Pair(8, 1)));

        blackPieces.add(PieceFactory.create('R', PieceColor.BLACK, new Pair(1, 8)));
        blackPieces.add(PieceFactory.create('N', PieceColor.BLACK, new Pair(2, 8)));
        blackPieces.add(PieceFactory.create('B', PieceColor.BLACK, new Pair(3, 8)));
        blackPieces.add(PieceFactory.create('Q', PieceColor.BLACK, new Pair(4, 8)));
        blackPieces.add(PieceFactory.create('K', PieceColor.BLACK, new Pair(5, 8)));
        blackPieces.add(PieceFactory.create('B', PieceColor.BLACK, new Pair(6, 8)));
        blackPieces.add(PieceFactory.create('N', PieceColor.BLACK, new Pair(7, 8)));
        blackPieces.add(PieceFactory.create('R', PieceColor.BLACK, new Pair(8, 8)));

        for (int col = 1; col <= 8; col++) {
            whitePieces.add(PieceFactory.create('P', PieceColor.WHITE, new Pair(col, 2)));
            blackPieces.add(PieceFactory.create('P', PieceColor.BLACK, new Pair(col, 7)));
        }
    }

    public void move(Piece pieceToMove, Pair target) throws IllegalMovementException {

        if (this.status.equals(MatchStatus.FINISHED) || this.status.equals(MatchStatus.TIED)) {
            throw new GameException("Cannot make a move in a finished game");
        }

        if (this.status.equals(MatchStatus.NEW)) {
            throw new GameException("A move cannot be made until another player is connected");
        }

        if (isWhiteTurn && pieceToMove.getColor() == PieceColor.BLACK) {
            throw new IllegalMovementException("You are trying to move a black piece but it's white's turn");
        }

        if (!isWhiteTurn && pieceToMove.getColor() == PieceColor.WHITE) {
            throw new IllegalMovementException("You are trying to move a white piece but it's black's turn");
        }


        List<Piece> allies = pieceToMove.getColor() == PieceColor.BLACK ?
                blackPieces.stream().filter(p -> !p.equals(pieceToMove)).toList()
                : whitePieces.stream().filter(p -> !p.equals(pieceToMove)).toList();
        List<Piece> enemies = pieceToMove.getColor() == PieceColor.BLACK ? whitePieces : blackPieces;

        pieceToMove.move(target, BOARD_LENGHT, allies, enemies);
        this.isWhiteTurn = !this.isWhiteTurn;
        verifyCheckmate();
        verifyDraw();
    }

    public void verifyCheckmate() {
        King whiteKing = (King) this.whitePieces.stream().filter(p -> p instanceof King).findFirst()
                .orElseThrow(() -> new GameInconsistencyException("White King missing"));
        King blackKing = (King) this.blackPieces.stream().filter(p -> p instanceof King).findFirst()
                .orElseThrow(() -> new GameInconsistencyException("Black King missing"));

        if (whiteKing.isCheckmate(BOARD_LENGHT,
                this.whitePieces.stream().filter(p -> !p.equals(whiteKing)).toList(),
                this.blackPieces
        )) {
            this.winner = this.blackPlayer;
            this.status = MatchStatus.FINISHED;
        }

        if (blackKing.isCheckmate(BOARD_LENGHT,
                this.blackPieces.stream().filter(p -> !p.equals(blackKing)).toList(),
                this.whitePieces)) {
            this.winner = this.whitePlayer;
            this.status = MatchStatus.FINISHED;
        }
    }

    public void verifyDraw() {
        King whiteKing = (King) this.whitePieces.stream().filter(p -> p instanceof King).findFirst()
                .orElseThrow(() -> new GameInconsistencyException("White King missing"));
        King blackKing = (King) this.blackPieces.stream().filter(p -> p instanceof King).findFirst()
                .orElseThrow(() -> new GameInconsistencyException("Black King missing"));

        if (whiteKing.isStaleMate(BOARD_LENGHT,
                this.whitePieces.stream().filter(p -> !p.equals(whiteKing)).toList(),
                this.blackPieces)
                || blackKing.isStaleMate(BOARD_LENGHT,
                this.blackPieces.stream().filter(p -> !p.equals(blackKing)).toList(),
                this.whitePieces)
                || isDeadPosition()
        ) {
            this.status = MatchStatus.TIED;
        }
    }

    public boolean isDeadPosition() {
        // king against king
        if (this.blackPieces.size() == 1 && this.whitePieces.size() == 1) {
            return this.blackPieces.stream().allMatch(p -> p instanceof King)
                    && this.whitePieces.stream().allMatch(p -> p instanceof King);

            // king against king and bishop or king against king and horse;
        } else if (this.blackPieces.size() == 2
                && this.blackPieces.stream().anyMatch(p -> p instanceof King)
                && this.blackPieces.stream().anyMatch(p -> p instanceof Bishop || p instanceof Knight)
                && this.whitePieces.stream().allMatch(p -> p instanceof King)) {
            return true;
        } else if (this.whitePieces.size() == 2
                && this.whitePieces.stream().anyMatch(p -> p instanceof King)
                && this.whitePieces.stream().anyMatch(p -> p instanceof Bishop || p instanceof Knight)
                && this.blackPieces.stream().allMatch(p -> p instanceof King)) {
            return true;

            // king and bishop against king and bishop, with the two bishops in squares of the same color
        } else if (this.blackPieces.size() == 2 && this.whitePieces.size() == 2
                && this.whitePieces.stream().anyMatch(p -> p instanceof King)
                && this.whitePieces.stream().anyMatch(p -> p instanceof Bishop)
                && this.blackPieces.stream().allMatch(p -> p instanceof King)
                && this.blackPieces.stream().anyMatch(p -> p instanceof Bishop)) {
            Bishop whiteBishop = (Bishop)
                    this.whitePieces.stream().filter(p -> p instanceof Bishop).findFirst().orElseThrow();
            Bishop blackBishop = (Bishop)
                    this.blackPieces.stream().filter(p -> p instanceof Bishop).findFirst().orElseThrow();

            if((whiteBishop.getPosition().getX() + whiteBishop.getPosition().getY()) % 2
                    == (blackBishop.getPosition().getX() + blackBishop.getPosition().getY()) % 2) {
                return true;
            }
        }

            return false;
    }

    public void promoteAPawn(Pawn promotedPawn, Piece newPiece) throws IllegalMovementException {
        if(promotedPawn.isPromoted(BOARD_LENGHT) && (newPiece instanceof Queen || newPiece instanceof Rook
                    || newPiece instanceof Bishop || newPiece instanceof Knight)) {
                promotedPawn.setAlive(false);
                newPiece.setColor(promotedPawn.getColor());
                newPiece.setPosition(promotedPawn.getPosition());
        } else {
            throw new IllegalMovementException("You cannot promote that pawn");
        }
    }
}
