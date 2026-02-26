import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import events.EndTurnClicked;
import events.Heartbeat;
import play.libs.Json;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;

/**
 * Tests for Epic 6 acceptance criteria:
 * SC-23: Action flags (hasMoved/hasAttacked)
 * SC-24: Tile bounds validation
 * SC-25: Dead unit cleanup
 * SC-26: Persistent GameState
 */
public class EpicSixTest {

    // -----------------------------------------------------------------------
    // SC-23: Action flags – hasMoved/hasAttacked enforced; reset only at start of turn
    // -----------------------------------------------------------------------

    /** Unit starts with hasMoved = false and hasAttacked = false. */
    @Test
    public void sc23_unitStartsWithActionFlagsCleared() {
        Unit unit = new Unit();
        assertFalse(unit.isHasMoved());
        assertFalse(unit.isHasAttacked());
    }

    /** Setting hasMoved/hasAttacked is reflected by the getters. */
    @Test
    public void sc23_actionFlagsCanBeSet() {
        Unit unit = new Unit();
        unit.setHasMoved(true);
        unit.setHasAttacked(true);
        assertTrue(unit.isHasMoved());
        assertTrue(unit.isHasAttacked());
    }

    /** EndTurnClicked resets hasMoved and hasAttacked for all units. */
    @Test
    public void sc23_endTurnResetsActionFlags() {
        CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
        BasicCommands.altTell = altTell;

        GameState gameState = new GameState();
        Unit unit = new Unit();
        unit.setHasMoved(true);
        unit.setHasAttacked(true);
        gameState.units.add(unit);

        EndTurnClicked endTurn = new EndTurnClicked();
        endTurn.processEvent(null, gameState, Json.newObject());

        assertFalse(unit.isHasMoved());
        assertFalse(unit.isHasAttacked());
    }

    /** Heartbeat does NOT reset action flags. */
    @Test
    public void sc23_flagsNotResetByHeartbeat() {
        GameState gameState = new GameState();
        Unit unit = new Unit();
        unit.setHasMoved(true);
        unit.setHasAttacked(true);
        gameState.units.add(unit);

        Heartbeat heartbeat = new Heartbeat();
        heartbeat.processEvent(null, gameState, Json.newObject());

        assertTrue(unit.isHasMoved());
        assertTrue(unit.isHasAttacked());
    }

    // -----------------------------------------------------------------------
    // SC-24: Bounds validation – 0<=x<=8 and 0<=y<=4; reject out-of-bounds
    // -----------------------------------------------------------------------

    /** Valid corner tiles are accepted. */
    @Test
    public void sc24_validCornerTilesAccepted() {
        assertNotNull(BasicObjectBuilders.loadTile(0, 0));
        assertNotNull(BasicObjectBuilders.loadTile(8, 0));
        assertNotNull(BasicObjectBuilders.loadTile(0, 4));
        assertNotNull(BasicObjectBuilders.loadTile(8, 4));
    }

    /** A tile in the middle of the board is accepted. */
    @Test
    public void sc24_validMiddleTileAccepted() {
        assertNotNull(BasicObjectBuilders.loadTile(4, 2));
    }

    /** x < 0 is rejected (returns null). */
    @Test
    public void sc24_negativeXRejected() {
        assertNull(BasicObjectBuilders.loadTile(-1, 2));
    }

    /** x > 8 is rejected (returns null). */
    @Test
    public void sc24_xTooLargeRejected() {
        assertNull(BasicObjectBuilders.loadTile(9, 2));
    }

    /** y < 0 is rejected (returns null). */
    @Test
    public void sc24_negativeYRejected() {
        assertNull(BasicObjectBuilders.loadTile(4, -1));
    }

    /** y > 4 is rejected (returns null). */
    @Test
    public void sc24_yTooLargeRejected() {
        assertNull(BasicObjectBuilders.loadTile(4, 5));
    }

    // -----------------------------------------------------------------------
    // SC-25: Dead cleanup – sweep health==0; deleteUnit; clear tile reference
    // -----------------------------------------------------------------------

