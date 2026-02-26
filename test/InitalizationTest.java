import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import commands.BasicCommands;
import commands.CheckMessageIsNotNullOnTell;
import events.Initalize;
import play.libs.Json;
import structures.GameState;
import structures.basic.Tile;
import utils.BasicObjectBuilders;

/**
 * This is an example of a JUnit test. In this case, we want to be able to test the logic
 * of our system without needing to actually start the web server. We do this by overriding
 * the altTell method in BasicCommands, which means whenever a command would normally be sent
 * to the front-end it is instead discarded. We can manually simulate messages coming from the
 * front-end by calling the processEvent method on the appropriate event processor.
 * @author Richard
 *
 */
public class InitalizationTest {

	/**
	 * This test simply checks that a boolean vairable is set in GameState when we call the
	 * initalize method for illustration.
	 */
	@Test
	public void checkInitalized() {
		
		// First override the alt tell variable so we can issue commands without a running front-end
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell(); // create an alternative tell
		BasicCommands.altTell = altTell; // specify that the alternative tell should be used
		
		// As we are not starting the front-end, we have no GameActor, so lets manually create
		// the components we want to test
		GameState gameState = new GameState(); // create state storage
		Initalize initalizeProcessor =  new Initalize(); // create an initalize event processor
		
		assertFalse(gameState.gameInitalised); // check we have not initalized
		
		// lets simulate recieveing an initalize message
		ObjectNode eventMessage = Json.newObject(); // create a dummy message
		initalizeProcessor.processEvent(null, gameState, eventMessage); // send it to the initalize event processor
		
		assertTrue(gameState.gameInitalised); // check that this updated the game state
		
		// lets also check that running commands don't actually do anything, since we have no front-end
		Tile tile = BasicObjectBuilders.loadTile(3, 2); // create a tile
		BasicCommands.drawTile(null, tile, 0); // draw tile, but will use altTell, so nothing should happen
		
	}

	/**
	 * SC-01: Checks that the 9x5 tile board is populated after initialization.
	 */
	@Test
	public void checkBoardInitialized() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Initalize initalizeProcessor = new Initalize();
		ObjectNode eventMessage = Json.newObject();
		initalizeProcessor.processEvent(null, gameState, eventMessage);

		assertNotNull(gameState.board);
		assertEquals(9, gameState.board.length);
		assertEquals(5, gameState.board[0].length);
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				assertNotNull("Tile [" + x + "," + y + "] should not be null", gameState.board[x-1][y-1]);
			}
		}
	}

	/**
	 * SC-03: Checks that avatars are deployed at correct tiles after initialization.
	 */
	@Test
	public void checkAvatarsDeployed() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Initalize initalizeProcessor = new Initalize();
		ObjectNode eventMessage = Json.newObject();
		initalizeProcessor.processEvent(null, gameState, eventMessage);

		// human avatar at tile [1,2]
		assertNotNull(gameState.humanAvatar);
		assertEquals(1, gameState.humanAvatar.getPosition().getTilex());
		assertEquals(2, gameState.humanAvatar.getPosition().getTiley());

		// AI avatar at tile [7,2]
		assertNotNull(gameState.aiAvatar);
		assertEquals(7, gameState.aiAvatar.getPosition().getTilex());
		assertEquals(2, gameState.aiAvatar.getPosition().getTiley());
	}

	/**
	 * SC-04: Checks player1 health and mana are set during initialization.
	 * Initial mana = turnNumber(1) + 1 = 2.
	 */
	@Test
	public void checkPlayerResourcesInitialized() {
		CheckMessageIsNotNullOnTell altTell = new CheckMessageIsNotNullOnTell();
		BasicCommands.altTell = altTell;

		GameState gameState = new GameState();
		Initalize initalizeProcessor = new Initalize();
		ObjectNode eventMessage = Json.newObject();
		initalizeProcessor.processEvent(null, gameState, eventMessage);

		assertNotNull(gameState.player1);
		assertEquals(20, gameState.player1.getHealth());
		assertEquals(2, gameState.player1.getMana());
	}

}
