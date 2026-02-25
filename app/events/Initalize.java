package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * 
 * { 
 *   messageType = “initalize”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		
		// // [SC-01] Backend Initialization: Set game state to initialized and define starting turn
		gameState.gameInitialised = true;
		gameState.turn = 1;
		
		// [SC-01] Board Generation: Iterate through a 9x5 loop to populate the Tile array
		
		for(int i=0;i<9;i++)
		{
			for(int j=0;j<5;j++)
			{
				Tile tile = BasicObjectBuilders.loadTile(i, j);
				gameState.tiles[i][j] = tile;
				BasicCommands.drawTile(out, tile, 0);
				try {Thread.sleep(20);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}

		// 	// ---------------------------------------------------------------------------------------------------------------------

// 	// [SC-03] Avatar Deployment: Place Human and AI units at mirrored board positions [cite: 355-360]
//         // Human avatar placed at [1,2], AI avatar placed at [7,2]
// 		// Set starting visual health and attack for Human and Ai Avatar

// 		// lOAD HUMAN UNIT
		Unit human = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, gameState.UnitId++, Unit.class);
		gameState.human_unit = human;
		gameState.placeUnit(gameState.human_unit, gameState.tiles[1][2]); 
		BasicCommands.drawUnit(out, gameState.human_unit, gameState.tiles[1][2]);
		try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}

// 		//	Show human's health
		BasicCommands.setUnitHealth(out, gameState.human_unit, 20);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
		
// 		//	Show human's attack
		BasicCommands.setUnitAttack(out, gameState.human_unit, 2);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}

// 		//	Load ai unit
		Unit ai = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, gameState.UnitId++, Unit.class);
		gameState.ai_unit = ai;
		gameState.placeUnit(gameState.ai_unit, gameState.tiles[7][2]); 
		BasicCommands.drawUnit(out, gameState.ai_unit, gameState.tiles[7][2]);
		try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}
		
		
// 		//	Show ai's health
		BasicCommands.setUnitHealth(out, gameState.ai_unit, 20);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
		
// 		//	Show ai's attack
		BasicCommands.setUnitAttack(out, gameState.ai_unit, 2);
		try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
		
		
// // ---------------------------------------------------------------------------------------------------------------------
	}

}


