package me.spoo.android

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.spoo.SpooClient
import me.spoo.SpooConfig
import me.spoo.android.auth.AuthManager
import me.spoo.android.auth.TokenStore
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.spoo.android.data.LinksRepository
import me.spoo.android.data.MockLinksRepository
import me.spoo.android.data.SdkLinksRepository
import me.spoo.android.data.SettingsRepository
import me.spoo.android.data.SwitchingLinksRepository
import me.spoo.android.widget.SpooWidget
import me.spoo.oauth.Session

/**
 * Composition root, by hand. Small enough that Hilt would be ceremony;
 * revisit when a third subsystem needs injection.
 */
class AppGraph(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
    }
}

class SpooApp : Application() {
    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        runCatching { me.spoo.android.widget.WidgetRefreshWorker.schedule(this) }
    }

    companion object {
        lateinit var graph: AppGraph
            private set
    }
}
