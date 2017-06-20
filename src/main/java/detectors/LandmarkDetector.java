package detectors;

import exceptions.LandmarkException;
import globals.Globals;
import org.bytedeco.javacpp.flandmark;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.CV_32FC3;
import static org.bytedeco.javacpp.opencv_core.cvRelease;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FILLED;

/**
 * Created by erd_9 on 12.06.2017.
 */
public class LandmarkDetector {
    private Rect faceDetectionRect = null;
    private int[] bbox = null;
    private int[] tempBBox = null;
    private File flandmarkModelFile = null;
    private flandmark.FLANDMARK_Model flandmarkModel = null;
    private double[] landmarks = null;
    private double[] tempLandmarks = null;
    private double[] bb = null;
    private double[] tempBB = null;
    private Mat originalFrame = null, grayFrame = null, hsvFrame = null, ycrcbFrame = null;
    private opencv_core.IplImage grayFrameIplImage = null;
    private Point[] faceBoundaryPoints = null, tempFaceBoundaryPoints = null;

    public LandmarkDetector(File file, Rect rect) {
        faceDetectionRect = rect;
        flandmarkModelFile = file;

        bb = new double[4];
        tempBB = new double[4];
        bbox = new int[4];
        tempBBox = new int[4];
        grayFrame = new Mat();
        hsvFrame = new Mat();
        tempLandmarks = new double[16];
        landmarks = new double[16];
        faceBoundaryPoints = new Point[4];
        tempFaceBoundaryPoints = new Point[4];
        ycrcbFrame = new Mat();

        //Flandmark Model
        //(10,11) (2,3)      (4,5) (12,13)
        //
        //             (0,1)
        //            (14,15)
        //       (6,7)       (8,9)
    }

    public void Initialize(Rect rect) {
        faceDetectionRect = rect;
    }

