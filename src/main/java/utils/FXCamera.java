package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import detectors.FaceDetector;
import detectors.LandmarkDetector;
import exceptions.FaceDetectorException;
import exceptions.LandmarkException;
import globals.Globals;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.image.*;
import org.bytedeco.javacpp.flandmark;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by erd_9 on 5.06.2017.
 */
public class FXCamera {
    private VideoCapture videoCapture = null;
    private ListView<String> availableCamerasListView = null;
    private int upperBoundGrabHist = 15;
    private int lowerBoundGrabHist = 0;
    private int frameCount = 0;
    private boolean once = true;
    private AreaChart<Number, Number> areaChart = null;
    private ScheduledExecutorService timer = null;
    private ImageView cameraImageView = null;
    private TextArea shellTextArea = null;
    private File tempFlandmarkModelFile = null;
    private File tempLbpCascadeFile = null;
    private FaceDetector faceDetector = null;
    private LandmarkDetector landmarkDetector = null;
    private Rect faceDetectionRect = null;

    public FXCamera(ListView<String> listview, AreaChart<Number, Number> areaChart, ImageView imageView, TextArea textArea){
        videoCapture = new VideoCapture();

        availableCamerasListView = listview;
        this.areaChart = areaChart;
        cameraImageView = imageView;
        shellTextArea = textArea;
    }

