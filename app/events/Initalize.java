package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
import structures.basic.Tile;
import utils.BasicObjectBuilders;

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
	}

}


