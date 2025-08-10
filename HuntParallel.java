/**
 * HuntParallel.java
 * @version Parallel solution
 * 
 * Most of the code is the same as the code in Hunt.java, except for some variable types.
 * Represents a search in the grid of a DungeonMap to identify the local maximum from a start point.
 *
 *
 *M. Kuttel 2025
 */
public class HuntParallel{
	private final int id;						//  identifier for this hunt
	private int posRow, posCol;		// Position in the dungeonMap
	private int steps; 				//number of steps to end of the search
	private boolean stopped;	// Did the search hit a previously searched location?
	private final DungeonMapParallel dungeon;
	public enum Direction {
		STAY,
		LEFT,
		RIGHT,
		UP,
		DOWN,
		UP_LEFT,
		UP_RIGHT,
		DOWN_LEFT,
		DOWN_RIGHT
	}

	public HuntParallel(int id, int pos_row, int pos_col, DungeonMapParallel dungeon) {
		this.id = id;
		this.posRow = pos_row; //randomly allocated
		this.posCol = pos_col; //randomly allocated
		this.dungeon = dungeon;
		this.stopped = false;
	}

	/**
	 * Find the local maximum mana from an initial starting point
	 * Uses the ExecutorService interface to check neighbouring directions all at once.
	 * Uses a List to store all tasks and another list to store their results.
	 * 
	 * @return the highest power/mana located
	 */
	public int findManaPeak() {
		int power=Integer.MIN_VALUE;
		Direction next;
		
		while(!dungeon.visited(posRow, posCol)) { // stop when hit existing path
			power=dungeon.getManaLevel(posRow, posCol);
			dungeon.setVisited(posRow, posCol, id);
			steps++;
			next = dungeon.getNextStepDirection(posRow, posCol);
			if(DungeonHunterParallel.DEBUG) System.out.println("Shadow "+getID()+" moving  "+next);
			switch(next) {
				case STAY: return power; //found local valley
				case LEFT:
					posRow--;
					break;
				case RIGHT:
					posRow=posRow+1;
					break;
				case UP:
					posCol=posCol-1;
					break;
				case DOWN:
					posCol=posCol+1;
					break;
				case UP_LEFT:
					posCol=posCol-1;
					posRow--;
					break;
				case UP_RIGHT:
					posCol=posCol-1;
					posRow=posRow+1;
					break;
				case DOWN_LEFT:
					posCol=posCol+1;
					posRow--;
					break;
				case DOWN_RIGHT:
					posCol=posCol+1;
					posRow=posRow+1;
			}
		}
		stopped=true;
		return power;
	}

	public int getID() { return id; }

	public int getPosRow() { return posRow;}

	public int getPosCol() { return posCol;}

	public int getSteps() { return steps;}

	public boolean isStopped() {return stopped;}
}