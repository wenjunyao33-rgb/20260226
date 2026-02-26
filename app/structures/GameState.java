package structures;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Card;

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

	// SC-16: spell target selection state
	public boolean awaitingTarget = false;
	public Card pendingSpell = null;
	public List<Card> playerHand = new ArrayList<>();

}