    public void Start(Mat originalFrame) throws LandmarkException {
        try {
            if (faceDetectionRect != null) {

                if (originalFrame != null && !originalFrame.empty()) {
                    this.originalFrame = originalFrame;
                    //originalFrame = new Mat(originalFrame, new Rect(faceDetectionRect.x, faceDetectionRect.y, faceDetectionRect.width, faceDetectionRect.height));

                    originalFrame = HistEq(originalFrame);

                    Imgproc.cvtColor(originalFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

                    originalFrame.convertTo(hsvFrame, CV_32FC3);
                    Imgproc.cvtColor(originalFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);
                    Core.normalize(hsvFrame, hsvFrame, 0.0d, 255.0d, Core.NORM_MINMAX, CV_32FC3);

                    Imgproc.cvtColor(originalFrame, ycrcbFrame, Imgproc.COLOR_BGR2YCrCb);
                } else
                    throw new LandmarkException("EXP: Frame Is Empty");

                if (flandmarkModelFile != null && flandmarkModelFile.exists()) {
                    flandmarkModel = flandmark.flandmark_init(flandmarkModelFile.getAbsolutePath());

                    if (flandmarkModel != null) {
                        landmarks = new double[2 * flandmarkModel.data().options().M()];

                        bbox[0] = faceDetectionRect.x;
                        bbox[1] = faceDetectionRect.y;
                        bbox[2] = faceDetectionRect.width + faceDetectionRect.x;
                        bbox[3] = faceDetectionRect.height + faceDetectionRect.y;

                        Globals.headSize = bbox[3] - bbox[1];

                        //System.out.println("RECT [X: " + bbox[0] + ", Y: " + bbox[1] + ", Width: " + (bbox[2] - bbox[0]) + ", Height: " + (bbox[3] - bbox[1]) + "]");

                        BufferedImage bufferedImage = new BufferedImage(grayFrame.width(), grayFrame.height(), BufferedImage.TYPE_BYTE_GRAY);
                        byte[] data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
                        grayFrame.get(0, 0, data);
                        OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
                        Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
                        grayFrameIplImage = iplConverter.convert(java2DFrameConverter.convert(bufferedImage));

                        if (flandmark.flandmark_detect(grayFrameIplImage, bbox, flandmarkModel, landmarks) == 0) {
                            flandmarkModel.bb().get(bb);
                        }

                        FindFaceBoundaries();
                    } else
                        throw new LandmarkException("EXP: Flandmark Model Is NULL");
                } else
                    throw new LandmarkException("EXP: Flandmark Model File Not Found");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Mat HistEq(Mat frame){
        //EQUALIZE HIST
        Mat hsvFrame = new Mat();
        Mat newValueChannel = new Mat();
        List<Mat> channels = new ArrayList<>(3);

        Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV);
        Core.split(hsvFrame, channels);

        Imgproc.equalizeHist(channels.get(2), newValueChannel);
        channels.remove(2);
        channels.add(newValueChannel);

        Core.merge(channels, hsvFrame);
        Imgproc.cvtColor(hsvFrame, frame, Imgproc.COLOR_HSV2BGR);

        return frame;
    }

    private boolean TestBGR(double b, double g, double r) {
        boolean b1 = (r > 95) && (g > 40) && (b > 20) && ((Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) > 15) && (Math.abs(r - g) > 15) && (r > g) && (r > b);
        boolean b2 = (r > 220) && (g > 210) && (b > 170) && (Math.abs(r - g) <= 15) && (r > b) && (g > b);

        return b1 | b2;
    }

    private boolean TestYcrCb(double y, double cr, double cb) {
        boolean b1 = cr <= 1.5862 * cb + 20;
        boolean b2 = cr >= 0.3448 * cb + 76.2069;
        boolean b3 = cr >= -4.5652 * cb + 234.5652;
        boolean b4 = cr <= -1.15 * cb + 301.75;
        boolean b5 = cr <= -2.2857 * cb + 432.85;

        return b1 && b2 && b3 && b4 && b5;
    }

    private boolean TestHSV(double h, double s, double v) {
        return (h < 25) || (h > 230);
    }

    private void FindFaceBoundaries() {
        int startPixelX = bbox[0];
        int startPixelY = bbox[1] + (bbox[3] - bbox[1]) / 2;
        int endPixelX = bbox[2];
        int endPixelY = bbox[3];

        int pointIndex = 0;

        for (int x = startPixelX; x < endPixelX; x++) {
            double[] values = originalFrame.get(startPixelY, x);
            double[] values2 = ycrcbFrame.get(startPixelY, x);
            double[] values3 = hsvFrame.get(startPixelY, x);
            if (TestBGR(values[0], values[1], values[2])) {
                if (TestYcrCb(values2[0], values2[1], values2[2])) {
                    if (TestHSV(values3[0], values3[1], values3[2])) {
                        System.out.println("LEFTMOST SKIN PIXEL FOUND");

                        faceBoundaryPoints[pointIndex] = new Point(x, startPixelY);
                        pointIndex++;
                        break;
                    }
                }
            }
        }

        for (int x = endPixelX; x > startPixelX; x--) {
            double[] values = originalFrame.get(startPixelY, x);
            double[] values2 = ycrcbFrame.get(startPixelY, x);
            double[] values3 = hsvFrame.get(startPixelY, x);
            if (TestBGR(values[0], values[1], values[2])) {
                if (TestYcrCb(values2[0], values2[1], values2[2]) && TestHSV(values3[0], values3[1], values3[2])) {
                    System.out.println("RIGHTMOST SKIN PIXEL FOUND");

                    if(pointIndex == 1)
                        faceBoundaryPoints[pointIndex] = new Point(x, startPixelY);
                    pointIndex++;
                    break;
                }

            }
        }

        startPixelX = bbox[0] + (bbox[2] - bbox[0]) / 2;
        startPixelY = bbox[1];
        endPixelX = bbox[2];
        endPixelY = bbox[3];

        for (int y = startPixelY; y <= endPixelY; y++) {
            double[] values = originalFrame.get(y, startPixelX);
            double[] values2 = ycrcbFrame.get(y, startPixelX);
            double[] values3 = hsvFrame.get(y, startPixelX);
            if (TestBGR(values[0], values[1], values[2]) && TestYcrCb(values2[0], values2[1], values2[2])) {
                if (TestHSV(values3[0], values3[1], values3[2])) {
                    System.out.println("TOPMOST SKIN PIXEL FOUND");

                    if(pointIndex == 2)
                        faceBoundaryPoints[pointIndex] = new Point(startPixelX, y);
                    pointIndex++;
                    break;
                }

            }
        }

        for (int y = endPixelY; y > startPixelY; y--) {
            double[] values = originalFrame.get(y, startPixelX);
            double[] values2 = ycrcbFrame.get(y, startPixelX);
            double[] values3 = hsvFrame.get(y, startPixelX);
            if (TestBGR(values[0], values[1], values[2])) {
                if (TestYcrCb(values2[0], values2[1], values2[2])) {
                    if (TestHSV(values3[0], values3[1], values3[2])) {
                        System.out.println("BOTTOMMOST SKIN PIXEL FOUND");

                        if(pointIndex == 3)
                            faceBoundaryPoints[pointIndex] = new Point(startPixelX, y);
                        break;
                    }
                }
            }
        }
    }

    public void Stop() {
        if (grayFrameIplImage != null)
            cvRelease(grayFrameIplImage);

        if (flandmarkModel != null && grayFrameIplImage != null)
            flandmark.flandmark_free(flandmarkModel);
    }

    public Mat BeginDrawing() {
        try {
            if (bbox[0] > 0 && bbox[1] > 0 && bbox[2] > 0 && bbox[3] > 0 &&
                    bb[0] > 0 && bb[1] > 0 && bb[2] > 0 && bb[3] > 0 &&
                    faceDetectionRect != null) {

                originalFrame = HistEq(originalFrame);

                //System.out.println("TEMP RECT [X: " + tempBBox[0] + ", Y: " + tempBBox[1] + ", Width: " + (tempBBox[2] - tempBBox[0]) + ", Height: " + (tempBBox[3] - tempBBox[1]) + "]");
                //System.out.println("RECT [X: " + bbox[0] + ", Y: " + bbox[1] + ", Width: " + (bbox[2] - bbox[0]) + ", Height: " + (bbox[3] - bbox[1]) + "]");

                //originalFrame = new Mat(originalFrame, new Rect(faceDetectionRect.x, faceDetectionRect.y, faceDetectionRect.width, faceDetectionRect.height));

                if (Math.abs(tempBBox[0] - bbox[0]) > 7 || Math.abs(tempBBox[1] - bbox[1]) > 7) {
                    //Imgproc.rectangle(originalFrame, new Point(bbox[0], bbox[1]), new Point(bbox[2], bbox[3]), new Scalar(255, 0, 0));
                    Imgproc.rectangle(originalFrame, new Point((int) bb[0], (int) bb[1]), new Point((int) bb[2], (int) bb[3]), new Scalar(0, 0, 255));
                    Imgproc.circle(originalFrame, new Point((int) landmarks[0], (int) landmarks[1]), 3, new Scalar(0, 0, 255), CV_FILLED, 8, 0);

                    for (int i = 2; i < landmarks.length; i += 2) {
                        if (i < 16)
                            Imgproc.circle(originalFrame, new Point((int) landmarks[i], (int) landmarks[i + 1]), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);
                    }

                    //CHIN
                    Imgproc.circle(originalFrame, new Point(bbox[0] + ((bbox[2] - bbox[0]) / 2), bbox[3]), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);

                    //EYEBROWS
                    double diffBetweenEyes = landmarks[4] - landmarks[2];
                    double heightOfEyeBrows = diffBetweenEyes / 2.05834829443d;
                    int eyebrowLeftX = (int) (landmarks[10] + (landmarks[2] - landmarks[10]) / 2);
                    int eyebrowLeftY = (int) (landmarks[3] - heightOfEyeBrows);
                    int eyebrowRightX = (int) (landmarks[4] + (landmarks[12] - landmarks[4]) / 2);
                    int eyebrowRightY = (int) (landmarks[5] - heightOfEyeBrows);
                    Imgproc.circle(originalFrame, new Point(eyebrowLeftX, eyebrowLeftY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);
                    Imgproc.circle(originalFrame, new Point(eyebrowRightX, eyebrowRightY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);

                    //UPPER & LOWER LIPS
                    int upperLipY = (int) (landmarks[15] + 24 * Globals.headSize / 180);
                    int lowerLipY = (int) (bbox[3] - (24 * Globals.headSize / 180));
                    Imgproc.circle(originalFrame, new Point(bbox[0] + (bbox[2] - bbox[0]) / 2, upperLipY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);
                    Imgproc.circle(originalFrame, new Point(bbox[0] + (bbox[2] - bbox[0]) / 2, lowerLipY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);

                    //FACE BOUNDARIES
                    if (faceBoundaryPoints[0] != null && faceBoundaryPoints[1] != null && faceBoundaryPoints[2] != null && faceBoundaryPoints[3] != null) {
                        Imgproc.circle(originalFrame, faceBoundaryPoints[0], 3, new Scalar(255, 255, 0), CV_FILLED, 8, 0);
                        Imgproc.circle(originalFrame, faceBoundaryPoints[1], 3, new Scalar(255, 255, 0), CV_FILLED, 8, 0);
                        Imgproc.circle(originalFrame, faceBoundaryPoints[2], 3, new Scalar(255, 255, 0), CV_FILLED, 8, 0);
                    }

                    tempBBox[0] = bbox[0];
                    tempBBox[1] = bbox[1];
                    tempBBox[2] = bbox[2];
                    tempBBox[3] = bbox[3];
                    tempBB[0] = bb[0];
                    tempBB[1] = bb[1];
                    tempBB[2] = bb[2];
                    tempBB[3] = bb[3];
                    tempFaceBoundaryPoints[0] = faceBoundaryPoints[0];
                    tempFaceBoundaryPoints[1] = faceBoundaryPoints[1];
                    tempFaceBoundaryPoints[2] = faceBoundaryPoints[2];
                    tempFaceBoundaryPoints[3] = faceBoundaryPoints[3];
                    if (tempLandmarks.length < landmarks.length)
                        tempLandmarks = new double[landmarks.length];
                    System.arraycopy(landmarks, 0, tempLandmarks, 0, landmarks.length);
                } else {
                    //Imgproc.rectangle(originalFrame, new Point(bbox[0], bbox[1]), new Point(bbox[2], bbox[3]), new Scalar(255, 0, 0));
                    Imgproc.rectangle(originalFrame, new Point((int) tempBB[0], (int) tempBB[1]), new Point((int) tempBB[2], (int) tempBB[3]), new Scalar(0, 0, 255));
                    Imgproc.circle(originalFrame, new Point((int) tempLandmarks[0], (int) tempLandmarks[1]), 3, new Scalar(0, 0, 255), CV_FILLED, 8, 0);

                    for (int i = 2; i < tempLandmarks.length; i += 2) {
                        if (i < 16)
                            Imgproc.circle(originalFrame, new Point((int) tempLandmarks[i], (int) tempLandmarks[i + 1]), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);
                    }

                    //CHIN
                    Imgproc.circle(originalFrame, new Point(tempBBox[0] + ((tempBBox[2] - tempBBox[0]) / 2), tempBBox[3]), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);

                    //EYEBROWS
                    double diffBetweenEyes = tempLandmarks[4] - tempLandmarks[2];
                    double heightOfEyeBrows = diffBetweenEyes / 2.05834829443d;
                    int eyebrowLeftX = (int) (tempLandmarks[10] + (tempLandmarks[2] - tempLandmarks[10]) / 2);
                    int eyebrowLeftY = (int) (tempLandmarks[3] - heightOfEyeBrows);
                    int eyebrowRightX = (int) (tempLandmarks[4] + (tempLandmarks[12] - tempLandmarks[4]) / 2);
                    int eyebrowRightY = (int) (tempLandmarks[5] - heightOfEyeBrows);
                    Imgproc.circle(originalFrame, new Point(eyebrowLeftX, eyebrowLeftY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);
                    Imgproc.circle(originalFrame, new Point(eyebrowRightX, eyebrowRightY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);

                    //UPPER & LOWER LIPS
                    int upperLipY = (int) (tempLandmarks[15] + 24 * Globals.headSize / 180);
                    int lowerLipY = (int) (tempBBox[3] - (24 * Globals.headSize / 180));
                    Imgproc.circle(originalFrame, new Point(tempBBox[0] + (tempBBox[2] - tempBBox[0]) / 2, upperLipY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);
                    Imgproc.circle(originalFrame, new Point(tempBBox[0] + (tempBBox[2] - tempBBox[0]) / 2, lowerLipY), 3, new Scalar(255, 0, 0), CV_FILLED, 8, 0);

                    //FACE BOUNDARIES
                    if (tempFaceBoundaryPoints[0] != null && tempFaceBoundaryPoints[1] != null && tempFaceBoundaryPoints[2] != null && tempFaceBoundaryPoints[3] != null) {
                        Imgproc.circle(originalFrame, tempFaceBoundaryPoints[0], 3, new Scalar(255, 255, 0), CV_FILLED, 8, 0);
                        Imgproc.circle(originalFrame, tempFaceBoundaryPoints[1], 3, new Scalar(255, 255, 0), CV_FILLED, 8, 0);
                        Imgproc.circle(originalFrame, tempFaceBoundaryPoints[2], 3, new Scalar(255, 255, 0), CV_FILLED, 8, 0);
                    }
                }

                return originalFrame;
            } else
                return null;
        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }
    }

    public Mat BeginDrawing(Mat frame) {
        originalFrame = frame;
        originalFrame = BeginDrawing();
        return originalFrame;
    }
}
