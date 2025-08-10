/* Solo-levelling Hunt for Dungeon Master
 * Reference sequential version 
 * Michelle Kuttel 2025, University of Cape Town
 * author of original Java code adapted with assistance from chatGPT for reframing 
 * and complex power - "mana" - function.
 * Inspired by  "Hill Climbing with Montecarlo"
 * EduHPC'22 Peachy Assignment developed by Arturo Gonzalez Escribano  (Universidad de Valladolid 2021/2022)
 */
/**
 * DungeonHunterParallel.java
 * @version Parallel solution
 *
 * Main driver for the Dungeon Hunter assignment.
 * This program initializes the dungeon map and performs a series of searches
 * to locate the global maximum.
 *
 * Usage:
 *   java DungeonHunterParallel <gridSize> <numSearches> <randomSeed>
 *
 */


import java.util.Random;
import java.util.concurrent.ForkJoinPool; //for the random search locations
import java.util.concurrent.RecursiveTask;

public class DungeonHunterParallel {
	static final boolean DEBUG=false;

	//timers for how long it all takes
	static long startTime = 0;
	static long endTime = 0;
	private static void tick() {startTime = System.currentTimeMillis(); }
	private static void tock(){endTime=System.currentTimeMillis(); }

    public static void main(String[] args)  {
    	
    	double xmin, xmax, ymin, ymax; //dungeon limits - dungeons are square
    	DungeonMapParallel dungeon;  //object to store the dungeon as a grid
    	
        // numSearches is a multiplier used to calculate the total number of searches to perform
     	int numSearches=10, gateSize= 10;	// gateSize represents the dimensions of the grid
                                            // the grid is square so if gateSize = 1, then grid is 10x10	
    	HuntParallel [] searches;		// Array of searches
  
    	Random rand = new Random();  //the random number generator
      	int randomSeed=0;  //set seed to have predictability for testing
        
    	if (args.length!=3) {
    		System.out.println("Incorrect number of command line arguments provided.");
    		System.exit(0);
    	}
    	
    	
    	/* Read argument values */
      	try {
    	    gateSize=Integer.parseInt( args[0] );
    	    if (gateSize <= 0) {
             throw new IllegalArgumentException("Grid size must be greater than 0.");
            }
    	
    	    numSearches = (int) (Double.parseDouble(args[1])*(gateSize*2)*(gateSize*2)*DungeonMapParallel.RESOLUTION);
    	
    	    randomSeed=Integer.parseInt( args[2] );
            if (randomSeed < 0) {
                throw new IllegalArgumentException("Random seed must be non-negative.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: All arguments must be numeric.");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
 
    	xmin =-gateSize;
    	xmax = gateSize;
    	ymin = -gateSize;
    	ymax = gateSize;
    	dungeon = new DungeonMapParallel(xmin,xmax,ymin,ymax,randomSeed); // Initialize dungeon
    	
    	int dungeonRows=dungeon.getRows();
    	int dungeonColumns=dungeon.getColumns();
     	searches= new HuntParallel[numSearches];

        for (int i = 0; i < numSearches; i++) {
            searches[i] = new HuntParallel(i+1, rand.nextInt(dungeonRows),
            rand.nextInt(dungeonColumns), dungeon);
        }

    	tick();  //start timer
        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        // Prepare tasks
        SearchResult result = forkJoinPool.invoke(new HuntTask(searches, 0, numSearches));
        forkJoinPool.shutdown();

        int max = result.maxMana;
        int finder = result.finderIndex;

   		tock(); //end timer
        
		System.out.printf("\t dungeon size: %d,\n", gateSize);
		System.out.printf("\t rows: %d, columns: %d\n", dungeonRows, dungeonColumns);
		System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", xmin, xmax, ymin, ymax );
		System.out.printf("\t Number searches: %d\n", numSearches );

		/*  Total computation time */
		System.out.printf("\n\t time: %d ms\n",endTime - startTime );
		int tmp=dungeon.getGridPointsEvaluated();
		System.out.printf("\tnumber dungeon grid points evaluated: %d  (%2.0f%s)\n",tmp,(tmp*1.0/(dungeonRows*dungeonColumns*1.0))*100.0, "%");

		/* Results*/
		System.out.printf("Dungeon Master (mana %d) found at:  ", max );
		System.out.printf("x=%.1f y=%.1f\n\n",dungeon.getXcoord(searches[finder].getPosRow()), dungeon.getYcoord(searches[finder].getPosCol()) );
		dungeon.visualisePowerMap("visualiseSearchParallel.png", false);
		dungeon.visualisePowerMap("visualiseSearchPathParallel.png", true);
    }

    /**
     * Stores the results of the search
     * i.e. Stores the hunter that found the dungeon master
     * and the mana value of the dungeon master.
     */
    private static class SearchResult {
        private int maxMana;
        private int finderIndex;

        public SearchResult(int maxMana, int finderIndex) {
            this.maxMana = maxMana;
            this.finderIndex = finderIndex;
        }
    }

    /**
     * The class deploys hunters to search for the dungeon master all at the same time.
     * Uses RecursiveTask to return a SearchResult object storing the mana value of the dungeon master
     * and the dungeon master's location.
     */
    private static class HuntTask extends RecursiveTask<SearchResult> {
        private static final int THRESHOLD = 10;    // modify later
        private final HuntParallel[] searches;
        private final int start;
        private final int end;

        public HuntTask(HuntParallel[] searches, int start, int end) {
            this.searches = searches;
            this.start = start;
            this.end = end;
        }

        @Override
        protected SearchResult compute() {
            if (end - start < THRESHOLD) {
                int max = Integer.MIN_VALUE;
                int finder = -1;
                for (int i = start; i < end; i++) {
                    int localMax = searches[i].findManaPeak();
                    if (localMax > max) {
                        max = localMax;
                        finder = i;
                    }
                    if (DungeonHunterParallel.DEBUG) {
                        System.out.println("Shadow " + searches[i].getID() + 
                        " finished at " + localMax + " in " + searches[i].getSteps());
                    }
                }
                return new SearchResult(max, finder);
            } else {
                int mid = (start + end) / 2;
                HuntTask left = new HuntTask(searches, start, mid);
                HuntTask right = new HuntTask(searches, mid, end);
                left.fork();
                SearchResult rightResult = right.compute();
                SearchResult lefResult = left.join();
                return (lefResult.maxMana >= rightResult.maxMana) ? lefResult : rightResult;
            }
        }
    }
}