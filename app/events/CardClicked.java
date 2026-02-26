package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import utils.CardResolver;
import utils.SummoningEngine;

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

int idx = handPosition - 1;
if (idx < 0 || idx >= gameState.player_cards.size()) return;
Card card = gameState.player_cards.get(idx);

// SC-06: clear any previous highlights now that we have a valid card
gameState.clearHighlights(out);

// SC-09: reject if the player cannot afford the card
if (card.getManacost() > gameState.player1.getMana()) {
BasicCommands.addPlayer1Notification(out, "Not enough Mana!", 2);
return;
}

gameState.selectedCard = card;
gameState.selectedCardPosition = handPosition;

// SC-06/07: if creature card, highlight legal summon tiles via SummoningEngine
if (CardResolver.isCreature(card)) {
SummoningEngine.highlightSummonTiles(out, gameState);
}
}

}
