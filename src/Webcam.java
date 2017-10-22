/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.image.BufferedImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class Webcam {
    Mat mat = new Mat();
    ImageConverter cvt = new ImageConverter();
    VideoCapture cap;
    int width = 640, height = 480;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    Webcam() {
        cap = new VideoCapture(0);
        if (!cap.isOpened()) {
            System.out.println("Camera Error");
        } else {
            System.out.println("Camera OK?");
            cap.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, width);
            cap.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, height);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        cap.read(mat);
        System.out.println("width, height = "+mat.cols()+", "+mat.rows());
    }   
    
    public BufferedImage getOneFrame(){
        mat = getNewImage(true);
        return cvt.mat2image(mat);        
    }
    
    public Mat getOneMatFrame(){
        mat = getNewImage(false);
        return mat;        
    }
    
    Mat tmp = new Mat();
    
    public Mat getNewImage(boolean bgr2rgb){
    	cap.read(mat); 
    	if (bgr2rgb)
    		Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2BGR);
    	return mat;
    }   
}