package structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

public class GameState {

	public boolean gameInitalised = false;
	public boolean something = false;

	@JsonIgnore public Tile[][] board = new Tile[9][5];
	@JsonIgnore public Player player1;
	@JsonIgnore public Player player2;
	@JsonIgnore public List<Card> player1Deck = new ArrayList<>();
	@JsonIgnore public List<Card> player2Deck = new ArrayList<>();
	@JsonIgnore public List<Card> player1Hand = new ArrayList<>();
	@JsonIgnore public List<Card> player2Hand = new ArrayList<>();
	@JsonIgnore public Unit player1Avatar;
	@JsonIgnore public Unit player2Avatar;
	@JsonIgnore public List<Unit> allUnits = new ArrayList<>();
	@JsonIgnore public int turnNumber = 1;
	@JsonIgnore public int activePlayer = 1;
	@JsonIgnore public boolean gameOver = false;
	@JsonIgnore public Card selectedCard = null;
	@JsonIgnore public int selectedHandPosition = -1;
	@JsonIgnore public Unit selectedUnit = null;
	@JsonIgnore public int nextUnitId = 2;
	@JsonIgnore public int player1ArtifactCharges = 0;
	@JsonIgnore public int player2ArtifactCharges = 0;
	@JsonIgnore private Random random = new Random();

	// ---- Keyword helpers ----

	public static boolean hasKeyword(Unit unit, String keyword) {
		if (unit == null || unit.getCardName() == null) return false;
		switch (keyword) {
			case "Provoke":
				return unit.getCardName().equals("Rock Pulveriser")
					|| unit.getCardName().equals("Swamp Entangler")
					|| unit.getCardName().equals("Silverguard Knight")
					|| unit.getCardName().equals("Ironcliff Guardian");
			case "Flying":
				return unit.getCardName().equals("Young Flamewing");
			case "Rush":
				return unit.getCardName().equals("Saberspine Tiger");
			case "Airdrop":
				return unit.getCardName().equals("Ironcliff Guardian");
			default:
				return false;
		}
	}

	// ---- Board helpers ----

	public boolean inBounds(int x, int y) {
		return x >= 0 && x < 9 && y >= 0 && y < 5;
	}

	public boolean isAdjacent(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1 && !(x1 == x2 && y1 == y2);
	}

