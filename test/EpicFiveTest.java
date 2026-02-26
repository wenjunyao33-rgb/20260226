import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import events.EndTurnClicked;
import play.libs.Json;
import structures.GameState;
import structures.basic.Card;

/**
 * JUnit tests for Epic 5 story cards SC-19 through SC-22.
 * Follows the same pattern as InitalizationTest.java.
 */
public class EpicFiveTest {

	@Before
	public void setUp() {
		// Override altTell so BasicCommands work without a real ActorRef/front-end
		BasicCommands.altTell = new CheckMessageIsNotNullOnTell();
	}

	// -----------------------------------------------------------------------
	// SC-19: End turn
	// -----------------------------------------------------------------------

	/** endTurnClicked triggers end-turn and control returns to human. */
	@Test
	public void sc19_endTurnReturnsControlToHuman() {
		GameState gameState = new GameState();
		assertEquals(1, gameState.currentPlayer);

		EndTurnClicked endTurn = new EndTurnClicked();
		ObjectNode msg = Json.newObject();
		endTurn.processEvent(null, gameState, msg);

		// After the AI has taken its turn, currentPlayer should be back to 1
		assertEquals(1, gameState.currentPlayer);
	}

	/** When game has ended, endTurnClicked must be ignored (human input disabled). */
	@Test
	public void sc19_ignoredWhenGameEnded() {
		GameState gameState = new GameState();
		gameState.gameEnded = true;
		int savedTurn = gameState.turnNumber;

		EndTurnClicked endTurn = new EndTurnClicked();
		ObjectNode msg = Json.newObject();
		endTurn.processEvent(null, gameState, msg);

		// Nothing should change
		assertEquals(savedTurn, gameState.turnNumber);
		assertEquals(1, gameState.currentPlayer);
	}

	// -----------------------------------------------------------------------
	// SC-20: Turn start – mana and card draw
	// -----------------------------------------------------------------------

	/** After end turn, human mana equals the new turnNumber + 1. */
	@Test
	public void sc20_humanManaEqualsNewTurnNumberPlusOne() {
		GameState gameState = new GameState();
		gameState.turnNumber = 0;

		EndTurnClicked endTurn = new EndTurnClicked();
		ObjectNode msg = Json.newObject();
		endTurn.processEvent(null, gameState, msg);

		// turnNumber increments to 1; mana = 1 + 1 = 2
		assertEquals(1, gameState.turnNumber);
		assertEquals(2, gameState.player1.getMana());
	}

	/** One card is drawn from deck to hand at human turn start. */
	@Test
	public void sc20_drawsOneCardOnTurnStart() {
		GameState gameState = new GameState();
		Card card = new Card();
		gameState.player1Deck.add(card);

		EndTurnClicked endTurn = new EndTurnClicked();
		ObjectNode msg = Json.newObject();
		endTurn.processEvent(null, gameState, msg);

		assertEquals(1, gameState.player1Hand.size());
		assertTrue(gameState.player1Deck.isEmpty());
	}

	/** No card is drawn when the hand is already at HAND_LIMIT. */
	@Test
	public void sc20_noDrawWhenHandFull() {
		GameState gameState = new GameState();
		for (int i = 0; i < GameState.HAND_LIMIT; i++) {
			gameState.player1Hand.add(new Card());
		}
		Card overflow = new Card();
		gameState.player1Deck.add(overflow);

		EndTurnClicked endTurn = new EndTurnClicked();
		ObjectNode msg = Json.newObject();
		endTurn.processEvent(null, gameState, msg);

		assertEquals(GameState.HAND_LIMIT, gameState.player1Hand.size());
		assertEquals(1, gameState.player1Deck.size());
	}

	// -----------------------------------------------------------------------
	// SC-21: AI turn
	// -----------------------------------------------------------------------

	/** AI receives mana = turnNumber + 1 (same formula as human). */
	@Test
	public void sc21_aiManaEqualsCurrentTurnNumberPlusOne() {
		GameState gameState = new GameState();
		gameState.turnNumber = 2;

		EndTurnClicked endTurn = new EndTurnClicked();
		ObjectNode msg = Json.newObject();
		endTurn.processEvent(null, gameState, msg);

		// AI gets mana before turnNumber increments: 2 + 1 = 3
		assertEquals(3, gameState.player2.getMana());
	}

	/** After AI turn, GameState reflects a consistent state (currentPlayer=1). */
	@Test
	public void sc21_aiTurnEndsAndReturnsToHuman() {
		GameState gameState = new GameState();
		assertFalse(gameState.gameEnded);

		EndTurnClicked endTurn = new EndTurnClicked();
		endTurn.processEvent(null, gameState, Json.newObject());

		assertEquals(1, gameState.currentPlayer);
		assertFalse(gameState.gameEnded);
	}

	// -----------------------------------------------------------------------
	// SC-22: Victory condition
	// -----------------------------------------------------------------------

	/** Game ends when player1 health drops to 0. */
	@Test
	public void sc22_gameEndsWhenPlayer1HealthIsZero() {
		GameState gameState = new GameState();
		gameState.player1.setHealth(0);

		assertFalse(gameState.gameEnded);
		EndTurnClicked.checkVictory(gameState);
		assertTrue(gameState.gameEnded);
	}

	/** Game ends when player2 health drops to 0. */
	@Test
	public void sc22_gameEndsWhenPlayer2HealthIsZero() {
		GameState gameState = new GameState();
		gameState.player2.setHealth(0);

		assertFalse(gameState.gameEnded);
		EndTurnClicked.checkVictory(gameState);
		assertTrue(gameState.gameEnded);
	}

	/** checkVictory returns false and does not end game when both players have health > 0. */
	@Test
	public void sc22_noVictoryWhenBothPlayersAlive() {
		GameState gameState = new GameState();
		// default health is 20 for both players

		boolean ended = EndTurnClicked.checkVictory(gameState);
		assertFalse(ended);
		assertFalse(gameState.gameEnded);
	}

	/** Further endTurnClicked events are ignored after game has ended (SC-22). */
	@Test
	public void sc22_actionsIgnoredAfterVictory() {
		GameState gameState = new GameState();
		gameState.player2.setHealth(0);
		EndTurnClicked.checkVictory(gameState); // game should be ended now
		assertTrue(gameState.gameEnded);

		int savedTurn = gameState.turnNumber;
		EndTurnClicked endTurn = new EndTurnClicked();
		endTurn.processEvent(null, gameState, Json.newObject());

		// Nothing must change
		assertEquals(savedTurn, gameState.turnNumber);
	}
}
