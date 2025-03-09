package net.transgressoft.musicott.events;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

public class StageReadyEvent extends ApplicationEvent {

    public final Stage primaryStage;

    public StageReadyEvent(Stage primaryStage) {
        super(primaryStage);
        this.primaryStage = primaryStage;
    }
}
