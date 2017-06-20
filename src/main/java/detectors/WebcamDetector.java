package detectors;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import javafx.scene.control.ListView;

import java.util.ArrayList;

/**
 * Created by erd_9 on 5.06.2017.
 */
public class WebcamDetector implements WebcamDiscoveryListener {
    ListView<String> availableCamerasListView;
    ArrayList<String> camerasArrayList;

    public WebcamDetector(ListView<String> listview){
        availableCamerasListView = listview;
    }

    public ArrayList<String> getAllAvailableCameras(){
        camerasArrayList = new ArrayList<>();

        for(Webcam webcam : Webcam.getWebcams()){
            camerasArrayList.add(webcam.getName().substring(0, webcam.getName().length() - 1));
        }

        Webcam.addDiscoveryListener(new WebcamDetector(availableCamerasListView));
        return camerasArrayList;
    }

    @Override
    public void webcamFound(WebcamDiscoveryEvent webcamDiscoveryEvent) {
        availableCamerasListView.getItems().add(webcamDiscoveryEvent.getWebcam().getName());
    }

    @Override
    public void webcamGone(WebcamDiscoveryEvent webcamDiscoveryEvent) {
        int goneCameraIndex = 0;

        for(String camera : availableCamerasListView.getItems()){
            if(webcamDiscoveryEvent.getWebcam().getName().contains(camera) && goneCameraIndex < availableCamerasListView.getItems().size())
                availableCamerasListView.getItems().remove(goneCameraIndex);
            else
                goneCameraIndex++;
        }
    }
}
