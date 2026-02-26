package structures;

import java.util.ArrayList;
import java.util.List;

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

	/** All units currently on the board. */
	public List<Unit> units = new ArrayList<>();

	/** The board grid: board[tilex][tiley], 1-indexed (indices 1..9 x 1..5). */
	public Tile[][] board = new Tile[10][6];

	/** The friendly unit the player has currently selected (null if none). */
	public Unit selectedUnit = null;

	/** Tiles currently highlighted as valid move destinations. */
	public List<Tile> highlightedTiles = new ArrayList<>();

	/** Which player's turn it is (1 or 2). */
	public int currentPlayer = 1;
	
}