    /** A unit with health > 0 is not removed by sweepDeadUnits. */
    @Test
    public void sc25_aliveUnitNotRemoved() {
        GameState gameState = new GameState();
        Unit unit = new Unit();
        unit.setHealth(10);
        gameState.units.add(unit);

        gameState.sweepDeadUnits();

        assertEquals(1, gameState.units.size());
    }

    /** A unit with health == 0 is removed by sweepDeadUnits. */
    @Test
    public void sc25_deadUnitRemovedBySweep() {
        GameState gameState = new GameState();
        Unit unit = new Unit();
        unit.setHealth(0);
        gameState.units.add(unit);

        gameState.sweepDeadUnits();

        assertEquals(0, gameState.units.size());
    }

    /** sweepDeadUnits clears the tile reference for a dead unit. */
    @Test
    public void sc25_tileReferenceClearedOnSweep() {
        GameState gameState = new GameState();
        Tile tile = BasicObjectBuilders.loadTile(3, 2);
        Unit unit = new Unit();
        unit.setHealth(0);
        unit.setPositionByTile(tile);
        gameState.units.add(unit);
        gameState.unitOnBoard[3][2] = unit;

        gameState.sweepDeadUnits();

        assertNull(gameState.unitOnBoard[3][2]);
    }

    /** deleteUnit removes the unit from the units list. */
    @Test
    public void sc25_deleteUnitRemovesFromList() {
        GameState gameState = new GameState();
        Unit unit = new Unit();
        gameState.units.add(unit);

        gameState.deleteUnit(unit);

        assertEquals(0, gameState.units.size());
    }

    /** deleteUnit clears the tile reference in unitOnBoard. */
    @Test
    public void sc25_deleteUnitClearsTileReference() {
        GameState gameState = new GameState();
        Tile tile = BasicObjectBuilders.loadTile(1, 1);
        Unit unit = new Unit();
        unit.setPositionByTile(tile);
        gameState.units.add(unit);
        gameState.unitOnBoard[1][1] = unit;

        gameState.deleteUnit(unit);

        assertNull(gameState.unitOnBoard[1][1]);
    }

    /** Unit default health is 20. */
    @Test
    public void sc25_unitDefaultHealthIs20() {
        Unit unit = new Unit();
        assertEquals(20, unit.getHealth());
    }

    // -----------------------------------------------------------------------
    // SC-26: Persistent state – all events update the single GameState instance
    // -----------------------------------------------------------------------

    /** The same GameState instance accumulates changes across multiple events. */
    @Test
    public void sc26_gameStatePersistsAcrossEvents() {
        CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
        BasicCommands.altTell = altTell;

        GameState gameState = new GameState();

        // Add a unit to verify units list persists
        Unit unit = new Unit();
        unit.setHasMoved(true);
        unit.setHealth(10);
        gameState.units.add(unit);
        assertEquals(1, gameState.units.size());

        // Heartbeat on the same instance leaves units intact
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.processEvent(null, gameState, Json.newObject());
        assertEquals(1, gameState.units.size());
        assertEquals(10, unit.getHealth());

        // EndTurnClicked resets flags on the same instance
        EndTurnClicked endTurn = new EndTurnClicked();
        endTurn.processEvent(null, gameState, Json.newObject());

        // unit flag was reset on the shared instance
        assertFalse(unit.isHasMoved());
        // unit still in the list (health > 0)
        assertEquals(1, gameState.units.size());
    }

    /** sweepDeadUnits only removes dead units, alive units persist on the same instance. */
    @Test
    public void sc26_sweepPreservesAliveUnitsOnSameInstance() {
        GameState gameState = new GameState();
        Unit alive = new Unit();
        alive.setHealth(15);
        Unit dead = new Unit();
        dead.setHealth(0);
        gameState.units.add(alive);
        gameState.units.add(dead);

        gameState.sweepDeadUnits();

        assertEquals(1, gameState.units.size());
        assertTrue(gameState.units.contains(alive));
    }
}
