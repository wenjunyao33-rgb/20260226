package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * 
 * { 
 *   messageType = "tileClicked"
 *   tilex = <x index of the tile>
 *   tiley = <y index of the tile>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class TileClicked implements EventProcessor{

@Override
public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

int tilex = message.get("tilex").asInt();
int tiley = message.get("tiley").asInt();

// SC-08: if a card is selected and the clicked tile is highlighted, play the card
if (gameState.selectedCard != null) {
Tile clickedTile = null;
for (Tile t : gameState.highlightedTiles) {
if (t.getTilex() == tilex && t.getTiley() == tiley) {
clickedTile = t;
break;
}
}
if (clickedTile != null) {
// SC-08: deduct mana cost and update UI
gameState.player1.setMana(gameState.player1.getMana() - gameState.selectedCard.getManacost());
BasicCommands.setPlayer1Mana(out, gameState.player1);
// clear selection state
gameState.clearHighlights(out);
gameState.selectedCard = null;
gameState.selectedCardPosition = -1;
}
}

if (gameState.something == true) {
// do some logic
}

}

}
