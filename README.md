[![Build Status](https://travis-ci.org/octaviospain/Musicott.svg?branch=master)](https://travis-ci.org/octaviospain/Musicott)
[![codecov](https://codecov.io/gh/octaviospain/Musicott/branch/master/graph/badge.svg)](https://codecov.io/gh/octaviospain/Musicott)
[![maintainability-rating](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=new_maintainability_rating)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)
[![reliability-rating](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=new_reliability_rating)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)
[![security-rating](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=new_security_rating)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)

[![loc-metric](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=ncloc)](https://sonarqube.com/component_measures/domain/Size?id=com.transgressoft%3Amusicott)
[![lines-metric](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=lines)](https://sonarqube.com/component_measures/domain/Size?id=com.transgressoft%3Amusicott)
[![comments-metric](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=comment_lines_density)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)
[![new-code-smells](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=code_smells)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)
[![bugs](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=bugs)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)
[![vulnerabilities-rating](https://sonarqube.com/api/badges/measure?key=com.transgressoft%3Amusicott&metric=vulnerabilities)](https://sonarqube.com/component_measures?id=com.transgressoft%3Amusicott)
[![license](https://img.shields.io/badge/license-GPL3v2-brightgreen.svg)](https://github.com/octaviospain/Musicott/blob/master/license/gpl.txt)


![Musicott title logo](http://imageshack.com/a/img921/1074/4WJ9yl.png)

#
Musicott is an application that manages and plays music files. Coded in Java 8 with JavaFX.
Uses [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger") to read and write the metadata,
[Json-io](https://github.com/jdereg/json-io "Json-io") for persistence,
[TestFx](https://github.com/TestFX/TestFX "TestFx") for testing, and some components from
[ControlsFx](https://bitbucket.org/controlsfx/controlsfx/ "ControlsFx").

[![Musicott-video](http://imageshack.com/a/img922/6599/x91wgY.png)](https://youtu.be/HHvfC3L8A3g)

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
[Download](https://github.com/octaviospain/Musicott/releases "Download") the last release (version 0.9-b).
Version 0.10-b needs some more tests and it will be published, in the meantime it's possible to run the last version from the master/develop branch and help with the testing thing ;)

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

## Copyright
Copyright (c) 2015-2016 Octavio Calleya.

Musicott is free software under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder
