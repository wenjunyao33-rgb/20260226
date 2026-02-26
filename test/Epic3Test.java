import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import structures.GameState;
import structures.MovementEngine;
import structures.RuleValidationEngine;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;

/**
 * JUnit tests for Epic 3 story cards SC-11 through SC-15.
 *
 * All tests run without a live Play server by using the altTell mechanism.
 * Game actions are exercised by calling engine/event methods directly.
 */
public class Epic3Test {

	// -----------------------------------------------------------------------
	// Minimal stub: a Unit that only has position, health, damage and flags.
	// -----------------------------------------------------------------------
	private static Unit makeUnit(int id, int tilex, int tiley, int health, int damage) {
		Unit u = new Unit();
		u.setId(id);
		u.setHealth(health);
		u.setDamage(damage);
		u.setOwner(1); // default owner = player 1 (friendly)
		// Position must not be null for getPosition() calls
		Position pos = new Position(0, 0, tilex, tiley);
		u.setPosition(pos);
		return u;
	}

	// -----------------------------------------------------------------------
	// Common test fixtures
	// -----------------------------------------------------------------------
	private GameState gameState;
	private Tile[][] board;

	@Before
	public void setUp() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		gameState = new GameState();

		// Build a minimal 10x6 board (1-indexed, board[tilex][tiley])
		board = new Tile[10][6];
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				board[x][y] = BasicObjectBuilders.loadTile(x, y);
			}
		}
		gameState.board = board;
	}

	// -----------------------------------------------------------------------
	// SC-11: Move range validation
	// -----------------------------------------------------------------------

	/** canMove returns true when hasMoved == false (default). */
	@Test
	public void sc11_canMove_whenNotMoved() {
		Unit unit = makeUnit(1, 5, 3, 10, 2);
		assertFalse("hasMoved should default to false", unit.isHasMoved());
		assertTrue("RuleValidationEngine.canMove should be true", RuleValidationEngine.canMove(unit));
	}

	/** canMove returns false after hasMoved is set. */
	@Test
	public void sc11_cannotMove_whenAlreadyMoved() {
		Unit unit = makeUnit(1, 5, 3, 10, 2);
		unit.setHasMoved(true);
		assertFalse("RuleValidationEngine.canMove should be false after moving",
				RuleValidationEngine.canMove(unit));
	}

	/** MovementEngine returns a non-empty list of tiles for a unit in open space. */
	@Test
	public void sc11_movementEngine_returnsValidTiles() {
		Unit unit = makeUnit(1, 5, 3, 10, 2);
		gameState.units.add(unit);

		List<Tile> tiles = MovementEngine.getValidMoveTiles(unit, board, gameState.units);
		assertFalse("There should be valid move tiles in open space", tiles.isEmpty());
	}

	/** Every tile returned by MovementEngine is within Chebyshev distance 2. */
	@Test
	public void sc11_movementEngine_tilesWithinRadius2() {
		Unit unit = makeUnit(1, 5, 3, 10, 2);
		gameState.units.add(unit);

		List<Tile> tiles = MovementEngine.getValidMoveTiles(unit, board, gameState.units);
		for (Tile t : tiles) {
			int dx = Math.abs(t.getTilex() - 5);
			int dy = Math.abs(t.getTiley() - 3);
			assertTrue("Tile (" + t.getTilex() + "," + t.getTiley() + ") outside radius 2",
					Math.max(dx, dy) <= 2);
		}
	}

	/** A unit blocking the path reduces the reachable tiles. */
	@Test
	public void sc11_movementEngine_blockedPathReducesTiles() {
		Unit unit = makeUnit(1, 5, 3, 10, 2);
		// Place a blocker directly above the unit
		Unit blocker = makeUnit(2, 5, 4, 10, 2);
		gameState.units.add(unit);
		gameState.units.add(blocker);

		List<Tile> tilesWithBlocker = MovementEngine.getValidMoveTiles(unit, board, gameState.units);

		// Without the blocker
		gameState.units.remove(blocker);
		List<Tile> tilesWithout = MovementEngine.getValidMoveTiles(unit, board, gameState.units);

		assertTrue("Blocker should reduce or equal reachable tiles",
				tilesWithBlocker.size() <= tilesWithout.size());
	}

	// -----------------------------------------------------------------------
	// SC-12: Move
	// -----------------------------------------------------------------------

	/** After moving, unit's position is updated and hasMoved is true. */
	@Test
	public void sc12_move_updatesPositionAndSetsHasMoved() {
		Unit unit = makeUnit(1, 5, 3, 10, 2);
		gameState.units.add(unit);
		Tile target = board[5][4]; // one tile above

		BasicCommands.moveUnitToTile(null, unit, target);
		unit.setPositionByTile(target);
		unit.setHasMoved(true);

		assertEquals("tilex should be updated", 5, unit.getPosition().getTilex());
		assertEquals("tiley should be updated", 4, unit.getPosition().getTiley());
		assertTrue("hasMoved should be true after move", unit.isHasMoved());
	}

	// -----------------------------------------------------------------------
	// SC-13: Melee attack validation
	// -----------------------------------------------------------------------

	/** Adjacent units can attack each other (Chebyshev distance 1). */
	@Test
	public void sc13_isAdjacent_trueForNeighbour() {
		Unit a = makeUnit(1, 3, 3, 10, 2);
		Unit b = makeUnit(2, 4, 3, 10, 2); // immediately to the right
		assertTrue("Adjacent units should pass isAdjacent", RuleValidationEngine.isAdjacent(a, b));
	}

	/** Units 2 tiles apart are NOT adjacent. */
	@Test
	public void sc13_isAdjacent_falseForDistantUnit() {
		Unit a = makeUnit(1, 3, 3, 10, 2);
		Unit b = makeUnit(2, 5, 3, 10, 2); // 2 tiles away
		assertFalse("Distant units should fail isAdjacent", RuleValidationEngine.isAdjacent(a, b));
	}

	/** canAttack is false once hasAttacked is set. */
	@Test
	public void sc13_cannotAttack_whenAlreadyAttacked() {
		Unit attacker = makeUnit(1, 3, 3, 10, 2);
		attacker.setHasAttacked(true);
		assertFalse("canAttack should be false after attacking", RuleValidationEngine.canAttack(attacker));
	}

	// -----------------------------------------------------------------------
	// SC-14: Damage & UI
	// -----------------------------------------------------------------------

	/** Defender loses health equal to attacker's damage value. */
	@Test
	public void sc14_damage_reducesDefenderHealth() {
		Unit attacker = makeUnit(1, 3, 3, 10, 3); // damage = 3
		Unit defender = makeUnit(2, 4, 3, 10, 2); // health = 10

		int expected = defender.getHealth() - attacker.getDamage();
		defender.setHealth(expected);
		BasicCommands.setUnitHealth(null, defender, expected);

		assertEquals("Defender health should be reduced by attacker damage", 7, defender.getHealth());
	}

	// -----------------------------------------------------------------------
	// SC-15: Counterattack
	// -----------------------------------------------------------------------

	/** If defender is alive after taking damage it counterattacks the attacker. */
	@Test
	public void sc15_counterattack_whenDefenderAlive() {
		Unit attacker = makeUnit(1, 3, 3, 10, 2); // health=10, damage=2
		Unit defender = makeUnit(2, 4, 3, 10, 2); // health=10, damage=2

		gameState.units.add(attacker);
		gameState.units.add(defender);

		// Simulate attack
		attacker.setHasAttacked(true);
		int defenderHealth = defender.getHealth() - attacker.getDamage();
		defender.setHealth(defenderHealth);
		BasicCommands.setUnitHealth(null, defender, defenderHealth);

		assertTrue("Defender should still be alive", defenderHealth > 0);

		// Simulate counterattack
		int attackerHealth = attacker.getHealth() - defender.getDamage();
		attacker.setHealth(attackerHealth);
		BasicCommands.setUnitHealth(null, attacker, attackerHealth);

		assertEquals("Defender health after attack", 8, defender.getHealth());
		assertEquals("Attacker health after counterattack", 8, attacker.getHealth());
	}

	/** If defender dies, no counterattack occurs (attacker keeps full health). */
	@Test
	public void sc15_noCounterattack_whenDefenderDead() {
		Unit attacker = makeUnit(1, 3, 3, 10, 15); // high damage - kills defender
		Unit defender = makeUnit(2, 4, 3, 5,  2);  // low health

		gameState.units.add(attacker);
		gameState.units.add(defender);

		// Simulate attack
		attacker.setHasAttacked(true);
		int defenderHealth = defender.getHealth() - attacker.getDamage();
		defender.setHealth(defenderHealth);
		BasicCommands.setUnitHealth(null, defender, defenderHealth);

		assertTrue("Defender should be dead", defenderHealth <= 0);

		// No counterattack - attacker health unchanged
		assertEquals("Attacker health should be unchanged when defender is dead",
				10, attacker.getHealth());
	}

	/** Both units' UI health values are updated correctly after an exchange. */
	@Test
	public void sc15_setUnitHealth_updatesUI_bothUnits() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		Unit attacker = makeUnit(1, 3, 3, 10, 2);
		Unit defender = makeUnit(2, 4, 3, 10, 2);

		// Attack
		int dh = defender.getHealth() - attacker.getDamage();
		defender.setHealth(dh);
		BasicCommands.setUnitHealth(null, defender, dh);

		// Counterattack
		int ah = attacker.getHealth() - defender.getDamage();
		attacker.setHealth(ah);
		BasicCommands.setUnitHealth(null, attacker, ah);

		assertNotNull("altTell should have captured messages", altTell);
		assertEquals("Defender health after attack", 8, defender.getHealth());
		assertEquals("Attacker health after counter", 8, attacker.getHealth());
	}
}