	public List<Unit> getAdjacentEnemyProvokers(Unit unit) {
		List<Unit> provokers = new ArrayList<>();
		int ux = unit.getPosition().getTilex();
		int uy = unit.getPosition().getTiley();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = ux + dx, ny = uy + dy;
				if (inBounds(nx, ny)) {
					Unit u = board[nx][ny].getUnitOnTile();
					if (u != null && u.getOwner() != unit.getOwner() && hasKeyword(u, "Provoke")) {
						provokers.add(u);
					}
				}
			}
		}
		return provokers;
	}

	// ---- Movement validation (SC-08, SC-14) ----

	public List<Tile> getValidMoveTiles(Unit unit) {
		List<Tile> valid = new ArrayList<>();
		if (unit.isHasMoved() || unit.getIsStunned()) return valid;

		if (!getAdjacentEnemyProvokers(unit).isEmpty()) return valid;

		int ux = unit.getPosition().getTilex();
		int uy = unit.getPosition().getTiley();

		if (hasKeyword(unit, "Flying")) {
			for (int x = 0; x < 9; x++)
				for (int y = 0; y < 5; y++)
					if (board[x][y].getUnitOnTile() == null) valid.add(board[x][y]);
			return valid;
		}

		int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
		Set<String> seen = new HashSet<>();
		for (int[] d : dirs) {
			int nx = ux + d[0], ny = uy + d[1];
			if (inBounds(nx, ny) && board[nx][ny].getUnitOnTile() == null) {
				if (seen.add(nx + "," + ny)) valid.add(board[nx][ny]);
				for (int[] d2 : dirs) {
					int nx2 = nx + d2[0], ny2 = ny + d2[1];
					if (inBounds(nx2, ny2) && board[nx2][ny2].getUnitOnTile() == null
							&& !(nx2 == ux && ny2 == uy)) {
						if (seen.add(nx2 + "," + ny2)) valid.add(board[nx2][ny2]);
					}
				}
			}
		}
		return valid;
	}

	// ---- Attack validation (SC-09, SC-13) ----

	public List<Unit> getValidAttackTargets(Unit unit) {
		List<Unit> targets = new ArrayList<>();
		if (unit.isHasAttacked() || unit.getIsStunned()) return targets;

		int ux = unit.getPosition().getTilex();
		int uy = unit.getPosition().getTiley();

		List<Unit> provokers = getAdjacentEnemyProvokers(unit);
		if (!provokers.isEmpty()) return provokers;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = ux + dx, ny = uy + dy;
				if (inBounds(nx, ny)) {
					Unit t = board[nx][ny].getUnitOnTile();
					if (t != null && t.getOwner() != unit.getOwner()) targets.add(t);
				}
			}
		}
		return targets;
	}

	// ---- Summon validation (SC-06, SC-17) ----

	public List<Tile> getValidSummonTiles(int player) {
		List<Tile> valid = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (Unit u : allUnits) {
			if (u.getOwner() != player) continue;
			int ux = u.getPosition().getTilex();
			int uy = u.getPosition().getTiley();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue;
					int nx = ux + dx, ny = uy + dy;
					if (inBounds(nx, ny) && board[nx][ny].getUnitOnTile() == null) {
						if (seen.add(nx + "," + ny)) valid.add(board[nx][ny]);
					}
				}
			}
		}
		return valid;
	}

	// ---- Clear UI ----

	public void clearHighlights(ActorRef out) {
		for (int x = 0; x < 9; x++)
			for (int y = 0; y < 5; y++)
				BasicCommands.drawTile(out, board[x][y], 0);
		try { Thread.sleep(30); } catch (InterruptedException e) {}
	}

	public void clearSelection(ActorRef out) {
		if (selectedCard != null && selectedHandPosition > 0) {
			int idx = selectedHandPosition - 1;
			if (idx < player1Hand.size()) {
				BasicCommands.drawCard(out, player1Hand.get(idx), selectedHandPosition, 0);
			}
		}
		selectedCard = null;
		selectedHandPosition = -1;
		selectedUnit = null;
		clearHighlights(out);
	}

	// ---- Hand management (SC-03) ----

	public void drawCardFromDeck(ActorRef out, int player) {
		if (player == 1) {
			if (player1Deck.isEmpty() || player1Hand.size() >= 6) return;
			player1Hand.add(player1Deck.remove(0));
			redrawPlayer1Hand(out);
		} else {
			if (player2Deck.isEmpty() || player2Hand.size() >= 6) return;
			player2Hand.add(player2Deck.remove(0));
		}
	}

	public void redrawPlayer1Hand(ActorRef out) {
		for (int i = 1; i <= 6; i++) BasicCommands.deleteCard(out, i);
		try { Thread.sleep(30); } catch (InterruptedException e) {}
		for (int i = 0; i < player1Hand.size(); i++) {
			BasicCommands.drawCard(out, player1Hand.get(i), i + 1, 0);
			try { Thread.sleep(30); } catch (InterruptedException e) {}
		}
	}

	// ---- Summon unit (SC-06) ----

	public Unit summonUnit(ActorRef out, Card card, Tile tile, int owner) {
		Unit unit = BasicObjectBuilders.loadUnit(card.getUnitConfig(), nextUnitId++, Unit.class);
		unit.setPositionByTile(tile);
		unit.setOwner(owner);
		unit.setAttack(card.getBigCard().getAttack());
		unit.setHealth(card.getBigCard().getHealth());
		unit.setMaxHealth(card.getBigCard().getHealth());
		unit.setCardName(card.getCardname());
		unit.setHasMoved(true);
		unit.setHasAttacked(true);

		if (hasKeyword(unit, "Rush")) {
			unit.setHasAttacked(false);
		}

		tile.setUnitOnTile(unit);
		allUnits.add(unit);

		BasicCommands.drawUnit(out, unit, tile);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		BasicCommands.setUnitAttack(out, unit, unit.getAttack());
		try { Thread.sleep(30); } catch (InterruptedException e) {}
		BasicCommands.setUnitHealth(out, unit, unit.getHealth());
		try { Thread.sleep(30); } catch (InterruptedException e) {}

		EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
		BasicCommands.playEffectAnimation(out, summonEffect, tile);
		try { Thread.sleep(500); } catch (InterruptedException e) {}

		triggerOpeningGambit(out, unit, tile);
		return unit;
	}

	public Unit summonWraithlingAt(ActorRef out, int owner, Tile tile) {
		if (tile == null || tile.getUnitOnTile() != null) return null;
		Unit w = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, nextUnitId++, Unit.class);
		w.setPositionByTile(tile);
		w.setOwner(owner);
		w.setAttack(1);
		w.setHealth(1);
		w.setMaxHealth(1);
		w.setCardName("Wraithling");
		w.setHasMoved(true);
		w.setHasAttacked(true);
		tile.setUnitOnTile(w);
		allUnits.add(w);
		BasicCommands.drawUnit(out, w, tile);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		BasicCommands.setUnitAttack(out, w, 1);
		BasicCommands.setUnitHealth(out, w, 1);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		return w;
	}

	// ---- Movement (SC-08) ----

	public void moveUnit(ActorRef out, Unit unit, Tile target) {
		Tile oldTile = board[unit.getPosition().getTilex()][unit.getPosition().getTiley()];
		oldTile.setUnitOnTile(null);
		target.setUnitOnTile(unit);
		unit.setPositionByTile(target);
		unit.setHasMoved(true);
		BasicCommands.moveUnitToTile(out, unit, target);
		try { Thread.sleep(2000); } catch (InterruptedException e) {}
	}

	// ---- Combat (SC-09, SC-10) ----

	public void performAttack(ActorRef out, Unit attacker, Unit defender) {
		attacker.setHasAttacked(true);
		attacker.setHasMoved(true);

		BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
		try { Thread.sleep(1000); } catch (InterruptedException e) {}

		defender.setHealth(defender.getHealth() - attacker.getAttack());
		BasicCommands.setUnitHealth(out, defender, defender.getHealth());
		BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.hit);
		try { Thread.sleep(500); } catch (InterruptedException e) {}

		syncAvatarHealth(out);

		if (defender.getIsAvatar()) triggerZeal(out, defender.getOwner());
		if (attacker.getIsAvatar()) triggerArtifactOnHit(out, attacker);

		if (defender.getHealth() <= 0) {
			destroyUnit(out, defender);
		} else {
			// Counter-attack (SC-10)
			BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.attack);
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
			attacker.setHealth(attacker.getHealth() - defender.getAttack());
			BasicCommands.setUnitHealth(out, attacker, attacker.getHealth());
			BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.hit);
			try { Thread.sleep(500); } catch (InterruptedException e) {}

			syncAvatarHealth(out);
			if (attacker.getIsAvatar()) triggerZeal(out, attacker.getOwner());

			if (attacker.getHealth() <= 0) {
				destroyUnit(out, attacker);
			} else {
				BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);
			}
			BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.idle);
		}
		checkGameOver(out);
	}

	// ---- Unit death (SC-11) ----

	public void destroyUnit(ActorRef out, Unit unit) {
		BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.death);
		try { Thread.sleep(1500); } catch (InterruptedException e) {}
		BasicCommands.deleteUnit(out, unit);
		try { Thread.sleep(200); } catch (InterruptedException e) {}

		int tx = unit.getPosition().getTilex();
		int ty = unit.getPosition().getTiley();
		if (inBounds(tx, ty)) board[tx][ty].setUnitOnTile(null);
		allUnits.remove(unit);

		if (unit.getIsAvatar()) {
			if (unit.getOwner() == 1) {
				player1.setHealth(0);
				BasicCommands.setPlayer1Health(out, player1);
			} else {
				player2.setHealth(0);
				BasicCommands.setPlayer2Health(out, player2);
			}
		}

		triggerDeathwatch(out, unit);
	}

	private void syncAvatarHealth(ActorRef out) {
		if (player1Avatar != null) {
			player1.setHealth(player1Avatar.getHealth());
			BasicCommands.setPlayer1Health(out, player1);
		}
		if (player2Avatar != null) {
			player2.setHealth(player2Avatar.getHealth());
			BasicCommands.setPlayer2Health(out, player2);
		}
	}

	// ---- Game Over (SC-12) ----

	public void checkGameOver(ActorRef out) {
		if (gameOver) return;
		if (player1Avatar.getHealth() <= 0 || !allUnits.contains(player1Avatar)) {
			gameOver = true;
			BasicCommands.addPlayer1Notification(out, "You Lost!", 60);
		} else if (player2Avatar.getHealth() <= 0 || !allUnits.contains(player2Avatar)) {
			gameOver = true;
			BasicCommands.addPlayer1Notification(out, "You Won!", 60);
		}
	}

	// ---- Deathwatch (SC-18) ----

	private void triggerDeathwatch(ActorRef out, Unit deadUnit) {
		for (Unit u : new ArrayList<>(allUnits)) {
			String name = u.getCardName();
			if (name == null) continue;
			switch (name) {
				case "Bad Omen":
					u.setAttack(u.getAttack() + 1);
					BasicCommands.setUnitAttack(out, u, u.getAttack());
					try { Thread.sleep(200); } catch (InterruptedException e) {}
					break;
				case "Shadow Watcher":
					u.setAttack(u.getAttack() + 1);
					u.setHealth(u.getHealth() + 1);
					u.setMaxHealth(u.getMaxHealth() + 1);
					BasicCommands.setUnitAttack(out, u, u.getAttack());
					BasicCommands.setUnitHealth(out, u, u.getHealth());
					try { Thread.sleep(200); } catch (InterruptedException e) {}
					break;
				case "Bloodmoon Priestess":
					List<Tile> adj = getAdjacentEmptyTiles(u);
					if (!adj.isEmpty()) {
						summonWraithlingAt(out, u.getOwner(), adj.get(random.nextInt(adj.size())));
					}
					break;
				case "Shadowdancer":
					Unit friendlyAvatar = (u.getOwner() == 1) ? player1Avatar : player2Avatar;
					Unit enemyAvatar = (u.getOwner() == 1) ? player2Avatar : player1Avatar;
					if (enemyAvatar != null && allUnits.contains(enemyAvatar)) {
						enemyAvatar.setHealth(enemyAvatar.getHealth() - 1);
						BasicCommands.setUnitHealth(out, enemyAvatar, enemyAvatar.getHealth());
					}
					if (friendlyAvatar != null && allUnits.contains(friendlyAvatar)) {
						friendlyAvatar.setHealth(Math.min(friendlyAvatar.getHealth() + 1, friendlyAvatar.getMaxHealth()));
						BasicCommands.setUnitHealth(out, friendlyAvatar, friendlyAvatar.getHealth());
					}
					syncAvatarHealth(out);
					try { Thread.sleep(200); } catch (InterruptedException e) {}
					break;
			}
		}
	}

	private List<Tile> getAdjacentEmptyTiles(Unit unit) {
		List<Tile> tiles = new ArrayList<>();
		int ux = unit.getPosition().getTilex();
		int uy = unit.getPosition().getTiley();
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue;
				int nx = ux + dx, ny = uy + dy;
				if (inBounds(nx, ny) && board[nx][ny].getUnitOnTile() == null)
					tiles.add(board[nx][ny]);
			}
		return tiles;
	}

	// ---- Opening Gambit (SC-19) ----

	private void triggerOpeningGambit(ActorRef out, Unit unit, Tile tile) {
		String name = unit.getCardName();
		if (name == null) return;
		switch (name) {
			case "Gloom Chaser": {
				int behindX = (unit.getOwner() == 1) ? tile.getTilex() - 1 : tile.getTilex() + 1;
				int behindY = tile.getTiley();
				if (inBounds(behindX, behindY) && board[behindX][behindY].getUnitOnTile() == null) {
					summonWraithlingAt(out, unit.getOwner(), board[behindX][behindY]);
				}
				break;
			}
			case "Nightsorrow Assassin": {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						if (dx == 0 && dy == 0) continue;
						int nx = tile.getTilex() + dx, ny = tile.getTiley() + dy;
						if (inBounds(nx, ny)) {
							Unit target = board[nx][ny].getUnitOnTile();
							if (target != null && target.getOwner() != unit.getOwner()
									&& !target.getIsAvatar()
									&& target.getHealth() < target.getMaxHealth()) {
								destroyUnit(out, target);
								return;
							}
						}
					}
				}
				break;
			}
			case "Silverguard Squire": {
				int frontX = (unit.getOwner() == 2) ? tile.getTilex() - 1 : tile.getTilex() + 1;
				int backX = (unit.getOwner() == 2) ? tile.getTilex() + 1 : tile.getTilex() - 1;
				int y = tile.getTiley();
				for (int tx : new int[]{frontX, backX}) {
					if (inBounds(tx, y)) {
						Unit ally = board[tx][y].getUnitOnTile();
						if (ally != null && ally.getOwner() == unit.getOwner()) {
							ally.setAttack(ally.getAttack() + 1);
							ally.setHealth(ally.getHealth() + 1);
							ally.setMaxHealth(ally.getMaxHealth() + 1);
							BasicCommands.setUnitAttack(out, ally, ally.getAttack());
							BasicCommands.setUnitHealth(out, ally, ally.getHealth());
							try { Thread.sleep(200); } catch (InterruptedException e) {}
						}
					}
				}
				break;
			}
		}
	}

	// ---- Zeal (SC-20) ----

	private void triggerZeal(ActorRef out, int damagedAvatarOwner) {
		for (Unit u : allUnits) {
			if (u.getOwner() == damagedAvatarOwner && "Silverguard Knight".equals(u.getCardName())) {
				u.setAttack(u.getAttack() + 2);
				BasicCommands.setUnitAttack(out, u, u.getAttack());
				try { Thread.sleep(200); } catch (InterruptedException e) {}
			}
		}
	}

	// ---- Artifact on-hit (SC-21) ----

	private void triggerArtifactOnHit(ActorRef out, Unit avatarUnit) {
		if (avatarUnit.getOwner() == 1 && player1ArtifactCharges > 0) {
			player1ArtifactCharges--;
			List<Tile> adj = getAdjacentEmptyTiles(avatarUnit);
			if (!adj.isEmpty()) summonWraithlingAt(out, 1, adj.get(random.nextInt(adj.size())));
		} else if (avatarUnit.getOwner() == 2 && player2ArtifactCharges > 0) {
			player2ArtifactCharges--;
			List<Tile> adj = getAdjacentEmptyTiles(avatarUnit);
			if (!adj.isEmpty()) summonWraithlingAt(out, 2, adj.get(random.nextInt(adj.size())));
		}
	}

	// ---- Spell casting (SC-07, SC-21-26) ----

	public boolean castSpell(ActorRef out, Card card, Tile targetTile, int caster) {
		String name = card.getCardname();
		Unit targetUnit = targetTile.getUnitOnTile();

		switch (name) {
			case "Horn of the Forsaken": {
				if (caster == 1) player1ArtifactCharges = 3;
				else player2ArtifactCharges = 3;
				EffectAnimation eff = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
				Tile avTile = (caster == 1)
						? board[player1Avatar.getPosition().getTilex()][player1Avatar.getPosition().getTiley()]
						: board[player2Avatar.getPosition().getTilex()][player2Avatar.getPosition().getTiley()];
				BasicCommands.playEffectAnimation(out, eff, avTile);
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
				return true;
			}
			case "Wraithling Swarm": {
				List<Tile> tiles = getValidSummonTiles(caster);
				Collections.shuffle(tiles, random);
				int ct = 0;
				for (Tile t : tiles) {
					if (ct >= 3) break;
					if (t.getUnitOnTile() == null) { summonWraithlingAt(out, caster, t); ct++; }
				}
				return true;
			}
			case "Dark Terminus": {
				if (targetUnit == null || targetUnit.getOwner() == caster || targetUnit.getIsAvatar()) return false;
				Tile dt = board[targetUnit.getPosition().getTilex()][targetUnit.getPosition().getTiley()];
				destroyUnit(out, targetUnit);
				summonWraithlingAt(out, caster, dt);
				return true;
			}
			case "Beamshock": {
				if (targetUnit == null || targetUnit.getOwner() == caster) return false;
				targetUnit.setIsStunned(true);
				EffectAnimation eff = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
				BasicCommands.playEffectAnimation(out, eff, targetTile);
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
				return true;
			}
			case "Sundrop Elixir": {
				if (targetUnit == null || targetUnit.getOwner() != caster) return false;
				targetUnit.setHealth(Math.min(targetUnit.getHealth() + 5, targetUnit.getMaxHealth()));
				BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getHealth());
				EffectAnimation eff = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
				BasicCommands.playEffectAnimation(out, eff, targetTile);
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
				syncAvatarHealth(out);
				return true;
			}
			case "Truestrike": {
				if (targetUnit == null || targetUnit.getOwner() == caster) return false;
				targetUnit.setHealth(targetUnit.getHealth() - 2);
				BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getHealth());
				EffectAnimation eff = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
				BasicCommands.playEffectAnimation(out, eff, targetTile);
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
				syncAvatarHealth(out);
				if (targetUnit.getHealth() <= 0) destroyUnit(out, targetUnit);
				checkGameOver(out);
				return true;
			}
			default:
				return false;
		}
	}

	public List<Tile> getValidSpellTargets(Card card, int caster) {
		List<Tile> valid = new ArrayList<>();
		String name = card.getCardname();
		switch (name) {
			case "Horn of the Forsaken":
			case "Wraithling Swarm": {
				Unit av = (caster == 1) ? player1Avatar : player2Avatar;
				if (av != null) valid.add(board[av.getPosition().getTilex()][av.getPosition().getTiley()]);
				break;
			}
			case "Dark Terminus":
				for (Unit u : allUnits)
					if (u.getOwner() != caster && !u.getIsAvatar())
						valid.add(board[u.getPosition().getTilex()][u.getPosition().getTiley()]);
				break;
			case "Beamshock":
			case "Truestrike":
				for (Unit u : allUnits)
					if (u.getOwner() != caster)
						valid.add(board[u.getPosition().getTilex()][u.getPosition().getTiley()]);
				break;
			case "Sundrop Elixir":
				for (Unit u : allUnits)
					if (u.getOwner() == caster)
						valid.add(board[u.getPosition().getTilex()][u.getPosition().getTiley()]);
				break;
		}
		return valid;
	}

	// ---- AI Controller (SC-02) ----

	public void executeAITurn(ActorRef out) {
		if (gameOver) return;
		BasicCommands.addPlayer1Notification(out, "Enemy Turn", 2);
		try { Thread.sleep(1000); } catch (InterruptedException e) {}

		aiPlayCards(out);
		aiMoveAndAttack(out);
	}

	private void aiPlayCards(ActorRef out) {
		boolean played = true;
		while (played && !gameOver) {
			played = false;
			for (int i = 0; i < player2Hand.size(); i++) {
				if (gameOver) return;
				Card card = player2Hand.get(i);
				if (card.getManacost() > player2.getMana()) continue;

				if (card.isCreature()) {
					List<Tile> tiles;
					if (hasKeywordByCardName(card.getCardname(), "Airdrop")) {
						tiles = new ArrayList<>();
						for (int x = 0; x < 9; x++)
							for (int y = 0; y < 5; y++)
								if (board[x][y].getUnitOnTile() == null) tiles.add(board[x][y]);
					} else {
						tiles = getValidSummonTiles(2);
					}
					if (tiles.isEmpty()) continue;
					summonUnit(out, card, pickTileClosestTo(tiles, player1Avatar), 2);
					player2.setMana(player2.getMana() - card.getManacost());
					BasicCommands.setPlayer2Mana(out, player2);
					player2Hand.remove(i);
					played = true;
					break;
				} else {
					if (aiCastSpell(out, card)) {
						player2.setMana(player2.getMana() - card.getManacost());
						BasicCommands.setPlayer2Mana(out, player2);
						player2Hand.remove(i);
						played = true;
						break;
					}
				}
			}
		}
	}

	private boolean aiCastSpell(ActorRef out, Card card) {
		switch (card.getCardname()) {
			case "Beamshock": {
				Unit best = null; int ba = -1;
				for (Unit u : allUnits) if (u.getOwner() == 1 && u.getAttack() > ba) { ba = u.getAttack(); best = u; }
				if (best == null) return false;
				return castSpell(out, card, board[best.getPosition().getTilex()][best.getPosition().getTiley()], 2);
			}
			case "Sundrop Elixir": {
				Unit md = null; int mx = 0;
				for (Unit u : allUnits) if (u.getOwner() == 2) { int d = u.getMaxHealth() - u.getHealth(); if (d > mx) { mx = d; md = u; } }
				if (md == null || mx == 0) return false;
				return castSpell(out, card, board[md.getPosition().getTilex()][md.getPosition().getTiley()], 2);
			}
			case "Truestrike": {
				Unit w = null; int mh = Integer.MAX_VALUE;
				for (Unit u : allUnits) if (u.getOwner() == 1 && u.getHealth() < mh) { mh = u.getHealth(); w = u; }
				if (w == null) return false;
				return castSpell(out, card, board[w.getPosition().getTilex()][w.getPosition().getTiley()], 2);
			}
			case "Horn of the Forsaken":
			case "Wraithling Swarm":
				return castSpell(out, card, board[player2Avatar.getPosition().getTilex()][player2Avatar.getPosition().getTiley()], 2);
			case "Dark Terminus": {
				Unit s = null; int ma = -1;
				for (Unit u : allUnits) if (u.getOwner() == 1 && !u.getIsAvatar() && u.getAttack() > ma) { ma = u.getAttack(); s = u; }
				if (s == null) return false;
				return castSpell(out, card, board[s.getPosition().getTilex()][s.getPosition().getTiley()], 2);
			}
			default: return false;
		}
	}

	private void aiMoveAndAttack(ActorRef out) {
		for (Unit unit : new ArrayList<>(allUnits)) {
			if (gameOver || !allUnits.contains(unit)) continue;
			if (unit.getOwner() != 2 || unit.getIsStunned()) continue;

			List<Unit> targets = getValidAttackTargets(unit);
			if (!targets.isEmpty()) {
				performAttack(out, unit, pickBestTarget(targets));
				continue;
			}

			List<Tile> moves = getValidMoveTiles(unit);
			if (!moves.isEmpty()) {
				moveUnit(out, unit, pickTileClosestTo(moves, player1Avatar));
				if (!unit.isHasAttacked()) {
					targets = getValidAttackTargets(unit);
					if (!targets.isEmpty()) performAttack(out, unit, pickBestTarget(targets));
				}
			}
		}
	}

	private Unit pickBestTarget(List<Unit> targets) {
		for (Unit t : targets) if (t.getIsAvatar()) return t;
		Unit best = targets.get(0);
		for (Unit t : targets) if (t.getHealth() < best.getHealth()) best = t;
		return best;
	}

	private Tile pickTileClosestTo(List<Tile> tiles, Unit target) {
		if (target == null || tiles.isEmpty()) return tiles.get(0);
		int tx = target.getPosition().getTilex(), ty = target.getPosition().getTiley();
		Tile best = tiles.get(0); int bd = Integer.MAX_VALUE;
		for (Tile t : tiles) {
			int d = Math.abs(t.getTilex() - tx) + Math.abs(t.getTiley() - ty);
			if (d < bd) { bd = d; best = t; }
		}
		return best;
	}

	private boolean hasKeywordByCardName(String cardName, String keyword) {
		if (cardName == null) return false;
		if ("Airdrop".equals(keyword)) return "Ironcliff Guardian".equals(cardName);
		if ("Rush".equals(keyword)) return "Saberspine Tiger".equals(cardName);
		return false;
	}
}
