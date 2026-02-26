package structures;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Card;
import structures.basic.Player;

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

	// SC-19/20/21: Turn management
	public int turnNumber = 0;      // incremented at the start of each human turn
	public int currentPlayer = 1;   // 1 = human, 2 = AI
	public boolean gameEnded = false; // SC-22

	// SC-20: Maximum cards in hand (SC-10 limit)
	public static final int HAND_LIMIT = 6;

	// SC-20/21: Maximum mana a player can have in a single turn
	public static final int MAX_MANA = 9;

	// SC-19/20/21: Players
	public Player player1 = new Player();
	public Player player2 = new Player();

	// SC-20: Hands and decks
	public List<Card> player1Hand = new ArrayList<Card>();
	public List<Card> player2Hand = new ArrayList<Card>();
	public List<Card> player1Deck = new ArrayList<Card>();
	public List<Card> player2Deck = new ArrayList<Card>();

}
