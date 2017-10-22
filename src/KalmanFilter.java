

public class KalmanFilter {	
	
	private int t = 1; //time variable
	private double[][] u = {{0.0}}; //model acceleration magnitude
	private double xNoise = 1; //measurement noise in x direction
	private double yNoise = 1; //measurement noise in y direction
	
	private double[][] Q = {{0},{0},{0},{0}}; //state model matrix [xpos;ypos;xvel;yvel]
	private double[][] P; //state covariance matrix
	private double[][] K; //Kalman gain matrix
	
	private double[][] Ez = {{xNoise, 0},{0, yNoise}};
	private double[][] Ex = {{t^4/4, 0, t^3/2, 0}, 
							 {0, t^4/4, 0, t^3/2},
							 {t^3/2, 0, t, 0},
							 {0, t^3/2, 0, t}};
	
	private double[][] A = {{1,0,t,0},{0,1,0,t},{0,0,1,0},{0,0,0,1}};
	private double[][] B = {{t^2/2},{t^2/2},{t},{t}};
	private double[][] C = {{1,0,0,0},{0,1,0,0}};
	private double[][] I = {{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}};
	
	public KalmanFilter(double[] centroid) {
		this.Q = new double[4][1];
		this.Q[0][0] = centroid[0];
		this.Q[1][0] = centroid[1];
		this.P = this.Ex;
		
	}
	
	public double[] predict(double[] centroid) {
		double[] result = new double[2];
		
		double[][] input = {{centroid[0]},{centroid[1]}};
		//predict next state based on previous state
		double[][] temp = multiplyMatrix(A,Q);
		double[][] temp2 = multiplyMatrix(B,u);
		Q = addMatrix(temp,temp2);
		
		//predict next covariance based on previous
		double[][] transA = transposeMatrix(A);
		temp = multiplyMatrix(A,P);
		temp = multiplyMatrix(temp, transA);
		P = addMatrix(temp, Ex);
		
		//calculate Kalman gain
		double[][] transC = transposeMatrix(C);
		temp = multiplyMatrix(C,P);
		temp = multiplyMatrix(temp,transC);
		temp = addMatrix(temp, Ez);
		temp = inverseMatrix(temp);
		temp2 = multiplyMatrix(P,transC);
		K = multiplyMatrix(temp2,temp);
		
		//calculate new state estimate
		temp = multiplyMatrix(C, Q);
		temp = subtractMatrix(input, temp);
		temp = multiplyMatrix(K, temp);
		Q = addMatrix(Q, temp);
		
		//calculate new covariance estimation
		temp = multiplyMatrix(K, C);
		temp = subtractMatrix(I,temp);
		P = multiplyMatrix(temp,P);
		
		result[0] = Q[0][0];
		result[1] = Q[1][0];
		return result;
	}
	
	public double[][] multiplyMatrix(double[][] A, double[][] B) {
        int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }

        double[][] result = new double[aRows][bColumns];

        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return result;
    }
	
	public double[][] addMatrix(double[][] A, double[][] B) {
		int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bColumns || aRows != bRows) {
            throw new IllegalArgumentException("Unequal array dimensions error: arrays must the same dimension.");
        }
        
		double[][] result = new double[aRows][aColumns];
		
		for (int i=0; i<aRows; i++) {
			for (int j=0; j<aColumns; j++) {
				result[i][j] = A[i][j] + B[i][j];
			}
		}
		
		return result;
	}
	
	protected double[][] subtractMatrix(double[][] A, double[][] B) {
		int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bColumns || aRows != bRows) {
            throw new IllegalArgumentException("Unequal array dimensions error: arrays must the same dimension.");
        }
        
		double[][] result = new double[aRows][aColumns];
		
		for (int i=0; i<aRows; i++) {
			for (int j=0; j<aColumns; j++) {
				result[i][j] = A[i][j] - B[i][j];
			}
		}
		
		return result;			
	}
		
	protected double[][] transposeMatrix(double[][] array) {
		double[][] result = new double[array[0].length][array.length];
		for (int i=0; i<array[0].length; i++) {
			for (int j=0; j<array.length; j++) {
				result[i][j] = array[j][i];
			}
		}
		
		return result;
	}
	
	protected double[][] inverseMatrix(double[][] array) {
		double[][] result = new double[array.length][array[0].length];
        
		int n = array.length;
        double b[][] = new double[n][n];
        int index[] = new int[n];
        
        for (int i=0; i<n; ++i) {
            b[i][i] = 1;
        }
        
        gaussian(array, index);
        
        for (int i=0; i<n-1; i++) {
        	for (int j=i+1; j<n; j++) {
        		for (int k=0; k<n; k++) {
        			b[index[j]][k] -= array[index[j]][i]*b[index[i]][k];
        		}
        	}
        }
        
        for (int i=0; i<n; ++i) {
            result[n-1][i] = b[index[n-1]][i]/array[index[n-1]][n-1];
            for (int j=n-2; j>=0; --j) {
                result[j][i] = b[index[j]][i];
                for (int k=j+1; k<n; ++k){
                    result[j][i] -= array[index[j]][k]*result[k][i];
                }
                result[j][i] /= array[index[j]][j];
            }
        }
        
        return result;
		
	}
	
	protected static void gaussian(double[][] a, int index[]) {
		int n = index.length;
        double c[] = new double[n];
 
        // Initialize the index
        for (int i=0; i<n; ++i) {
            index[i] = i;
        }
        // Find the rescaling factors, one from each row
        for (int i=0; i<n; ++i) {
            double c1 = 0;
            for (int j=0; j<n; ++j) {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }
 
        // Search the pivoting element from each column
        int k = 0;
        for (int j=0; j<n-1; ++j) {
            double pi1 = 0;
            for (int i=j; i<n; ++i) {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) {
                    pi1 = pi0;
                    k = i;
                }
            }
 
            // Interchange rows according to the pivoting order
            int itmp = index[j];
            index[j] = index[k];
            index[k] = itmp;
            for (int i=j+1; i<n; ++i) {
                double pj = a[index[i]][j]/a[index[j]][j];
 
                // Record pivoting ratios below the diagonal
                a[index[i]][j] = pj;
 
                // Modify other elements accordingly
                for (int l=j+1; l<n; ++l) {
                    a[index[i]][l] -= pj*a[index[j]][l];
                }
            }
        }
	}
		
}
