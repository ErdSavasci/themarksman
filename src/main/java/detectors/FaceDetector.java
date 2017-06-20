package detectors;

import exceptions.FaceDetectorException;
import globals.Globals;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;

/**
 * Created by erd_9 on 12.06.2017.
 */
public class FaceDetector {
    private MatOfRect faceDetections = null;
    private File lbpCascadeFile = null;
    private Mat grayFrame = null;

    public FaceDetector(File file){
        faceDetections = new MatOfRect();
        lbpCascadeFile = file;
        grayFrame = new Mat();
    }

    public Rect Start(Mat originalFrame) throws FaceDetectorException{
        try{
            if(originalFrame != null && !originalFrame.empty())
                Imgproc.cvtColor(originalFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            else
                throw new FaceDetectorException("EXP: Frame is empty");

            if(lbpCascadeFile != null && lbpCascadeFile.exists()){
                CascadeClassifier faceDetector = new CascadeClassifier(lbpCascadeFile.getAbsolutePath());
                faceDetector.detectMultiScale(grayFrame, faceDetections, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE, new Size(Globals.absoluteFaceSize, Globals.absoluteFaceSize), new Size());
                System.out.println(String.format("Detected %s face(s)", faceDetections.toArray().length));

                if(faceDetections != null && faceDetections.toArray().length > 0){
                    Rect firstRect = faceDetections.toArray()[0];
                    int index = 1;

                    while (index < faceDetections.toArray().length) {
                        Rect rect = faceDetections.toArray()[index];
                        if (rect.width > firstRect.width && rect.height > firstRect.height) {
                            firstRect = rect;
                        }
                        index++;
                    }

                    return firstRect;
                }
                else
                    return null;
            }
            else {
                throw new FaceDetectorException("EXP: LBPCascade File is NULL");
            }
        }
        catch(Exception ex){
            ex.printStackTrace();

            return null;
        }
    }
}
