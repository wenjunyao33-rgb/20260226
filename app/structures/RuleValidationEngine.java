package structures;

import structures.basic.Unit;

/**
 * Validates game rules for unit actions (SC-11, SC-13).
 * All methods are stateless and operate on the supplied unit data.
 */
public class RuleValidationEngine {

	/**
	 * SC-11: A unit may move only if it has not already moved this turn.
	 * @param unit the friendly unit to check
	 * @return true if the unit is allowed to move
	 */
	public static boolean canMove(Unit unit) {
		return !unit.isHasMoved();
	}

	/**
	 * SC-13: A unit may attack only if it has not already attacked this turn.
	 * @param attacker the attacking unit
	 * @return true if the unit is allowed to attack
	 */
	public static boolean canAttack(Unit attacker) {
		return !attacker.isHasAttacked();
	}

	/**
	 * SC-13: Two units are melee-adjacent when they are within one step of each
	 * other in both the x and y directions (i.e. Chebyshev distance == 1).
	 * @param a first unit
	 * @param b second unit
	 * @return true if the two units are adjacent on the board
	 */
	public static boolean isAdjacent(Unit a, Unit b) {
		int dx = Math.abs(a.getPosition().getTilex() - b.getPosition().getTilex());
		int dy = Math.abs(a.getPosition().getTiley() - b.getPosition().getTiley());
		return dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0);
	}
}
