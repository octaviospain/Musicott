package net.transgressoft.musicott.view

import net.transgressoft.musicott.search.SearchCoordinator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Kotlin supplement for [SearchFlowITConfiguration] providing the real [SearchCoordinator]
 * bean. Extracted to a Kotlin source file because Java cannot call Kotlin constructors that
 * use default parameter values without explicit [@JvmOverloads][kotlin.jvm.JvmOverloads] — and
 * adding that annotation to [SearchCoordinator] causes Spring's autowiring to fail when
 * multiple overloads are present. Keeping construction in Kotlin avoids both issues.
 *
 * The coordinator is created with `debounceMillis = 0` so that non-blank queries execute
 * immediately without the production 400 ms wait. This keeps tests deterministic while still
 * exercising the real Dispatchers.Default → Dispatchers.JavaFx coroutine thread-hop path.
 */
@Configuration
class SearchFlowITCoordinatorSupplier {

    @Bean(destroyMethod = "close")
    fun searchCoordinator(
        applicationEventPublisher: ApplicationEventPublisher
    ): SearchCoordinator = SearchCoordinator(applicationEventPublisher, debounceMillis = 0)
}
