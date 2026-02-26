package structures;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Computes valid movement tiles for a unit (SC-11).
 * Movement range is 2 tiles in any orthogonal or diagonal direction.
 * A path to the destination must not be blocked by another unit.
 */
public class MovementEngine {

	/** Maximum movement radius (Chebyshev / king-move distance). */
	private static final int MOVE_RADIUS = 2;

	/**
	 * Returns all tiles within movement range of the given unit that the unit
	 * can legally reach (path must be clear).
	 *
	 * @param unit   the unit that wants to move
	 * @param board  board[tilex][tiley] (1-based indices, size [10][6])
	 * @param units  all units currently on the board
	 * @return list of reachable destination tiles
	 */
	public static List<Tile> getValidMoveTiles(Unit unit, Tile[][] board, List<Unit> units) {
		List<Tile> result = new ArrayList<>();
		int ux = unit.getPosition().getTilex();
		int uy = unit.getPosition().getTiley();

		for (int dx = -MOVE_RADIUS; dx <= MOVE_RADIUS; dx++) {
			for (int dy = -MOVE_RADIUS; dy <= MOVE_RADIUS; dy++) {
				if (dx == 0 && dy == 0) continue;
				// Chebyshev distance check
				if (Math.max(Math.abs(dx), Math.abs(dy)) > MOVE_RADIUS) continue;

				int tx = ux + dx;
				int ty = uy + dy;

				// Board boundary check (1-based: x in [1,9], y in [1,5])
				if (tx < 1 || tx > 9 || ty < 1 || ty > 5) continue;

				Tile target = board[tx][ty];
				if (target == null) continue;

				// Destination must be unoccupied
				if (isOccupied(tx, ty, unit, units)) continue;

				// Path to destination must be clear
				if (isPathClear(unit, target, units)) {
					result.add(target);
				}
			}
		}
		return result;
	}

	/**
	 * Checks whether the straight-line path from the unit to the target tile
	 * is free of other units.  For a two-step move the intermediate tile is
	 * also checked.
	 *
	 * @param unit   the moving unit
	 * @param target the destination tile
	 * @param units  all units on the board (including the moving unit)
	 * @return true if no unit blocks the path
	 */
	public static boolean isPathClear(Unit unit, Tile target, List<Unit> units) {
		int ux = unit.getPosition().getTilex();
		int uy = unit.getPosition().getTiley();
		int tx = target.getTilex();
		int ty = target.getTiley();

		int dx = Integer.signum(tx - ux);
		int dy = Integer.signum(ty - uy);

		int steps = Math.max(Math.abs(tx - ux), Math.abs(ty - uy));
		int cx = ux + dx;
		int cy = uy + dy;
		for (int i = 0; i < steps - 1; i++) {
			if (isOccupied(cx, cy, unit, units)) return false;
			cx += dx;
			cy += dy;
		}
		return true;
	}

	/** Returns true if any unit other than {@code mover} occupies (tx, ty). */
	private static boolean isOccupied(int tx, int ty, Unit mover, List<Unit> units) {
		for (Unit u : units) {
			if (u == mover) continue;
			if (u.getPosition().getTilex() == tx && u.getPosition().getTiley() == ty) {
				return true;
			}
		}
		return false;
	}
}
