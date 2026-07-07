package net.transgressoft.musicott

import mu.KotlinLogging
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.music.audio.AlbumDetails
import net.transgressoft.commons.music.audio.AudioItemManipulationException
import net.transgressoft.commons.music.audio.AudioMetadataIO
import net.transgressoft.musicott.events.EditAudioItemsMetadataEvent
import net.transgressoft.musicott.events.ExceptionEvent
import net.transgressoft.musicott.events.StatusMessageUpdateEvent
import net.transgressoft.musicott.logging.RingBufferHolder
import net.transgressoft.musicott.view.EditController.AudioItemMetadataChange
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MusicLibraryEventSubscriber(
    private val audioMetadataIO: AudioMetadataIO,
    private val applicationEventPublisher: ApplicationEventPublisher) {
    private val logger = KotlinLogging.logger {}

    @EventListener
    fun editAudioItemsListener(editAudioItemsMetadataEvent: EditAudioItemsMetadataEvent) {
        val mark = RingBufferHolder.INSTANCE.warnErrorCount()
        val change = editAudioItemsMetadataEvent.audioItemMetadataChange
        editAudioItemsMetadataEvent.audioItems.forEach { updatedAudioItem ->
            try {
                applyMetadataChange(updatedAudioItem, change)
                audioMetadataIO.writeMetadata(updatedAudioItem)
            } catch (exception: AudioItemManipulationException) {
                logger.error("Error during audio metadata edition", exception)
                applicationEventPublisher.publishEvent(ExceptionEvent(exception, this))
            }
        }
        val delta = (RingBufferHolder.INSTANCE.warnErrorCount() - mark).toInt()
        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Metadata updated", delta, this))
    }

    /**
     * Applies the collected editor changes onto the live item so the reactive setters fire and the
     * library projections (all-tracks table, genre/album/artist views) re-key. A {@code null} field
     * in the change means "leave untouched"; only fields the editor actually resolved are written.
     * All writes happen inside a single [ObservableAudioItem.mutate] block so downstream projections
     * re-key once per item rather than once per field.
     */
    private fun applyMetadataChange(audioItem: ObservableAudioItem, change: AudioItemMetadataChange) {
        audioItem.mutate {
            change.title()?.let { title = it }
            change.artist()?.let { artist = it }
            change.genres()?.let { genres = it }
            change.comments()?.let { comments = it }
            change.trakNum()?.let { trackNumber = it }
            change.discNum()?.let { discNumber = it }
            change.bpm()?.let { bpm = it }
            change.coverImageBytes()?.let { coverImageBytes = it }

            // Album attributes live on the immutable AlbumDetails value; rebuild it once from the
            // current album, overriding only the fields the editor resolved, so a single album
            // property change re-keys the album projection.
            val albumName = change.albumName()
            val albumArtist = change.albumArtist()
            val compilation = change.isCompilation()
            val year = change.year()
            val label = change.label()
            if (albumName != null || albumArtist != null || compilation != null || year != null || label != null) {
                album = AlbumDetails(
                    albumName ?: album.name,
                    albumArtist ?: album.albumArtist,
                    compilation ?: album.isCompilation,
                    year ?: album.year,
                    label ?: album.label)
            }
        }
    }
}
