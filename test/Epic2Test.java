import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import commands.DummyTell;
import events.CardClicked;
import events.Initalize;
import events.TileClicked;
import play.libs.Json;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import utils.BasicObjectBuilders;
import utils.CardResolver;
import utils.OrderedCardLoader;
import utils.SummoningEngine;

/**
 * Tests for SC-05 through SC-10.
 */
public class Epic2Test {

	/** Simple DummyTell that counts how many messages are sent. */
	private static class CountingTell implements DummyTell {
		public List<ObjectNode> messages = new ArrayList<ObjectNode>();
		@Override
		public void tell(ObjectNode message) {
			assertNotNull(message);
			messages.add(message);
		}
		public long countByType(String type) {
			return messages.stream()
				.filter(m -> type.equals(m.path("messagetype").asText()))
				.count();
		}
		public boolean hasNotification(String text) {
			return messages.stream()
				.filter(m -> "addPlayer1Notification".equals(m.path("messagetype").asText()))
				.anyMatch(m -> text.equals(m.path("text").asText()));
		}
	}

	private GameState gameState;
	private CountingTell tell;

	@Before
	public void setUp() {
		tell = new CountingTell();
		BasicCommands.altTell = tell;
		gameState = new GameState();
		Initalize init = new Initalize();
		init.processEvent(null, gameState, Json.newObject());
	}

	// -------------------------------------------------------------------------
	// SC-05: Starting hand
	// -------------------------------------------------------------------------

	@Test
	public void sc05_handHasThreeCards() {
		assertEquals("player_cards should have 3 cards after init", 3, gameState.player_cards.size());
	}

	@Test
	public void sc05_drawCardSentForSlots1to3() {
		long drawCardCount = tell.countByType("drawCard");
		assertTrue("drawCard should be sent at least 3 times for hand slots 1..3", drawCardCount >= 3);
	}

	// -------------------------------------------------------------------------
	// SC-06: Select card / CardResolver / clearHighlights
	// -------------------------------------------------------------------------

	@Test
	public void sc06_cardResolverDistinguishesCreature() {
		List<Card> deck = OrderedCardLoader.getPlayer1Cards(1);
		Card creatureCard = null;
		Card spellCard = null;
		for (Card c : deck) {
			if (c.getIsCreature() && creatureCard == null) creatureCard = c;
			if (!c.getIsCreature() && spellCard == null) spellCard = c;
		}
		assertNotNull(creatureCard);
		assertNotNull(spellCard);
		assertTrue(CardResolver.isCreature(creatureCard));
		assertFalse(CardResolver.isCreature(spellCard));
	}

	@Test
	public void sc06_clearHighlightsRemovesHighlights() {
		// manually add a highlighted tile
		Tile t = BasicObjectBuilders.loadTile(2, 2);
		gameState.highlightedTiles.add(t);
		assertEquals(1, gameState.highlightedTiles.size());
		gameState.clearHighlights(null);
		assertEquals("highlightedTiles should be empty after clearHighlights", 0, gameState.highlightedTiles.size());
	}

	@Test
	public void sc06_cardClickedUpdatesSelectedCard() {
		// hand position 1 - card 0 in player_cards
		ObjectNode msg = Json.newObject();
		msg.put("position", 1);
		// Give player enough mana
		gameState.player1.setMana(9);
		new CardClicked().processEvent(null, gameState, msg);
		assertNotNull("selectedCard should be set after clicking a card", gameState.selectedCard);
		assertEquals(1, gameState.selectedCardPosition);
	}

	// -------------------------------------------------------------------------
	// SC-07: Summon highlights
	// -------------------------------------------------------------------------

	@Test
	public void sc07_summoningEngineFindsAdjacentTiles() {
		List<Tile> legal = SummoningEngine.getLegalSummonTiles(gameState);
		assertFalse("Should find at least one legal summon tile", legal.isEmpty());
		// Human avatar is at [1,2], so [2,2] must be a legal tile (adjacent)
		boolean found = false;
		for (Tile t : legal) {
			if (t.getTilex() == 2 && t.getTiley() == 2) { found = true; break; }
		}
		assertTrue("Tile [2,2] should be legal (adjacent to human avatar at [1,2])", found);
	}

