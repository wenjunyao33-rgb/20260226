package structures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import structures.basic.Unit;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {

	
	public boolean gameInitalised = false;
	
	public boolean something = false;

	// SC-25/SC-26: All units currently in play
	public List<Unit> units = new ArrayList<>();

	// SC-25: Board tile-to-unit mapping (x: 0-8, y: 0-4)
	public Unit[][] unitOnBoard = new Unit[9][5];

	/**
	 * SC-25: Remove a unit from the game and clear its tile reference.
	 * @param unit the unit to delete
	 */
	public void deleteUnit(Unit unit) {
		units.remove(unit);
		if (unit.getPosition() != null) {
			int x = unit.getPosition().getTilex();
			int y = unit.getPosition().getTiley();
			if (x >= 0 && x <= 8 && y >= 0 && y <= 4) {
				unitOnBoard[x][y] = null;
			}
		}
	}

	/**
	 * SC-25: Sweep all units with health == 0 and delete them.
	 */
	public void sweepDeadUnits() {
		Iterator<Unit> it = units.iterator();
		while (it.hasNext()) {
			Unit u = it.next();
			if (u.getHealth() == 0) {
				if (u.getPosition() != null) {
					int x = u.getPosition().getTilex();
					int y = u.getPosition().getTiley();
					if (x >= 0 && x <= 8 && y >= 0 && y <= 4) {
						unitOnBoard[x][y] = null;
					}
				}
				it.remove();
			}
		}
	}

}
