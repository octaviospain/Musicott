package net.transgressoft.musicott.view;

import net.transgressoft.musicott.events.ErrorEvent;
import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.events.InvalidAudioItemsForEditionEvent;
import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ErrorDialogController.fxml")
@Controller ("errorPresenter")
public class ErrorDialogController implements ErrorPresenter {

    private static final String INVALID_AUDIO_ITEMS_MESSAGE = "Some tracks cannot be edited because they were not found";
    private final Image commonErrorImage = ApplicationImage.COMMON_ERROR.get();
    private final Image lastFmErrorImage = ApplicationImage.LASTFM_LOGO.get();

    @FXML
    private AnchorPane root;
    @FXML
    private BorderPane rootBorderPane;
    @FXML
    private ImageView errorImageView;
    @FXML
    private Button okButton;
    @FXML
    private Label titleLabel;
    @FXML
    private Hyperlink seeDetailsHyperlink;
    @FXML
    private ToggleButton seeDetailsToggleButton;
    @FXML
    private BorderPane contentBorderPane;
    @FXML
    private BorderPane bottomBorderPane;
    @FXML
    private HBox seeDetailsHBox;
    @FXML
    private VBox contentVBox;
    @FXML
    private VBox textAreaVBox;
    @FXML
    private TextArea detailsTextArea;
    @FXML
    private Hyperlink reportInGithubHyperlink;
    @FXML
    private Label contentLabel;

    private Supplier<Stage> stageSupplier;
    private Stage stage;
    private SimpleWebRedirectionService webRedirectionService;

    @FXML
    public void initialize() {
        reportInGithubHyperlink.setOnAction(event -> {
            if (webRedirectionService != null)
                webRedirectionService.navigateToGithub();
        });
        contentVBox.getChildren().remove(reportInGithubHyperlink);

        rootBorderPane.getChildren().remove(textAreaVBox);
        bottomBorderPane.getChildren().remove(seeDetailsHBox);
        EventHandler<ActionEvent> expandablePaneHandler = event -> {
            if (! rootBorderPane.getChildren().contains(textAreaVBox)) {
                rootBorderPane.setCenter(textAreaVBox);
                seeDetailsToggleButton.setSelected(false);
            } else {
                rootBorderPane.getChildren().remove(textAreaVBox);
                seeDetailsToggleButton.setSelected(true);
            }
            contentVBox.getScene().getWindow().sizeToScene();
        };
        seeDetailsToggleButton.setOnAction(expandablePaneHandler);
        seeDetailsHyperlink.setOnAction(expandablePaneHandler);
        okButton.setOnAction(event -> okButton.getScene().getWindow().hide());
    }

    @Override
    public void show(String message) {
        show(message, null, null);
    }

    @Override
    public void show(String message, Throwable exception) {
        show(message, null, exception);
    }

    @Override
    public void show(String message, String content) {
        show(message, content, null);
    }

    @Override
    public void show(String message, String content, Throwable exception) {
        if (exception == null)
            showWithExpandableContent(message, content, Collections.emptyList());
        else {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            showWithExpandableContent(message, content, Collections.singleton(stringWriter.toString()));
        }
    }

    @Override
    public void showWithExpandableContent(String message, Collection<String> messagesToBeExpanded) {
        showWithExpandableContent(message, null, messagesToBeExpanded);
    }

    @Override
    public void showWithExpandableContent(String message, String content, Collection<String> messagesToBeExpanded) {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = stageSupplier.get();
                stage.setTitle("Error");
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root));
            }
            titleLabel.setText(message);
            setErrorContent(content);
            checkDetailsArea(messagesToBeExpanded);
            putCommonGraphic();

            stage.show();
            stage.toFront();
        });
    }

    private void setErrorContent(String content) {
        if (content == null) {
            contentVBox.getChildren().remove(contentLabel);
            if (! contentVBox.getChildren().contains(reportInGithubHyperlink))
                contentVBox.getChildren().add(reportInGithubHyperlink);
            contentLabel.setText("");
        } else {
            contentLabel.setText(content);
            if (! contentVBox.getChildren().contains(contentLabel))
                contentVBox.getChildren().add(contentLabel);
            contentVBox.getChildren().remove(reportInGithubHyperlink);
        }
    }

    private void putCommonGraphic() {
        if (! errorImageView.getImage().equals(commonErrorImage)) {
            errorImageView.setImage(commonErrorImage);
        }
    }

    private void setDetailsAreaContent(Collection<String> content) {
        detailsTextArea.clear();
        for (String msg : content)
            detailsTextArea.appendText(msg + "\n");
    }

    private void checkDetailsArea(Collection<String> messagesToBeExpanded) {
        if (! messagesToBeExpanded.isEmpty()) {
            setDetailsAreaContent(messagesToBeExpanded);
            addExpandableButtons();
        } else {
            rootBorderPane.getChildren().remove(textAreaVBox);
            bottomBorderPane.getChildren().remove(seeDetailsHBox);
        }
    }

    private void addExpandableButtons() {
        if (! bottomBorderPane.getChildren().contains(seeDetailsHBox))
            bottomBorderPane.setLeft(seeDetailsHBox);
    }

    @Override
    public void showLastFmError(String message, String content) {
        titleLabel.setText(message);
        setErrorContent(content);
        putLastFmGraphic();
        Platform.runLater(stage::show);
    }

    private void putLastFmGraphic() {
        if (! errorImageView.getImage().equals(lastFmErrorImage))
            errorImageView.setImage(lastFmErrorImage);
    }

    @EventListener
    public void invalidAudioItemsEventListener(InvalidAudioItemsForEditionEvent invalidAudioItemsForEditionEvent) {
        showWithExpandableContent(
            INVALID_AUDIO_ITEMS_MESSAGE,
            null,
            invalidAudioItemsForEditionEvent.invalidAudioItems.stream().map(audioItem -> audioItem.getPath().toString()).collect(Collectors.toSet()));
    }

    @EventListener
    public void errorEvent(ErrorEvent errorEvent) {
        show(errorEvent.title, errorEvent.content);
    }

    @EventListener
    public void exceptionEventListener(ExceptionEvent exceptionEvent) {
        show("An exception occurred", exceptionEvent.exception);
    }

    @Autowired
    public void setStageSupplier(Supplier<Stage> stageSupplier) {
        this.stageSupplier = stageSupplier;
    }

    @Autowired (required = false)
    public void setWebRedirectionService(SimpleWebRedirectionService webRedirectionService) {
        this.webRedirectionService = webRedirectionService;
    }
}
