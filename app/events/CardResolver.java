package events;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;
import utils.BasicObjectBuilders;

/**
 * SC-17: Routes card plays to the appropriate engine.
 * Spell cards (isCreature == false) are routed to SpellEngine.
 */
public class CardResolver {

	public static void resolve(ActorRef out, GameState gameState, int tilex, int tiley) {
		if (gameState.awaitingTarget && gameState.pendingSpell != null
				&& !gameState.pendingSpell.isCreature()) {
			Tile targetTile = BasicObjectBuilders.loadTile(tilex, tiley);
			SpellEngine.applySpell(out, gameState.pendingSpell, targetTile);
		}
		gameState.awaitingTarget = false;
		gameState.pendingSpell = null;
	}

}
