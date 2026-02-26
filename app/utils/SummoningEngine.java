package utils;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Computes and highlights legal summoning tiles adjacent to friendly units.
 * SC-07: legal tiles are those orthogonally or diagonally adjacent to any
 * friendly unit that lie within the 9x5 board bounds.
 */
public class SummoningEngine {

	/**
	 * Returns all board tiles adjacent to any friendly unit.
	 */
	public static List<Tile> getLegalSummonTiles(GameState gameState) {
		List<Tile> legal = new ArrayList<Tile>();
		for (Unit u : gameState.friendlyUnits) {
			int ux = u.getPosition().getTilex();
			int uy = u.getPosition().getTiley();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue;
					int nx = ux + dx;
					int ny = uy + dy;
					if (nx >= 1 && nx <= 9 && ny >= 1 && ny <= 5) {
						Tile t = gameState.board[nx - 1][ny - 1];
						if (!legal.contains(t)) legal.add(t);
					}
				}
			}
		}
		return legal;
	}

	/**
	 * Highlights all legal summon tiles with mode=1 (drawTile) and stores
	 * them in gameState.highlightedTiles.
	 */
	public static void highlightSummonTiles(ActorRef out, GameState gameState) {
		List<Tile> tiles = getLegalSummonTiles(gameState);
		for (Tile t : tiles) {
			BasicCommands.drawTile(out, t, 1);
			gameState.highlightedTiles.add(t);
		}
	}
}
