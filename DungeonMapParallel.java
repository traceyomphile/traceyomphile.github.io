
/**
 * DungeonMapParallel.java
 * @version Parallel Solution
 * Most of the code is the same as DungeonMap.java, except for the parts
 * where parallelism was implemented (the visualisePowerMap method)
 *
 * Represents the dungeon terrain for the Dungeon Hunter assignment.
 *Methods to compute  power (mana) values in the dungeon grid,
 * to find the neighbouring cell with highest mana value and 
 * to visualise the power of visited cells .
 *
 *
 * Michelle Kuttel, parallelized by Tracey Letlape
 * 2025
 */

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import javax.imageio.ImageIO;

public class DungeonMapParallel {

	public static final int PRECISION = 10000;
	public static final int RESOLUTION = 5;

    // Shared dx and dy objects to avoid reinitialization everytime the method is called.
	// Rember to change later if automarker does not expect extra fields.
    public static final int[] dirX = {-1, 1, 0, 0, -1, 1, -1, 1};
    public static final int[] dirY  = {0, 0, -1, 1, -1, -1, 1, 1};

	private final int rows, columns; //dungeonGrid size
	private final double xmin, xmax, ymin, ymax; //x and y dungeon limits
	private final int [][] manaMap;
	private final int [][] visit;
	private int dungeonGridPointsEvaluated;
    private final double bossX;
    private final double bossY;
    private final double decayFactor;

    //constructor
	public DungeonMapParallel(	double xmin, double xmax, 
			double ymin, double ymax, 
			int seed) {
		super();
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;

		this.rows = (int) Math.round((xmax-xmin)*RESOLUTION); //the grid resolution is fixed
		this.columns =  (int) Math.round((ymax-ymin)*RESOLUTION);//the grid resolution is fixed

		// Randomly place the boss peak
		Random rand;
		if(seed==0) rand= new Random(); //no fixed seed
		else rand= new Random(seed);
        double xRange = xmax - xmin;
        this.bossX = xmin + (xRange) * rand.nextDouble();
        this.bossY = ymin + (ymax - ymin) * rand.nextDouble();
     	// Calculate decay factor based on range
        this.decayFactor = 2.0 / (xRange * 0.1);  // adjust scaling factor to control width

		manaMap = new int[rows][columns];
		visit = new int[rows][columns];
		dungeonGridPointsEvaluated = 0;

		/* Terrain initialization, uses Arrays.fill */
		for (int i = 0; i < rows; i++) {
			Arrays.fill(manaMap[i], Integer.MIN_VALUE);
			Arrays.fill(visit[i], -1);
		}
	}

	// has this site been visited before?
	boolean visited( int x, int y) {
		 return visit[x][y] != -1;
	}

	void setVisited( int x, int y, int id) {
		if (visit[x][y]==-1) //don't reset
			visit[x][y]= id;
	}

	 /**
	     * Evaluates mana at a dungeonGrid  coordinate (x, y) in the dungeon,
	     * and writes it to the map.
	     *
	     * @param x_coord The x-coordinate in the dungeon grid.
	     * @param y_coord The y-coordinate in the dungeon grid.
	     * @return A double value representing the mana value at (x, y).
	     */
	int getManaLevel( int x, int y) {
		if (visited(x,y)) return manaMap[x][y];  //don't recalculate 
		if (manaMap[x][y]>Integer.MIN_VALUE) return manaMap[x][y];
		
		/* Calculate the coordinates of the point in the ranges */
		double x_coord = xmin + ( (xmax - xmin) / rows ) * x;
		double y_coord = ymin + ( (ymax - ymin) / columns ) * y;
		double dx = x_coord - bossX;
		double dy = y_coord - bossY;
		double distanceSquared = dx * dx + dy * dy;
		
		/* The function to compute the mana value value */
		/*DO NOT CHANGE this - unless you are testing, but then put it back!*/
		double mana = (2 * Math.sin(x_coord + 0.1 * Math.sin(y_coord / 5.0) + Math.PI / 2) *
                Math.cos((y_coord + 0.1 * Math.cos(x_coord / 5.0) + Math.PI / 2) / 2.0) +
            0.7 * Math.sin((x_coord * 0.5) + (y_coord * 0.3) + 0.2 * Math.sin(x_coord / 6.0) + Math.PI / 2) +
            0.3 * Math.sin((x_coord * 1.5) - (y_coord * 0.8) + 0.15 * Math.cos(y_coord / 4.0)) +
            -0.2 * Math.log(Math.abs(y_coord - Math.PI * 2) + 0.1) +
            0.5 * Math.sin((x_coord * y_coord) / 4.0 + 0.05 * Math.sin(x_coord)) +
            1.5 * Math.cos((x_coord + y_coord) / 5.0 + 0.1 * Math.sin(y_coord)) +
            3.0 * Math.exp(-0.03 * ((x_coord - bossX - 15) * (x_coord - bossX - 15) +
                                    (y_coord - bossY + 10) * (y_coord - bossY + 10))) +
            8.0 * Math.exp(-0.01 * distanceSquared) +                 
            2.0 / (1.0 + 0.05 * distanceSquared)); 
		
		/* Transform to fixed point precision */
		int fixedPoint = (int)( PRECISION * mana );
		manaMap[x][y]=fixedPoint;
		dungeonGridPointsEvaluated++;//keep count
		return fixedPoint;
	}

