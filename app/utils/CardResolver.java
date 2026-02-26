package utils;

import structures.basic.Card;

/**
 * Utility that examines card metadata to distinguish creature (Unit) cards
 * from spell cards.
 */
public class CardResolver {

	/**
	 * Returns true if the card is a creature (Unit) card, false if it is a spell.
	 */
	public static boolean isCreature(Card card) {
		return card.getIsCreature();
	}
}
