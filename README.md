# Musicott
A JavaFX application for import, organize and use music files.

* ID3 mp3 tag parser with [mp3agic](https://github.com/mpatric/mp3agic "mp3agic")
* Ogg Vorbis Comment Flac parser with [JAudioTagger](https://bitbucket.org/ijabz/jaudiotagger "jAudioTagger")
* Persistence with [Json-io](https://github.com/jdereg/json-io "Json-io")
* Testing with [TestFx](https://github.com/TestFX/TestFX "TestFx"), [JUnit](https://github.com/junit-team/junit "JUnit"), [Mockito](https://github.com/mockito/mockito "Mockito") & [PowerMock](https://github.com/jayway/powermock "PowerMock").

## Features for now
* Import mp3 and flac files from within a folder
* Delete rows in the table
* Edit single or multiple rows

### To be implemented
* Fix ID3 tags
* Write ID3 tags on edit
* Play mp3, wav, m4a and flac files

## Changes history
In [CHANGES.md](https://github.com/octaviospain/Musicott/tree/master/CHANGES.md "Changes")

## License
Musicott is under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder