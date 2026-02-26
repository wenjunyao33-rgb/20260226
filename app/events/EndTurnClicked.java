package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
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

		if (gameState.gameOver || gameState.activePlayer != 1) return;

		// Clear any selection
		gameState.clearSelection(out);

		// ── SC-02: Switch to Player 2 (AI) turn ──
		gameState.activePlayer = 2;

		// Reset Player 2 units for their turn
		for (Unit u : gameState.allUnits) {
			if (u.getOwner() == 2) {
				u.setHasMoved(false);
				u.setHasAttacked(false);
				// SC-24: Clear stun at start of owner's turn
				u.setIsStunned(false);
			}
		}

		// SC-04: Set Player 2 mana
		int mana = Math.min(gameState.turnNumber + 1, 9);
		gameState.player2.setMana(mana);
		BasicCommands.setPlayer2Mana(out, gameState.player2);
		try { Thread.sleep(100); } catch (InterruptedException e) {}

		// SC-03: Player 2 draws a card
		gameState.drawCardFromDeck(out, 2);

		// SC-02: AI takes its turn
		gameState.executeAITurn(out);

		if (gameState.gameOver) return;

		// ── Switch back to Player 1 ──
		gameState.turnNumber++;
		gameState.activePlayer = 1;

		// Reset Player 1 units for their turn
		for (Unit u : gameState.allUnits) {
			if (u.getOwner() == 1) {
				u.setHasMoved(false);
				u.setHasAttacked(false);
				u.setIsStunned(false);
			}
		}

		// SC-04: Set Player 1 mana (increases each turn, max 9)
		int p1Mana = Math.min(gameState.turnNumber + 1, 9);
		gameState.player1.setMana(p1Mana);
		BasicCommands.setPlayer1Mana(out, gameState.player1);
		try { Thread.sleep(100); } catch (InterruptedException e) {}

		// SC-03: Player 1 draws a card
		gameState.drawCardFromDeck(out, 1);

		BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
	}

}
