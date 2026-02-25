package structures;

import structures.basic.Tile;

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

	//	// [SC-01] Board Representation
	public Tile[][] tiles = new Tile[9][5];
	
}
