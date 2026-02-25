package structures;

import java.util.ArrayList;
import java.util.List;

import structures.basic.BetterUnit;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {

	
// [SC-01] Global Flags
	public boolean gameInitialised = false;
	public int turn = 0;

// [SC-01] Board Representation
	public Tile[][] tiles = new Tile[9][5];

//	[SC-03] Avatar and Unit Management
	public Unit human_unit = new Unit();
	public Unit ai_unit = new Unit();
	public int UnitId = 0;

	//	connect unit and tile
	public void placeUnit(Unit unit, Tile tile) {

	    // Remove from old tile
	    if (unit.getUnitTile() != null) {
	        unit.getUnitTile().setTileUnit(null);
	    }

	    // Link both sides
	    unit.setUnitTile(tile);
	    tile.setTileUnit(unit);

	    // Sync position
	    unit.setPositionByTile(tile);
	}

	//  // [SC-04] Player Resource Tracking
    public Player human_player = new Player();
    public Player ai_player = new Player();
	
}
