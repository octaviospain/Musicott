package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesLibrary;
import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable wizard-internal draft holding the data the user collects across the iTunes
 * import wizard steps. Lives only for the duration of a single wizard session and is
 * never persisted or published. Step controllers read and write its fields through
 * their entry / exit hooks; the wizard owner reads it once on confirmation to construct
 * the {@link net.transgressoft.commons.music.itunes.ItunesImportPolicy} passed to
 * {@link net.transgressoft.musicott.service.MediaImportService#importSelectedPlaylists}.
 *
 * @author Octavio Calleya
 */
class ItunesImportDraft {

    Path libraryPath;
    ItunesLibrary parsedLibrary;
    final List<ItunesPlaylist> selectedPlaylists = new ArrayList<>();
    boolean useFileMetadata = true;
    boolean holdPlayCount = true;
    boolean writeMetadata = true;
}
