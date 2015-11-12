---
title: Musicott by octaviospain
---
# Musicott
JavaFX application that manages and plays music files. Uses [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger") to read and write the metadata, [Json-io](https://github.com/jdereg/json-io "Json-io") for persistence, and [JUnit](https://github.com/junit-team/junit "JUnit") & [TestFx](https://github.com/TestFX/TestFX "TestFx") for testing. Also [ControlsFx](https://bitbucket.org/controlsfx/controlsfx/ "ControlsFx") and [TarsosTranscoder](https://github.com/JorenSix/TarsosTranscoder "TarsosTranscoder").

![Musicott screenshot 1](https://dl.dropboxusercontent.com/u/3596661/main.png)

![Musicott screenshot 2](https://dl.dropboxusercontent.com/u/3596661/main2.png)

## Features for now
* Play mp3, wav and m4a (with ALAC encoding) files
* Import mp3, wav and flac files
* Edit metadata of the audio files and add a cover image
* Search files
* Waveform display of mp3 and wav

### Coming soon
* Import and play more file formats
* Import from itunes library
* Get metadata from 3rd party services (Beatport, discogs, etc)
* Advanced search feature
* Show cover image on table
* Manage tracks in several lists
* A row in the table showing the playlists in which the track is
* Library statistics

## Download
[Download](https://github.com/octaviospain/Musicott/releases "Download") the last release (version 0.7)

## How to build and run
To build you need at least Java 8 update 40

 1. Clone Musicott at any directory `git clone https://github.com/octaviospain/Musicott.git`
 2. Build with maven at Musicott's root folder (where pom.xml is) with `mvn package`
 3. Run with `java -cp target/Musicott-x.y.z.jar com.musicott.MainApp`

## Manual
Check out the [Wiki pages](https://github.com/octaviospain/Musicott/wiki "Wiki") for help about using Musicott.

## Changes history
In [CHANGES.md]({{ sit.url}}/Musicott/changes.html "Changes")

## License
Musicott is free softare under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder