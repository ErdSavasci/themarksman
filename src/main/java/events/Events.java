package events;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;

/**
 * Created by erd_9 on 10.06.2017.
 */
public class Events {
    private static TextArea textArea;

    public static void initialize(TextArea txtArea){
        textArea = txtArea;

        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            textArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
