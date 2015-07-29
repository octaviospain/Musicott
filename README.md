# Musicott
A JavaFX application for import, organize and play music files.

![Musicott screenshot 1](https://dl.dropboxusercontent.com/u/3596661/main.png)

![Musicott screenshot 2](https://dl.dropboxusercontent.com/u/3596661/edit.png)

![Musicott screenshot 3](https://dl.dropboxusercontent.com/u/3596661/import.png)

* ID3 mp3 tag parser with [mp3agic](https://github.com/mpatric/mp3agic "mp3agic")
* Ogg Vorbis Comment Flac parser with [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger")
* Persistence with [Json-io](https://github.com/jdereg/json-io "Json-io")
* Testing with [TestFx](https://github.com/TestFX/TestFX "TestFx"), [JUnit](https://github.com/junit-team/junit "JUnit"), [Mockito](https://github.com/mockito/mockito "Mockito") & [PowerMock](https://github.com/jayway/powermock "PowerMock").

## Features for now
* Play mp3 files
* Import mp3 and flac files from within a folder
* Delete rows in the table
* Edit single or multiple rows
* Correct Mp3 and Flac metadata showing

### To be implemented
* Get/set cover image in the file
* Fix metadata from 3rd party
* Write metadata tags on edit
* Play wav, m4a and flac files
* Advanced search feature
* Show cover image on table
* Ability to manage different formats for a single track (implementing a TreeTableView)
* Managing of HD space giving the option to move files to external drives but maintaining the info in the table

## Changes history
In [CHANGES.md](https://github.com/octaviospain/Musicott/tree/master/CHANGES.md "Changes")

## License
Musicott is under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder