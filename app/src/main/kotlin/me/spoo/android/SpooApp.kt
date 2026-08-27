package me.spoo.android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.spoo.SpooClient
import me.spoo.SpooConfig
import me.spoo.android.auth.AuthManager
import me.spoo.android.auth.TokenStore
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.spoo.android.data.LinksRepository
import me.spoo.android.data.MockLinksRepository
import me.spoo.android.data.SdkLinksRepository
import me.spoo.android.data.SettingsRepository
import me.spoo.android.data.SwitchingLinksRepository
import me.spoo.android.widget.SpooWidget
import me.spoo.android.widget.writeWidgetTheme
import me.spoo.oauth.Session

/**
 * Composition root, by hand. Small enough that Hilt would be ceremony;
 * revisit when a third subsystem needs injection.
 */
class AppGraph(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget widget recomposition (ui-mode flips, etc.). */
    fun pushWidgetUpdate(context: Context) {
        val appContext = context.applicationContext
        scope.launch { runCatching { SpooWidget().updateAll(appContext) } }
    }
    private val clientTag = "app-android/${BuildConfig.VERSION_NAME}"

    private fun newClient(session: Session?) = SpooClient(
        SpooConfig(
            session = session,
            baseUrl = BuildConfig.SPOO_BASE_URL,
            clientTag = clientTag,
        ),
    )

    private val anonClient = newClient(null)

    @Volatile
    private var currentClient: SpooClient = anonClient

    val tokenStore = TokenStore(context)
    val settingsRepository = SettingsRepository(context)
    val authManager = AuthManager(tokenStore, anonClient, scope)
    val linksRepository: LinksRepository = SwitchingLinksRepository(
        real = SdkLinksRepository(
            clientProvider = { currentClient },
            onSessionExpired = { authManager.signOut() },
        ),
        mock = MockLinksRepository(),
        scope = scope,
        mockEnabled = settingsRepository.settings.map { it.mockData }.distinctUntilChanged(),
    )

    init {
        scope.launch {
            authManager.restore()
            authManager.session.collect { session ->
                currentClient = if (session == null) anonClient else newClient(session)
                runCatching { linksRepository.refresh() }
            }
        }
        // Keep home-screen widgets in sync with whatever the app knows.
        val appContext = context.applicationContext
        scope.launch {
            linksRepository.links.collect {
                runCatching { SpooWidget().updateAll(appContext) }
            }
        }
        // Theme choices recolor widgets immediately: write the trio into
        // every widget's STATE first (warm sessions only recompose, they
        // never re-run provideGlance), then poke a recomposition.
        scope.launch {
            settingsRepository.settings
                .map { Triple(it.themeMode, it.useDeviceColors, it.seedColor) }
                .distinctUntilChanged()
                .collect {
                    runCatching {
                        val settings = settingsRepository.settings.first()
                        val ids = GlanceAppWidgetManager(appContext)
                            .getGlanceIds(SpooWidget::class.java)
                        for (id in ids) {
                            updateAppWidgetState(appContext, id) {
                                it.writeWidgetTheme(settings)
                            }
                        }
                        SpooWidget().updateAll(appContext)
                    }
                }
        }
    }
}

class SpooApp : Application() {
    private var lastNightMask = -1

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        lastNightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        runCatching { me.spoo.android.widget.WidgetRefreshWorker.schedule(this) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Widget CHART bitmaps are baked with the palette at render time;
        // the declarative parts re-theme themselves on a system light/dark
        // flip but the raster doesn't — re-render on ui-mode changes.
        val nightMask = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMask != lastNightMask) {
            lastNightMask = nightMask
            graph.pushWidgetUpdate(this)
        }
    }

    companion object {
        lateinit var graph: AppGraph
            private set
    }
}
