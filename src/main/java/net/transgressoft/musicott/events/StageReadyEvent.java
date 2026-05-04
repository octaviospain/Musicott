package net.transgressoft.musicott.events;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

public class StageReadyEvent extends ApplicationEvent {

    // JavaFX Stage isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final Stage primaryStage;

    public StageReadyEvent(Stage primaryStage) {
        super(primaryStage);
        this.primaryStage = primaryStage;
    }
}
