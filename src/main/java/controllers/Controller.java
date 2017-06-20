package controllers;

import com.github.sarxos.webcam.Webcam;
import detectors.WebcamDetector;
import events.Events;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.omg.CORBA.Environment;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import utils.FXCamera;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ImageView bannerImageView;
    @FXML
    private ImageView cameraImageView;
    @FXML
    private TextField chatTextField;
    @FXML
    private ListView<String> availableCamerasListView;
    @FXML
    private AreaChart<Number, Number> areaChart;
    @FXML
    private NumberAxis numberAxisX, numberAxisY;
    @FXML
    private ImageView gifImageView1, gifImageView2, gifImageView3;
    @FXML
    private TextArea shellTextArea;

    private FXCamera fxCamera;
    private WebcamDetector webcamDetector;

    public Controller(){

    }

    @FXML
    public void initialize() {
        fxCamera = new FXCamera(availableCamerasListView, areaChart, cameraImageView, shellTextArea);
        webcamDetector = new WebcamDetector(availableCamerasListView);

        Events.initialize(shellTextArea);

        shellTextArea.setText("Starting Services..");

        try{
            gifImageView1.setImage(new Image(getClass().getClassLoader().getResource("images/banner.gif").toExternalForm()));
            gifImageView2.setImage(new Image(getClass().getClassLoader().getResource("images/banner.gif").toExternalForm()));
            gifImageView3.setImage(new Image(getClass().getClassLoader().getResource("images/banner.gif").toExternalForm()));
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        availableCamerasListView.setItems(FXCollections.observableArrayList(webcamDetector.getAllAvailableCameras()));
        availableCamerasListView.refresh();

        shellTextArea.appendText(System.getProperty("line.separator") + "Starting Camera..");

        fxCamera.startCamera();

        numberAxisY.setUpperBound(fxCamera.getResolution().getWidth() * fxCamera.getResolution().getHeight());
    }

    public void stopCamera(){
        fxCamera.stopCamera();
    }
}
