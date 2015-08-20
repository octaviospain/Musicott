# Musicott
A JavaFX application that manages and plays music files.

![Musicott screenshot 1](https://dl.dropboxusercontent.com/u/3596661/main.png)

![Musicott screenshot 2](https://dl.dropboxusercontent.com/u/3596661/main2.png)

* ID3 mp3 tag parser with [mp3agic](https://github.com/mpatric/mp3agic "mp3agic")
* Ogg Vorbis Comment Flac parser with [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger")
* Persistence with [Json-io](https://github.com/jdereg/json-io "Json-io")
* Testing with [TestFx](https://github.com/TestFX/TestFX "TestFx"), [JUnit](https://github.com/junit-team/junit "JUnit"), [Mockito](https://github.com/mockito/mockito "Mockito") & [PowerMock](https://github.com/jayway/powermock "PowerMock").

## Features for now
* Play mp3, wav and m4a files
* Import mp3, wav and flac files
* Edit metadata of the audio files and add a cover image
* Search files

### To be implemented
* Play flac files
* Get metadata from 3rd party services (Beatport, discogs, etc)
* Advanced search feature
* Show cover image on table
* Ability to manage different formats for a single track (implementing a TreeTableView)
* Managing of HD space giving the option to move files to external drives but maintaining the info in the table

## Download
[Download](https://github.com/octaviospain/Musicott/releases "Download") the last release (version 0.6)

## Manual
Check out the [Wiki pages](https://github.com/octaviospain/Musicott/wiki "Wiki") for help about using Musicott.

## Changes history
In [CHANGES.md](https://github.com/octaviospain/Musicott/tree/master/CHANGES.md "Changes")

## License
Musicott is free softare under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder