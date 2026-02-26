package events;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.*;

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

		if (gameState.gameOver || gameState.activePlayer != 1) return;

		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

		if (!gameState.inBounds(tilex, tiley)) return;
		Tile tile = gameState.board[tilex][tiley];
		Unit unitOnTile = tile.getUnitOnTile();

		// ── Case 1: A card is selected from hand ──
		if (gameState.selectedCard != null) {
			Card card = gameState.selectedCard;
			int handPos = gameState.selectedHandPosition;

			if (card.isCreature()) {
				// SC-06: Summon creature on valid tile
				List<Tile> validTiles;
				if (card.getCardname() != null && card.getCardname().equals("Ironcliff Guardian")) {
					// SC-17: Airdrop
					validTiles = new java.util.ArrayList<>();
					for (int x = 0; x < 9; x++)
						for (int y = 0; y < 5; y++)
							if (gameState.board[x][y].getUnitOnTile() == null)
								validTiles.add(gameState.board[x][y]);
				} else {
					validTiles = gameState.getValidSummonTiles(1);
				}

				if (validTiles.contains(tile) && unitOnTile == null) {
					gameState.clearSelection(out);
					gameState.summonUnit(out, card, tile, 1);
					// Deduct mana
					gameState.player1.setMana(gameState.player1.getMana() - card.getManacost());
					BasicCommands.setPlayer1Mana(out, gameState.player1);
					// Remove card from hand
					gameState.player1Hand.remove(card);
					gameState.redrawPlayer1Hand(out);
				} else {
					gameState.clearSelection(out);
				}
			} else {
				// SC-07: Cast spell
				List<Tile> validTargets = gameState.getValidSpellTargets(card, 1);
				if (validTargets.contains(tile)) {
					gameState.clearSelection(out);
					boolean success = gameState.castSpell(out, card, tile, 1);
					if (success) {
						gameState.player1.setMana(gameState.player1.getMana() - card.getManacost());
						BasicCommands.setPlayer1Mana(out, gameState.player1);
						gameState.player1Hand.remove(card);
						gameState.redrawPlayer1Hand(out);
					}
				} else {
					gameState.clearSelection(out);
				}
			}
			return;
		}

		// ── Case 2: A unit is already selected ──
		if (gameState.selectedUnit != null) {
			Unit selected = gameState.selectedUnit;

			if (unitOnTile != null && unitOnTile.getOwner() == 2) {
				// SC-09: Attack enemy unit
				List<Unit> attackTargets = gameState.getValidAttackTargets(selected);
				if (attackTargets.contains(unitOnTile)) {
					gameState.clearSelection(out);
					gameState.performAttack(out, selected, unitOnTile);
				} else {
					// Try to move adjacent and then attack
					if (!selected.isHasMoved() && !selected.isHasAttacked()) {
						Tile bestMove = findMoveToAttack(gameState, selected, unitOnTile);
						if (bestMove != null) {
							gameState.clearSelection(out);
							gameState.moveUnit(out, selected, bestMove);
							// Now attack
							List<Unit> newTargets = gameState.getValidAttackTargets(selected);
							if (newTargets.contains(unitOnTile)) {
								gameState.performAttack(out, selected, unitOnTile);
							}
						} else {
							gameState.clearSelection(out);
						}
					} else {
						gameState.clearSelection(out);
					}
				}
			} else if (unitOnTile == null) {
				// SC-08: Move to empty tile
				List<Tile> validMoves = gameState.getValidMoveTiles(selected);
				if (validMoves.contains(tile)) {
					gameState.clearSelection(out);
					gameState.moveUnit(out, selected, tile);
					// After move, show attack targets if available
					List<Unit> attackTargets = gameState.getValidAttackTargets(selected);
					if (!attackTargets.isEmpty()) {
						gameState.selectedUnit = selected;
						for (Unit t : attackTargets) {
							Tile enemyTile = gameState.board[t.getPosition().getTilex()][t.getPosition().getTiley()];
							BasicCommands.drawTile(out, enemyTile, 2);
						}
					}
				} else {
					gameState.clearSelection(out);
				}
			} else if (unitOnTile == selected) {
				// Clicked same unit — deselect
				gameState.clearSelection(out);
			} else if (unitOnTile.getOwner() == 1) {
				// Clicked another friendly unit — switch selection
				gameState.clearSelection(out);
				selectUnit(out, gameState, unitOnTile);
			} else {
				gameState.clearSelection(out);
			}
			return;
		}

		// ── Case 3: Nothing selected, click a tile ──
		if (unitOnTile != null && unitOnTile.getOwner() == 1) {
			selectUnit(out, gameState, unitOnTile);
		}
	}

	private void selectUnit(ActorRef out, GameState gs, Unit unit) {
		gs.selectedUnit = unit;
		gs.selectedCard = null;
		gs.selectedHandPosition = -1;

		// SC-08: Highlight valid move tiles (white)
		List<Tile> moveTiles = gs.getValidMoveTiles(unit);
		for (Tile t : moveTiles) {
			BasicCommands.drawTile(out, t, 1);
		}

		// SC-09: Highlight valid attack targets (red)
		List<Unit> attackTargets = gs.getValidAttackTargets(unit);
		for (Unit t : attackTargets) {
			Tile enemyTile = gs.board[t.getPosition().getTilex()][t.getPosition().getTiley()];
			BasicCommands.drawTile(out, enemyTile, 2);
		}
	}

	private Tile findMoveToAttack(GameState gs, Unit attacker, Unit target) {
		int tx = target.getPosition().getTilex();
		int ty = target.getPosition().getTiley();
		List<Tile> moveTiles = gs.getValidMoveTiles(attacker);
		Tile best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Tile t : moveTiles) {
			if (gs.isAdjacent(t.getTilex(), t.getTiley(), tx, ty)) {
				int dist = Math.abs(t.getTilex() - tx) + Math.abs(t.getTiley() - ty);
				if (dist < bestDist) { bestDist = dist; best = t; }
			}
		}
		return best;
	}
}
