package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
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

		gameState.gameInitalised = true;

		gameState.something = true;

		// SC-01: populate 9x5 tile array
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				gameState.board[x-1][y-1] = BasicObjectBuilders.loadTile(x, y);
			}
		}

		// SC-03: deploy avatars
		Tile humanTile = gameState.board[0][1]; // tile [1,2]
		Unit human = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, Unit.class);
		human.setPositionByTile(humanTile);
		gameState.humanAvatar = human;
		BasicCommands.drawUnit(out, human, humanTile);

		Tile aiTile = gameState.board[6][1]; // tile [7,2]
		Unit ai = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 1, Unit.class);
		ai.setPositionByTile(aiTile);
		gameState.aiAvatar = ai;
		BasicCommands.drawUnit(out, ai, aiTile);

		// SC-04: set initial player resources (mana = turnNumber + 1 = 1 + 1 = 2)
		gameState.player1 = new Player(20, gameState.turnNumber + 1);
		BasicCommands.setPlayer1Health(out, gameState.player1);
		BasicCommands.setPlayer1Mana(out, gameState.player1);
	}

}