	@Test
	public void sc07_creatureCardHighlightsTiles() {
		// Find a creature card in hand
		int position = -1;
		for (int i = 0; i < gameState.player_cards.size(); i++) {
			if (gameState.player_cards.get(i).getIsCreature()) { position = i + 1; break; }
		}
		if (position == -1) return; // no creature in starting hand, skip
		gameState.player1.setMana(9);
		ObjectNode msg = Json.newObject();
		msg.put("position", position);
		long drawTileBefore = tell.countByType("drawTile");
		new CardClicked().processEvent(null, gameState, msg);
		long drawTileAfter = tell.countByType("drawTile");
		assertTrue("drawTile(mode=1) should be sent for each legal tile", drawTileAfter > drawTileBefore);
		assertFalse("highlightedTiles should be non-empty", gameState.highlightedTiles.isEmpty());
	}

	// -------------------------------------------------------------------------
	// SC-08: Mana cost
	// -------------------------------------------------------------------------

	@Test
	public void sc08_manaDeductedOnSuccessfulPlay() {
		// Select a creature card
		int position = -1;
		for (int i = 0; i < gameState.player_cards.size(); i++) {
			if (gameState.player_cards.get(i).getIsCreature()) { position = i + 1; break; }
		}
		if (position == -1) return;
		gameState.player1.setMana(9);
		int cost = gameState.player_cards.get(position - 1).getManacost();

		ObjectNode cardMsg = Json.newObject();
		cardMsg.put("position", position);
		new CardClicked().processEvent(null, gameState, cardMsg);

		// Click a highlighted tile
		assertFalse(gameState.highlightedTiles.isEmpty());
		Tile target = gameState.highlightedTiles.get(0);
		ObjectNode tileMsg = Json.newObject();
		tileMsg.put("tilex", target.getTilex());
		tileMsg.put("tiley", target.getTiley());
		new TileClicked().processEvent(null, gameState, tileMsg);

		assertEquals("Mana should be reduced by card cost", 9 - cost, gameState.player1.getMana());
		assertTrue("setPlayer1Mana should have been called", tell.countByType("setPlayer1Mana") >= 2);
	}

	// -------------------------------------------------------------------------
	// SC-09: Not enough mana
	// -------------------------------------------------------------------------

	@Test
	public void sc09_notEnoughManaNotification() {
		// Find a card with manacost > 0
		int position = -1;
		for (int i = 0; i < gameState.player_cards.size(); i++) {
			if (gameState.player_cards.get(i).getManacost() > 0) { position = i + 1; break; }
		}
		if (position == -1) return; // all free cards, skip
		gameState.player1.setMana(0);
		ObjectNode msg = Json.newObject();
		msg.put("position", position);
		new CardClicked().processEvent(null, gameState, msg);
		assertTrue("Should show 'Not enough Mana!' notification",
			tell.hasNotification("Not enough Mana!"));
		assertNull("selectedCard should remain null when rejected", gameState.selectedCard);
	}

	// -------------------------------------------------------------------------
	// SC-10: Hand limit
	// -------------------------------------------------------------------------

	@Test
	public void sc10_handLimitIsSix() {
		assertEquals(6, GameState.MAX_HAND_SIZE);
	}

	@Test
	public void sc10_drawCardToHandRespectsLimit() {
		// Fill hand to 6
		List<Card> deck = OrderedCardLoader.getPlayer1Cards(2); // 2 copies = 20 cards
		int added = 0;
		for (Card c : deck) {
			if (gameState.player_cards.size() >= GameState.MAX_HAND_SIZE) break;
			gameState.drawCardToHand(null, c);
			added++;
		}
		assertEquals("Hand should be at MAX_HAND_SIZE", GameState.MAX_HAND_SIZE, gameState.player_cards.size());

		// Now try to add one more - should be rejected
		long drawCardCountBefore = tell.countByType("drawCard");
		boolean result = gameState.drawCardToHand(null, deck.get(0));
		assertFalse("drawCardToHand should return false when hand is full", result);
		assertEquals("Hand size should still be MAX_HAND_SIZE", GameState.MAX_HAND_SIZE, gameState.player_cards.size());
		assertEquals("drawCard should NOT be sent when hand is full",
			drawCardCountBefore, tell.countByType("drawCard"));
	}
}
