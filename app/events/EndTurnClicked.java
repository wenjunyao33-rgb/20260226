package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * 
 * { 
 *   messageType = "endTurnClicked"
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// SC-22: ignore all actions once the game has ended
		if (gameState.gameEnded) return;

		// SC-19: disable human input - hand control to AI
		gameState.currentPlayer = 2;

		// SC-21: AI turn - follows same validation rules (mana = turnNumber + 1)
		int aiMana = Math.min(gameState.turnNumber + 1, GameState.MAX_MANA);
		gameState.player2.setMana(aiMana);
		BasicCommands.setPlayer2Mana(out, gameState.player2);

		// SC-22: check victory after AI interactions
		if (checkVictory(gameState)) return;

		// SC-21: AI ends its turn; return control to human (SC-19)
		gameState.currentPlayer = 1;

		// SC-20: start next human turn - increment turn counter and set mana
		gameState.turnNumber++;
		int mana = Math.min(gameState.turnNumber + 1, GameState.MAX_MANA);
		gameState.player1.setMana(mana);
		BasicCommands.setPlayer1Mana(out, gameState.player1);

		// SC-20: draw 1 card respecting hand limit (SC-10)
		if (gameState.player1Hand.size() < GameState.HAND_LIMIT && !gameState.player1Deck.isEmpty()) {
			Card card = gameState.player1Deck.remove(0);
			gameState.player1Hand.add(card);
			BasicCommands.drawCard(out, card, gameState.player1Hand.size(), 0 /* normal mode */);
		}
	}

	/**
	 * Checks whether either player's avatar has health <= 0 (SC-22).
	 * If so, sets gameEnded = true and returns true.
	 * @param gameState current game state
	 * @return true if the game has ended
	 */
	public static boolean checkVictory(GameState gameState) {
		if (gameState.player1.getHealth() <= 0 || gameState.player2.getHealth() <= 0) {
			gameState.gameEnded = true;
			return true;
		}
		return false;
	}

}
