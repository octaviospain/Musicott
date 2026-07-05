package net.transgressoft.musicott.test

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.music.audio.AlbumDetails
import net.transgressoft.commons.music.audio.Artist
import net.transgressoft.commons.music.audio.AudioItemMetadata
import net.transgressoft.commons.music.audio.AudioItemTestAttributes
import net.transgressoft.commons.music.audio.Genre
import net.transgressoft.commons.music.audio.audioAttributes
import net.transgressoft.commons.music.audio.audioItemTrackDiscNumberComparator
import net.transgressoft.lirp.entity.ReactiveEntityBase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.ReadOnlySetProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleSetProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.scene.image.Image
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional
import java.util.function.Consumer

/**
 * Builds observable audio item test fixtures with an explicit involved-artist set.
 *
 * The view layer renders related-artist and album labels from an item's `artistsInvolved`, so
 * these tests need to assert against an exact involved-artist membership rather than the derived
 * set produced by the title parser heuristics. The mocked generator in `music-commons-fx-test`
 * derives that set from the title/artist strings and cannot express an arbitrary membership, so
 * this factory returns a lightweight concrete item with real JavaFX properties whose
 * `artistsInvolved` is exactly the supplied set.
 */
object FxAudioItems {

    /**
     * Creates an observable audio item with an explicit involved-artist set.
     *
     * @param attributes action that mutates the generated attributes before the item is built
     * @param artistsInvolved exact artist set returned by the item and its JavaFX property
     * @return observable audio item with real JavaFX properties
     */
    @JvmStatic
    fun createFxAudioItem(attributes: Consumer<AudioItemTestAttributes>, artistsInvolved: Set<Artist>): ObservableAudioItem {
        val generated = Arb.audioAttributes().next()
        attributes.accept(generated)
        return TestObservableAudioItem(generated, artistsInvolved)
    }

    private class TestObservableAudioItem(
        attributes: AudioItemTestAttributes,
        override val artistsInvolved: Set<Artist>
    ) : ObservableAudioItem,
        Comparable<ObservableAudioItem>,
        ReactiveEntityBase<Int, ObservableAudioItem>() {

        private val metadata = attributes.metadata

        override val id: Int = attributes.id
        override val uniqueId: String = "${attributes.path.fileName}-${metadata.title}-${attributes.id}"
        override val path: Path = attributes.path
        override val duration: Duration = metadata.duration
        override val bitRate: Int = metadata.bitRate
        override val encoder: String? = metadata.encoder
        override val encoding: String? = metadata.encoding
        override val dateOfCreation: LocalDateTime = attributes.dateOfCreation
        override val playCount: Short = attributes.playCount
        override val fileName: String = path.fileName.toString()
        override val extension: String = path.fileName.toString().substringAfterLast('.', "")
        override val length: Long = 0

        override val titleProperty: StringProperty = SimpleStringProperty(this, "title", metadata.title)
        override var title: String
            get() = titleProperty.get()
            set(value) = titleProperty.set(value)

        override val artistProperty: ObjectProperty<Artist> = SimpleObjectProperty(this, "artist", metadata.artist)
        override var artist: Artist
            get() = artistProperty.get()
            set(value) = artistProperty.set(value)

        override val albumProperty: ObjectProperty<AlbumDetails> = SimpleObjectProperty(this, "album", metadata.album)
        override var album: AlbumDetails
            get() = albumProperty.get()
            set(value) = albumProperty.set(value)

        override val genresProperty: ObjectProperty<Set<Genre>> = SimpleObjectProperty(this, "genres", metadata.genres)
        override var genres: Set<Genre>
            get() = genresProperty.get()
            set(value) = genresProperty.set(value)

        override val commentsProperty: StringProperty = SimpleStringProperty(this, "comments", metadata.comments)
        override var comments: String?
            get() = commentsProperty.get()
            set(value) = commentsProperty.set(value)

        override val trackNumberProperty: IntegerProperty = SimpleIntegerProperty(this, "track number", metadata.trackNumber?.toInt() ?: 0)
        override var trackNumber: Short?
            get() = trackNumberProperty.get().takeIf { it > 0 }?.toShort()
            set(value) = trackNumberProperty.set(value?.toInt() ?: 0)

        override val discNumberProperty: IntegerProperty = SimpleIntegerProperty(this, "disc number", metadata.discNumber?.toInt() ?: 0)
        override var discNumber: Short?
            get() = discNumberProperty.get().takeIf { it > 0 }?.toShort()
            set(value) = discNumberProperty.set(value?.toInt() ?: 0)

        override val bpmProperty: FloatProperty = SimpleFloatProperty(this, "bpm", metadata.bpm ?: 0f)
        override var bpm: Float?
            get() = bpmProperty.get().takeIf { it > 0f }
            set(value) = bpmProperty.set(value ?: 0f)

        override val coverImageProperty: ReadOnlyObjectProperty<Optional<Image>> =
            SimpleObjectProperty(this, "cover image", Optional.empty())

        override val artistsInvolvedProperty: ReadOnlySetProperty<Artist> =
            SimpleSetProperty(this, "artists involved", FXCollections.observableSet(artistsInvolved))

        override val lastDateModifiedProperty: ReadOnlyObjectProperty<LocalDateTime> =
            SimpleObjectProperty(this, "last date modified", attributes.lastDateModified)

        override val dateOfCreationProperty: ReadOnlyProperty<LocalDateTime> =
            SimpleObjectProperty(this, "date of creation", dateOfCreation)

        override val playCountProperty: ReadOnlyIntegerProperty =
            SimpleIntegerProperty(this, "play count", playCount.toInt())

        override var coverImageBytes: ByteArray? = metadata.coverBytes

        override fun setPlayCount(count: Short): Unit =
            throw UnsupportedOperationException("play count is immutable on this test fixture")

        override fun mutate(action: ObservableAudioItem.() -> Unit) = mutateAndPublish { action() }

        override fun compareTo(other: ObservableAudioItem): Int =
            audioItemTrackDiscNumberComparator<ObservableAudioItem>().compare(this, other)

        override fun clone(): ObservableAudioItem =
            TestObservableAudioItem(
                AudioItemTestAttributes(
                    path = path,
                    id = id,
                    metadata =
                        AudioItemMetadata(
                            title = title,
                            artist = artist,
                            album = album,
                            genres = genres,
                            comments = comments,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            bpm = bpm,
                            encoder = encoder,
                            encoding = encoding,
                            bitRate = bitRate,
                            duration = duration,
                            coverBytes = coverImageBytes
                        ),
                    dateOfCreation = dateOfCreation,
                    lastDateModified = lastDateModified,
                    playCount = playCount
                ),
                artistsInvolved
            )
    }
}