	//work out where to go next - move in direction of highest mana
	 /**
     * Function to return the neighbouring cell direction with highest mana 
     * @param x_coord The x-coordinate in the dungeon grid.
     * @param y_coord The y-coordinate in the dungeon grid.
     * @return the direction of highest mana.
     */
	HuntParallel.Direction getNextStepDirection(int x, int y) {
		HuntParallel.Direction climbDirection = HuntParallel.Direction.STAY;
	    int localMax = getManaLevel(x, y);

	    final HuntParallel.Direction[] directions = {
	        HuntParallel.Direction.LEFT,
	        HuntParallel.Direction.RIGHT,
	        HuntParallel.Direction.UP,
	        HuntParallel.Direction.DOWN,
	        HuntParallel.Direction.UP_LEFT,
	        HuntParallel.Direction.UP_RIGHT,
	        HuntParallel.Direction.DOWN_LEFT,
	        HuntParallel.Direction.DOWN_RIGHT
	    };

	    for (int i = 0; i < dirX.length; i++) {
	        int newX = x + dirX[i];
	        int newY = y + dirY[i];

            if (newX < 0 || newX >= rows || newY < 0 ||newY >= columns) continue;

            int power = getManaLevel(newX, newY);
            if (power > localMax) {
                localMax = power;
                climbDirection = directions[i];

                if (power == Integer.MAX_VALUE) break;
            }
	    }
	    return climbDirection;
	}

	/**
     * Generates an image from the dungeon grid.
     * Unvisited cells are colored black, while visited cells follow a black→purple→red→white gradient.
     *
     * @param filename The name of the output PNG file.
     */
	public void visualisePowerMap(String filename, boolean path) {
	    int width = manaMap.length;
	    int height = manaMap[0].length;

	    //output image
	    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

	    // Find min and max for normalization (ignore unvisited sites)
	    int min = Integer.MAX_VALUE;
	    int max = Integer.MIN_VALUE;	    
	    
	    for (int x = 0; x < width; x++) {
	        for (int y = 0; y < height; y++) {
	            int value = manaMap[x][y];
	            if (value==Integer.MIN_VALUE)  continue; // ignore unvisited sites
	            if (value < min) min = value;
	            if (value > max) max = value;
	        }
	    }
	    // Prevent division by zero if everything has the same value
	    double range = (max > min) ? (max - min) : 1.0;

	    // Map height values to colors
		ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		pool.invoke(new VisualisePowerTask(0, width, image, min, range, path));
        try {
	        File output = new File(filename);
	        ImageIO.write(image, "png", output);
	        System.out.println("map saved to " + filename);
	    } catch (IOException | NullPointerException e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * Maps normalized height [0..1] to black → purple → red → white.
	 */
	private Color mapHeightToColor(double normalized) {
	    normalized = Math.max(0, Math.min(1, normalized)); // clamp to [0,1]

	    int r, g, b;

	    if (normalized < 0.33) {
	        // Black -> Purple
	        double t = normalized / 0.33;
	        r = (int) (128 * t); // purple has some red
	        g = 0;
	        b = (int) (128 + 127 * t); // increasing blue
	    } 
	    else if (normalized < 0.66) {
	        // Purple -> Red
	        double t = (normalized - 0.33) / 0.33;
	        r = (int) (128 + 127 * t); // red dominates
	        g = 0;
	        b = (int) (255 - 255 * t); // fade out blue
	    } 
	    else {
	        // Red -> White
	        double t = (normalized - 0.66) / 0.34;
	        r = 255;
	        g = (int) (255 * t);
	        b = (int) (255 * t);
	    }

	    return new Color(r, g, b);
	}

	public int getGridPointsEvaluated() {
		return dungeonGridPointsEvaluated;
	}

	public double getXcoord(int x) {
		return xmin + ( (xmax - xmin) / rows ) * x;
	}
	public double getYcoord(int y) {
		return ymin + ( (ymax - ymin) / columns ) * y;
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	/**
	 * The VisualisePowerTask class sets the RGB color for the output image concurrently by using the ForkJoinPool framework
	 * Uses a Sequential-cutoff of 50
	 * @author Tracey Letlape
	 * 
	 */
	private class VisualisePowerTask extends RecursiveAction {
        private static int THRESHOLD;
        private final int startX, endX, min;
        private final BufferedImage image;
        private final double range;
        private final boolean path;

        public VisualisePowerTask(int startX, int endX, BufferedImage image, int min, double range, boolean path) {
            this.startX = startX;
            this.endX = endX;
            this.image = image;
            this.min = min;
            this.range = range;
            this.path = path;
			THRESHOLD = (int)((rows * columns) / (Runtime.getRuntime().availableProcessors() * 6));
        }

        @Override
        protected void compute() {
            if (endX - startX < THRESHOLD) {
                seqCompute();
            } else {
                int mid = (startX + endX) / 2;

				// safeguard against infinite recursion
				if (mid == startX || mid == endX) {
					seqCompute();
					return;
				}
				
                VisualisePowerTask left = new VisualisePowerTask(startX, mid, image, min, range, path);
                VisualisePowerTask right = new VisualisePowerTask(mid, endX, image, min, range, path);
                left.fork();
                right.compute();
                left.join();
            }
        }

		private void seqCompute() {
			for (int x = startX; x < endX; x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					Color color;
					if (path && !visited(x, y)) {
						color = Color.BLACK;
					} else if (manaMap[x][y] == Integer.MIN_VALUE) {
						color = Color.BLACK;
					} else {
						double normalized = (manaMap[x][y] - min) / range;
						color = mapHeightToColor(normalized);
					}
					image.setRGB(x, image.getHeight()  - 1 - y, color.getRGB());
				}
			}
		}
    }
}
