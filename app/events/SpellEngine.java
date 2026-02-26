package events;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * SC-17: Applies spell effects on the target tile and triggers playEffectAnimation.
 */
public class SpellEngine {

	public static void applySpell(ActorRef out, Card spell, Tile targetTile) {
		EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
		BasicCommands.playEffectAnimation(out, effect, targetTile);
	}

}
