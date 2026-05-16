package net.transgressoft.musicott

import mu.KotlinLogging
import net.transgressoft.commons.music.audio.AudioItemManipulationException
import net.transgressoft.commons.music.audio.AudioMetadataIO
import net.transgressoft.musicott.events.EditAudioItemsMetadataEvent
import net.transgressoft.musicott.events.ExceptionEvent
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
        editAudioItemsMetadataEvent.audioItems.forEach { updatedAudioItem ->
            try {
                audioMetadataIO.writeMetadata(updatedAudioItem)
            } catch (exception: AudioItemManipulationException) {
                logger.error("Error during audio metadata edition", exception)
                applicationEventPublisher.publishEvent(ExceptionEvent(exception, this))
            }
        }
    }
}