    public void startCamera(){
        int selectedCameraIndex = 0;
        int cameraIndex = 0;
        double maxWidth = 0.0d;
        double maxHeight = 0.0d;

        if(availableCamerasListView.getSelectionModel().getSelectedIndex() >= 0){
            videoCapture.open(availableCamerasListView.getSelectionModel().getSelectedIndex());
            selectedCameraIndex = availableCamerasListView.getSelectionModel().getSelectedIndex();
        }
        else{
            videoCapture.open(0);
        }

        for(Webcam webcam : Webcam.getWebcams()){
            if(cameraIndex == selectedCameraIndex){
                for(Dimension dim : webcam.getDevice().getResolutions()){
                    if(dim.getWidth() > maxWidth){
                        maxWidth = dim.getWidth();
                        maxHeight = dim.getHeight();
                    }
                }
            }
        }

        try {
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 640);
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 480);
        }
        catch(Exception ex){
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, WebcamResolution.QXGA.getSize().getWidth());
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, WebcamResolution.QXGA.getSize().getHeight());
        }

        Runnable frameGrabberRunnable = () -> {
            javafx.scene.image.Image frame = processFrame();

            Platform.runLater(() -> cameraImageView.setImage(frame));

            if(once) {
                shellTextArea.appendText(System.getProperty("line.separator") + "Scanning Face..");
                once = false;
            }

        };

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(frameGrabberRunnable, 0, 33, TimeUnit.MILLISECONDS);
    }

    public void stopCamera(){
        if(videoCapture.isOpened()){
            if(timer != null)
                timer.shutdown();

            if(videoCapture != null)
                videoCapture.release();

            if(landmarkDetector != null)
                landmarkDetector.Stop();
        }
    }

    private void saveFrameToFile(javafx.scene.image.Image frame){
        try{
            File outputImageLoc = File.createTempFile("frame", ".png");
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(frame, null);

            ImageIO.write(bufferedImage, "png", outputImageLoc);
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public boolean isOpened(){
        return videoCapture.isOpened();
    }

    public void read(Mat frame){
        videoCapture.read(frame);
    }

    public Dimension getResolution(){
        Dimension dimension = new Dimension();
        dimension.setSize(videoCapture.get(Videoio.CV_CAP_PROP_FRAME_WIDTH), videoCapture.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT));

        return dimension;
    }

    private File copyFileToExternalLocation(File toBeCopiedFile, String fileName){
        try{
            if(fileName.equals("flandmark")){
                FileInputStream fileInputStream = new FileInputStream(toBeCopiedFile.getAbsolutePath());
                File tempModelFile = File.createTempFile("flandmark_model", ".dat");
                FileOutputStream fileOutputStream = new FileOutputStream(tempModelFile.getAbsolutePath());
                if(tempModelFile.exists()) {
                    byte []b = new byte[1024];
                    int noOfBytes = 0;
                    while ((noOfBytes = fileInputStream.read(b)) != -1) {
                        fileOutputStream.write(b, 0, noOfBytes);
                    }
                    fileInputStream.close();
                    fileOutputStream.close();

                    Globals.flandmarkModelTempFileName = tempModelFile.getAbsolutePath();
                }

                return tempModelFile;
            }
            else if(fileName.equals("lbpcascade")){
                FileInputStream fileInputStream = new FileInputStream(toBeCopiedFile.getAbsolutePath());
                File tempHaarCascadeFile = File.createTempFile("lbpcascade_frontalface", ".xml");
                FileOutputStream fileOutputStream = new FileOutputStream(tempHaarCascadeFile.getAbsolutePath());
                if(tempHaarCascadeFile.exists()) {
                    byte []b = new byte[1024];
                    int noOfBytes = 0;
                    while ((noOfBytes = fileInputStream.read(b)) != -1) {
                        fileOutputStream.write(b, 0, noOfBytes);
                    }
                    fileInputStream.close();
                    fileOutputStream.close();

                    Globals.haarCascadeTempFileName = tempHaarCascadeFile.getAbsolutePath();
                }

                return tempHaarCascadeFile;
            }
            else
                return null;
        }
        catch(IOException ex){
            ex.printStackTrace();

            return null;
        }
    }

    private javafx.scene.image.Image processFrame(){
        Mat originalFrame = new Mat();
        Mat modifiedOriginalFrame = new Mat();
        Mat histFrame = new Mat();
        Mat hsvFrame = new Mat();
        Mat grayFrame = new Mat();
        Mat croppedOriginalFrame = new Mat();
        java.util.List<Mat> frameList = new ArrayList<>();
        frameList.add(hsvFrame);

        //B(lue) G(reen) R(ed) model is used for color images
        if(videoCapture.isOpened()){
            videoCapture.read(originalFrame);

            //DRAW HISTOGRAM
            Imgproc.cvtColor(originalFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);
            drawHistogram(hsvFrame, frameList);

            //PROCESS
            if(frameCount % 3 == 0){
                if(Globals.flandmarkModelTempFileName == null){
                    try{
                        File landmarkModelFile = new File(getClass().getClassLoader().getResource("data/flandmark_model.dat").toURI());
                        tempFlandmarkModelFile = copyFileToExternalLocation(landmarkModelFile, "flandmark");
                    }
                    catch(URISyntaxException ex){
                        ex.printStackTrace();
                    }
                }
                if(Globals.haarCascadeTempFileName == null){
                    try{
                        File lbpcascadeFile = new File(getClass().getClassLoader().getResource("lbpcascades/lbpcascade_frontalface.xml").toURI());
                        tempLbpCascadeFile = copyFileToExternalLocation(lbpcascadeFile, "lbpcascade");
                    }
                    catch(URISyntaxException ex){
                        ex.printStackTrace();
                    }
                }

                if(Globals.absoluteFaceSize == 0){
                    int height = originalFrame.rows();
                    if(Math.round(height * 0.2f) > 0)
                        Globals.absoluteFaceSize = Math.round(height * 0.2f);
                }

                faceDetector = new FaceDetector(tempLbpCascadeFile);
                try {
                    faceDetectionRect = faceDetector.Start(originalFrame);
                    if(once)
                        landmarkDetector = new LandmarkDetector(tempFlandmarkModelFile, faceDetectionRect);
                    else
                        landmarkDetector.Initialize(faceDetectionRect);

                    try{
                        landmarkDetector.Start(originalFrame);
                        modifiedOriginalFrame = landmarkDetector.BeginDrawing();
                    }
                    catch(LandmarkException ex){
                        ex.printStackTrace();
                    }
                }
                catch(FaceDetectorException ex){
                    ex.printStackTrace();
                }
            }
            else{
                landmarkDetector.Initialize(faceDetectionRect);
                modifiedOriginalFrame = landmarkDetector.BeginDrawing(originalFrame);
            }

            frameCount++;

            //ROTATE
            if(modifiedOriginalFrame != null && !modifiedOriginalFrame.empty()) {
                Core.flip(modifiedOriginalFrame, modifiedOriginalFrame, 1);
                return convertMatToWritableImage(modifiedOriginalFrame);
            }
            else if(!originalFrame.empty()){
                Core.flip(originalFrame, originalFrame, 1);
                return convertMatToWritableImage(originalFrame);
            }
            else{
                return null;
            }
        }
        else {
            startCamera();

            if(videoCapture.isOpened()){
                //DRAW HISTOGRAM
                Imgproc.cvtColor(originalFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);
                drawHistogram(hsvFrame, frameList);

                //ROTATE
                Core.flip(originalFrame, originalFrame, 1);
                return convertMatToWritableImage(originalFrame);
            }
            else{
                try{
                    javafx.scene.image.Image image = new javafx.scene.image.Image(getClass().getClassLoader().getResource("images/no_signal.jpg").toExternalForm());
                    return image;
                }
                catch(Exception ex){
                    return null;
                }
            }
        }
    }

    private javafx.scene.image.Image convertMatToImage(Mat frame){
        if(!frame.empty()){
            byte[] bytes = new byte[frame.channels() * frame.width() * frame.height()];
            frame.get(0, 0, bytes);
            BufferedImage bufferedImage = new BufferedImage(frame.cols(), frame.width(), frame.channels() > 1 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY);
            final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
            System.arraycopy(bytes, 0, targetPixels, 0, bytes.length);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        }
        else
            return null;
    }

    private WritableImage convertMatToWritableImage(Mat frame){
        if(frame != null && !frame.empty()){
            try{
                MatOfByte matOfByte = new MatOfByte();
                Imgcodecs.imencode(".png", frame, matOfByte);
                byte[] bytes = matOfByte.toArray();
                InputStream inputStream = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                return SwingFXUtils.toFXImage(bufferedImage, null);
            }
            catch(IOException ex){
                ex.printStackTrace();

                return null;
            }
        }
        else
            return null;
    }

    private void drawHistogram(Mat histFrame, List<Mat> frameList){
        Imgproc.calcHist(frameList, new MatOfInt(0), new Mat(), histFrame, new MatOfInt(256), new MatOfFloat(0, 256));

        if(lowerBoundGrabHist == 0 || lowerBoundGrabHist >= upperBoundGrabHist){
            Platform.runLater(() -> areaChart.getData().clear());
            XYChart.Series seriesHSV = new XYChart.Series();
            seriesHSV.setName("Intensity");
            int histHeight = histFrame.height();
            for (int i = 0; i < histHeight; i++){
                double intensity = histFrame.get(i, 0)[0];
                seriesHSV.getData().add(new XYChart.Data(i, (int)intensity));
            }
            Platform.runLater(() -> areaChart.getData().add(seriesHSV));

            lowerBoundGrabHist = 1;
            upperBoundGrabHist = 16;
        }
        else
            lowerBoundGrabHist++;
    }
}
