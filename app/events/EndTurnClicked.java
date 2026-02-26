package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Unit;

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
		// SC-23: reset hasMoved and hasAttacked for all units at start of next turn
		for (Unit unit : gameState.units) {
			unit.setHasMoved(false);
			unit.setHasAttacked(false);
		}
	}

}
