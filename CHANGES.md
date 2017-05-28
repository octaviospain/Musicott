# Changes

### Version 0.10.1
* Improved Itunes import. Now the application shows a window to let the user selects which playlists to import.
* Refactored with guice's dependency injection framework.
* Refactored to use maven multi modules.

### Version 0.10
* Added Artists navigation mode. A list of artists is shown, and clicking one artist shows their albums, with useful information of them.
* Typing on the search field while on the artists mode filters the artists list by the query.
* Added cover thumbnail on the bottom right corner of the table that changes when hovering the rows
* Also when a playlist is shown, the cover of the playlist changes when hovering
* Drag & drop tracks to tracksLibrary is now possible
* Drag & drop tracksLibrary into folders is now possible
* Ability to reorder the playlist queue by drag & drop
* Improved audio files & iTunes importing
* Several bug fixes and performance improvements

### Version 0.9
* New dark theme
* Added a navigation area with playlist and the showing mode
* Added a information pane at the top of the table when a playlist is shown, with a cover image,
the number of tracks and the size of the playlist
* You can hide/show the navigation pane with <kbd>CMD</kbd> + <kbd>SHIFT</kbd> + <kbd>R</kbd>
 and the table info pane with <kbd>CMD</kbd> + <kbd>SHIFT</kbd> + <kbd>H</kbd>
* You can add tracks to a playlist by drag and drop them
* You can set a new image when editing tracks by drag and drop the file to the image view on the window


### Version 0.8
* Ability to import from Itunes, selecting the "iTunes Music Library.xml" file
* LastFM API. The user can log in and Musicott will scrobble listened tracks
* Fixed a bug and now the waveform image is shown for all tracks

### Version 0.7
* Added waveform image for mostly all mp3 files and all wav files
* Better design
* Big refactor implementing better concurrent model