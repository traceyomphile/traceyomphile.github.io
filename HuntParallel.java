import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * HuntParallel.java
 * @version Parallel solution
 * Most of the code is the same as the code in Hunt.java, except for the parts where parallelism was implemented.
 * Represents a search in the grid of a DungeonMap to identify the local maximum from a start point.
 *
 *
 *M. Kuttel 2025
 */
public class HuntParallel {
	private int id;						//  identifier for this hunt
	private int posRow, posCol;		// Position in the dungeonMap
	private int steps; 				//number of steps to end of the search
	private boolean stopped;	// Did the search hit a previously searched location?
	private DungeonMapParallel dungeon;
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
	 * 
	 * @return the highest power/mana located
	 */
	public int findManaPeak() {
		int power=Integer.MIN_VALUE;
		Direction next = Direction.STAY;

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		
		while(!dungeon.visited(posRow, posCol)) { // stop when hit existing path
			power=dungeon.getManaLevel(posRow, posCol);
			dungeon.setVisited(posRow, posCol, id);
			steps++;

			// Build list of directions and their deltas
			List<Callable<DirectionPower>> tasks = Arrays.asList(
				() -> getDirectionPower(HuntParallel.Direction.LEFT, posRow - 1, posCol),
				() -> getDirectionPower(HuntParallel.Direction.RIGHT, posRow + 1, posCol),
				() -> getDirectionPower(HuntParallel.Direction.UP, posRow, posCol - 1),
				() -> getDirectionPower(HuntParallel.Direction.DOWN, posRow, posCol + 1),
				() -> getDirectionPower(HuntParallel.Direction.UP_LEFT, posRow - 1, posCol - 1),
				() -> getDirectionPower(HuntParallel.Direction.UP_RIGHT, posRow + 1, posCol - 1),
				() -> getDirectionPower(HuntParallel.Direction.DOWN_LEFT, posRow - 1, posCol + 1),
				() -> getDirectionPower(HuntParallel.Direction.DOWN_RIGHT, posRow + 1, posCol + 1)
			);

			try {
				List<Future<DirectionPower>> results = executor.invokeAll(tasks);

				DirectionPower best = new DirectionPower(HuntParallel.Direction.STAY, power);
				for (Future<DirectionPower> f : results) {
					DirectionPower dp = f.get();
					if (dp.mana > best.mana && !dungeon.visited(dp.row, dp.col)) {
						best = dp;
					}
				}

				next = best.direction;
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				break;
			}

			if(DungeonHunter.DEBUG) System.out.println("Shadow "+getID()+" moving  "+next);
			switch(next) {
				case STAY: 
					executor.shutdown();	// remove later if mentioned nowhere in the slides.
					return power; //found local valley
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
					posRow=posRow+1;
					posRow--;
					break;
				case DOWN_RIGHT:
					posCol=posCol+1;
					posRow=posRow+1;
			}
		}

		executor.shutdown();
		stopped=true;
		return power;
	}

	private static class DirectionPower {
		private Direction direction;
		private int mana, row, col;

		public DirectionPower(Direction direction, int mana) {
			this.direction = direction;
			this.mana = mana;
		}

		public DirectionPower(Direction direction, int mana, int row, int col) {
			this.direction = direction;
			this.mana = mana;
			this.row = row;
			this.col = col;
		}
	}

	private DirectionPower getDirectionPower(Direction direction, int row, int col) {
		if (!dungeon.inBounds(row, col)) return new DirectionPower(direction, dungeon.getManaLevel(row, col), row, col);
		return new DirectionPower(direction, dungeon.getManaLevel(row, col), row, col);
	}

	public int getID() { return id; }

	public int getPosRow() { return posRow;}

	public int getPosCol() { return posCol;}

	public int getSteps() { return steps;}

	public boolean isStopped() {return stopped;}

}
