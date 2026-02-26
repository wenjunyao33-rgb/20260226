package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.MovementEngine;
import structures.RuleValidationEngine;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;

import java.util.List;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * 
 * { 
 *   messageType = "tileClicked"
 *   tilex = <x index of the tile>
 *   tiley = <y index of the tile>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class TileClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

		Tile clickedTile = gameState.board[tilex][tiley];

		// Find unit on clicked tile (if any)
		Unit clickedUnit = getUnitAt(tilex, tiley, gameState);

		if (gameState.selectedUnit != null) {
			// --- A friendly unit is already selected ---

			// Check if the clicked tile is a highlighted (valid move) tile
			if (isHighlighted(clickedTile, gameState) && clickedUnit == null) {
				// SC-12: Move the selected unit to this tile
				moveUnit(out, gameState, gameState.selectedUnit, clickedTile);
				clearHighlights(out, gameState);
				gameState.selectedUnit = null;
				return;
			}

			// Check if clicked tile holds an enemy unit (attack)
			if (clickedUnit != null && clickedUnit != gameState.selectedUnit
					&& clickedUnit.getOwner() != gameState.currentPlayer) {
				Unit attacker = gameState.selectedUnit;
				Unit defender = clickedUnit;
				// SC-13: Validate adjacency and hasAttacked
				if (RuleValidationEngine.isAdjacent(attacker, defender)
						&& RuleValidationEngine.canAttack(attacker)) {
					performAttack(out, gameState, attacker, defender);
				}
				clearHighlights(out, gameState);
				gameState.selectedUnit = null;
				return;
			}

			// Otherwise deselect
			clearHighlights(out, gameState);
			gameState.selectedUnit = null;
		}

		// --- No unit selected: try to select a friendly unit ---
		if (clickedUnit != null && clickedUnit.getOwner() == gameState.currentPlayer
				&& RuleValidationEngine.canMove(clickedUnit)) {
			// SC-11: Highlight valid move tiles
			gameState.selectedUnit = clickedUnit;
			List<Tile> validTiles = MovementEngine.getValidMoveTiles(
					clickedUnit, gameState.board, gameState.units);
			gameState.highlightedTiles = validTiles;
			for (Tile t : validTiles) {
				BasicCommands.drawTile(out, t, 1); // mode 1 = highlighted
			}
		}
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	/** Returns the unit occupying (tilex, tiley), or null if the tile is empty. */
	private Unit getUnitAt(int tilex, int tiley, GameState gameState) {
		for (Unit u : gameState.units) {
			if (u.getPosition().getTilex() == tilex
					&& u.getPosition().getTiley() == tiley) {
				return u;
			}
		}
		return null;
	}

	/** Returns true if the given tile is in the highlighted list. */
	private boolean isHighlighted(Tile tile, GameState gameState) {
		if (tile == null) return false;
		for (Tile t : gameState.highlightedTiles) {
			if (t.getTilex() == tile.getTilex() && t.getTiley() == tile.getTiley()) {
				return true;
			}
		}
		return false;
	}

	/** Clears all tile highlights back to normal (mode 0). */
	private void clearHighlights(ActorRef out, GameState gameState) {
		for (Tile t : gameState.highlightedTiles) {
			BasicCommands.drawTile(out, t, 0);
		}
		gameState.highlightedTiles.clear();
	}

	/** SC-12: Move unit to target tile and update GameState. */
	private void moveUnit(ActorRef out, GameState gameState, Unit unit, Tile target) {
		BasicCommands.moveUnitToTile(out, unit, target);
		unit.setPositionByTile(target);
		unit.setHasMoved(true);
	}

	/**
	 * SC-13/14/15: Attacker attacks defender.
	 * Triggers attack animation, applies damage, and performs counterattack if
	 * the defender survives.
	 */
	private void performAttack(ActorRef out, GameState gameState,
			Unit attacker, Unit defender) {
		// SC-13: trigger attack animation
		BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
		attacker.setHasAttacked(true);

		// SC-14: apply damage and update UI
		int defenderHealth = defender.getHealth() - attacker.getDamage();
		defender.setHealth(defenderHealth);
		BasicCommands.setUnitHealth(out, defender, defenderHealth);

		if (defenderHealth <= 0) {
			// Defender is dead - play death animation
			BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.death);
			gameState.units.remove(defender);
			return;
		}

		// SC-15: Counterattack if defender is still alive
		BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.attack);
		int attackerHealth = attacker.getHealth() - defender.getDamage();
		attacker.setHealth(attackerHealth);
		BasicCommands.setUnitHealth(out, attacker, attackerHealth);

		if (attackerHealth <= 0) {
			BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.death);
			gameState.units.remove(attacker);
		}
	}

}
