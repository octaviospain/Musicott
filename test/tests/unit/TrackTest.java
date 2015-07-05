/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package tests.unit;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.musicott.model.ObservableTrack;
import com.musicott.model.Track;
import com.musicott.task.parser.Mp3Parser;

/**
 * @author Octavio Calleya
 *
 */
public class TrackTest {

	@Test
	public void trackFromObservableTrackTest() throws Exception {
		File f = new File("/Users/octavio/Music/iTunes/iTunes Media/Music/Virgil Enzinger/Decoded/Decode (Sasha Carassi Remix).mp3");
		ObservableTrack obsTrack = Mp3Parser.parseMp3File(f);
		Track track = Track.trackFromObservableTrack(obsTrack);
		assertEquals(obsTrack.getTrackID(),track.getTrackID());
		assertEquals(obsTrack.getFileFolder(),track.getFileFolder());
		assertEquals(obsTrack.getFileName(),track.getFileName());
		assertEquals(obsTrack.getName().get(),track.getName());
		assertEquals(obsTrack.getAlbum().get(),track.getAlbum());
		assertEquals(obsTrack.getArtist().get(),track.getArtist());
		assertEquals(obsTrack.getGenre().get(),track.getGenre());
		assertEquals(obsTrack.getComments().get(),track.getComments());
		assertEquals(obsTrack.getAlbumArtist().get(),track.getAlbumArtist());
		assertEquals(obsTrack.getLabel().get(),track.getLabel());
		assertEquals(obsTrack.getSize().get(),track.getSize());
		assertEquals(obsTrack.getTotalTime().get(),track.getTotalTime());
		assertEquals(obsTrack.getTrackNumber().get(),track.getTrackNumber());
		assertEquals(obsTrack.getYear().get(),track.getYear());
		assertEquals(obsTrack.getBitRate().get(),track.getBitRate());
		assertEquals(obsTrack.getPlayCount().get(),track.getPlayCount());
		assertEquals(obsTrack.getDiscNumber().get(),track.getDiscNumber());
		assertEquals(obsTrack.getBPM().get(),track.getBPM());
		assertEquals(obsTrack.getHasCover().get(),track.isHasCover());
		assertEquals(obsTrack.getIsInDisk().get(),track.isInDisk());
		assertEquals(obsTrack.getIsCompilation().get(),track.isCompilation());
		assertEquals(obsTrack.getDateModified().get(),track.getDateModified());
		assertEquals(obsTrack.getDateAdded().get(),track.getDateAdded());
	}
}