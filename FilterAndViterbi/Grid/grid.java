package Grid;

import Grid.gridSquare;

import java.util.ArrayList;

import Data.minHeap;
import FilterAndViterbi.prob;

/*
 * Author: Esther Shimanovich
 * Stores gridsquares and computes probability over them
 * 
 */
public class grid {

	private gridSquare[][] Grid = null;
	private int size;
	public String name = "";
	public gridSquare highestProbSquare;
	public static ArrayList<gridSquare> tenMostLikelyTrajectories = new ArrayList<gridSquare>();

	public grid(String name) {
		this.name = name;
	}

	public grid(gridSquare[][] grid, int size, String name) {
		this.Grid = new gridSquare[size][size];
		this.size = size;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				this.getGrid()[i][j] = grid[i][j].copy();
			}
		}
		this.size = size;
		this.name = name;
	}

	// Constructor
	public grid() {

		this.size = input.size;
		this.Grid = new gridSquare[size][size];
		for (int i = 0; i < input.size; i++) {
			for (int j = 0; j < input.size; j++) {
				this.Grid[i][j] = new gridSquare(i, j, size, input.noBlocked, input.attributes[i * size + j]);
			}
		}
	}

	// Sets Probability distributions after a movement is made
	public void move(int down, int right) {
		// For every square in the grid

		for (int i = 0; i < this.size; i++) {
			for (int j = 0; j < this.size; j++) {
				gridSquare currentSquare = this.getGrid()[i][j];
				gridSquare formerSquare = null;
				// get former square
				if (!boundaryOrBlocked(i, j, -1 * down, -1 * right)) {
					formerSquare = this.getGrid()[i - down][j - right];
				}
				// add probability of something moving towards it, given that
				// former square is not blocked or null
				if (formerSquare != null) {
					currentSquare.newProb = prob.move * formerSquare.oldProb;
					currentSquare.probArrive = prob.move * formerSquare.oldProb;
				}
				// add probability of staying in same block
				gridSquare nextSquare = null;
				if (!boundaryOrBlocked(i, j, down, right)) {
					nextSquare = this.getGrid()[i + down][j + right];
				}
				if (nextSquare != null) {
					currentSquare.newProb += prob.stay * currentSquare.oldProb;
					currentSquare.probStay = prob.stay * currentSquare.oldProb;
				} else {
					currentSquare.newProb += currentSquare.oldProb;
					currentSquare.probStay = currentSquare.oldProb;
				}
			}
		}
	}

	// Checks if it is a boundary or blocked cell
	public boolean boundaryOrBlocked(int i, int j, int down, int right) {
		// Not a Boundary
		if ((i + down < this.size) && (i + down >= 0) && (j + right < this.size) && (j + right >= 0)) {
			// and not a Blocked Cell
			if (!this.getGrid()[i + down][j + right].attribute.equals("B")) {
				return false;
			}
		}
		return true;
	}

	// Applies Viterbi Algorithm
	// Takes max probability and stores the most likely sequence for each block
	public void viterbi(int down, int right) {
		for (int i = 0; i < this.size; i++) {
			for (int j = 0; j < this.size; j++) {
				gridSquare currentGrid = this.Grid[i][j];
				if (currentGrid.probStay >= currentGrid.probArrive) {
					currentGrid.newProb = currentGrid.probStay;
					if (input.saveSequences) {
						currentGrid.newSequence = currentGrid.sequence + ", (" + i + ", " + j + ")";
					}
				} else if (!currentGrid.attribute.equals("B")) {
					currentGrid.newProb = currentGrid.probArrive;
					if (input.saveSequences) {
						currentGrid.newSequence = this.Grid[i - down][j - right].sequence + ", (" + i + ", " + j + ")";
					}
				}
			}
		}
	}

	// Probability distribution given a certain reading
	public void sense(String reading) {
		double probSum = 0;
		// For every square in the grid
		for (int i = 0; i < this.size; i++) {
			for (int j = 0; j < this.size; j++) {
				gridSquare currentSquare = this.getGrid()[i][j];
				if (currentSquare.attribute.equals(reading)) {
					currentSquare.newProb *= prob.correct;
				} else {
					currentSquare.newProb *= prob.incorrect;
				}
				if (!currentSquare.attribute.equals("B")) {
					probSum += currentSquare.newProb;
				}
			}
		}

		// Normalize the probabilities
		normalize(probSum);

	}

	// Normalizes the Probabilities
	// updates sequence
	// detects highest probability
	private void normalize(double probSum) {
		this.highestProbSquare = this.Grid[0][0];
		highestProbSquare.mostLikelySequence = true;
		minHeap tenMostLikely = new minHeap();
		for (int i = 0; i < this.size; i++) {
			for (int j = 0; j < this.size; j++) {
				gridSquare currentSquare = this.getGrid()[i][j];
				if (input.activateTenMostLikely) {
					tenMostLikely.insert(currentSquare);
				}
				currentSquare.sequence = currentSquare.newSequence;
				if (!currentSquare.attribute.equals("B")) {
					currentSquare.oldProb = currentSquare.newProb / probSum;
					currentSquare.newProb = 0;
				}
				if (currentSquare.oldProb > highestProbSquare.oldProb) {
					highestProbSquare.mostLikelySequence = false;
					currentSquare.mostLikelySequence = true;
					highestProbSquare = currentSquare;
				}
			}
		}
		if (input.activateTenMostLikely) {
			// insert into ten most likely
			for (int i = 0; i < 10; i++) {
				tenMostLikelyTrajectories.add(tenMostLikely.delete());
			}
		}
		/*
		 * for debugging probSum = 0; for (int i = 0; i < this.size; i++) { for
		 * (int j = 0; j < this.size; j++) { gridSquare currentSquare =
		 * this.getGrid()[i][j]; probSum += currentSquare.oldProb; } }
		 * System.out.println(probSum);
		 */
	}

	public gridSquare[][] getGrid() {
		return Grid;
	}

	public void setGrid(gridSquare[][] grid) {
		Grid = grid;
	}

	// Print out the probabilities in a grid format on the console, for
	// debugging
	public void printString() {
		for (int i = 0; i < this.size; i++) {
			for (int j = 0; j < this.size; j++) {
				System.out.print(decToFrac(this.Grid[i][j].oldProb) + " ");
			}
			System.out.println("");
		}
		System.out.println("");
		return;
	}

	// Converts decimals to fractions in string form
	static private String decToFrac(double x) {
		if (x < 0) {
			return "-" + decToFrac(-x);
		}
		double tolerance = 1.0E-6;
		double h1 = 1;
		double h2 = 0;
		double k1 = 0;
		double k2 = 1;
		double b = x;
		do {
			double a = Math.floor(b);
			double aux = h1;
			h1 = a * h1 + h2;
			h2 = aux;
			aux = k1;
			k1 = a * k1 + k2;
			k2 = aux;
			b = 1 / (b - a);
		} while (Math.abs(x - h1 / k1) > x * tolerance);

		return (int) h1 + "/" + (int) k1;
	}
}
