package com.musicott.view;

/**
 * @author Octavio Calleya
 */
public interface MusicottController {

	String LAYOUTS_PATH = "/view/";

	String ROOT_LAYOUT = LAYOUTS_PATH + "RootLayout.fxml";
	String NAVIGATION_LAYOUT = LAYOUTS_PATH +  "NavigationLayout.fxml";
	String PRELOADER_LAYOUT = LAYOUTS_PATH + "PreloaderPromptLayout.fxml";
	String EDIT_LAYOUT = LAYOUTS_PATH + "EditLayout.fxml";
	String PLAYQUEUE_LAYOUT = LAYOUTS_PATH + "PlayQueueLayout.fxml";
	String PROGRESS_LAYOUT = LAYOUTS_PATH + "ProgressLayout.fxml";
	String PREFERENCES_LAYOUT = LAYOUTS_PATH + "PreferencesLayout.fxml";
	String PLAYER_LAYOUT = LAYOUTS_PATH + "PlayerLayout.fxml";

	String DEFAULT_COVER_IMAGE = "/images/default-cover-image.png";
	String MUSICOTT_ICON = "/images/musicotticon.png";
}