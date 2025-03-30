package net.transgressoft.musicott

import mu.KotlinLogging
import net.transgressoft.commons.event.StandardCrudEvent.Type.CREATE
import net.transgressoft.commons.event.StandardCrudEvent.Type.DELETE
import net.transgressoft.commons.event.StandardCrudEvent.Type.UPDATE
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.fx.music.audio.ObservableAudioItemJsonRepository
import net.transgressoft.commons.music.audio.AudioItemManipulationException
import net.transgressoft.commons.music.audio.event.AudioItemEventSubscriber
import net.transgressoft.musicott.events.EditAudioItemsMetadataEvent
import net.transgressoft.musicott.events.ExceptionEvent
import net.transgressoft.musicott.events.UpdateArtistViewEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MusicLibraryEventSubscriber(
    private val audioRepository: ObservableAudioItemJsonRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : AudioItemEventSubscriber<ObservableAudioItem>("MusicLibraryEventBus") {
    private val logger = KotlinLogging.logger {}

    init {
        audioRepository.subscribe(this)
        addOnErrorEventAction { exception: Throwable ->
            logger.error("Exception occurred processing audio items", exception)
        }
        addOnNextEventAction(CREATE, UPDATE, DELETE) { event ->
            val artistViews =
                event.entities.values
                    .groupBy(ObservableAudioItem::artist)
                    .mapValues { audioRepository.getArtistCatalog(it.key).get() }

            applicationEventPublisher.publishEvent(UpdateArtistViewEvent(artistViews, this))
        }
    }

    @EventListener
    fun editAudioItemsListener(editAudioItemsMetadataEvent: EditAudioItemsMetadataEvent) {
        editAudioItemsMetadataEvent.audioItems.forEach { updatedAudioItem ->
            try {
                updatedAudioItem.writeMetadata()
            } catch (exception: AudioItemManipulationException) {
                logger.error("Error during audio metadata edition", exception)
                applicationEventPublisher.publishEvent(ExceptionEvent(exception, this))
            }
        }
    }
}
