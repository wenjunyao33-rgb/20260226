package events;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.*;

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

		if (gameState.gameOver || gameState.activePlayer != 1) return;

		int handPosition = message.get("position").asInt();

		// Clear previous selection
		gameState.clearSelection(out);

		int idx = handPosition - 1;
		if (idx < 0 || idx >= gameState.player1Hand.size()) return;
		Card card = gameState.player1Hand.get(idx);

		// SC-05: Check if player can afford the card
		if (card.getManacost() > gameState.player1.getMana()) {
			BasicCommands.addPlayer1Notification(out, "Not enough mana", 2);
			return;
		}

		// Select the card
		gameState.selectedCard = card;
		gameState.selectedHandPosition = handPosition;
		gameState.selectedUnit = null;

		// SC-05: Highlight the card
		BasicCommands.drawCard(out, card, handPosition, 1);

		// SC-05: Show valid target tiles
		if (card.isCreature()) {
			List<Tile> summonTiles;
			// SC-17: Airdrop — can be placed anywhere
			if (GameState.hasKeyword(null, "Airdrop") == false &&
				card.getCardname() != null && card.getCardname().equals("Ironcliff Guardian")) {
				summonTiles = new java.util.ArrayList<>();
				for (int x = 0; x < 9; x++)
					for (int y = 0; y < 5; y++)
						if (gameState.board[x][y].getUnitOnTile() == null)
							summonTiles.add(gameState.board[x][y]);
			} else {
				summonTiles = gameState.getValidSummonTiles(1);
			}
			for (Tile t : summonTiles) {
				BasicCommands.drawTile(out, t, 1); // white highlight
			}
		} else {
			// Spell — show valid spell targets
			List<Tile> targets = gameState.getValidSpellTargets(card, 1);
			for (Tile t : targets) {
				BasicCommands.drawTile(out, t, 2); // red highlight for spells
			}
		}
	}

}
