package net.transgressoft.musicott;

import net.transgressoft.musicott.events.StageReadyEvent;
import net.transgressoft.musicott.events.StopApplicationEvent;
import net.transgressoft.musicott.view.ErrorDialogController;
import net.transgressoft.musicott.view.MainController;
import net.transgressoft.musicott.view.PreferencesController;

import javafx.scene.Scene;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private final FxWeaver fxWeaver;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public PrimaryStageInitializer(FxWeaver fxWeaver) {
        this.fxWeaver = fxWeaver;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        fxWeaver.loadView(PreferencesController.class);
        fxWeaver.loadView(ErrorDialogController.class);
        var scene = new Scene(fxWeaver.loadView(MainController.class));
        event.primaryStage.setOnCloseRequest(e -> applicationEventPublisher.publishEvent(new StopApplicationEvent(this)));
        event.primaryStage.setScene(scene);
        event.primaryStage.show();
    }
}
