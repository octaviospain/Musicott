[![Build Status](https://travis-ci.org/octaviospain/Musicott.svg?branch=master)](https://travis-ci.org/octaviospain/Musicott)
[![codecov](https://codecov.io/gh/octaviospain/Musicott/branch/master/graph/badge.svg)](https://codecov.io/gh/octaviospain/Musicott)
[![license](https://img.shields.io/badge/license-GPL3v2-brightgreen.svg)](https://github.com/octaviospain/Musicott/blob/master/license/gpl.txt)


![Musicott title logo](http://imageshack.com/a/img921/1074/4WJ9yl.png)

#
Musicott is an application that manages and plays music files. Coded in Java 8 with JavaFX.
Uses [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger") to read and write the metadata,
[Json-io](https://github.com/jdereg/json-io "Json-io") for persistence,
[TestFx](https://github.com/TestFX/TestFX "TestFx") for testing, and some components from
[ControlsFx](https://bitbucket.org/controlsfx/controlsfx/ "ControlsFx").

Testing with Junit 5, TestFX and maven is quite difficult, help is welcomed !

![Musicott screenshot 1](http://imageshack.com/a/img923/9629/3UzBQE.png)

### Features
* Play mp3, wav and m4a (with ALAC encoding) files
* Import mp3, wav and flac files
* Shows waveform of mp3, wav and m4a files
* Import from Itunes Library xml
* Scrobble on LastFM

### Not yet
* Advanced search feature
* Music library statistics
* Smart playlists

## Download
[Download](https://github.com/octaviospain/Musicott/releases "Download") the last release (version 0.9.2-b);

## How to build and run
To build you need at least Java 8 update 40 and Maven

 1. Clone Musicott at any directory `git clone https://github.com/octaviospain/Musicott.git`
 2. Build with maven at Musicott's root folder (where `pom.xml` is located)  with `mvn package`
 3. Run with `mvn exec:java -D exec.mainClass=com.transgressoft.musicott.MusicottApplication`

## How to contribute

If you want to add a feature or fix a bug in Musicott, you can submit a pull request as follows:

 1. Fork the project
 2. Write the code of the feature with some `javadoc` and the necessary comments
 3. Please don't auto-format the code already written as it would make more difficult to see what was changed
 4. Add some tests! **Doing it in the `tests` branch is preferable**
 5. Commit
 6. Submit a pull request **on the `develop` branch**

## Manual
Check out the [Wiki pages](https://github.com/octaviospain/Musicott/wiki "Wiki") for help about using Musicott.

## Changes history

### Version 0.9
* New dark theme
* Added a navigation area with playlist and the showing modes
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

## Copyright
Copyright (c) 2015-2017 Octavio Calleya.

Musicott is free software under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder
