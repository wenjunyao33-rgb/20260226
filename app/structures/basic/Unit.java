package structures.basic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id (this is used by the front-end.
 * Each unit has a current UnitAnimationType, e.g. move,
 * or attack. The position is the physical position on the
 * board. UnitAnimationSet contains the underlying information
 * about the animation frames, while ImageCorrection has
 * information for centering the unit on the tile. 
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Unit {

	@JsonIgnore
	protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file
	
	int id;
	UnitAnimationType animation;
	Position position;
	UnitAnimationSet animations;
	ImageCorrection correction;

	@JsonIgnore int attack;
	@JsonIgnore int health;
	@JsonIgnore int maxHealth;
	@JsonIgnore int owner;
	@JsonIgnore boolean hasMoved;
	@JsonIgnore boolean hasAttacked;
	@JsonIgnore boolean isAvatar;
	@JsonIgnore boolean isStunned;
	@JsonIgnore String cardName;

	public Unit() {}
	
	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;
		
		position = new Position(0,0,0,0);
		this.correction = correction;
		this.animations = animations;
	}
	
	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;
		
		position = new Position(currentTile.getXpos(),currentTile.getYpos(),currentTile.getTilex(),currentTile.getTiley());
		this.correction = correction;
		this.animations = animations;
	}
	
	
	
	public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
			ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = animation;
		this.position = position;
		this.animations = animations;
		this.correction = correction;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public UnitAnimationType getAnimation() {
		return animation;
	}
	public void setAnimation(UnitAnimationType animation) {
		this.animation = animation;
	}

	public ImageCorrection getCorrection() {
		return correction;
	}

	public void setCorrection(ImageCorrection correction) {
		this.correction = correction;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public UnitAnimationSet getAnimations() {
		return animations;
	}

	public void setAnimations(UnitAnimationSet animations) {
		this.animations = animations;
	}
	
	/**
	 * This command sets the position of the Unit to a specified
	 * tile.
	 * @param tile
	 */
	@JsonIgnore
	public void setPositionByTile(Tile tile) {
		position = new Position(tile.getXpos(),tile.getYpos(),tile.getTilex(),tile.getTiley());
	}

	@JsonIgnore public int getAttack() { return attack; }
	public void setAttack(int attack) { this.attack = attack; }
	@JsonIgnore public int getHealth() { return health; }
	public void setHealth(int health) { this.health = health; }
	@JsonIgnore public int getMaxHealth() { return maxHealth; }
	public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
	@JsonIgnore public int getOwner() { return owner; }
	public void setOwner(int owner) { this.owner = owner; }
	@JsonIgnore public boolean isHasMoved() { return hasMoved; }
	public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }
	@JsonIgnore public boolean isHasAttacked() { return hasAttacked; }
	public void setHasAttacked(boolean hasAttacked) { this.hasAttacked = hasAttacked; }
	@JsonIgnore public boolean getIsAvatar() { return isAvatar; }
	public void setIsAvatar(boolean isAvatar) { this.isAvatar = isAvatar; }
	@JsonIgnore public boolean getIsStunned() { return isStunned; }
	public void setIsStunned(boolean isStunned) { this.isStunned = isStunned; }
	@JsonIgnore public String getCardName() { return cardName; }
	public void setCardName(String cardName) { this.cardName = cardName; }
}
