/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.image.BufferedImage;
import org.opencv.core.Core;
import org.opencv.core.Mat;

/**
 *
 * @author Huiying Shen
 */
public class ImageConverter {
    Mat mat;
    public BufferedImage image;
    byte[] data;
    int nByte = 3;

    public BufferedImage mat2image(Mat mat){
        if (mat.channels()!=3  && mat.channels()!=1) return null;
        nByte = mat.channels();
        allocateSpace(mat);
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, mat.cols(),mat.rows(), data);
        return image;
    }
            
	void allocateSpace(Mat mat) {
		int w = mat.cols(), h = mat.rows();
		if (data == null || data.length != w * h * nByte)
			data = new byte[w * h * nByte];

		if (image == null || image.getWidth() != w || image.getHeight() != h) {

			if (nByte == 3)
				image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			if (nByte == 1)
				image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		}
	}

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
    
}
