package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Card;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a card.
 * The event returns the position in the player's hand the card resides within.
 * 
 * { 
 *   messageType = "cardClicked"
 *   position = <hand index position [1-6]>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class CardClicked implements EventProcessor{

@Override
public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

int handPosition = message.get("position").asInt();

// SC-16: if the clicked card is a spell, set awaitingTarget
if (handPosition >= 1 && handPosition <= gameState.playerHand.size()) {
Card card = gameState.playerHand.get(handPosition - 1);
if (!card.isCreature()) {
gameState.awaitingTarget = true;
gameState.pendingSpell = card;
}
}

}

}
