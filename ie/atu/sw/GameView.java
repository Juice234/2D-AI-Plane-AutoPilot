package ie.atu.sw;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.ThreadLocalRandom.current;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

public class GameView extends JPanel implements ActionListener{
	//Some constants
	private static final long serialVersionUID	= 1L;
	private static final int MODEL_WIDTH 		= 30;
	private static final int MODEL_HEIGHT 		= 20;
	private static final int SCALING_FACTOR 	= 30;
	
	private static final int MIN_TOP 			= 2;
	private static final int MIN_BOTTOM 		= 18;
	private static final int PLAYER_COLUMN 		= 15;
	private static final int TIMER_INTERVAL 	= 100;
	
	private static final byte ONE_SET 			=  1;
	private static final byte ZERO_SET 			=  0;

	/*
	 * The 30x20 game grid is implemented using a linked list of 
	 * 30 elements, where each element contains a byte[] of size 20. 
	 */
	private LinkedList<byte[]> model = new LinkedList<>();

	//These two variables are used by the cavern generator. 
	private int prevTop = MIN_TOP;
	private int prevBot = MIN_BOTTOM;
	
	//Once the timer stops, the game is over
	private Timer timer;
	private long time;
	
	private int playerRow = 11;
	private int index = MODEL_WIDTH - 1; //Start generating at the end
	private Dimension dim;
	
	//Some fonts for the UI display
	private Font font = new Font ("Dialog", Font.BOLD, 50);
	private Font over = new Font ("Dialog", Font.BOLD, 100);

	//The player and a sprite for an exploding plane
	private Sprite sprite;
	private Sprite dyingSprite;
	
	private boolean auto;
	private int lastMoveDirection = 0;
	private BufferedWriter writer;
	private boolean keyPressedSinceLastTick = false;
	BasicNetwork network;
	private int movementLoggingCounter = 0;

	public GameView(boolean auto) throws Exception{
		this.auto = auto; //Use the autopilot
		setBackground(Color.LIGHT_GRAY);
		setDoubleBuffered(true);
		try {
		    network = (BasicNetwork)EncogDirectoryPersistence.loadObject(new File("classificationModel.eg"));
		} catch (Exception e) {
		    e.printStackTrace();
		    return;
		}
		//Creates a viewing area of 900 x 600 pixels
		dim = new Dimension(MODEL_WIDTH * SCALING_FACTOR, MODEL_HEIGHT * SCALING_FACTOR);
    	super.setPreferredSize(dim);
    	super.setMinimumSize(dim);
    	super.setMaximumSize(dim);
		
    	initModel();
    	
		timer = new Timer(TIMER_INTERVAL, this); //Timer calls actionPerformed() every second
		timer.start();
		
        try {
            // Open the file 
            writer = new BufferedWriter(new FileWriter("game_data.csv", true));
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	//Build our game grid
	private void initModel() {
		for (int i = 0; i < MODEL_WIDTH; i++) {
			model.add(new byte[MODEL_HEIGHT]);
		}
	}
	
	public void setSprite(Sprite s) {
		this.sprite = s;
	}
	
	public void setDyingSprite(Sprite s) {
		this.dyingSprite = s;
	}
	
	//Called every second by actionPerformed(). Paint methods are usually ugly.
	public void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2 = (Graphics2D)g;
        
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, dim.width, dim.height);
        
        int x1 = 0, y1 = 0;
        for (int x = 0; x < MODEL_WIDTH; x++) {
        	for (int y = 0; y < MODEL_HEIGHT; y++){  
    			x1 = x * SCALING_FACTOR;
        		y1 = y * SCALING_FACTOR;

        		if (model.get(x)[y] != 0) {
            		if (y == playerRow && x == PLAYER_COLUMN) {
            			timer.stop(); //Crash...
            		}
            		g2.setColor(Color.BLACK);
            		g2.fillRect(x1, y1, SCALING_FACTOR, SCALING_FACTOR);
        		}
        		
        		if (x == PLAYER_COLUMN && y == playerRow) {
        			if (timer.isRunning()) {
            			g2.drawImage(sprite.getNext(), x1, y1, null);
        			}else {
            			g2.drawImage(dyingSprite.getNext(), x1, y1, null);
        			}
        			
        		}
        	}
        }
        
        /*
         * Not pretty, but good enough for this project... The compiler will
         * tidy up and optimise all of the arithmetics with constants below.
         */
        g2.setFont(font);
        g2.setColor(Color.RED);
        g2.fillRect(1 * SCALING_FACTOR, 15 * SCALING_FACTOR, 400, 3 * SCALING_FACTOR);
        g2.setColor(Color.WHITE);
        g2.drawString("Time: " + (int)(time * (TIMER_INTERVAL/1000.0d)) + "s", 1 * SCALING_FACTOR + 10, (15 * SCALING_FACTOR) + (2 * SCALING_FACTOR));
        
        if (!timer.isRunning()) {
			g2.setFont(over);
			g2.setColor(Color.RED);
			g2.drawString("Game Over!", MODEL_WIDTH / 5 * SCALING_FACTOR, MODEL_HEIGHT / 2* SCALING_FACTOR);
        }
	}

	//Move the plane up or down
	public void move(int step) {
	    playerRow += step;
	    lastMoveDirection = step; // Update the last move direction based on the step value
	    keyPressedSinceLastTick = true;
	}

	
	
    private double calculateDistanceToNearestObstacle() {
        for (int x = PLAYER_COLUMN + 1; x < MODEL_WIDTH; x++) {
            if (model.get(x)[playerRow] != ZERO_SET) { // Check if there's an obstacle in the player row
                return x - PLAYER_COLUMN; // Distance to the nearest obstacle
            }
        }
        return MODEL_WIDTH - PLAYER_COLUMN; // No obstacle found in the visible columns
    }
    
