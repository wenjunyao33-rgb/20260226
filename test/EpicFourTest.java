import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import events.CardClicked;
import events.CardResolver;
import events.TileClicked;
import play.libs.Json;
import structures.GameState;
import structures.basic.BetterUnit;
import structures.basic.Card;

/**
 * Tests for Epic 4: SC-16 (spell target select), SC-17 (spell resolve),
 * SC-18 (keywords/abilities).
 */
public class EpicFourTest {

	// -------------------------------------------------------------------------
	// SC-16: Clicking a spell card sets awaitingTarget=true and pendingSpell
	// -------------------------------------------------------------------------

	@Test
	public void sc16_spellCardClick_setsAwaitingTargetAndPendingSpell() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Card spellCard = new Card();
		spellCard.setIsCreature(false);
		gameState.playerHand.add(spellCard); // hand position 1

		ObjectNode message = Json.newObject();
		message.put("position", 1);

		new CardClicked().processEvent(null, gameState, message);

		assertTrue(gameState.awaitingTarget);
		assertTrue(spellCard == gameState.pendingSpell);
	}

	@Test
	public void sc16_creatureCardClick_doesNotSetAwaitingTarget() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Card creatureCard = new Card();
		creatureCard.setIsCreature(true);
		gameState.playerHand.add(creatureCard);

		ObjectNode message = Json.newObject();
		message.put("position", 1);

		new CardClicked().processEvent(null, gameState, message);

		assertFalse(gameState.awaitingTarget);
		assertNull(gameState.pendingSpell);
	}

	// -------------------------------------------------------------------------
	// SC-17: CardResolver routes spell to SpellEngine; TileClicked uses it
	// -------------------------------------------------------------------------

	@Test
	public void sc17_cardResolver_clearsStateAfterResolve() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Card spellCard = new Card();
		spellCard.setIsCreature(false);
		gameState.awaitingTarget = true;
		gameState.pendingSpell = spellCard;

		CardResolver.resolve(null, gameState, 3, 2);

		assertFalse(gameState.awaitingTarget);
		assertNull(gameState.pendingSpell);
	}

	@Test
	public void sc17_tileClick_whenAwaitingTarget_resolvesSpell() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Card spellCard = new Card();
		spellCard.setIsCreature(false);
		gameState.awaitingTarget = true;
		gameState.pendingSpell = spellCard;

		ObjectNode message = Json.newObject();
		message.put("tilex", 3);
		message.put("tiley", 2);

		new TileClicked().processEvent(null, gameState, message);

		assertFalse(gameState.awaitingTarget);
		assertNull(gameState.pendingSpell);
	}

	// -------------------------------------------------------------------------
	// SC-18: Keywords / abilities
	// -------------------------------------------------------------------------

	@Test
	public void sc18_openingGambitTriggersOnlyOnce() {
		Set<String> keywords = new HashSet<>();
		keywords.add("OpeningGambit");
		BetterUnit unit = new BetterUnit(keywords);

		assertTrue(unit.triggerOpeningGambit());
		assertFalse(unit.triggerOpeningGambit()); // must not trigger again
	}

	@Test
	public void sc18_openingGambitDoesNotTriggerWithoutKeyword() {
		BetterUnit unit = new BetterUnit(new HashSet<>());
		assertFalse(unit.triggerOpeningGambit());
	}

	@Test
	public void sc18_provokePresent() {
		Set<String> keywords = new HashSet<>();
		keywords.add("Provoke");
		BetterUnit unit = new BetterUnit(keywords);
		assertTrue(unit.hasProvoke());
	}

	@Test
	public void sc18_provokeAbsent() {
		BetterUnit unit = new BetterUnit(new HashSet<>());
		assertFalse(unit.hasProvoke());
	}

	@Test
	public void sc18_rushPresent() {
		Set<String> keywords = new HashSet<>();
		keywords.add("Rush");
		BetterUnit unit = new BetterUnit(keywords);
		assertTrue(unit.hasRush());
	}

	@Test
	public void sc18_rushAbsent() {
		BetterUnit unit = new BetterUnit(new HashSet<>());
		assertFalse(unit.hasRush());
	}

	@Test
	public void sc18_deathwatchPresent() {
		Set<String> keywords = new HashSet<>();
		keywords.add("Deathwatch");
		BetterUnit unit = new BetterUnit(keywords);
		assertTrue(unit.triggerDeathwatch());
	}

	@Test
	public void sc18_deathwatchAbsent() {
		BetterUnit unit = new BetterUnit(new HashSet<>());
		assertFalse(unit.triggerDeathwatch());
	}

}
