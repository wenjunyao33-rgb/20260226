package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;

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
		// SC-04: refresh mana at start of next turn (turnNumber + 1)
		gameState.turnNumber++;
		if (gameState.player1 != null) {
			gameState.player1.setMana(gameState.turnNumber + 1);
			BasicCommands.setPlayer1Mana(out, gameState.player1);
		}
	}

}
