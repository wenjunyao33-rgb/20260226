package structures;

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

	
	public boolean gameInitalised = false;
	
	public boolean something = false;

	// SC-01: 9x5 board of tiles (indices 1..9 x, 1..5 y, stored at [x-1][y-1])
	public Tile[][] board = new Tile[9][5];

	// SC-03: avatar units
	public Unit humanAvatar = null;
	public Unit aiAvatar = null;

	// SC-04: player resources
	public Player player1 = null;
	public int turnNumber = 1;
	
}
