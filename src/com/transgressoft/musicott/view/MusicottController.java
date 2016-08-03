package com.transgressoft.musicott.view;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.services.*;

/**
 * Interface that represent a controller of the Musicott application.
 * Stores constants of layout files, logos, css stylesheets and
 * references to the singleton classes.
 *
 * @author Octavio Calleya
 * @version 0.9-b
 * @since 0.9
 */
public interface MusicottController {

	String LAYOUTS_PATH = "/view/";
	String IMAGES_PATH = "/images/";
	String STYLES_PATH = "/css/";

	String ROOT_LAYOUT = LAYOUTS_PATH + "RootLayout.fxml";
	String NAVIGATION_LAYOUT = LAYOUTS_PATH + "NavigationLayout.fxml";
	String PRELOADER_INIT_LAYOUT = LAYOUTS_PATH + "PreloaderLayout.fxml";
	String PRELOADER_FIRST_USE_PROMPT = LAYOUTS_PATH + "PreloaderPromptLayout.fxml";
	String EDIT_LAYOUT = LAYOUTS_PATH + "EditLayout.fxml";
	String PLAYQUEUE_LAYOUT = LAYOUTS_PATH + "PlayQueueLayout.fxml";
	String PROGRESS_LAYOUT = LAYOUTS_PATH + "ProgressLayout.fxml";
	String PREFERENCES_LAYOUT = LAYOUTS_PATH + "PreferencesLayout.fxml";
	String PLAYER_LAYOUT = LAYOUTS_PATH + "PlayerLayout.fxml";

	String DEFAULT_COVER_IMAGE = IMAGES_PATH + "default-cover-image.png";
	String LASTFM_LOGO = IMAGES_PATH + "lastfm-logo.png";
	String MUSICOTT_ICON = IMAGES_PATH + "musicotticon.png";
	String MUSICOTT_ABOUT_LOGO = IMAGES_PATH + "musicott-about-logo.png";

	String BASE_STYLE = STYLES_PATH + "base.css";
	String DIALOG_STYLE = STYLES_PATH + "dialog.css";
	String TRACK_TABLE_STYLE = STYLES_PATH + "tracktable.css";

	MusicLibrary musicLibrary = MusicLibrary.getInstance();
	MainPreferences preferences = MainPreferences.getInstance();
	PlayerFacade player = PlayerFacade.getInstance();
	ServiceDemon serviceDemon = ServiceDemon.getInstance();
	StageDemon stageDemon = StageDemon.getInstance();
	ErrorDemon errorDemon = ErrorDemon.getInstance();
}