    private void autoMove() {
        double[] gameState = sample(); // Use same method for gamestate
        double[] inputState = Arrays.copyOf(gameState, gameState.length - 1); 

        double[] output = new double[network.getOutputCount()]; 
        network.compute(inputState, output); 

        // Find the index of the highest probability output
        int predictedIndex = 0;
        double maxProbability = output[0];
        for (int i = 1; i < output.length; i++) {
            if (output[i] > maxProbability) {
                maxProbability = output[i];
                predictedIndex = i;
            }
        }

        // Convert to move action
        int predictedMove = predictedIndex - 1;

        System.out.println("Model's decision (predicted move): " + predictedMove);
        move(predictedMove); // Execute the move
    }


	
	//Called every second by the timer 
	public void actionPerformed(ActionEvent e) {
		time++; //Update our timer
		this.repaint(); //Repaint the cavern
		
		//Update the next index to generate
		index++;
		index = (index == MODEL_WIDTH) ? 0 : index;
		
	    if (!keyPressedSinceLastTick) {
	        lastMoveDirection = 0; //  No movement  lastMoveDirection
	    }
		generateNext(); 
		keyPressedSinceLastTick = false;//Generate the next part of the cave
		if (auto) autoMove();
		//autoMove();
		

	    if (time %2== 0) {
	        double[] gameState = sample(); // Capture the current game state
	        // Save the game state and last action
	        if (shouldLogState()) {
	            saveSample(gameState); // Save the game state 
	        }

	        keyPressedSinceLastTick = false;

	        // Print the sampled game state
	        //System.out.println("Sampled Game State:");
	        int counter = 1;
	        for (int i = 0; i < gameState.length - 3; i++) { // Loop through the environment sample, excluding the last 3 features
	            //System.out.println("Obstacle[" + counter + "]: " + gameState[i]);
	            counter++;
	        }

	        // Print additional features
	       // System.out.println("Player Row: " + gameState[gameState.length - 3]);
	       // System.out.println("Last Move Direction: " + gameState[gameState.length - 2]);
	       //System.out.println("Distance to Nearest Obstacle: " + gameState[gameState.length - 1]);
	    }
	}
	private boolean shouldLogState() {
	    if (lastMoveDirection != 0) {
	        movementLoggingCounter = 3; // Reset counter when movement occurs
	        return true; // Always log when there's movement
	    } else if (movementLoggingCounter > 0) {
	        movementLoggingCounter--; 
	        return true; 
	    }
	    return false; 
	}
	
	private void generateNext() {
		var next = model.pollFirst(); 
		model.addLast(next); //Move the head to the tail
		Arrays.fill(next, ONE_SET); //Fill everything in
		
		
		//Flip a coin to determine if we could grow or shrink the cave
		var minspace = 4; //Smaller values will create a cave with smaller spaces
		prevTop += current().nextBoolean() ? 1 : -1; 
		prevBot += current().nextBoolean() ? 1 : -1;
		prevTop = max(MIN_TOP, min(prevTop, prevBot - minspace)); 		
		prevBot = min(MIN_BOTTOM, max(prevBot, prevTop + minspace));

		//Fill in the array with the carved area
		Arrays.fill(next, prevTop, prevBot, ZERO_SET);
	}
	

	public double[] sample() {
	    int rows = 7; // 3 rows above and under player
	    int columns = 3; // 3 columns in front of the player
	    int extraFeatures = 3;
	    var vector = new double[rows * columns + extraFeatures];
	    var index = 0;

	    int startRow = Math.max(playerRow - 3, 0);
	    int endRow = Math.min(playerRow + 3, MODEL_HEIGHT - 1);
	    int startColumn = PLAYER_COLUMN + 1; // Start from the column right in front of the player
	    int endColumn = Math.min(startColumn + columns, MODEL_WIDTH); 

	    // Sample the  grid area
	    for (int x = startColumn; x < endColumn; x++) {
	        for (int y = startRow; y <= endRow; y++) {
	            vector[index++] = model.get(x % MODEL_WIDTH)[y]; 
	        }
	    }

	    // Additional features
	    double scaledPlayerRow = (playerRow - 0) / (double)(MODEL_HEIGHT - 1); // Min is 0, Max is MODEL_HEIGHT - 1
	    vector[index++] = scaledPlayerRow; // Player row normalized
	    double distanceToNearestObstacle = calculateDistanceToNearestObstacle();
	    double scaledDistanceToNearestObstacle = (distanceToNearestObstacle - 0) / (double)(MODEL_WIDTH - PLAYER_COLUMN); // Obstacle normalized
	    vector[index++] = scaledDistanceToNearestObstacle; // Distance to the nearest obstacle
	    vector[index++] = lastMoveDirection;

	    return vector;
	}

	
	//Save sample to csv
	public void saveSample(double[] gameState) {
	    try {
	        StringBuilder sb = new StringBuilder();
	        for (double value : gameState) {
	            sb.append(value).append(","); 
	        }
	        
	        sb.setLength(sb.length() - 1); 
	        sb.append("\n");
	        
	        writer.write(sb.toString());
	        writer.flush(); // Flush after writing
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


	
	
	/*
	 * Resets and restarts the game when the "S" key is pressed
	 */
	public void reset() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model.stream() 	//Zero out the grid
		     .forEach(n -> Arrays.fill(n, 0, n.length, ZERO_SET));
		playerRow = 11;		//Centre the plane
		time = 0; 			//Reset the clock
		timer.restart();	//Start the animation
	}
}