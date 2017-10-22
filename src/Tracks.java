import org.opencv.core.*;

public class Tracks {
	
	public int id;
	public Rect bbox;
	public double[] centroid;
	public KalmanFilter kalmanFilter;
	public int age;
	public int totalVisibleCount;
	public int consecInvisibleCount;
		
	public Tracks(int id, Rect bbox, double[] centroid ) { 
		this.id = id;
		this.bbox = bbox;
		this.centroid = centroid;
		
		this.kalmanFilter = new KalmanFilter(centroid);
		this.age = 1;
		this.totalVisibleCount = 1;
		this.consecInvisibleCount = 0;
		
	}
	
	public void increaseAge() {	
		age += 1;
	}
	
	public void increaseVisibleCount() {
		totalVisibleCount += 1;
		consecInvisibleCount = 0;
	}
	
	public void increaseInvisibleCount() {
		consecInvisibleCount += 1;
	}
	
	public double[] kalmanPredict() {
		double[] result = new double[2];
		result = kalmanFilter.predict(centroid);
		return result;
	}
	
	public void updateBoxCentroid(Rect inBbox, double[] inCentroid) {
		bbox = inBbox;
		centroid = inCentroid;
	}

}

