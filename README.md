# Musicott
Musicott is an application that manages and plays music files. Coded in Java 8 with JavaFX.
Uses [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger") to read and write the metadata,
[Json-io](https://github.com/jdereg/json-io "Json-io") for persistence,
[TestFx](https://github.com/TestFX/TestFX "TestFx") for testing, and some components from
[ControlsFx](https://bitbucket.org/controlsfx/controlsfx/ "ControlsFx").

![Musicott screenshot 1](https://dl.dropboxusercontent.com/u/3596661/main.png)

### Features
* Play mp3, wav and m4a (with ALAC encoding) files
* Import mp3, wav and flac files
* Shows waveform of mp3, wav and m4a files
* Import from Itunes Library xml
* Scrobble on LastFM

### Not yet
* Advanced search feature
* Show cover image on table
* Music library statistics
* Smart playlists

## Download
[Download](https://github.com/octaviospain/Musicott/releases "Download") the last release (version 0.8).
Current last version 0.9-b.

## How to build and run
To build you need at least Java 8 update 40 and Maven

 1. Clone Musicott at any directory `git clone https://github.com/octaviospain/Musicott.git`
 2. Build with maven at Musicott's root folder (where `pom.xml` is located)  with `mvn package`
 3. Run with `java -cp target/Musicott-x.y.z.jar com.transgressoft.musicott.MusicottApplication`

## Manual
Check out the [Wiki pages](https://github.com/octaviospain/Musicott/wiki "Wiki") for help about using Musicott.

## Changes history

### Version 0.9
* Added a navigation area with playlist and the showing modes
* Added a information pane at the top of the table when a playlist is shown, with a cover image,
the number of tracks and the size of the playlist
* You can hide/show the navigation pane with <kbd>CMDD</kbd> + <kbd>SHIFT</kbd> + <kbd>R</kbd>
 and the table info pane with <kbd>CMD</kbd> + <kbd>SHIFT</kbd> + <kbd>H</kbd>


### Version 0.8
* Ability to import from Itunes, selecting the "iTunes Music Library.xml" file
* LastFM API. The user can log in and Musicott will scrobble listened tracks
* Fixed a bug and now the waveform image is shown for all tracks

### Version 0.7
* Added waveform image for mostly all mp3 files and all wav files
* Better design
* Big refactor implementing better concurrent model

## License
Musicott is free softare under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder