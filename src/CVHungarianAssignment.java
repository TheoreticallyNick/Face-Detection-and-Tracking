
import java.util.Arrays;

public class CVHungarianAssignment {
	
	public final double[][] costMatrix;
	public final int rows, cols, dim;
	public int[][] assignments;
	public final boolean[] assignedTracks;
	public final boolean[] assignedDetections;
	public int[] unassignedTracks;
	public int[] unassignedDetections;
	public int costOfNonAssignment;
	
	public CVHungarianAssignment(double[][] costMatrix, int costOfNonAssignment) {
		this.dim = Math.max(costMatrix.length, costMatrix[0].length);
		this.rows = costMatrix.length;
		this.cols = costMatrix[0].length;
		this.assignments = new int[0][2];
		//initialize boolean arrays
		this.assignedTracks = new boolean[this.rows];
		Arrays.fill(this.assignedTracks, false);
		this.assignedDetections = new boolean[this.cols];
		Arrays.fill(this.assignedDetections, false);
		//intialize unassigment arrays
		this.unassignedTracks = new int[0];
		this.unassignedDetections = new int[0];
		//initialize class cost matrix
		this.costMatrix = new double[this.rows][this.cols];
		this.costOfNonAssignment = costOfNonAssignment;
		
		for (int i=0; i<this.rows; i++) {
			if (i < costMatrix.length) {
				if(costMatrix[i].length != this.cols) {
					throw new IllegalArgumentException("Irregular cost matrix!");
				}
				
				for (int j=0; j < this.cols; j++) {
					if (Double.isInfinite(costMatrix[i][j])) {
						throw new IllegalArgumentException("Infinite cost!");
					}
					if (Double.isNaN(costMatrix[i][j])) {
						throw new IllegalArgumentException("NaN cost!");
					}
				}
				
				this.costMatrix[i] = Arrays.copyOf(costMatrix[i], this.cols);
			} else {
				this.costMatrix[i] = new double[this.cols];
			}
		}
		
	}
	
	public void solveCost() {
		reduce();
		solveAssignments();
		solveUnassignments();
	}
	
	protected void reduce() {
		for (int i=0; i < rows; i++) {
			double min = Double.POSITIVE_INFINITY;
			for (int j=0; j < cols; j++) {
				if (costMatrix[i][j] < min) {
					min = costMatrix[i][j];
				}
			}
			
			for (int j=0; j < cols; j++) {
				costMatrix[i][j] -= min;
			}
		}
	}
	
	protected void solveAssignments() {
		for (int i=0; i < rows; i++) {
			for (int j=0; j < cols; j++) {
				if (assignedTracks[i] == false && assignedDetections[j] == false 
						&& costMatrix[i][j] == 0) {
					assignments = append2DArray(assignments, i, j);
					assignedTracks[i] = true;
					assignedDetections[j] = true;
				} else {
					
				}
			}
		}
	}
	
	protected void solveUnassignments() {
		if (cols < rows) {
			for (int i=0; i < dim; i++) {
				if (assignedTracks[i] == false) {
					unassignedTracks = appendArray(unassignedTracks, i);
				}
			}
		} else {
			for (int i=0; i < dim; i++) {
				if (assignedDetections[i] == false) {
					unassignedDetections = appendArray(unassignedDetections, i);
				}
			}
		}
	}
	
	protected int[] appendArray(int[] array, int a) {
		int[] result = new int[array.length + 1];
		for (int i=0; i<array.length; i++) {
			result[i] = array[i];
		}
		
		result[array.length] = a;
		return result;
	}
	
	protected int[][] append2DArray(int[][] array, int a, int b) {
		int[][] result = new int[array.length + 1][2];
		for (int i=0; i<array.length; i++) {
			for (int j=0; j<2; j++) {
				result[i][j] = array[i][j];
			}
		}
		
		result[array.length][0] = a;
		result[array.length][1] = b;
		
		return result;
	}
}