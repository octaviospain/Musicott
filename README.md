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

## Download
[Download](https://github.com/octaviospain/Musicott/releases "Download") the last release (version 0.6)

## Build
I recommend to use Eclipse IDE witch e(fx)clipse plugin installed. To do so follow these steps:
1. Create a new JavaFX project.
2. Clone the repository in the folder of the project
3. Right-click on the project and click Configure > Convert to Maven Project.
4. Configure the build path adding the .jar libraries at `/lib` and set `/test/` and `/resources/` as source folders.
5. Open `build.fxbuild`, select "exe" for Windows or "dmg" for OS X in the "Packaging Format" field, and deselect "Convert CSS into binary form". Then click "Generate ant build.xml"
 6. After that you must add these two lines to `build.xml`:

Find `<filelist>` tag and write at the end `<file name="${basedir}"/>`
 
Find `<fx:resources id="appRes">` tag and write before the closing tag `</fx:resources>` the following: `<fx:fileset dir="dist" includes="resources/**"/>`

You have to do steop 6 *every time* you generate the `build.xml` file because e(fx)clipse doesn`t.

Then Right-click on `build.xml` and click Run As > Ant Build. The deployed package will be at `/build/deploy/bundles`

## Manual
Check out the [Wiki pages](https://github.com/octaviospain/Musicott/wiki "Wiki") for help about using Musicott.

## Changes history
In [CHANGES.md](https://github.com/octaviospain/Musicott/tree/master/CHANGES.md "Changes")

## License
Musicott is free softare under GNU GPL version 3 license. The license of Musicott and the licenses of the included libraries in this software are in the [LICENSE](https://github.com/octaviospain/Musicott/tree/master/license "License") folder