package com.transgressoft.musicott.util;

import com.google.common.collect.*;
import com.transgressoft.musicott.model.*;
import org.junit.jupiter.api.*;

import static com.transgressoft.musicott.util.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class Utils_GetArtistsInvolvedInTrack {

    Track testTrack;
    ImmutableSet<String> expectedArtists;

    @Test
    @DisplayName ("Empty track")
    void emptyTrack() {
        testTrack = new Track("", "");
        expectedArtists = ImmutableSet.of();

        assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
    }

    @Nested
    @DisplayName ("In artist field")
    class namesInArtistField {

        @Test
        @DisplayName ("One name")
        void oneNameInArtist() {
            initTrackWithArtistAndResult("Adam Beyer", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        private void initTrackWithArtistAndResult(String artistString, String... expectedArtist) {
            testTrack = new Track("", "");
            testTrack.setArtist(artistString);
            expectedArtists = ImmutableSet.<String> builder().add(expectedArtist).build();
        }

        @Test
        @DisplayName ("One name with trail spaces")
        void oneNameWithSpacesInArtist() {
            initTrackWithArtistAndResult("Adam Beyer    ", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("One name with leading spaces")
        void oneNameWithLeadingSpacesInArtist() {
            initTrackWithArtistAndResult("Adam      Beyer", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("One name with leading and trailing spaces")
        void oneNameWthLeadingAndTrailingSpacesInArtist() {
            initTrackWithArtistAndResult("Adam     Beyer    ", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Nested
        @DisplayName ("Comma separated")
        class commaSeparated {

            @Test
            @DisplayName ("Two names")
            void twoNamesCommaSeparated() {
                initTrackWithArtistAndResult("Adam Beyer, Ida Engberg", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two names with trailing spaces")
            void twoNamesCommaSeparatedWithTrailingSpaces() {
                initTrackWithArtistAndResult("Adam Beyer  , Ida Engberg   ", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two names with leading spaces")
            void twoNamesCommaSeparatedWithLeadingSpaces() {
                initTrackWithArtistAndResult("Adam    Beyer, Ida   Engberg", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names")
            void threeNamesCommaSeparated() {
                initTrackWithArtistAndResult("Adam Beyer, Ida Engberg, UMEK", "Adam Beyer", "Ida Engberg", "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names with leading and trailing spaces")
            void threeNamesCommaWithLeadingAndTrailingSpacsSeparated() {
                initTrackWithArtistAndResult("Adam    Beyer  ,   Ida  Engberg ,   UMEK ", "Adam Beyer", "Ida Engberg",
                                             "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two repeated names")
            void twoNamesRepeatedCommaSeparated() {
                initTrackWithArtistAndResult("Adam Beyer, Adam Beyer", "Adam Beyer");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }
        }

        @Nested
        @DisplayName ("& separated")
        class andpersandSeparated {

            @Test
            @DisplayName ("Two names")
            void twoNamesAndpersandSeparated() {
                initTrackWithArtistAndResult("Adam Beyer & Ida Engberg", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two names with leading and trailing spaces")
            void twoNamesAndpersandWithSpacesSeparated() {
                initTrackWithArtistAndResult("Adam   Beyer  &     Ida Engberg ", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names")
            void threeNamesAndpersandSeparated() {
                initTrackWithArtistAndResult("Adam Beyer & Ida Engberg & UMEK", "Adam Beyer", "Ida Engberg", "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names with leading and trailing spaces")
            void threeNamesAndpersandWithSpacesSeparated() {
                initTrackWithArtistAndResult("Adam   Beyer  & Ida  Engberg &  UMEK ", "Adam Beyer", "Ida Engberg",
                                             "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two repeated names")
            void twoNamesRepeatedAnpersandSeparated() {
                initTrackWithArtistAndResult("Adam Beyer & Adam Beyer", "Adam Beyer");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }
        }

        @Nested
        @DisplayName ("'vs' separated")
        class vsSeparated {

            @Test
            @DisplayName ("Two names")
            void twoNamesvsSeparated() {
                initTrackWithArtistAndResult("Adam Beyer vs Ida Engberg", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names")
            void threeNamesVsSeparated() {
                initTrackWithArtistAndResult("Adam Beyer vs Ida Engberg vs UMEK", "Adam Beyer", "Ida Engberg", "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two repeated names")
            void twoNamesRepeatedVsSeparated() {
                initTrackWithArtistAndResult("Adam Beyer vs Adam Beyer", "Adam Beyer");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }
        }

        @Nested
        @DisplayName ("'versus' separated")
        class versusSeparated {

            @Test
            @DisplayName ("Two names")
            void twoNamesVersusSeparated() {
                initTrackWithArtistAndResult("Adam Beyer versus Ida Engberg", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names")
            void threeNamesVersusSeparated() {
                initTrackWithArtistAndResult("Adam Beyer versus Ida Engberg versus UMEK", "Adam Beyer", "Ida Engberg",
                                             "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two repeated names")
            void twoNamesRepeatedVersusSeparated() {
                initTrackWithArtistAndResult("Adam Beyer versus Adam Beyer", "Adam Beyer");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }
        }

        @Nested
        @DisplayName ("'vs.' separated")
        class versusDotSeparated {

            @Test
            @DisplayName ("Two names")
            void twoNamesVsDotSeparated() {
                initTrackWithArtistAndResult("Adam Beyer vs. Ida Engberg", "Adam Beyer", "Ida Engberg");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Three names")
            void threeNamesVsDotSeparated() {
                initTrackWithArtistAndResult("Adam Beyer vs. Ida Engberg vs. UMEK", "Adam Beyer", "Ida Engberg",
                                             "UMEK");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Two repeated names")
            void twoNamesRepeatedVsDotSeparated() {
                initTrackWithArtistAndResult("Adam Beyer vs. Adam Beyer", "Adam Beyer");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }
        }

        @Nested
        @DisplayName ("Comma and & separated")
        class commaAndpersandSeparated {

            @Test
            @DisplayName ("Three names")
            void threeNamesCommaAndpersandSeparated() {
                initTrackWithArtistAndResult("Adam Beyer, Ida Engberg & Ansome", "Adam Beyer", "Ida Engberg", "Ansome");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Four names")
            void fourNamesCommaAndpersandSeparated() {
                initTrackWithArtistAndResult("Adam Beyer & Ida Engberg, UMEK & Ansome", "Adam Beyer", "Ida Engberg",
                                             "UMEK", "Ansome");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Five names")
            void fiveNamesCommaAndpersandSeparated() {
                initTrackWithArtistAndResult("Adam Beyer & UMEK, Showtek, Ansome & Ida Engberg", "Adam Beyer",
                                             "Ida Engberg", "UMEK", "Showtek", "Ansome");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }

            @Test
            @DisplayName ("Five name with leading and trailing spaces")
            void fiveNamesCommaAndpersandWithSpacesTest() {
                initTrackWithArtistAndResult(" Adam  Beyer , UMEK  & Showtek , Ansome   & Ida   Engberg ", "Adam Beyer",
                                             "Ida Engberg", "UMEK", "Showtek", "Ansome");
                assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
            }
        }
    }

    @Nested
    @DisplayName ("In name field")
    class namesInsideParenthesisInNameField {

        @Test
        @DisplayName ("Just the track name")
        void justTheTrackName() {
            initTrackWithNameAndResult("Song name");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        private void initTrackWithNameAndResult(String nameString, String... expectedArtist) {
            testTrack = new Track("", "");
            testTrack.setName(nameString);
            expectedArtists = ImmutableSet.<String> builder().add(expectedArtist).build();
        }

        @Test
        @DisplayName ("Original mix")
        void originalMix() {
            initTrackWithNameAndResult("Song name (Original Mix)");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Edit version")
        void editVersion() {
            initTrackWithNameAndResult("Song name (Special time edit)");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Ends with 'Remix'")
        void endsWithRemix() {
            initTrackWithNameAndResult("Song name (Adam Beyer Remix)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'Remix' with useless spaces")
        void hasRemixWithUselessSpaces() {
            initTrackWithNameAndResult(" Song   name ( Adam   Beyer  Remix)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'Remix by'")
        void hasRemixBy() {
            initTrackWithNameAndResult("Song name (Remix by Adam Beyer)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Starts with 'Remix by' with useless spaces")
        void hasRemixByWithUselessSpaces() {
            initTrackWithNameAndResult("Song   name  (Remix    by  Adam   Beyer)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'Ft'")
        void hasFt() {
            initTrackWithNameAndResult("Song name ft Adam Beyer", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'Ft' inside parenthesis")
        void hasFtInsideParenthesis() {
            initTrackWithNameAndResult("Song name (ft Adam Beyer)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'Feat'")
        void hasFeat() {
            initTrackWithNameAndResult("Song name feat Adam Beyer", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'Feat' inside parenthesis")
        void hasFeatInsideParenthesis() {
            initTrackWithNameAndResult("Song name (feat Adam Beyer)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'featuring'")
        void hasFeaturing() {
            initTrackWithNameAndResult("Song name featuring Adam Beyer", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'featuring' inside parenthesis")
        void hasFeaturingInsideParenthesis() {
            initTrackWithNameAndResult("Song name (featuring Adam Beyer)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Has 'With'")
        void hasWith() {
            initTrackWithNameAndResult("Song name (With Adam Beyer)", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Two artists inside parenthesis divided by & ending with Remix")
        void twoArtistsDividedByAndpersandEndingWithRemix() {
            initTrackWithNameAndResult("Song name (Adam Beyer & Pete Tong Remix)", "Adam Beyer", "Pete Tong");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }
    }

    @Nested
    @DisplayName ("In album artist field")
    class namesInsideAlbumArtistField {

        @Test
        @DisplayName ("One name")
        void oneNameInAlbumArtist() {
            initTrackWithNameAndResult("Adam Beyer", "Adam Beyer");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        private void initTrackWithNameAndResult(String albumArtistString, String... expectedArtist) {
            testTrack = new Track("", "");
            testTrack.setAlbumArtist(albumArtistString);
            expectedArtists = ImmutableSet.<String> builder().add(expectedArtist).build();
        }

        @Test
        @DisplayName ("Two names separated by commas")
        void twoNamesInAlbumArtistCommSeparated() {
            initTrackWithNameAndResult("Adam Beyer, UMEK", "Adam Beyer", "UMEK");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Two names separated by &")
        void twoNamesInAlbumArtistAndpersandSeparated() {
            initTrackWithNameAndResult("Adam Beyer & Pete Tong", "Adam Beyer", "Pete Tong");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Three names separated by & and comma")
        void threeNamesInAlbumArtistAndpersandCommaSeparated() {
            initTrackWithNameAndResult("Adam Beyer, Pete Tong & UMEK", "Adam Beyer", "Pete Tong", "UMEK");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }
    }

    @Nested
    @DisplayName ("In artist, name and album artist fields")
    class namesInArtistNameAndAlbumFields {

        @Test
        @DisplayName ("Simple name, one artist, same album artist")
        void simpleNameOneArtistSameAlbumArtist() {
            initTrackWithNameAndResult("Song name", "Pete Tong", "Pete Tong", "Pete Tong");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        private void initTrackWithNameAndResult(String name, String artist, String albumArtist,
                String... expectedArtist) {
            testTrack = new Track("", "");
            testTrack.setName(name);
            testTrack.setArtist(artist);
            testTrack.setAlbumArtist(albumArtist);
            expectedArtists = ImmutableSet.<String> builder().add(expectedArtist).build();
        }

        @Test
        @DisplayName ("Simple name, one artist, one album artist")
        void simpleNameOneArtistOneAlbumArtist() {
            initTrackWithNameAndResult("Song name", "Pete Tong", "Jeff Mills", "Pete Tong", "Jeff Mills");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Simple name, two artists, same album artist")
        void simleNameTwoArtistsSameAlbumArtist() {
            initTrackWithNameAndResult("Song name", "Pete Tong, UMEK", "Pete Tong", "Pete Tong", "UMEK");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Name with 'Remix', one artist, no album artist")
        void nameWithRemixOneArtistNoAlbumArtist() {
            initTrackWithNameAndResult("Song name (Ansome Remix)", "Pete Tong", "", "Pete Tong", "Ansome");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Name with featuring, two artists with comma, one repeated album artist")
        void oneNameOneArtistOneAlbumArtist() {
            initTrackWithNameAndResult("Song name featuring Lulu Perez", "Pete Tong & Ansome", "Pete Tong", "Pete Tong",
                                       "Lulu Perez", "Ansome");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }

        @Test
        @DisplayName ("Name with 'Remix by', two artists with &, one other album artist")
        void nameWithRemixByTwoArtistsWithAndpersandOneOtherAlbumArtist() {
            initTrackWithNameAndResult("Song name (Remix by Bonobo)", "Laurent Garnier & Rone", "Pete Tong",
                                       "Pete Tong", "Bonobo", "Laurent Garnier", "Rone");
            assertEquals(expectedArtists, getArtistsInvolvedInTrack(testTrack));
        }
    }
}