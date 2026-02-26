package structures;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Card;
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

	// SC-05/SC-10: player hand (max 6 cards)
	public static final int MAX_HAND_SIZE = 6;
	public List<Card> player_cards = new ArrayList<Card>();

	// SC-06: currently selected card and its hand position (1-based)
	public Card selectedCard = null;
	public int selectedCardPosition = -1;

	// SC-07: tiles currently highlighted on the board
	public List<Tile> highlightedTiles = new ArrayList<Tile>();

	// SC-07: friendly units on the board (used by SummoningEngine)
	public List<Unit> friendlyUnits = new ArrayList<Unit>();

	/**
	 * SC-06: Clears all tile highlights by redrawing each tile in normal mode (0).
	 */
	public void clearHighlights(ActorRef out) {
		for (Tile t : highlightedTiles) {
			BasicCommands.drawTile(out, t, 0);
		}
		highlightedTiles.clear();
	}

	/**
	 * SC-10: Attempts to add a card to the player's hand and draw it in the UI.
	 * Returns false (and does nothing) if the hand is already at MAX_HAND_SIZE.
	 */
	public boolean drawCardToHand(ActorRef out, Card card) {
		if (player_cards.size() >= MAX_HAND_SIZE) {
			return false;
		}
		int position = player_cards.size() + 1;
		player_cards.add(card);
		BasicCommands.drawCard(out, card, position, 0);
		return true;
	}
}
