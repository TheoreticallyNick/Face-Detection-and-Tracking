import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.Converters;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

public class FaceTrackMain extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	public BufferedImage image, fgMask;
	public Webcam webcam = new Webcam();
	ImageConverter cvt1 = new ImageConverter();	
	ImageConverter cvt3 = new ImageConverter();
	BackgroundSubtractorMOG2 fgbg =Video.createBackgroundSubtractorMOG2();
	Mat mask = new Mat(), bg = new Mat();
	Mat getMask(Mat mat){
		fgbg.apply(mat, mask);
		return mask;
	}
	int border = 5;
	
	public static Tracks[] tracks;
	public static int nextId = 1;
	public static Rect[] eyeBoxArray;
	public static double[][] centroidArray;
	public static int[][] assignments;
	public static int[] unassignedTracks;
	public static int[] unassignedDetections;
	public static int maxInvisibleCount = 5;
	
	public static String xmlFile = "C:\\Users\\NickTheoret\\Desktop\\opencv\\build\\etc\\haarcascades\\haarcascade_frontalface_default.xml";
	
	static { 
		try {
		System.load("C:\\Users\\NickTheoret\\Desktop\\opencv\\build\\x64\\vc14\\bin\\opencv_ffmpeg320_64.dll");
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Native code library failed to load.\n" + e);
			System.exit(1);
		}
	}
	
	public FaceTrackMain() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 642, 482);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
        new CascadeRunner().start();	
	}
	
	public static void main(String args[]) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		CascadeClassifier faceCascade = new CascadeClassifier(xmlFile);
		
		if(!faceCascade.load(xmlFile)) {
			System.out.println("Error Loading XML File");
		} else {
			System.out.println("Success Loading XML");
		}
		//invokeLater enables *swing threads to operate, 
		//it is not actually going to run this method later
		EventQueue.invokeLater(new Runnable() { 
			public void run() {
				try {
					FaceTrackMain frame = new FaceTrackMain();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public class CascadeRunner extends Thread {
		public void run() {
			while (contentPane == null || contentPane.getGraphics() == null) {
				try {Thread.sleep(1000);}
				catch (InterruptedException ex) {}
			}
			
			CascadeClassifier faceCascade = new CascadeClassifier(xmlFile);
				
        	Mat mat = new Mat();
        	Mat mat_gray = new Mat();
			while (true) {	
				//pull an image from the camera
				mat = webcam.getOneMatFrame();
				if (image != null)
					//if image is present, detect multiscale and begin the tracking thread
					Imgproc.cvtColor(mat, mat_gray, Imgproc.COLOR_BGRA2GRAY);
				
					Thread trackRunner = new Thread(new TrackRunner(mat));

		        	MatOfRect eyeBoxesMat = new MatOfRect();
		        	
		        	faceCascade.detectMultiScale(mat_gray, eyeBoxesMat);
		        	
		        	//get carBoxArray and centroid data
		        	eyeBoxArray = eyeBoxesMat.toArray();
		        	centroidArray = getCentroids(eyeBoxArray);
		        	
		        	if (trackRunner.isAlive()) {
		        		try {
							trackRunner.join();
							trackRunner.start();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
		        		
		        	} else {
		        		trackRunner.start();
		        	}
		        	
				try {
					Thread.sleep(0);
				} catch (InterruptedException ex) {
				}
			}
		}
	}
	
	public class TrackRunner extends Thread {
		private Mat mat;
		public TrackRunner(Mat mat) {
			this.mat = mat;
		}
		public void run() {
			
			predictNewLocationsOfTracks();
        	assignDetectionsToTracks();
        	
        	updateAssignedTracks();
        	updateUnassignedTracks();
        	deleteLostTracks();
        	createNewTracks();
        	
        	mat = displayResults(mat);
        	image = Mat2BufferedImage(mat);
        	
        	Graphics g = contentPane.getGraphics();	
        	
    		//image = cvt3.mat2image(mat);
    		if (image != null)
    			g.drawImage(image, 0,0, rootPane);
		}
		
	}
	
	@Override
    public void paint(Graphics g) {
        super.paint(g);
        g = contentPane.getGraphics();

        if (image!=null){
            setSize(image.getWidth()+5, image.getHeight()+5);
            g.drawImage(image, 0, 0, rootPane);
        }
	}
	
	public static void predictNewLocationsOfTracks() {
		if (tracks == null || tracks.length == 0) {
			return;
		}
		
		for (int i=0; i<tracks.length; i++) {
			Rect bbox = tracks[i].bbox;
			
			double[] predictedCentroid = tracks[i].kalmanPredict();
			
			tracks[i].bbox.x = (int) predictedCentroid[0] - bbox.width/2;
			tracks[i].bbox.y = (int) predictedCentroid[1] - bbox.height/2;			
		}
	}
	
	public static void assignDetectionsToTracks(){
		if (tracks == null || tracks.length == 0) {
			unassignedDetections = new int[centroidArray.length];
			for (int i=0; i<centroidArray.length; i++) {
				unassignedDetections[i] = i;
			}
			return;
		} else {
		
			double[][] costMatrix = new double[tracks.length][centroidArray.length];
			
			for (int i=0; i<costMatrix.length; i++) {
				for (int j=0; j<costMatrix[0].length; j++) {
					double a = tracks[i].kalmanPredict()[0] - centroidArray[j][0];
					double b = tracks[i].kalmanPredict()[1] - centroidArray[j][1];
					costMatrix[i][j] = Math.sqrt(Math.pow(a,2) + Math.pow(b, 2));
				}
			}
			int costOfNonAssignment = 20;
			
			CVHungarianAssignment assignTracksToDetections = new CVHungarianAssignment(costMatrix, costOfNonAssignment);
			assignTracksToDetections.solveCost();
			assignments = assignTracksToDetections.assignments;
			unassignedTracks = assignTracksToDetections.unassignedTracks;
			unassignedDetections = assignTracksToDetections.unassignedDetections;
		}
		
	}
	
	public static void updateUnassignedTracks() {
		if (tracks == null || tracks.length == 0) {
			return;
		} else if (unassignedTracks == null) {
			return;
		}
		for (int i=0; i<unassignedTracks.length; i++) {
			tracks[unassignedTracks[i]].increaseInvisibleCount();
			tracks[unassignedTracks[i]].increaseAge();
		}
	}
	
	public static void deleteLostTracks() {
		if (tracks == null || tracks.length == 0) {
			return;
		}
		Tracks[] results = new Tracks[0];
		for (int i=0; i<tracks.length; i++) {
			if (tracks[i].consecInvisibleCount > maxInvisibleCount) {
				continue;
			} else {
				results = appendArray(results, tracks[i]);
			}
		}
		tracks = results;
	}
	
	public static void updateAssignedTracks() {
		if (assignments == null) {
			return;
		}
		for (int i=0; i<assignments.length; i++) {
			tracks[assignments[i][0]].updateBoxCentroid(eyeBoxArray[assignments[i][1]], centroidArray[assignments[i][1]]);
			tracks[assignments[i][0]].increaseAge();
			tracks[assignments[i][0]].increaseVisibleCount();
		}
	}
	
	public static void createNewTracks() {
		for (int i=0; i<unassignedDetections.length; i++) {
			Tracks newTrack = new Tracks(nextId,eyeBoxArray[unassignedDetections[i]],centroidArray[unassignedDetections[i]]);
			nextId++;
			tracks = appendTracksArray(newTrack);
		}
		
	}
	
	public Mat displayResults(Mat frame) {
		//draw boxes and centroids
	    if (tracks != null) {	
			for (int i=0; i<tracks.length; i++) {
	    		if (tracks[i].totalVisibleCount > 20) {
	    			Imgproc.rectangle(frame, new Point(tracks[i].bbox.x, tracks[i].bbox.y), 
	    					new Point(tracks[i].bbox.x + tracks[i].bbox.width, tracks[i].bbox.y + tracks[i].bbox.height), new Scalar(255, 0, 0));
	    			Imgproc.putText(frame, Integer.toString(tracks[i].id), 
	    					new Point(tracks[i].bbox.x, tracks[i].bbox.y), 
	    					Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(255,0,0));
	    			Imgproc.circle(frame, new Point(tracks[i].kalmanPredict()[0], tracks[i].kalmanPredict()[1]), 4, new Scalar(255,100,100));
	    		}
	    	}
	    }
	    
	    return frame;
	}
	
	public static int[] Mat2IntArray(Mat matData) {
		
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		Converters.Mat_to_vector_int(matData, list);
		
		int size = list.size();
		int[] result = new int[size];
		Integer[] temp = list.toArray(new Integer[size]);
		for (int n = 0; n < size; ++n) {
		    result[n] = temp[n];
		}
		return result;
	}
	
	public static double[][] getCentroids(Rect[] rectangleArray) {
		
		double[][] results = new double[rectangleArray.length][2];
		
		for (int i=0; i<rectangleArray.length; i++) {
				results[i][0] = rectangleArray[i].x + rectangleArray[i].width/2;
				results[i][1] = rectangleArray[i].y + rectangleArray[i].height/2;				
			}
		
		return results;
	}
	
	public static Tracks[] appendTracksArray(Tracks newTrack) {
		if (tracks != null) {
			Tracks[] result = new Tracks[tracks.length + 1];
			for (int i=0; i<tracks.length; i++) {
				result[i] = tracks[i];
			}
			
			result[tracks.length] = newTrack;
			return result;
		} else {
			Tracks[] result = new Tracks[1];
			result[0] = newTrack;
			return result;
		}
		
	}

	public static BufferedImage Mat2BufferedImage(Mat m) {
		//Method converts a Mat to a Buffered Image
		int type = BufferedImage.TYPE_BYTE_GRAY;
	     if ( m.channels() > 1 ) {
	         type = BufferedImage.TYPE_3BYTE_BGR;
	     }
	     int bufferSize = m.channels()*m.cols()*m.rows();
	     byte [] b = new byte[bufferSize];
	     m.get(0,0,b); // get all the pixels
	     BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
	     final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	     System.arraycopy(b, 0, targetPixels, 0, b.length);  
	     return image;
	}
	
	public static Tracks[] appendArray(Tracks[] array, Tracks track) {
			Tracks[] result = new Tracks[array.length + 1];
			for (int i=0; i<array.length; i++) {
				result[i] = array[i];
			}
			result[result.length - 1] = track;
			return result;
	}
}

