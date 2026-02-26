package events;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 *
 * {
 *   messageType = "initalize"
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		if (gameState.gameInitalised) return;
		gameState.gameInitalised = true;
		gameState.something = true;

		// SC-01: Draw the 9x5 board
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 5; y++) {
				gameState.board[x][y] = BasicObjectBuilders.loadTile(x, y);
				BasicCommands.drawTile(out, gameState.board[x][y], 0);
				try { Thread.sleep(20); } catch (InterruptedException e) {}
			}
		}

		// SC-01: Create players with 20 health
		gameState.player1 = new Player(20, 0);
		gameState.player2 = new Player(20, 0);

		// SC-01: Place Human Avatar at (1,2)
		gameState.player1Avatar = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
		Tile p1Tile = gameState.board[1][2];
		gameState.player1Avatar.setPositionByTile(p1Tile);
		gameState.player1Avatar.setOwner(1);
		gameState.player1Avatar.setAttack(2);
		gameState.player1Avatar.setHealth(20);
		gameState.player1Avatar.setMaxHealth(20);
		gameState.player1Avatar.setIsAvatar(true);
		gameState.player1Avatar.setCardName("Human Avatar");
		p1Tile.setUnitOnTile(gameState.player1Avatar);
		gameState.allUnits.add(gameState.player1Avatar);
		BasicCommands.drawUnit(out, gameState.player1Avatar, p1Tile);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		BasicCommands.setUnitAttack(out, gameState.player1Avatar, 2);
		BasicCommands.setUnitHealth(out, gameState.player1Avatar, 20);
		try { Thread.sleep(100); } catch (InterruptedException e) {}

		// SC-01: Place AI Avatar at (7,2)
		gameState.player2Avatar = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 1, Unit.class);
		Tile p2Tile = gameState.board[7][2];
		gameState.player2Avatar.setPositionByTile(p2Tile);
		gameState.player2Avatar.setOwner(2);
		gameState.player2Avatar.setAttack(2);
		gameState.player2Avatar.setHealth(20);
		gameState.player2Avatar.setMaxHealth(20);
		gameState.player2Avatar.setIsAvatar(true);
		gameState.player2Avatar.setCardName("AI Avatar");
		p2Tile.setUnitOnTile(gameState.player2Avatar);
		gameState.allUnits.add(gameState.player2Avatar);
		BasicCommands.drawUnit(out, gameState.player2Avatar, p2Tile);
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		BasicCommands.setUnitAttack(out, gameState.player2Avatar, 2);
		BasicCommands.setUnitHealth(out, gameState.player2Avatar, 20);
		try { Thread.sleep(100); } catch (InterruptedException e) {}

		// SC-01: Set player health display
		BasicCommands.setPlayer1Health(out, gameState.player1);
		BasicCommands.setPlayer2Health(out, gameState.player2);
		try { Thread.sleep(100); } catch (InterruptedException e) {}

		// SC-01: Create and shuffle decks
		gameState.player1Deck = OrderedCardLoader.getPlayer1Cards(2);
		Collections.shuffle(gameState.player1Deck);
		gameState.player2Deck = OrderedCardLoader.getPlayer2Cards(2);
		Collections.shuffle(gameState.player2Deck);

		// SC-01: Draw initial hand of 3 cards each
		for (int i = 0; i < 3; i++) {
			gameState.drawCardFromDeck(out, 1);
			gameState.drawCardFromDeck(out, 2);
		}

		// SC-04: Set initial mana (turn 1 = 2 mana)
		gameState.player1.setMana(2);
		BasicCommands.setPlayer1Mana(out, gameState.player1);
		try { Thread.sleep(100); } catch (InterruptedException e) {}

		BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
	}

}
