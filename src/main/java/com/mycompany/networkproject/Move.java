package com.mycompany.networkproject;

public class Move {
    public final int sx, sy, dx, dy;
    public final PieceType promotion;

    public Move(int sx, int sy, int dx, int dy) {
        this(sx, sy, dx, dy, null);
    }

    public Move(int sx, int sy, int dx, int dy, PieceType promotion) {
        this.sx = sx;
        this.sy = sy;
        this.dx = dx;
        this.dy = dy;
        this.promotion = promotion;
    }

    public static boolean insideBoard(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public boolean isInsideBoard() {
        return insideBoard(sx, sy) && insideBoard(dx, dy);
    }

    @Override
    public String toString() {
        return sx + "," + sy + "," + dx + "," + dy + (promotion == null ? "" : "," + promotion.name());
    }

    public static Move fromProtocol(String[] parts) {
        if (parts.length < 5 || !"MOVE".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid MOVE message format");
        }
        int sx = Integer.parseInt(parts[1]);
        int sy = Integer.parseInt(parts[2]);
        int dx = Integer.parseInt(parts[3]);
        int dy = Integer.parseInt(parts[4]);
        PieceType promotion = null;
        if (parts.length >= 6 && !parts[5].isBlank()) {
            promotion = PieceType.valueOf(parts[5]);
            if (promotion == PieceType.KING || promotion == PieceType.PAWN) {
                throw new IllegalArgumentException("Invalid promotion piece");
            }
        }
        // Do not reject out-of-board coordinates here.
        // Let GameState.playMove(...) validate them and return
        // MoveResult.fail("Move is outside board bounds") so the client receives
        // the same ERROR|... protocol used for other rejected moves.
        return new Move(sx, sy, dx, dy, promotion);
    }
}
