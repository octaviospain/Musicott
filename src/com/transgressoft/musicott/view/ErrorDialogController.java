/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.view;

import com.transgressoft.musicott.*;
import javafx.application.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

import java.io.*;
import java.util.*;

/**
 * Controller class of the error alert window that shows an error message and
 * optionally a text area with exception stack traces or multiple error messages.
 *
 * @author Octavio Calleya
 */
public class ErrorDialogController implements MusicottController {

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
    private TextArea detailsTextArea;
    @FXML
    private Hyperlink reportInGithubHyperlink;
    @FXML
    private Label contentLabel;
    private Image commonErrorImage = new Image(getClass().getResourceAsStream(COMMON_ERROR_IMAGE));
    private Image lastFmErrorImage = new Image(getClass().getResourceAsStream(LASTFM_LOGO));

    @FXML
    public void initialize() {
        reportInGithubHyperlink.setOnAction(event -> {
            HostServices hostServices = StageDemon.getInstance().getApplicationHostServices();
            Hyperlink link = (Hyperlink) event.getSource();
            String str = link == null ? "" : link.getText();
            if (str.equals("Report this error on github.")) {
                hostServices.showDocument("https://github.com/octaviospain/Musicott/issues");
            }
        });
        contentVBox.getChildren().remove(reportInGithubHyperlink);

        rootBorderPane.getChildren().remove(detailsTextArea);
        bottomBorderPane.getChildren().remove(seeDetailsHBox);
        EventHandler<ActionEvent> expandablePaneHandler = event -> {
            if (! rootBorderPane.getChildren().contains(detailsTextArea)) {
                rootBorderPane.setCenter(detailsTextArea);
                seeDetailsToggleButton.setSelected(false);
            }
            else {
                rootBorderPane.getChildren().remove(detailsTextArea);
                seeDetailsToggleButton.setSelected(true);
            }
        };
        seeDetailsToggleButton.setOnAction(expandablePaneHandler);
        seeDetailsHyperlink.setOnAction(expandablePaneHandler);
        okButton.setOnAction(event -> okButton.getScene().getWindow().hide());
    }

    public void prepareDialog(String message, String content, Exception exception) {
        titleLabel.setText(message);
        setErrorContent(content);
        checkDetailsArea(exception);
        putCommonGraphic();
    }

    private void setErrorContent(String content) {
        if (content == null) {
            contentVBox.getChildren().remove(contentLabel);
            if (! contentVBox.getChildren().contains(reportInGithubHyperlink))
                contentVBox.getChildren().add(reportInGithubHyperlink);
            contentLabel.setText("");
        }
        else {
            contentLabel.setText(content);
            if (! contentVBox.getChildren().contains(contentLabel))
                contentVBox.getChildren().add(contentLabel);
            contentVBox.getChildren().remove(reportInGithubHyperlink);
        }
    }

    private void checkDetailsArea(Exception exception) {
        if (exception != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            setDetailsAreaContent(Collections.singletonList(stringWriter.toString()));
            addExpandableButtons();
        }
        else {
            removeDetailsArea();
            removeExpandableButtons();
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

    public void prepareDialogWithMessages(String message, String content, Collection<String> errors) {
        titleLabel.setText(message);
        setErrorContent(content);
        checkDetailsArea(errors);
        putCommonGraphic();
    }

    private void checkDetailsArea(Collection<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            setDetailsAreaContent(errors);
            addExpandableButtons();
        }
        else {
            removeDetailsArea();
            removeExpandableButtons();
        }
    }

    private void removeDetailsArea() {
        if (contentBorderPane.getChildren().contains(detailsTextArea))
            contentBorderPane.getChildren().remove(detailsTextArea);
    }

    private void removeExpandableButtons() {
        if (bottomBorderPane.getChildren().contains(seeDetailsHBox))
            bottomBorderPane.getChildren().remove(seeDetailsHBox);
    }

    private void addExpandableButtons() {
        if (! bottomBorderPane.getChildren().contains(seeDetailsHBox))
            bottomBorderPane.setLeft(seeDetailsHBox);
    }

    public void prepareLastFmDialog(String message, String content) {
        titleLabel.setText(message);
        setErrorContent(content);
        putLastFmGraphic();
    }

    private void putLastFmGraphic() {
        if (! errorImageView.getImage().equals(lastFmErrorImage))
            errorImageView.setImage(lastFmErrorImage);
    }

    String getErrorTitle() {
        return titleLabel.getText();
    }

    String getErrorContent() {
        return contentLabel.getText().isEmpty() ? reportInGithubHyperlink.getText() : contentLabel.getText();
    }

    String getDetailsAreaText() {
        return detailsTextArea.getText();
    }
}