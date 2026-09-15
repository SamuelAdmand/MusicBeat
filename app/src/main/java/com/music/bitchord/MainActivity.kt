package com.music.bitchord

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import com.music.bitchord.feature.localmusic.ui.components.LocalPermissionCard
import com.music.bitchord.ui.screens.LOCAL_TAB_SONGS
import com.music.bitchord.ui.screens.LOCAL_TAB_ALBUMS
import com.music.bitchord.ui.screens.LOCAL_TAB_ARTISTS
import com.music.bitchord.ui.screens.LOCAL_TAB_FOLDERS
import com.music.bitchord.ui.screens.LOCAL_TAB_PLAYLISTS
import com.music.bitchord.feature.localsearch.ui.LocalSearchScreen
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.bitchord.data.LocalMediaRepository
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.screens.SettingsScreen
import com.music.bitchord.playback.LinkRequest
import com.music.bitchord.playback.MusicLink
import com.music.bitchord.playback.OriginalVersion
import com.music.bitchord.playback.PlayerDeepLink
import com.music.bitchord.playback.QueueBuilder
import com.music.bitchord.playback.QueueShuffle
import com.music.bitchord.playback.autoplaySectionStart
import com.music.bitchord.playback.beginRadioQueue
import com.music.bitchord.playback.commitRadioQueue
import com.music.bitchord.playback.fromAutoplay
import com.music.bitchord.playback.hasYouTubeOriginal
import com.music.bitchord.playback.loadAutoplayTracks
import com.music.bitchord.playback.playSongs
import com.music.bitchord.playback.toMediaItem
import com.music.bitchord.playback.toSong
import com.music.bitchord.playback.toDirectYouTubeMediaItem
import com.music.bitchord.playback.toggleAutoplay
import com.music.bitchord.playback.upgradeQuality
import com.music.bitchord.download.DownloadSession
import com.music.bitchord.download.DownloadStore
import com.music.bitchord.download.DownloadTarget
import com.music.bitchord.download.Downloads
import com.music.bitchord.ui.components.BrowseActionsSheet
import com.music.bitchord.ui.components.BrowseTarget
import com.music.bitchord.ui.components.DownloadManagerSheet
import com.music.bitchord.ui.components.PlaylistPickerSheet
import com.music.bitchord.ui.components.SongActionsSheet
import com.music.bitchord.playback.rememberMediaController
import com.music.bitchord.playback.rememberPlayerState
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.components.BottomFadeScrim
import com.music.bitchord.ui.components.BottomTab
import com.music.bitchord.ui.components.FLOATING_BAR_MAX_WIDTH
import com.music.bitchord.ui.components.FloatingBottomBar
import com.music.bitchord.ui.components.GlassNavBar
import com.music.bitchord.ui.components.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.music.bitchord.ui.components.FrostedTopBar
import com.music.bitchord.ui.components.LastfmLoginAlert
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.ui.components.ListenBrainzTokenAlert
import com.music.bitchord.ui.components.MiniPlayer
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.components.TopFadeBlur
import com.music.bitchord.ui.components.topBarContentPadding
import com.music.bitchord.ui.components.AppLanguageDialog
import com.music.bitchord.ui.components.LyricsSourcesDialog
import com.music.bitchord.ui.icons.BitChordIcons
import androidx.media3.common.Player
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.ui.player.NowPlayingScreen
import com.music.bitchord.ui.player.dockedPlayerAvailable
import com.music.bitchord.ui.player.dockedPlayerWidth
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.feature.localsongactions.ui.components.LocalLyricsEditorSheet
import com.music.bitchord.feature.localsongactions.ui.components.LocalSongDetailsSheet
import com.music.bitchord.feature.localsongactions.ui.components.LocalTagEditorSheet
import com.music.bitchord.ui.screens.DetailScreen
import com.music.bitchord.ui.screens.LocalMusicScreen
import com.music.bitchord.ui.replay.ReplayScreen
import com.music.bitchord.ui.replay.cards
import com.music.bitchord.ui.replay.ReplayShareSheet
import com.music.bitchord.ui.replay.ReplayStories
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.replay.rememberReplayState
import com.music.bitchord.ui.theme.BitChordTheme
import com.music.bitchord.ui.theme.rememberArtworkPalette
import com.music.bitchord.ui.theme.SystemBarIcons
import com.music.bitchord.ui.utils.rememberIosOverscrollFactory
import com.music.bitchord.ui.performance.resolvePerformanceRefreshRate
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import java.util.Locale

/** A full first screen of a native YouTube Music radio before AutoPlay tops it up. */
private const val INITIAL_RADIO_TRACKS = 24

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Before the composition, so a cold launch from a widget's artwork has
        // the request already standing by the time BitChordApp first reads it.
        PlayerDeepLink.consume(intent)
        // Likewise for a link tapped or shared from another app — see [MusicLink].
        MusicLink.consume(intent)
        setContent {
            val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
            val highPerformance by AppSettings.highPerformanceMode.collectAsStateWithLifecycle()
            val liquidGlassEnabled by AppSettings.liquidGlass.collectAsStateWithLifecycle()
            val iosOverscrollFactory = rememberIosOverscrollFactory()
            val performanceRefreshRate by AppSettings.performanceRefreshRate.collectAsStateWithLifecycle()
            val composeView = LocalView.current
            LaunchedEffect(highPerformance, performanceRefreshRate, composeView) {
                applyPerformanceMode(highPerformance, performanceRefreshRate, composeView)
            }
            val darkTheme = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            BitChordTheme(darkTheme = darkTheme) {
                // The glass surfaces sample this layer, and a layer records only
                // what is drawn into it — which, for BitChord, is a page that
                // paints no background of its own. Everywhere a page is not
                // showing artwork the recording is transparent, so the glass had
                // nothing to blur there and you saw straight through it to the
                // sharp page underneath: album art came through the bar blurred
                // and text came through it untouched. The window's background is
                // the floor the pages have always been drawn against, so it is
                // laid down here too and the recording is opaque like the screen.
                val windowBackground = MaterialTheme.colorScheme.background
                val paintBackdrop: ContentDrawScope.() -> Unit = remember(windowBackground) {
                    {
                        drawRect(windowBackground)
                        drawContent()
                    }
                }
                val appBackdrop = rememberLayerBackdrop(onDraw = paintBackdrop)
                CompositionLocalProvider(
                    LocalOverscrollFactory provides iosOverscrollFactory,
                    LocalLiquidGlassEnabled provides liquidGlassEnabled,
                    LocalAppBackdrop provides appBackdrop,
                ) {
                // The window's width, measured rather than asked for.
                //
                // `Configuration.screenWidthDp` is the wrong question here: in a
                // freeform or desktop window it can report the display rather
                // than the window it is actually in, and it lands a beat late
                // when that window is dragged. The layout downstream splits in
                // two on the strength of this number and sizes both halves from
                // it, so a stale one is a player pane sized for a window that no
                // longer exists and a page squeezed to a sliver to pay for it.
                // A measured constraint cannot be stale — it is the very width
                // the split is about to be laid out in.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    BitChordApp(darkTheme = darkTheme, windowWidth = maxWidth, appBackdrop = appBackdrop)
                }
                }
            }
        }
    }

    /**
     * Requests a window refresh rate without forcing a display mode or
     * resolution. Android may still lower it for temperature, battery state or
     * hardware limits, which is why Settings describes this as a preference.
     */
    private fun applyPerformanceMode(enabled: Boolean, refreshRate: Int, composeView: View) {
        val supportedRefreshRate = composeView.display.resolvePerformanceRefreshRate(refreshRate)
        window.attributes = window.attributes.apply {
            preferredRefreshRate = if (enabled) supportedRefreshRate.toFloat() else 0f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRatePowerSavingsBalanced(!enabled)
            composeView.requestedFrameRate = if (enabled) {
                supportedRefreshRate.toFloat()
            } else {
                View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
            }
        }
    }

    /**
     * The other half of the relay. This activity is `singleTask`, so once it is
     * running a second tap on the widget does not rebuild anything — it arrives
     * here, and [onCreate] never runs again.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Replaces what getIntent() returns, so the extra this consumes is the
        // one that just arrived and not the one the task was started with.
        setIntent(intent)
        PlayerDeepLink.consume(intent)
        MusicLink.consume(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitChordApp(
    darkTheme: Boolean,
    /** The width of the window this is laid out in — see the call site. */
    windowWidth: Dp,
    appBackdrop: LayerBackdrop,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val hazeState = remember { HazeState() }
    // Recording the backdrop layer costs a draw pass, so it only runs when the
    // nav bar's glass surface actually has something to sample.
    val glassActive = LocalLiquidGlassEnabled.current && isGlassSupported()
    // "Reduce dynamic blur" keeps the glass bar's *shape* — the folding
    // now-playing-and-tabs component is a layout, not an effect, and dropping
    // back to the two stacked bars would be answering a question about material
    // with a different screen. What it drops is the sampling: the surfaces fill
    // solid (see [Modifier.liquidGlass]) and the whole-page layer recording
    // below goes with them, which is the part that costs a draw pass.
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val glassSamplesBackdrop = glassActive && !reduceDynamicBlur
    // What folds [GlassNavBar] between its expanded and inline shapes. Held here
    // rather than inside the bar because the page's scroll is what drives it,
    // and the page is a sibling of the bar rather than a child.
    val navBarScroll = rememberFloatingTabBarScrollConnection()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // Whether there is room to keep the player open beside the page rather than
    // raising it over one. Read all over what follows, because most of what the
    // page does about the player is really about which of the two it is: no mini
    // player standing in for one that is already there, no sheet to raise, and
    // the bottom inset the mini player was holding handed back to the page.
    val playerDocked = dockedPlayerAvailable(windowWidth)
    /**
     * Whether the player's *sheet* is up.
     *
     * Only ever set where there is a sheet to set it for. Docked, the player is
     * open whatever this says, and the things that read it — the light status
     * bar glyphs the artwork needs, the sheet itself — are all asking the one
     * question this used to answer on its own: is the player covering the page?
     */
    var showNowPlaying by remember { mutableStateOf(false) }
    // The far end of the relay from a widget's artwork. Cleared here rather than
    // where it was set, so the request is spent by being served — see
    // [PlayerDeepLink.handled]. The sheet itself is gated on there being a track,
    // so on a cold launch this simply arms it and it opens as the controller
    // connects. Docked there is nothing to raise: the player is already up, and
    // the tap has been honoured by the time it arrives.
    val openPlayerRequested by PlayerDeepLink.pending.collectAsStateWithLifecycle()
    LaunchedEffect(openPlayerRequested) {
        if (openPlayerRequested) {
            if (!playerDocked) showNowPlaying = true
            PlayerDeepLink.handled()
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showReplay by remember { mutableStateOf(false) }
    var replayStory by remember { mutableStateOf<ReplayStoryPage?>(null) }
    var showReplayShare by remember { mutableStateOf(false) }
    var replaySharePage by remember { mutableStateOf<ReplayStoryPage?>(null) }
    var showLyricsSources by remember { mutableStateOf(false) }
    var showAppLanguage by remember { mutableStateOf(false) }
    var showListenBrainzLogin by remember { mutableStateOf(false) }
    var showLastfmLogin by remember { mutableStateOf(false) }
    var showDownloadManager by remember { mutableStateOf(false) }
    var songActions by remember { mutableStateOf<Song?>(null) }
    /**
     * Whether the track menu that is up was opened from the player.
     *
     * Its copy of the menu carries rows nothing else offers — a sleep timer,
     * the track log, share — and until now "opened from the player" and "the
     * player is on screen" were the same sentence, because the player was a
     * sheet and nothing else could be up behind it. On a tablet the player is
     * never *the* thing on screen: it is always beside whatever is, so the
     * question has to be answered by whoever opened the menu.
     */
    var menuFromPlayer by remember { mutableStateOf(false) }
    /** Holding a row anywhere but the player — the menu without the player's rows. */
    val openSongMenu: (Song) -> Unit = { song ->
        menuFromPlayer = false
        songActions = song
    }
    // Whether the player's album/artist lookup (below, for the current track)
    // is still in flight — read by the long-press sheet so it can show a
    // loading row instead of the two just being absent while it waits.
    var linksLoading by remember { mutableStateOf(false) }
    // Which track the playlist picker is adding, or null when it's closed.
    // Separate from [songActions] so the menu can close behind it — the picker
    // is the next step, not a second sheet stacked on the first.
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    // The picker opened from the Library tab, where there is no track and
    // creating the playlist is the whole errand.
    var creatingPlaylist by remember { mutableStateOf(false) }
    // Which album or playlist the collection menu is open on, or null when it
    // is shut. One slot for every surface that can open it — the shelves on
    // three tabs, the search rows, the artist page's carousels, the release
    // page's own overflow — because only one of them can be held at a time.
    var browseActions by remember { mutableStateOf<BrowseTarget?>(null) }
    val autoplay by AppSettings.autoplay.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()
    // Incremented each time the search tab is re-tapped while already selected,
    // which SearchScreen uses as a signal to focus the input field.
    var searchFocusTrigger by remember { mutableIntStateOf(0) }
    // Invalidates an in-flight radio lookup when a later play request wins.
    var playRequestGeneration by remember { mutableIntStateOf(0) }
    // Starting radio from the item already playing must not replace that media
    // item just to add UI metadata. This temporary label covers that seed; all
    // following radio items carry radioName in their MediaItem extras.
    var activeRadioSeed by remember { mutableStateOf<Pair<String, String>?>(null) }

    // The player fills the screen with dark artwork whichever theme is on, so
    // it keeps light glyphs; every other surface follows the theme. Replay's
    // page and stories are the same case — dark artwork either way.
    SystemBarIcons(dark = !darkTheme && !showNowPlaying && !showReplay && replayStory == null)

    val detailStack by viewModel.detailStack.collectAsStateWithLifecycle()
    val detail = detailStack.lastOrNull()
    // Local Music has no artwork to wash the bar in, so it renders with a
    // plain status bar rather than the artwork-driven blur other detail
    // pages (album/artist/playlist) get. Downloads is the same page, and the
    // tab row it now carries sits directly under the bar, so it needs the same
    // treatment — an artwork blur over it would tint the tabs.
    //
    // A downloaded playlist's page is under `local:` too and is none of that: it
    // has a cover and a track list, so it takes the bar every other release page
    // takes. Hence the folder question rather than the prefix.
    val isLocalDetail = detail?.browseId.isDeviceFolder()
    // Not keyed on the browse id and not remembered here: each page's choice
    // lives in AppSettings keyed by that page — Spotify-style, one playlist's
    // order never imposes itself on another, and every page keeps its own
    // across visits.
    val detailSongSorts by AppSettings.detailSongSorts.collectAsStateWithLifecycle()
    val songSort = detail?.browseId?.let { detailSongSorts[it] } ?: SongSort.DEFAULT
    var songSortMenuOpen by remember { mutableStateOf(false) }
    val likeStatuses by viewModel.likeStatuses.collectAsStateWithLifecycle()
    // Which tracks are being held on YouTube's own upload, so the player's menu
    // offers the way back out of a revert rather than the revert again.
    val pinnedToOriginal by OriginalVersion.pinned.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistsLoading by viewModel.playlistsLoading.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsSource by viewModel.lyricsSource.collectAsStateWithLifecycle()
    val lyricsChecked by viewModel.lyricsChecked.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    // Settings has no tab of its own — it sits on top of whatever tab was
    // selected. A pushed album/artist page (from the player, search, etc.)
    // should surface above it rather than being hidden behind it.
    LaunchedEffect(detail) { if (detail != null) showSettings = false }

    // The Downloads page is a snapshot of the folder, taken when it was opened.
    // Saving a track or deleting one while it is on screen changes what belongs
    // on it — and now that the page groups by artist and album, a stale list is
    // stale counts and a missing row in three places rather than one. So it is
    // taken again whenever the record of what's on disk changes.
    val savedDownloads by Downloads.saved.collectAsStateWithLifecycle()
    val localMusicFolderUri by AppSettings.localMusicFolderUri.collectAsStateWithLifecycle()
    val filterNonMusicAudio by AppSettings.filterNonMusicAudio.collectAsStateWithLifecycle()
    val librarySort by AppSettings.librarySort.collectAsStateWithLifecycle()
    // The releases those files were asked for as — read here rather than in the
    // page so the Downloads folder recomposes when one is added, the same way it
    // does when a file is.
    val savedCollections by Downloads.collections.collectAsStateWithLifecycle()
    // The playlists among them, for the Library page's On Device shelf. Read off
    // both records: the collection record is what says a playlist was downloaded
    // as a playlist, and what is on disk is what says it still has anything left
    // to open.
    val downloadedPlaylists = remember(savedCollections, savedDownloads) {
        Downloads.savedPlaylists()
    }
    // What a browse id is recorded under in Downloads.collections, when it names
    // a release downloaded whole — see BrowseTarget.downloadId. A downloaded
    // playlist's own page and its card both carry the id under the
    // `local:playlist:` prefix; a release still reachable by its real id (an
    // album's own page, a search hit) is looked up directly under that instead.
    val downloadIdFor: (String?) -> String? = { id ->
        id?.let { Downloads.recordIdOf(it) ?: it }?.takeIf { it in savedCollections }
    }
    LaunchedEffect(savedDownloads, savedCollections, detail?.browseId) {
        val open = detail?.browseId ?: return@LaunchedEffect
        // A downloaded playlist's page is a snapshot of the same folder and goes
        // stale for the same reasons — and it is the one page a delete can empty
        // out entirely, which is worth saying rather than leaving rows behind
        // that play nothing.
        if (open == "local:downloads" || Downloads.recordIdOf(open) != null) {
            viewModel.reloadLocalDetail(open)
        }
    }
    LaunchedEffect(localMusicFolderUri, filterNonMusicAudio) {
        if (detail?.browseId == "local:all") {
            viewModel.reloadLocalDetail("local:all")
        }
    }

    val controller = rememberMediaController()
    val player = rememberPlayerState(controller)
    val shuffleEnabled by QueueShuffle.enabled.collectAsStateWithLifecycle()
    // A conversion is deliberately scoped to the current listening session.
    // Keeping the complete original row here lets Revert restore the exact
    // video upload, including its title and playlist identity, rather than
    // trying to reconstruct it from the catalogue match.
    var convertedFromVideo by remember { mutableStateOf<Song?>(null) }
    var convertedAudioId by remember { mutableStateOf<String?>(null) }
    var switchingAudioVersion by remember { mutableStateOf(false) }

    // Lyrics follow whatever is playing; duration lands a beat after the track.
    // Keyed on the lyric settings too, so turning a source on or off applies to
    // the track already playing rather than only the next one.
    val syncedLyricsEnabled by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    LaunchedEffect(player.song?.videoId, player.durationMs, syncedLyricsEnabled, lyricsSources) {
        player.song?.let {
            viewModel.loadLyrics(
                it.videoId,
                it.title,
                it.artist,
                player.durationMs,
                it.albumName,
                it.localUri,
            )
        }
    }

    var hasStoragePermission by remember {
        mutableStateOf(LocalMediaRepository.hasStoragePermission(context))
    }
    val localSongsState by viewModel.localSongs.collectAsStateWithLifecycle()
    val localSongs = (localSongsState as? UiState.Success)?.data.orEmpty()
    val localEmptyMessage = (localSongsState as? UiState.Error)?.message

    LaunchedEffect(Unit) {
        if (LocalMediaRepository.hasStoragePermission(context)) {
            hasStoragePermission = true
            viewModel.loadLocalMusic()
        }
    }

    val searchListState = rememberLazyListState()
    val currentListState = searchListState
    val refreshing = false
    val currentPull = null
    val scrolled by remember(currentListState) {
        derivedStateOf {
            currentListState.firstVisibleItemIndex > 0 ||
                currentListState.firstVisibleItemScrollOffset > 24
        }
    }

    // A pushed album/artist/playlist page has a large header of its own — the
    // sleeve, or an artist's photo running edge to edge — which owns the title
    // until it is scrolled away, exactly as a tab's big heading does. The state
    // is hoisted because the bar lives beside that page rather than inside it,
    // and is rebuilt per page: pushing a second one must not inherit the
    // first's scroll offset.
    // As [detailListState], for Replay: its own large heading owns the title
    // until it is scrolled away, and the bar lives out here rather than on the
    // page. Rebuilt per opening so reopening starts at the top.
    val replayListState = rememberLazyListState()
    val replayScrolled by remember(replayListState) {
        derivedStateOf {
            replayListState.firstVisibleItemIndex > 0 ||
                replayListState.firstVisibleItemScrollOffset > 24
        }
    }

    val detailListState = remember(detail?.browseId) { LazyListState() }
    val detailTitleDrop = with(LocalDensity.current) { DETAIL_TITLE_DROP.toPx() }
    val detailScrolled by remember(detailListState, detailTitleDrop) {
        derivedStateOf {
            detailListState.firstVisibleItemIndex > 0 ||
                detailListState.firstVisibleItemScrollOffset > detailTitleDrop
        }
    }

    // Held, not rebuilt. `listOf` hands back a new instance on every pass, and a
    // List is not a type the compiler can call stable, so under strong skipping
    // the bar this is handed to compares it by identity, never matches, and so
    // can never skip. This composable re-runs on every frame of a scroll — it
    // reads [scrolled] — which made the whole floating bar, both of its states
    // and every glass surface on them recompose once per frame for the length of
    // a fold. Keyed on the labels so a locale change still rebuilds it.
    val songsLabel = stringResource(R.string.songs)
    val albumsLabel = stringResource(R.string.albums)
    val artistsLabel = stringResource(R.string.artists)
    val foldersLabel = "Folders"
    val searchLabel = stringResource(R.string.search)
    val tabs = remember(songsLabel, albumsLabel, artistsLabel, foldersLabel, searchLabel) {
        listOf(
            BottomTab(songsLabel, Icons.Rounded.MusicNote),
            BottomTab(albumsLabel, Icons.Rounded.Album),
            BottomTab(artistsLabel, Icons.Rounded.Person),
            BottomTab(foldersLabel, Icons.Rounded.Folder),
            BottomTab(searchLabel, BitChordIcons.Search),
        )
    }

    val scope = rememberCoroutineScope()

    val play: (List<Song>, Int) -> Unit = { songs, index ->
        playRequestGeneration++
        activeRadioSeed = null
        scope.launch {
            controller?.playSongs(songs, index)
            // Nothing to raise where the player is already open beside the page.
            if (!playerDocked) showNowPlaying = true
        }
    }
    LaunchedEffect(player.song?.videoId) {
        if (activeRadioSeed?.first != player.song?.videoId) activeRadioSeed = null
        if (player.song?.videoId != convertedAudioId) {
            convertedFromVideo = null
            convertedAudioId = null
            switchingAudioVersion = false
        }
    }

    /**
     * A song picked on its own — off a home card or a search hit — starts a
     * station rather than queueing the list it was shown in. Searching
     * "Perfect" and tapping the top hit otherwise queues twenty covers and
     * remixes of the same song. Album, artist and playlist pages keep [play],
     * where the surrounding list *is* the thing the user asked for.
     */
    val playRadio: (Song) -> Unit = { song ->
        playRequestGeneration++
        activeRadioSeed = null
        scope.launch {
            controller?.playSongs(listOf(song), 0)
            if (!playerDocked) showNowPlaying = true
        }
    }

    /**
     * Starts the explicit station offered by every song overflow menu.
     *
     * The related tracks come from YouTube Music's own RDAMVM watch queue.
     * Loading happens before the player is touched so a failed request cannot
     * destroy the queue already playing. Once ready, the whole old queue is
     * replaced in one Media3 operation.
     */
    val startRadio: (Song) -> Unit = { song ->
        val originalController = controller
        if (originalController != null) {
            val request = ++playRequestGeneration
            // Ignore AutoPlay's tail: it may legitimately grow while the
            // request is in flight and does not mean the listener chose a
            // different queue. A new album/song queue does.
            val originalManualQueue = (0 until originalController.mediaItemCount)
                .map { originalController.getMediaItemAt(it) }
                .filterNot { it.fromAutoplay }
                .map { it.mediaId }
            scope.launch {
                val seed = song.copy(radioName = song.title)
                val related = loadAutoplayTracks(
                    existing = listOf(seed),
                    seedSong = seed,
                    limit = INITIAL_RADIO_TRACKS,
                ).getOrElse {
                    if (request == playRequestGeneration) {
                        Toast.makeText(context, R.string.couldnt_load_tracks, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                if (related.isEmpty()) {
                    if (request == playRequestGeneration) {
                        Toast.makeText(context, R.string.couldnt_load_tracks, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val activeController = controller
                val activeManualQueue = (0 until activeController.mediaItemCount)
                    .map { activeController.getMediaItemAt(it) }
                    .filterNot { it.fromAutoplay }
                    .map { it.mediaId }
                if (request != playRequestGeneration || activeManualQueue != originalManualQueue) {
                    return@launch
                }
                // Cancel any armed crossfade/AutoPlay work and erase the old
                // cold-start snapshot before the visible queue is replaced.
                activeController.beginRadioQueue()
                val currentIndex = activeController.currentMediaItemIndex
                val currentItem = activeController.currentMediaItem
                if (currentIndex >= 0 && currentItem?.mediaId == song.videoId) {
                    // Keep the current MediaItem itself untouched. Replacing it,
                    // even with the same song, reparses the source at position
                    // zero and audibly stops/restarts the track.
                    if (currentIndex + 1 < activeController.mediaItemCount) {
                        activeController.removeMediaItems(currentIndex + 1, activeController.mediaItemCount)
                    }
                    if (currentIndex > 0) activeController.removeMediaItems(0, currentIndex)
                    activeController.addMediaItems(1, related.map { it.toMediaItem() })
                    activeRadioSeed = song.videoId to song.title
                } else {
                    activeRadioSeed = null
                    activeController.playSongs(listOf(seed) + related, 0)
                }
                // Make this station — never the queue from before it — what a
                // fresh process restores, even if it is killed immediately.
                activeController.commitRadioQueue()
                Toast.makeText(
                    context,
                    context.getString(R.string.radio_started, song.title),
                    Toast.LENGTH_SHORT,
                ).show()
                if (!playerDocked) showNowPlaying = true
            }
        }
    }
    val addToQueue: (Song) -> Unit = { song ->
        scope.launch {
            // The end of what the user queued, not the end of the queue: a song
            // asked for by name outranks whatever AutoPlay lined up behind it.
            controller?.let {
                val queued = song.copy(radioName = it.currentMediaItem?.toSong()?.radioName)
                it.addMediaItem(it.autoplaySectionStart(), queued.toMediaItem())
            }
        }
    }
    val playNext: (Song) -> Unit = { song ->
        scope.launch {
            controller?.let {
                val queued = song.copy(radioName = it.currentMediaItem?.toSong()?.radioName)
                it.addMediaItem(
                    (it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount),
                    queued.toMediaItem(),
                )
            }
        }
    }
    val onSongSwipe: (Song) -> Unit = { song ->
        if (AppSettings.swipeToPlayNext.value) playNext(song) else addToQueue(song)
    }

    /**
     * Opens an artist or release page given its browse id — or, failing that,
     * its name.
     *
     * Replay's charts are the reason this exists. An artist there is counted by
     * *name*, because a name is the only thing every track carries: a browse id
     * rides along only when the row that queued the track happened to have one,
     * which for a home-feed card or an AutoPlay suggestion it does not. So half
     * the rows on a chart would have nothing to open, and a row that does
     * nothing when tapped is worse than a row that isn't tappable — it reads as
     * the app having failed rather than as the app not offering.
     *
     * Searching for the name is what the user would do next anyway, and it is
     * what the app already does to find a video's catalogue release (see
     * [YtMusicRepository.resolveAudio]). A search that finds nothing says so,
     * which is at least an answer.
     */
    fun openByName(
        browseId: String?,
        name: String,
        subtitle: String?,
        type: BrowseType,
        artwork: String? = null,
    ) {
        if (browseId != null) {
            val credit = subtitle ?: context.getString(
                if (type == BrowseType.ARTIST) R.string.artist else R.string.album,
            )
            viewModel.openDetail(browseId, name, credit, artwork, type)
            return
        }
        scope.launch {
            val filter = if (type == BrowseType.ARTIST) {
                SearchFilter.ARTISTS
            } else {
                SearchFilter.ALBUMS
            }
            val query = listOfNotNull(name, subtitle).joinToString(" ")
            val hit = YtMusicRepository.search(query, filter).getOrNull()
                ?.filterIsInstance<SearchResult.Browse>()
                ?.firstOrNull()
                ?.item
            if (hit == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.couldnt_find, name),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                viewModel.openDetail(
                    hit.browseId,
                    hit.title,
                    hit.subtitle,
                    hit.thumbnailUrl ?: artwork,
                    hit.type,
                )
            }
        }
    }

    /**
     * A whole album, playlist or library list onto the queue in one go.
     *
     * [next] picks which of the two positions the single-track menu already
     * offers it lands in — straight after the current track, or behind
     * everything else the user queued but still ahead of AutoPlay (see
     * [addToQueue]). The list keeps its own running order either way: this
     * *adds* a release, it doesn't start one, so [QueueShuffle] has no say here.
     *
     * There is usually nothing on screen to show for it — the queue panel is
     * shut, the current track carries on — so the count is said out loud, the
     * same way a batch download is.
     */
    val queueSongs: (List<Song>, Boolean) -> Unit = { songs, next ->
        if (songs.isNotEmpty()) {
            scope.launch {
                val c = controller
                if (c == null || c.mediaItemCount == 0) {
                    // Nothing to queue behind. "Add to queue" on a silent
                    // player can only mean start here — and adding without
                    // preparing would leave the list sitting in a player that
                    // never gets round to it.
                    play(songs, 0)
                } else {
                    val at = if (next) {
                        (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
                    } else {
                        c.autoplaySectionStart()
                    }
                    val radioName = c.currentMediaItem?.toSong()?.radioName
                    c.addMediaItems(at, songs.map { it.copy(radioName = radioName).toMediaItem() })
                    val message = context.resources.getQuantityString(
                        if (next) R.plurals.songs_will_play_next else R.plurals.songs_added_to_queue,
                        songs.size,
                        songs.size,
                    )
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val addSongsToQueue: (List<Song>) -> Unit = { songs -> queueSongs(songs, false) }
    val playSongsNext: (List<Song>) -> Unit = { songs -> queueSongs(songs, true) }

    // ---- Links from outside the app ----

    /**
     * A YouTube Music link tapped elsewhere on the device, a link shared into
     * BitChord, or "play something" said to the assistant — see [MusicLink].
     *
     * Keyed on the controller as well as the request, because a link is as
     * often as not what cold-starts the app: the session it has to play into is
     * still connecting the first time this runs, and returning empty-handed
     * without spending the request is what lets the second run serve it.
     */
    val linkRequest by MusicLink.pending.collectAsStateWithLifecycle()
    LaunchedEffect(linkRequest, controller) {
        val request = linkRequest ?: return@LaunchedEffect
        // Nothing here can be served without somewhere to play it — even the
        // branches that only push a page are a beat away from a tap on one of
        // its rows, and half-serving a request would spend it.
        val session = controller ?: return@LaunchedEffect
        when (request) {
            is LinkRequest.Track -> {
                val song = YtMusicRepository.trackLinks(request.videoId).getOrNull()
                if (song == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.couldnt_open_link),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    // A link is one song named on purpose, which is exactly the
                    // case [playRadio] exists for: play it and let AutoPlay
                    // carry on, rather than queueing something around it.
                    playRadio(song)
                }
            }
            is LinkRequest.Page -> {
                showNowPlaying = false
                // Titled by the page itself once it lands — a link carries a
                // browse id and nothing else. See MainViewModel.openDetail.
                viewModel.openDetail(request.browseId, title = "")
            }
            is LinkRequest.Search -> {
                val songs = if (!request.play) null else {
                    YtMusicRepository.search(request.query, SearchFilter.SONGS).getOrNull()
                        ?.filterIsInstance<SearchResult.Track>()
                }
                val top = songs?.firstOrNull()?.song
                if (top != null) {
                    playRadio(top)
                } else {
                    // Either the link was a search to look at, or "play X"
                    // found nothing to start — and the results are a better
                    // answer to a spoken request than silence is.
                    showNowPlaying = false
                    selectedTab = TAB_SEARCH
                    viewModel.searchFor(request.query)
                }
            }
            // "Play music", nothing named. The playback service restores its
            // bounded queue before the controller connects, so this resumes
            // both a live session and one recovered after process death.
            LinkRequest.Resume -> if (session.mediaItemCount > 0) session.play()
        }
        MusicLink.handled()
    }

    // ---- Album / playlist menu ----

    /**
     * Holding an album or playlist card, wherever one is drawn.
     *
     * Artists are left out. An artist page is a selection of their work rather
     * than a running order, and "add Radiohead to the queue" has no answer that
     * isn't a guess — so holding an artist card does nothing, as it did before.
     */
    val onBrowseLongPress: (ShelfItem) -> Unit = { item ->
        val id = item.browseId
        val type = id?.let { viewModel.browseTypeOf(it) }
        if (id != null && type != BrowseType.ARTIST) {
            browseActions = BrowseTarget(
                browseId = id,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
                type = type ?: BrowseType.OTHER,
                downloadId = downloadIdFor(id),
            )
        }
    }

    /**
     * The track a song card stands for, or null if the card is a collection.
     *
     * The card's own subtitle is billed as "Song • Chelsea Wolfe"; only the
     * credit belongs in the field the player, mini player and everything
     * downstream read.
     */
    val shelfSong: (ShelfItem) -> Song? = { item ->
        item.videoId?.let { videoId ->
            Song(
                videoId = videoId,
                title = item.title,
                artist = InnertubeParser.artistFromSubtitle(item.subtitle),
                thumbnailUrl = item.thumbnailUrl,
            )
        }
    }

    /**
     * Holding a card on a feed whose shelves mix tracks with collections —
     * Quick picks and Recently played are songs, Listen again is either.
     *
     * [onBrowseLongPress] alone answered only half of them: a track card
     * carries a videoId and no browse id, so holding one fell through its
     * null check and nothing opened. Dispatched on the same test as the tap
     * below, so a card that plays a song offers the track menu and a card that
     * opens a page offers the album / playlist one.
     */
    val onShelfLongPress: (ShelfItem) -> Unit = { item ->
        val song = shelfSong(item)
        if (song != null) openSongMenu(song) else onBrowseLongPress(item)
    }

    /**
     * Hands [action] the target's whole track list.
     *
     * A card has no tracks behind it — its page was never opened — so the
     * listing is fetched first, all of it, and the menu that asked has already
     * closed by the time it lands. A release page's overflow passes the rows it
     * is already showing and this is immediate.
     *
     * Album rows arrive with no album name of their own, the same way they do on
     * the page (see `withAlbum` below), so the title is stamped on here too —
     * otherwise a track queued from an album card reaches the player and the
     * download folder with nothing to file it under.
     */
    val withBrowseSongs: (BrowseTarget, (List<Song>) -> Unit) -> Unit = { target, action ->
        val stamp: (List<Song>) -> Unit = { songs ->
            action(
                if (target.type == BrowseType.ALBUM) {
                    songs.map { it.copy(albumName = it.albumName ?: target.title) }
                } else {
                    songs
                },
            )
        }
        when {
            target.songs.isNotEmpty() -> stamp(target.songs)
            target.browseId == null ->
                Toast.makeText(context, context.getString(R.string.no_tracks_here), Toast.LENGTH_SHORT).show()
            else -> viewModel.collectSongs(target.browseId, target.thumbnailUrl) { result ->
                result.fold(
                    onSuccess = stamp,
                    onFailure = {
                        val message = it.message ?: context.getString(R.string.couldnt_load_tracks)
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    // ---- Downloads ----
    // Two permissions, and never both on one device: writing to the shared
    // Music folder needs storage access below API 29 and none at all from
    // 29 on, where MediaStore grants an app its own rows; notifications are
    // only asked for from API 33. So the branches below are mutually exclusive
    // by SDK level, and nothing here can stack two dialogs on each other.
    var downloadPending by remember { mutableStateOf<List<Song>>(emptyList()) }
    /**
     * What the pending batch was asked for as, held alongside it for the same
     * reason: the storage-permission dialog is a round trip through another
     * process, and the release has to survive it or a whole album granted
     * permission arrives as forty loose tracks.
     */
    var downloadPendingFrom by remember { mutableStateOf<DownloadTarget?>(null) }
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Refusing costs the progress notification, not the download. */ }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val songs = downloadPending
        val from = downloadPendingFrom
        downloadPending = emptyList()
        downloadPendingFrom = null
        when {
            songs.isEmpty() -> Unit
            granted -> {
                songs.forEach { Downloads.enqueue(context, it, from?.title) }
                if (from != null) Downloads.markRequested(from.id, songs.map { it.videoId })
            }
            // The one case where refusing is fatal: below API 29 there is no
            // other way to reach the Music folder.
            else -> Toast
                .makeText(context, context.getString(R.string.storage_required_save), Toast.LENGTH_SHORT)
                .show()
        }
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            viewModel.reloadLocalDetail("local:all")
            viewModel.loadLocalMusic()
        } else {
            Toast.makeText(context, context.getString(R.string.storage_required_read), Toast.LENGTH_SHORT).show()
        }
    }
    val requestAudioPermission: () -> Unit = {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        mediaPermissionLauncher.launch(perm)
    }
    // Shared by the Library tab itself and by a shelf's "Show all" page, so a
    // card opens the same way from either.
    val onLibraryItemClick: (ShelfItem) -> Unit = { item ->
        item.browseId?.let { id ->
            if (id == "local:all" && !LocalMediaRepository.hasStoragePermission(context)) {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                mediaPermissionLauncher.launch(perm)
            }
            // Left set rather than cleared: a card opened from a shelf's
            // "Show all" page stacks a detail page over it exactly as one
            // opened from the Library tab stacks over that, so back from the
            // release lands on the grid rather than skipping past it. Every
            // place that reads `libraryShowAll` alongside `detail` favours
            // `detail` while both are set — see the AnimatedContent below.
            viewModel.openDetail(
                browseId = id,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
            )
        }
    }
    // Takes a list so a single tap on an album/playlist header can queue the
    // whole thing — the permission dance only needs to happen once for the
    // batch, not once per track.
    //
    // [from] is what the list *is*, when it is a release rather than a
    // selection: an album or a playlist. It is recorded whole, so the Downloads
    // page can offer the thing that was tapped back rather than the forty rows
    // it decomposed into — see [Downloads.rememberCollection]. Null for a single
    // track, which is not a release however many of them are asked for one at a
    // time.
    val startDownload: (List<Song>, DownloadTarget?) -> Unit = { requested, from ->
        val saved = Downloads.saved.value
        // Already on disk, and already queued or running: neither needs asking
        // again. What's left is what a tap on "Download" actually means.
        //
        // The release's cover is stamped onto any row that hasn't got one, as a
        // last check before the tap becomes a file.
        //
        // An album page bills its artwork once, in the header — its track rows
        // carry no thumbnail at all, see [InnertubeParser.parseResponsiveListItem]
        // — and a row that reaches [MediaTagger.artworkFor] with a null url is a
        // track saved with no cover in the file and none in [SavedSongMetadata]
        // either, so nothing downstream can draw one afterwards.
        // `MainViewModel.withArtwork` normally fills those in as a page loads and
        // covers the usual route here; this is the backstop for a list that
        // reached this function some other way, and it is worth having precisely
        // because the failure is silent and permanent — the file is written
        // without a cover, and re-downloading adopts the untagged copy rather
        // than replacing it.
        val songs = requested
            .filter { it.videoId !in saved }
            .map { song ->
                val cover = from?.thumbnailUrl
                if (song.thumbnailUrl.isNullOrBlank() && !cover.isNullOrBlank()) {
                    song.copy(thumbnailUrl = cover)
                } else {
                    song
                }
            }
        // Asked here as well as inside [Downloads.enqueue] — not instead of it.
        // Enqueue is the invariant and has to refuse whoever calls it, including
        // the storage-permission continuation below, which resumes long enough
        // after this check for the connection to have changed under it. This is
        // the one place that knows the tap was for forty tracks and can say so
        // once, rather than leaving forty identical failed rows to be read.
        val blocked = songs.isNotEmpty() && !AppSettings.downloadsAllowedNow
        if (songs.isNotEmpty() && !blocked) {
            val needsStorage = AppSettings.exportDownloads.value && DownloadStore.needsLegacyPermission() &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED

            // Asked for here rather than at launch because here is where it means
            // something: a download is the first thing this app does that the user
            // is expected to walk away from.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (needsStorage) {
                downloadPending = songs
                downloadPendingFrom = from
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                songs.forEach { Downloads.enqueue(context, it, from?.title) }
                if (from != null) Downloads.markRequested(from.id, songs.map { it.videoId })
            }
        }
        // Recorded for everything that was asked for, not just what still has to
        // be fetched: a release whose tracks are already on the device is still
        // that release, and the point of the record is to group them. Skipped
        // only when the whole batch was refused, since then there will be
        // nothing on disk for it to group.
        if (from != null && !blocked) {
            Downloads.rememberCollection(from, requested)
        }
        when {
            // The row's own icon reports a queued download, so a single tap
            // normally needs no toast — but a refused one leaves the row exactly
            // as it was, and a button that visibly does nothing is worse than a
            // long message. So this one is said whatever the count.
            blocked -> Toast.makeText(
                context,
                context.getString(R.string.wifi_only_download_refusal),
                Toast.LENGTH_LONG,
            ).show()
            requested.size > 1 -> {
                val message = if (songs.isEmpty()) {
                    context.getString(R.string.already_downloaded)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.downloading_song_count,
                        songs.size,
                        songs.size,
                    )
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    /**
     * One track on its own, which is never a release.
     *
     * A track reached through AutoPlay or a radio queue often has no duration
     * string at all — YouTube's watch-queue rows don't always send
     * `lengthText` — while the player itself knows exactly how long the same
     * track runs once it has loaded. Backfilled from there when it's the song
     * on screen, so downloading it isn't handed a duration of zero and
     * silently skipped for lyrics — see [LyricsTag.forTrack].
     */
    val downloadSong: (Song) -> Unit = { song ->
        val withDuration = if (song.durationMillis() <= 0L &&
            player.song?.videoId == song.videoId && player.durationMs > 0L
        ) {
            song.copy(durationText = formatDurationText(player.durationMs))
        } else {
            song
        }
        startDownload(listOf(withDuration), null)
    }

    // Content padding leaves room for the frosted bar above and the tab bar
    // (plus mini player) below, so nothing is ever trapped under the glass.
    //
    // The top is measured off the bar rather than guessed at: the bar is pinned
    // under the status bar inset, and that inset varies by device and by window,
    // so a fixed number only ever lines up on the one device it was picked on.
    // See [topBarContentPadding].
    val listPadding = PaddingValues(
        top = topBarContentPadding(),
        bottom = if (player.song != null && !playerDocked) 210.dp else 140.dp,
    )

    // What colour the page currently under the bars is. The fades either end
    // of the screen are flat colour wherever their blur has least to say, so
    // handing them the theme's background puts a black band on a page that is
    // washed in an artwork's colour instead. Off a detail page this resolves
    // to the theme's background anyway, which is exactly right there.
    val detailPalette = rememberArtworkPalette(detail?.thumbnailUrl)

    // One set of numbers for the cards, the page, the stories and the shared
    // picture, so they cannot disagree. Read while any of them is on screen —
    // which includes the Library tab, since the cards live at the top of it.
    // See [rememberReplayState].
    val replayOpen = showReplay || replayStory != null || showReplayShare
    val (replay, setReplayPeriod) = rememberReplayState(replayOpen)
    val replayCards = remember(replay.summary) {
        replay.summary?.takeUnless { it.isEmpty }?.cards(context).orEmpty()
    }

    // ---- The track in the player ----
    // Whatever started this track knew its title and its artwork, but rarely
    // which album or artist page it belongs to. Fill that in while the player
    // is actually up: on a tablet that is from the moment the track starts,
    // since the pane never goes down; on a phone it is when the sheet is
    // raised, so playing an album from the mini player still costs nothing.
    val playerShowing = playerDocked || showNowPlaying
    var links by remember { mutableStateOf<Song?>(null) }
    LaunchedEffect(player.song?.videoId, playerShowing) {
        links = null
        linksLoading = false
        if (!playerShowing) return@LaunchedEffect
        val current = player.song ?: return@LaunchedEffect
        if (current.albumId != null && current.artistId != null) return@LaunchedEffect
        linksLoading = true
        links = YtMusicRepository.trackLinks(current.videoId).getOrNull()
        linksLoading = false
    }
    val playerSong = player.song?.let { current ->
        val extra = links?.takeIf { it.videoId == current.videoId } ?: return@let current
        current.copy(
            artistId = current.artistId ?: extra.artistId,
            albumId = current.albumId ?: extra.albumId,
            albumName = current.albumName ?: extra.albumName,
        )
    }
    // The three-dot menu snapshots the track into songActions when it's opened,
    // so a menu opened before the lookup above resolves would otherwise be
    // stuck without album/artist rows even after the ids come in. Keep it in
    // sync while it's showing this track.
    LaunchedEffect(playerSong) {
        if (playerSong != null && songActions?.videoId == playerSong.videoId) {
            songActions = playerSong
        }
    }

    // The player's whole parameter list, in one place because there are two
    // places it can be mounted: the sheet a phone raises over the page, and
    // the pane a tablet keeps beside it. [docked] is the only difference
    // between the two, and only ever one of them is in the tree.
    val nowPlaying: @Composable (Song, Boolean) -> Unit = { song, docked ->
        val displayedSong = activeRadioSeed
            ?.takeIf { (videoId, _) -> song.radioName == null && videoId == song.videoId }
            ?.let { (_, name) -> song.copy(radioName = name) }
            ?: song
        NowPlayingScreen(
            song = displayedSong,
            windowWidth = windowWidth,
            isPlaying = player.isPlaying,
            isLoading = player.isLoading,
            positionMs = player.position.positionMs,
            durationMs = player.durationMs,
            isAudioVersion = convertedAudioId == song.videoId,
            audioVersionSwitching = switchingAudioVersion,
            qualityUpgraded = player.isQualityUpgraded,
            onToggleAudioVersion = audioVersion@{
                val c = controller ?: return@audioVersion
                val index = c.currentMediaItemIndex
                if (index !in 0 until c.mediaItemCount) return@audioVersion
                val original = convertedFromVideo
                if (original != null && convertedAudioId == song.videoId) {
                    val position = c.currentPosition
                    val wasPlaying = c.isPlaying
                    c.replaceMediaItem(index, original.toMediaItem())
                    c.seekTo(index, position)
                    if (wasPlaying) c.play()
                    convertedFromVideo = null
                    convertedAudioId = null
                    return@audioVersion
                }
                if (!song.isVideo || switchingAudioVersion) return@audioVersion
                scope.launch {
                    switchingAudioVersion = true
                    TrackLog.d("Player", "audio switch requested for '${song.title}'", song.videoId)
                    val audio = runCatching { YtMusicRepository.resolveAudio(song) }.getOrNull()
                    switchingAudioVersion = false
                    // A match can be absent or the listener may have skipped
                    // while it was being found. Neither should change a queue.
                    if (audio == null || audio.videoId == song.videoId) {
                        TrackLog.w("Player", "audio switch found no distinct official song", song.videoId)
                        return@launch
                    }
                    if (c.currentMediaItemIndex != index || c.currentMediaItem?.mediaId != song.videoId) {
                        TrackLog.d("Player", "audio switch discarded; listener changed track", song.videoId)
                        return@launch
                    }
                    val position = c.currentPosition
                    val wasPlaying = c.isPlaying
                    convertedFromVideo = song
                    convertedAudioId = audio.videoId
                    TrackLog.d("Player", "audio switch applying '${audio.title}' (${audio.videoId})", song.videoId)
                    c.replaceMediaItem(index, audio.copy(isVideoOrigin = true).toMediaItem())
                    c.seekTo(index, position)
                    if (wasPlaying) c.play()
                }
            },
            onPlayPause = {
                controller?.let { if (it.isPlaying) it.pause() else it.play() }
            },
            onNext = { controller?.seekToNextMediaItem() },
            onPrevious = { controller?.seekToPrevious() },
            onSeekFraction = { fraction ->
                controller?.let { player ->
                    // Read at the moment of the seek, not from the
                    // polled snapshot the screen draws with: a track
                    // change updates the current item before it updates
                    // the duration, so a fraction dropped seconds after
                    // a transition would otherwise be scaled by the
                    // previous song's length.
                    val duration = player.duration
                    if (duration > 0) {
                        player.seekTo(
                            (fraction * duration).toLong()
                                .coerceIn(0L, (duration - SEEK_END_GUARD_MS).coerceAtLeast(0L)),
                        )
                    }
                }
            },
            onSeek = { target ->
                controller?.let { player ->
                    // Clamped here rather than at each caller because
                    // not every caller can clamp. The scrubber's target
                    // is a fraction of the duration and cannot overrun,
                    // but a tapped lyric line seeks to a timestamp from
                    // whichever transcription matched on title, artist
                    // and duration — and a match against a slightly
                    // longer master puts every line late, so a tap near
                    // the end asks for a position past the end of this
                    // stream. Media3 answers that by clamping to the
                    // final millisecond, which ends the track and starts
                    // the next one: tapping the last line of a song
                    // skipped it.
                    val duration = player.duration
                    player.seekTo(
                        if (duration > 0) {
                            target.coerceIn(0L, (duration - SEEK_END_GUARD_MS).coerceAtLeast(0L))
                        } else {
                            target.coerceAtLeast(0L)
                        },
                    )
                }
            },
            queue = player.queue,
            queueIndex = player.queueIndex,
            hasPrevious = player.hasPrevious,
            hasNext = player.hasNext,
            repeatMode = player.repeatMode,
            shuffleEnabled = shuffleEnabled,
            autoplayEnabled = autoplay,
            signedIn = signedIn,
            likeStatus = likeStatuses[song.videoId] ?: LikeStatus.INDIFFERENT,
            onToggleLike = { viewModel.toggleLike(song.videoId) },
            onToggleShuffle = { controller?.let { it2 -> QueueShuffle.toggle(it2); AppSettings.setShuffleEnabled(QueueShuffle.enabled.value) } },
            onCycleRepeat = {
                controller?.let {
                    val next = when (it.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    // Persist the new repeat mode so it survives app restarts.
                    AppSettings.setRepeatMode(next)
                    // Nothing else to do here: PlaybackService watches the
                    // repeat mode itself and takes AutoPlay's tracks out of
                    // the queue for the duration of repeat-all — and, unlike
                    // this screen, is still around to put them back when the
                    // loop ends.
                    it.repeatMode = next
                }
            },
            onToggleAutoplay = {
                // PlaybackService owns the setting and queue extension so
                // this path and the notification use exactly one loader.
                controller?.toggleAutoplay()
            },
            onJumpTo = { controller?.seekToDefaultPosition(it) },
            onRemoveFromQueue = { controller?.removeMediaItem(it) },
            onMoveInQueue = { from, to -> controller?.moveMediaItem(from, to) },
            // The enriched copy, not player.song — otherwise the menu
            // hides the album and artist rows even once their browse
            // ids have been resolved.
            onOpenMenu = {
                menuFromPlayer = true
                songActions = song
            },
            onOpenAlbum = { id ->
                showNowPlaying = false
                viewModel.openDetail(
                    id,
                    song.albumName ?: song.title,
                    song.artist,
                    song.thumbnailUrl,
                    BrowseType.ALBUM,
                )
            },
            onOpenArtist = { id ->
                showNowPlaying = false
                // No artwork: this track's cover isn't the artist's
                // picture, and the page fills its own in once loaded.
                viewModel.openDetail(
                    id,
                    song.artist,
                    context.getString(R.string.artist),
                    null,
                    BrowseType.ARTIST,
                )
            },
            lyrics = lyrics,
            lyricsSource = lyricsSource,
            lyricsUnavailable = lyricsChecked && lyrics.isNullOrEmpty(),
            docked = docked,
            onClearQueue = {
                // Keep what's playing; drop everything queued after it.
                controller?.let { c ->
                    if (c.mediaItemCount > c.currentMediaItemIndex + 1) {
                        c.removeMediaItems(c.currentMediaItemIndex + 1, c.mediaItemCount)
                    }
                }
            },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // A pushed album/artist/playlist page replaces the tab content but
        // leaves the tab bar and mini player in place.
        // Replay's three layers unwind in the order they were opened. Ahead of
        // every other handler because they are drawn over everything else.
        BackHandler(enabled = showReplayShare) { showReplayShare = false }
        BackHandler(enabled = replayStory != null && !showReplayShare) { replayStory = null }
        BackHandler(
            enabled = showReplay && !showSettings && replayStory == null && !showReplayShare,
        ) {
            showReplay = false
        }
        BackHandler(
            enabled = detail != null && !showSettings && !showReplay,
        ) { viewModel.closeDetail() }
        // One back step out of Settings lands on Songs rather than exiting
        BackHandler(enabled = showSettings) {
            showSettings = false
            if (detail == null && !showReplay) selectedTab = TAB_SONGS
        }
        BackHandler(
            enabled = detail == null && !showSettings && !showReplay && selectedTab != TAB_SONGS,
        ) {
            selectedTab = TAB_SONGS
        }
        BackHandler(enabled = showListenBrainzLogin) { showListenBrainzLogin = false }
        BackHandler(enabled = showLastfmLogin) { showLastfmLogin = false }

        // On a tablet the page and the player stand side by side rather than
        // one over the other: everything a phone stacks in a single column —
        // the feed, the frosted bars, the tab row — becomes the left half of
        // a row, and the player is the right. Off a tablet the row has the
        // one child it always had and changes nothing.
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = when {
                        showSettings -> "settings"
                        showReplay -> "replay"
                        detail != null -> detail.browseId
                        else -> "$TAB_KEY$selectedTab"
                    },
                    transitionSpec = {
                        val tabSwap = initialState.startsWith(TAB_KEY) && targetState.startsWith(TAB_KEY)
                        if (tabSwap) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                        }
                    },
                    modifier = Modifier
                        .hazeSource(hazeState)
                        .then(
                            if (glassActive) {
                                Modifier
                                    .then(
                                        if (glassSamplesBackdrop) {
                                            Modifier.layerBackdrop(appBackdrop)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .nestedScroll(navBarScroll)
                            } else {
                                Modifier
                            },
                        ),
                    label = "content",
                ) { key ->
                    val live = detailStack.lastOrNull()?.takeIf {
                        it.browseId == key && key != "settings" && key != "replay"
                    }
                    // Held for the same reason, one step further on: a popped
                    // page is off the stack before it has finished animating
                    // out, so `live` goes null under it and it would spend its
                    // exit drawing whatever is underneath instead of itself.
                    // Per slot, since each is remembered against its own key.
                    val held = remember(key) { mutableStateOf(live) }
                    if (live != null) held.value = live
                    val page = held.value
                    if (key == "replay") {
                        ReplayScreen(
                            state = replay,
                            holder = "",
                            onPeriodChange = setReplayPeriod,
                            onOpenStory = { replayStory = it },
                            onPlaySong = playRadio,
                            onOpenArtist = { id, name ->
                                showReplay = false
                                openByName(id, name, null, BrowseType.ARTIST)
                            },
                            onOpenAlbum = { id, title, artist, art ->
                                showReplay = false
                                openByName(id, title, artist, BrowseType.ALBUM, art)
                            },
                            onShare = {
                                replaySharePage = null
                                showReplayShare = true
                            },
                            contentPadding = listPadding,
                            listState = replayListState,
                        )
                    } else if (key == "settings") {
                        SettingsScreen(
                            windowWidth = windowWidth,
                            onOpenReplay = {
                                showSettings = false
                                showReplay = true
                            },
                            onLyricsSources = { showLyricsSources = true },
                            onAppLanguage = { showAppLanguage = true },
                            contentPadding = listPadding,
                        )
                    } else if (page != null && page.browseId.isDeviceFolder()) {
                        // Local Music and Downloads — both the tabbed Songs / Artists /
                        // Albums view. Two folders of tracks already on the device, so
                        // there is nothing to tell them apart on screen beyond what is
                        // in them and what to say when that is nothing.
                        //
                        // A single downloaded playlist is not one of these: it has one
                        // running order and nothing to tab through, so it falls to the
                        // release page below.
                        val localState = page.songs
                        val localSongs = (localState as? com.music.bitchord.data.model.UiState.Success)
                            ?.data.orEmpty()
                        // Only the Downloads folder has releases behind it: Local
                        // Music is files this app never asked for, so there is
                        // nothing on record about how they were grouped. Keyed on
                        // the record as well as the list, so downloading an album
                        // while its folder is open adds the folder rather than
                        // waiting for the page to be reopened.
                        val downloadCollections = remember(localSongs, savedCollections) {
                            if (page.browseId == "local:downloads") {
                                Downloads.collectionsAmong(localSongs)
                            } else {
                                emptyList()
                            }
                        }
                        LocalMusicScreen(
                            songs = localSongs,
                            collections = downloadCollections,
                            isDownloads = page.browseId == "local:downloads",
                            currentSong = player.song,
                            isPlaying = player.isPlaying,
                            onDeleteDownloads = { selected ->
                                scope.launch {
                                    selected.forEach { song -> Downloads.delete(context, song.videoId) }
                                }
                            },
                            onSongClick = play,
                            onSongLongPress = openSongMenu,
                            onSongSwipe = onSongSwipe,
                            onShuffle = { songs ->
                                QueueShuffle.enableForNextQueue()
                                play(songs, songs.indices.random())
                            },
                            emptyMessage = (localState as? com.music.bitchord.data.model.UiState.Error)
                                ?.message,
                            // An album or artist here is a grouping of rows rather than
                            // a page, so the menu is handed the rows themselves — there
                            // is no id anything could be fetched with.
                            onCollectionLongPress = { label, grouped ->
                                // An artist grouping is never one of these — only a
                                // release downloaded whole has a record to match,
                                // which is exactly the distinction `asked` draws in
                                // `albumEntries`.
                                val downloadId = downloadCollections.firstOrNull {
                                    it.title == label && it.songs == grouped
                                }?.id
                                browseActions = BrowseTarget(
                                    browseId = null,
                                    title = label,
                                    subtitle = grouped.firstOrNull()?.artist.orEmpty()
                                        .takeUnless { it == label }
                                        .orEmpty(),
                                    thumbnailUrl = grouped.firstOrNull()?.thumbnailUrl,
                                    songs = grouped,
                                    downloadId = downloadId,
                                )
                            },
                            contentPadding = listPadding,
                        )
                    } else if (page != null) {
                        // An album page's rows carry no album name of their own — the
                        // release is billed once, in the header the rows hang under — so
                        // the page title is stamped on as they leave for the download
                        // queue or the track menu. Without it every track saved from an
                        // album arrives in the Downloads folder with nothing to group it
                        // under, and its Albums tab stays empty however much is in it.
                        val withAlbum: (Song) -> Song = { song ->
                            if (page.type == BrowseType.ALBUM) {
                                song.copy(albumName = song.albumName ?: page.title)
                            } else {
                                song
                            }
                        }
                        DetailScreen(
                            page = page,
                            currentSong = player.song,
                            isPlaying = player.isPlaying,
                            listState = detailListState,
                            onSongClick = play,
                            onSongLongPress = { openSongMenu(withAlbum(it)) },
                            onSongSwipe = onSongSwipe,
                            onShuffle = { songs ->
                                // Shuffle goes on first so the queue is built shuffled
                                // as it is set — the random pick here only decides
                                // which track leads it.
                                QueueShuffle.enableForNextQueue()
                                play(songs, songs.indices.random())
                            },
                            onSectionItemClick = { item ->
                                item.browseId?.let { id ->
                                    viewModel.openDetail(
                                        browseId = id,
                                        title = item.title,
                                        subtitle = item.subtitle,
                                        thumbnailUrl = item.thumbnailUrl,
                                        type = BrowseType.ALBUM,
                                    )
                                }
                            },
                            onSectionItemLongPress = onBrowseLongPress,
                            // The page's own tracks, so the sheet has them already and
                            // Play, Shuffle and Open are the buttons beside the one that
                            // opened it rather than rows on it. Download is the other
                            // way round: the header no longer carries it, so the sheet
                            // is where a whole release is asked for — and the tracks
                            // arrive stamped with the album they came off, which is what
                            // the download record groups them under.
                            onMore = { songs ->
                                browseActions = BrowseTarget(
                                    browseId = page.browseId,
                                    title = page.title,
                                    subtitle = page.subtitle,
                                    thumbnailUrl = page.thumbnailUrl,
                                    type = page.type,
                                    songs = songs.map(withAlbum),
                                    fromCard = false,
                                    downloadId = downloadIdFor(page.browseId),
                                )
                            },
                            onArtistClick = { id, name ->
                                viewModel.openDetail(id, name, "Artist", null, BrowseType.ARTIST)
                            },
                            onAddSuggested = { song -> viewModel.addSuggestedSong(page.browseId, song) },
                            // Saving is an account action, so it isn't offered to a
                            // guest at all — same as the like and add-to-playlist rows
                            // in the track menu.
                            onToggleLibrary = if (signedIn) {
                                { viewModel.toggleLibrary(page.browseId) }
                            } else {
                                null
                            },
                            // Same rule for the artist page's subscribe circle:
                            // a channel subscription is the account's, so a
                            // guest is never shown the button.
                            onToggleSubscription = if (signedIn) {
                                { viewModel.toggleSubscription(page.browseId) }
                            } else {
                                null
                            },
                            songSort = songSort,
                            contentPadding = listPadding,
                        )
                    } else when (key.removePrefix(TAB_KEY).toIntOrNull() ?: selectedTab) {
                        TAB_SONGS -> if (!hasStoragePermission) {
                            Box(modifier = Modifier.fillMaxSize().padding(listPadding), contentAlignment = Alignment.Center) {
                                LocalPermissionCard(onRequestPermission = requestAudioPermission)
                            }
                        } else {
                            LocalMusicScreen(
                                songs = localSongs,
                                collections = emptyList(),
                                isDownloads = false,
                                currentSong = player.song,
                                isPlaying = player.isPlaying,
                                onSongClick = play,
                                onSongLongPress = openSongMenu,
                                onSongSwipe = onSongSwipe,
                                onShuffle = { songs ->
                                    QueueShuffle.enableForNextQueue()
                                    play(songs, songs.indices.random())
                                },
                                emptyMessage = localEmptyMessage,
                                onCollectionLongPress = { label, grouped ->
                                    browseActions = BrowseTarget(
                                        browseId = null,
                                        title = label,
                                        subtitle = grouped.firstOrNull()?.artist.orEmpty()
                                            .takeUnless { it == label }
                                            .orEmpty(),
                                        thumbnailUrl = grouped.firstOrNull()?.thumbnailUrl,
                                        songs = grouped,
                                        downloadId = null,
                                    )
                                },
                                contentPadding = listPadding,
                                initialTab = LOCAL_TAB_SONGS,
                                showTabRow = false,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onDeleteSong = { viewModel.loadLocalMusic() },
                            )
                        }
                        TAB_ALBUMS -> if (!hasStoragePermission) {
                            Box(modifier = Modifier.fillMaxSize().padding(listPadding), contentAlignment = Alignment.Center) {
                                LocalPermissionCard(onRequestPermission = requestAudioPermission)
                            }
                        } else {
                            LocalMusicScreen(
                                songs = localSongs,
                                collections = emptyList(),
                                isDownloads = false,
                                currentSong = player.song,
                                isPlaying = player.isPlaying,
                                onSongClick = play,
                                onSongLongPress = openSongMenu,
                                onSongSwipe = onSongSwipe,
                                onShuffle = { songs ->
                                    QueueShuffle.enableForNextQueue()
                                    play(songs, songs.indices.random())
                                },
                                emptyMessage = localEmptyMessage,
                                onCollectionLongPress = { label, grouped ->
                                    browseActions = BrowseTarget(
                                        browseId = null,
                                        title = label,
                                        subtitle = grouped.firstOrNull()?.artist.orEmpty()
                                            .takeUnless { it == label }
                                            .orEmpty(),
                                        thumbnailUrl = grouped.firstOrNull()?.thumbnailUrl,
                                        songs = grouped,
                                        downloadId = null,
                                    )
                                },
                                contentPadding = listPadding,
                                initialTab = LOCAL_TAB_ALBUMS,
                                showTabRow = false,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onDeleteSong = { viewModel.loadLocalMusic() },
                            )
                        }
                        TAB_ARTISTS -> if (!hasStoragePermission) {
                            Box(modifier = Modifier.fillMaxSize().padding(listPadding), contentAlignment = Alignment.Center) {
                                LocalPermissionCard(onRequestPermission = requestAudioPermission)
                            }
                        } else {
                            LocalMusicScreen(
                                songs = localSongs,
                                collections = emptyList(),
                                isDownloads = false,
                                currentSong = player.song,
                                isPlaying = player.isPlaying,
                                onSongClick = play,
                                onSongLongPress = openSongMenu,
                                onSongSwipe = onSongSwipe,
                                onShuffle = { songs ->
                                    QueueShuffle.enableForNextQueue()
                                    play(songs, songs.indices.random())
                                },
                                emptyMessage = localEmptyMessage,
                                onCollectionLongPress = { label, grouped ->
                                    browseActions = BrowseTarget(
                                        browseId = null,
                                        title = label,
                                        subtitle = grouped.firstOrNull()?.artist.orEmpty()
                                            .takeUnless { it == label }
                                            .orEmpty(),
                                        thumbnailUrl = grouped.firstOrNull()?.thumbnailUrl,
                                        songs = grouped,
                                        downloadId = null,
                                    )
                                },
                                contentPadding = listPadding,
                                initialTab = LOCAL_TAB_ARTISTS,
                                showTabRow = false,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onDeleteSong = { viewModel.loadLocalMusic() },
                            )
                        }
                        TAB_FOLDERS -> if (!hasStoragePermission) {
                            Box(modifier = Modifier.fillMaxSize().padding(listPadding), contentAlignment = Alignment.Center) {
                                LocalPermissionCard(onRequestPermission = requestAudioPermission)
                            }
                        } else {
                            LocalMusicScreen(
                                songs = localSongs,
                                collections = emptyList(),
                                isDownloads = false,
                                currentSong = player.song,
                                isPlaying = player.isPlaying,
                                onSongClick = play,
                                onSongLongPress = openSongMenu,
                                onSongSwipe = onSongSwipe,
                                onShuffle = { songs ->
                                    QueueShuffle.enableForNextQueue()
                                    play(songs, songs.indices.random())
                                },
                                emptyMessage = localEmptyMessage,
                                onCollectionLongPress = { label, grouped ->
                                    browseActions = BrowseTarget(
                                        browseId = null,
                                        title = label,
                                        subtitle = grouped.firstOrNull()?.artist.orEmpty()
                                            .takeUnless { it == label }
                                            .orEmpty(),
                                        thumbnailUrl = grouped.firstOrNull()?.thumbnailUrl,
                                        songs = grouped,
                                        downloadId = null,
                                    )
                                },
                                contentPadding = listPadding,
                                initialTab = LOCAL_TAB_FOLDERS,
                                showTabRow = false,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onDeleteSong = { viewModel.loadLocalMusic() },
                            )
                        }
                        TAB_SEARCH -> if (!hasStoragePermission) {
                            Box(modifier = Modifier.fillMaxSize().padding(listPadding), contentAlignment = Alignment.Center) {
                                LocalPermissionCard(onRequestPermission = requestAudioPermission)
                            }
                        } else {
                            LocalSearchScreen(
                                songs = localSongs,
                                currentSong = player.song,
                                isPlaying = player.isPlaying,
                                onSongClick = play,
                                onSongLongPress = openSongMenu,
                                onSongSwipe = onSongSwipe,
                                onAlbumClick = { title, songs, art ->
                                    browseActions = BrowseTarget(
                                        browseId = null,
                                        title = title,
                                        subtitle = songs.firstOrNull()?.artist.orEmpty(),
                                        thumbnailUrl = art,
                                        songs = songs,
                                        downloadId = null,
                                    )
                                    viewModel.openLocalDetail(
                                        browseId = "local:album:$title",
                                        title = title,
                                        subtitle = songs.firstOrNull()?.artist.orEmpty(),
                                        thumbnailUrl = art,
                                        type = BrowseType.ALBUM,
                                        songs = songs,
                                    )
                                },
                                onArtistClick = { artist, songs ->
                                    viewModel.openLocalDetail(
                                        browseId = "local:artist:$artist",
                                        title = artist,
                                        subtitle = "${songs.size} songs",
                                        thumbnailUrl = songs.firstNotNullOfOrNull { it.thumbnailUrl },
                                        type = BrowseType.ARTIST,
                                        songs = songs,
                                    )
                                },
                                onFolderClick = { folderName, songs ->
                                    viewModel.openLocalDetail(
                                        browseId = "local:folder:$folderName",
                                        title = folderName,
                                        subtitle = "${songs.size} songs",
                                        thumbnailUrl = null,
                                        type = BrowseType.PLAYLIST,
                                        songs = songs,
                                    )
                                },
                                recentSearches = searchHistory,
                                onRecordSearch = { term -> viewModel.recordSearch() },
                                onRemoveSearch = viewModel::removeSearch,
                                onClearSearchHistory = viewModel::clearSearchHistory,
                                contentPadding = listPadding,
                            )
                        }
                        else -> Box(modifier = Modifier.fillMaxSize())
                    }
                }

                // Every top bar is a fade rather than a pane — see [TopFadeBlur].
                // Drawn before the bar so the bar's own content sits on top of it.
                val isDetailVisible = detail != null && !isLocalDetail && !showSettings && !showReplay
                TopFadeBlur(
                    hazeState = hazeState,
                    // Replay paints its own full-bleed black backdrop up under the
                    // status bar, exactly as a release page's artwork does.
                    pageColor = when {
                        showReplay -> Color.Black
                        isDetailVisible -> detailPalette.wash
                        else -> MaterialTheme.colorScheme.background
                    },
                    scrimColor = when {
                        showReplay -> Color.Black
                        isDetailVisible -> detailPalette.background
                        else -> MaterialTheme.colorScheme.background
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                FrostedTopBar(
                    title = when {
                        showSettings -> stringResource(R.string.settings)
                        showReplay -> stringResource(R.string.replay)
                        detail != null -> detail.title
                        else -> tabs[selectedTab].label
                    },
                    scrolled = when {
                        showSettings -> true
                        showReplay -> replayScrolled
                        detail != null -> detailScrolled
                        else -> scrolled || selectedTab == TAB_SEARCH
                    },
                    refreshing = false,
                    pullFraction = { 0f },
                    onBack = when {
                        showSettings -> ({ showSettings = false })
                        showReplay -> ({ showReplay = false })
                        detail != null -> ({ viewModel.closeDetail(); Unit })
                        else -> null
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                    actions = {
                        if (!showSettings) {
                            if (detail != null && !isLocalDetail && detail.type != BrowseType.ARTIST) {
                                IconButton(onClick = { songSortMenuOpen = true }) {
                                    Icon(
                                        Icons.Rounded.Sort,
                                        contentDescription = stringResource(R.string.sort_songs),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                )

                // Drawn before the bars so their own glass reads on top of it.
                BottomFadeScrim(
                    withMiniPlayer = player.song != null && !playerDocked,
                    // Not the wash: by the foot of the screen the page has finished
                    // easing out of it and into this, so this is what is actually
                    // under the tab bar.
                    pageColor = if (isDetailVisible) detailPalette.background else MaterialTheme.colorScheme.background,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                // One tab handler, whichever bar is drawing it.
                val onTabSelected: (Int) -> Unit = { index ->
                    if (index == TAB_SEARCH && selectedTab == TAB_SEARCH) {
                        searchFocusTrigger++
                    } else {
                        if (index != TAB_SEARCH) {
                            searchFocusTrigger = 0
                        }
                        viewModel.clearDetail()
                        showSettings = false
                        showReplay = false
                        selectedTab = index
                    }
                }

                if (glassActive) {
                    // Liquid glass replaces the two stacked bars with the single
                    // component they are stacked to imitate: the now playing
                    // controls dock into the tab bar rather than riding above it,
                    // and the pair folds together on scroll. See [GlassNavBar].
                    GlassNavBar(
                        tabs = tabs,
                        selectedIndex = selectedTab,
                        onTabSelected = onTabSelected,
                        scrollConnection = navBarScroll,
                        song = player.song?.takeUnless { playerDocked },
                        isPlaying = player.isPlaying,
                        isLoading = player.isLoading,
                        onPlayPause = {
                            controller?.let { if (it.isPlaying) it.pause() else it.play() }
                        },
                        onNext = { controller?.seekToNextMediaItem() },
                        onExpand = { showNowPlaying = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                            .fillMaxWidth(),
                    )
                } else Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Capped and centred rather than run to the page's edges
                        // — see [FLOATING_BAR_MAX_WIDTH]. It sits on the Column
                        // rather than on each bar so the two are held to the same
                        // width and keep the shared left and right edge they have
                        // on a phone. Before fillMaxWidth, so the fill has
                        // already been bounded by the time it is applied.
                        .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Only where the player isn't already open beside the page:
                    // a bar whose whole job is to stand in for the player, next
                    // to the player, is a second copy of what is already there.
                    player.song?.takeUnless { playerDocked }?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = player.isPlaying,
                            isLoading = player.isLoading,
                            hazeState = hazeState,
                            onPlayPause = {
                                controller?.let { if (it.isPlaying) it.pause() else it.play() }
                            },
                            onNext = { controller?.seekToNextMediaItem() },
                            onExpand = { showNowPlaying = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    FloatingBottomBar(
                        tabs = tabs,
                        selectedIndex = selectedTab,
                        hazeState = hazeState,
                        onTabSelected = onTabSelected,
                    )
                }
            }

            // The player, open for as long as the app is. There is no way to
            // put it away and nothing to put it away for — the pane is its
            // own space rather than something borrowed from the page.
            if (playerDocked) {
                DockedPlayer(
                    song = playerSong,
                    width = dockedPlayerWidth(windowWidth),
                    content = { current -> nowPlaying(current, true) },
                )
            }
        }

        // ---- Now Playing ----
        // Only raised where it isn't already open beside the page.
        if (!playerDocked && showNowPlaying && playerSong != null) {
            ModalBottomSheet(
                onDismissRequest = { showNowPlaying = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                // The player fills the screen and paints its own background to
                // the very top, so the sheet's default 28.dp top corners would
                // only cut two notches out of the artwork behind the status bar.
                shape = RectangleShape,
                containerColor = Color.Transparent,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            ) {
                nowPlaying(playerSong, false)
            }
        }

        // ---- Replay stories ----
        // Mounted out here rather than inside the pane above, because a story
        // covers the window: on a tablet the page is only the left half of the
        // row, and a story laid out inside it would run alongside the player
        // instead of over it.
        replayStory?.let { start ->
            replay.summary?.takeUnless { it.isEmpty }?.let { summary ->
                ReplayStories(
                    summary = summary,
                    start = start,
                    onClose = { replayStory = null },
                    onShare = { card ->
                        replaySharePage = card
                        showReplayShare = true
                    },
                    paused = showReplayShare,
                )
            }
        }

        // ---- Share the Replay ----
        if (showReplayShare) {
            replay.summary?.takeUnless { it.isEmpty }?.let { summary ->
                ModalBottomSheet(
                    onDismissRequest = { showReplayShare = false },
                    // Straight to full height. The sheet is a picture and two
                    // buttons, and half-open it showed the picture with both
                    // buttons below the fold — a sheet whose only two controls
                    // need a drag to reach.
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    ReplayShareSheet(
                        summary = summary,
                        holder = "",
                        memberSince = replay.memberSince,
                        page = replaySharePage,
                        onDismiss = { showReplayShare = false },
                    )
                }
            }
        }

        // ---- Album / playlist detail ----
        // ---- Long-press track actions ----
        songActions?.let { song ->
            val isLocal = song.localUri != null || song.localPath != null ||
                song.videoId.startsWith("content://") || song.videoId.startsWith("file://")
            if (isLocal) {
                var showTagEditor by remember { mutableStateOf(false) }
                var showLyricsEditor by remember { mutableStateOf(false) }

                if (showTagEditor) {
                    LocalTagEditorSheet(
                        song = song,
                        onDismissRequest = {
                            showTagEditor = false
                            songActions = null
                        },
                        onTagsSaved = {
                            showTagEditor = false
                            songActions = null
                            viewModel.loadLocalMusic()
                        },
                    )
                } else if (showLyricsEditor) {
                    LocalLyricsEditorSheet(
                        song = song,
                        onDismissRequest = {
                            showLyricsEditor = false
                            songActions = null
                        },
                    )
                } else {
                    LocalSongDetailsSheet(
                        song = song,
                        onDismissRequest = { songActions = null },
                        onLyricsEditorClick = { showLyricsEditor = true },
                        onTagEditorClick = { showTagEditor = true },
                    )
                }
            } else {
                // Set by whoever opened it — see [menuFromPlayer]. It cannot be
                // read off the player's own visibility any more, because on a
                // tablet the player is visible whatever the menu was opened from.
                val fromPlayer = menuFromPlayer
            val share: () -> Unit = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.videoId}")
                }
                context.startActivity(Intent.createChooser(sendIntent, song.title))
                songActions = null
            }
            // Navigating has to take the player down with the sheet, or the
            // page it opens lands behind a still-covering player.
            // The track's cover stands in for an album's, but never for an
            // artist's picture — that page loads its own.
            val openPage: (String, String, String, BrowseType) -> Unit = { id, title, sub, type ->
                songActions = null
                showNowPlaying = false
                val art = song.thumbnailUrl.takeUnless { type == BrowseType.ARTIST }
                viewModel.openDetail(id, title, sub, art, type)
            }
            // The library toggle needs tokens only YouTube can mint, and the
            // rating it comes back with is more authoritative than anything
            // the library feed knew — so the menu asks as it opens.
            LaunchedEffect(song.videoId) { viewModel.loadSongMenu(song.videoId) }
            // "Remove from this playlist" is only a sentence on a playlist
            // page the account can actually edit, and only for a row that
            // carries the per-entry id a removal is expressed in.
            val editable = viewModel.editablePlaylist(detail?.browseId)
                ?.takeIf { !fromPlayer && song.setVideoId != null }
            ModalBottomSheet(
                onDismissRequest = { songActions = null },
                // The sheet paints itself in the track's own colours, corners
                // and drag handle included — see SongActionsSheet.
                containerColor = Color.Transparent,
                dragHandle = null,
            ) {
                SongActionsSheet(
                    song = song,
                    signedIn = signedIn,
                    likeStatus = likeStatuses[song.videoId] ?: LikeStatus.INDIFFERENT,
                    onPlayNext = { playNext(song); songActions = null },
                    onAddToQueue = { addToQueue(song); songActions = null },
                    onStartRadio = { startRadio(song); songActions = null },
                    // Stays open: the row it replaces itself with is the
                    // progress, and closing the sheet would hide the only
                    // answer to "did that work?".
                    onDownload = { downloadSong(song) },
                    // The sheet stays up for a rating: it shows the new state
                    // in place, and people often thumb a song and then queue it.
                    onToggleLike = { viewModel.toggleLike(song.videoId) },
                    onToggleDislike = { viewModel.toggleDislike(song.videoId) },
                    onAddToPlaylist = {
                        songActions = null
                        viewModel.loadPlaylists()
                        playlistTarget = song
                    },
                    onRemoveFromPlaylist = editable?.let {
                        {
                            songActions = null
                            viewModel.removeFromPlaylist(it.browseId, song)
                        }
                    },
                    onOpenAlbum = { id ->
                        openPage(
                            id,
                            song.albumName ?: song.title,
                            song.artist,
                            BrowseType.ALBUM,
                        )
                    },
                    onOpenArtist = { id ->
                        openPage(id, song.artist, context.getString(R.string.artist), BrowseType.ARTIST)
                    },
                    // Only the player's copy of a track is ever missing these
                    // and backfilling — a row opened from a list already has
                    // whatever ids it's ever going to have.
                    resolvingLinks = fromPlayer && linksLoading,
                    showSleepTimer = fromPlayer,
                    // Offered for every playing track with a YouTube upload
                    // behind it, not only for one an upgrade visibly swapped:
                    // a source ranked above YouTube can be playing its own
                    // idea of the song from the first second, and a wrong
                    // match sounds like a wrong match whether or not anything
                    // announced itself. See [Song.hasYouTubeOriginal].
                    onRollbackToOriginal = if (fromPlayer &&
                        song.hasYouTubeOriginal() &&
                        // Nothing to revert *from*: the listener is hearing a
                        // file they saved, not a stream anything chose.
                        song.localUri == null &&
                        // Already there, and the menu says so with the row
                        // below instead.
                        song.videoId !in pinnedToOriginal &&
                        controller?.currentMediaItem?.mediaId == song.videoId
                    ) {
                        rollback@{
                            val c = controller ?: return@rollback
                            val index = c.currentMediaItemIndex
                            if (index !in 0 until c.mediaItemCount ||
                                c.currentMediaItem?.mediaId != song.videoId
                            ) return@rollback
                            // Written down before the item is replaced, so
                            // every entry built for this song from here on is
                            // built as this one — see [OriginalVersion]. Without
                            // it the revert lasted exactly as long as this queue
                            // entry did, and the next play put the listener back
                            // on the copy they had just rejected.
                            OriginalVersion.pin(song.videoId)
                            val position = c.currentPosition
                            val wasPlaying = c.isPlaying
                            c.replaceMediaItem(index, song.toDirectYouTubeMediaItem())
                            c.seekTo(index, position)
                            if (wasPlaying) c.play()
                            songActions = null
                        }
                    } else {
                        null
                    },
                    // The way back, and the only one: a pinned track is held
                    // off the automatic search on purpose, so nothing but this
                    // will ever offer it a better copy again.
                    onUpgradeQuality = if (fromPlayer && song.videoId in pinnedToOriginal &&
                        // A track playing off a file the listener saved is not
                        // playing a stream anything could upgrade — the pin on
                        // it is only waiting for the day it is streamed again.
                        song.localUri == null &&
                        controller?.currentMediaItem?.mediaId == song.videoId
                    ) {
                        {
                            controller.upgradeQuality()
                            songActions = null
                        }
                    } else {
                        null
                    },
                    // Hidden outright when there's no real YouTube id behind
                    // this row to build a link from — SongActionsSheet already
                    // drops it for a local file via `isOffline`, this catches
                    // the rest.
                    onShare = share.takeIf { song.videoId.isNotBlank() },
                    onCopyLog = if (fromPlayer) {
                        {
                            songActions = null
                            scope.launch {
                                val text = TrackLog.forTrack(song, NerdStats.current.value)
                                clipboard.setText(AnnotatedString(text))
                                // The line count, not just "copied": it is the
                                // one thing the system's own paste confirmation
                                // doesn't say, and an empty log is a real
                                // outcome worth seeing rather than a silent one.
                                Toast.makeText(
                                    context,
                                    context.resources.getQuantityString(
                                        R.plurals.log_copied_line_count,
                                        text.lineSequence().count(),
                                        text.lineSequence().count(),
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

        // ---- Download manager ----
        // The batch view of what the top-bar indicator is counting. Dismissing
        // it is what marks the batch seen, and marking it on the way *out*
        // rather than on the way in is deliberate: it is the outcome the user is
        // signing off on, and while the sheet is up there may not be one yet.
        if (showDownloadManager) {
            val closeDownloadManager = {
                showDownloadManager = false
                DownloadSession.markSeen()
            }
            BackHandler(onBack = closeDownloadManager)
            ModalBottomSheet(
                onDismissRequest = closeDownloadManager,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                DownloadManagerSheet(onDismiss = closeDownloadManager)
            }
        }

        // ---- Add to playlist / new playlist ----
        // One sheet for both, because they are one decision: the list of
        // playlists with a way to make another. `creatingPlaylist` opens it
        // straight onto the form, which is what the Library tile means.
        if (playlistTarget != null || creatingPlaylist) {
            val target = playlistTarget
            val dismiss = {
                playlistTarget = null
                creatingPlaylist = false
            }
            ModalBottomSheet(
                onDismissRequest = dismiss,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PlaylistPickerSheet(
                    playlists = playlists,
                    loading = playlistsLoading,
                    song = target,
                    startCreating = target == null,
                    onPick = { playlist ->
                        target?.let { viewModel.addToPlaylist(playlist, it) }
                        dismiss()
                    },
                    onCreate = { title, privacy ->
                        viewModel.createPlaylist(title, privacy, target)
                        dismiss()
                    },
                )
            }
        }

        // ---- Album / playlist actions ----
        // Opened by holding a card on any tab, or from the release page's own
        // overflow. What a track's long-press menu is to one song, this is to
        // the whole release — the queue rows above all.
        browseActions?.let { target ->
            // Every row here closes the menu first: the tracks may still have to
            // be fetched, and leaving the sheet up over a request nothing on it
            // reports on reads as a tap that didn't land.
            val act: ((List<Song>) -> Unit) -> () -> Unit = { action ->
                {
                    browseActions = null
                    withBrowseSongs(target, action)
                }
            }
            // Whose playlist this is, asked here rather than carried in by
            // whatever opened the sheet.
            //
            // Only the playlist's own page states it (see
            // InnertubeParser.parsePlaylistOwned), so a card has to send for the
            // answer and the sheet is already up by the time it lands — hence
            // read as state rather than settled once when the target was built.
            // Rename and Delete are absent until the answer says they apply, so
            // the sheet's worst moment is a beat without them on the user's own
            // playlist, rather than offering to delete a stranger's.
            LaunchedEffect(target.browseId) {
                viewModel.resolvePlaylistOwnership(target.browseId)
            }
            // Spelt out here rather than left to MainViewModel.editablePlaylist,
            // which is the same rule over the same two lists: that reads them as
            // plain values, which is right for a click handler and invisible to
            // Compose. Both are read from collected state so this sheet actually
            // recomposes when the answer arrives.
            val ownedPlaylists by viewModel.playlistOwned.collectAsStateWithLifecycle()
            val playlist = target.browseId
                ?.takeIf { signedIn && ownedPlaylists[it] == true }
                ?.let { id -> playlists.firstOrNull { it.browseId == id } }
            val remote = target.browseId?.startsWith("local:") == false
            val pinnedPlaylists by AppSettings.pinnedPlaylists.collectAsStateWithLifecycle()
            val pinnableId = target.browseId?.takeIf { target.type == BrowseType.PLAYLIST }
            ModalBottomSheet(
                onDismissRequest = { browseActions = null },
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                BrowseActionsSheet(
                    // The live answer, not the one the target was built with.
                    target = target.copy(playlist = playlist),
                    onPlayNext = act(playSongsNext),
                    onAddToQueue = act(addSongsToQueue),
                    onPlay = act { songs -> play(songs, 0) }.takeIf { target.fromCard },
                    onShuffle = act { songs ->
                        // As on a release page: shuffle goes on before the queue
                        // is built, so it is built shuffled rather than played
                        // out of order.
                        QueueShuffle.enableForNextQueue()
                        play(songs, songs.indices.random())
                    }.takeIf { target.fromCard },
                    onOpen = target.browseId
                        ?.takeIf { target.fromCard }
                        ?.let { id ->
                            {
                                browseActions = null
                                viewModel.openDetail(
                                    browseId = id,
                                    title = target.title,
                                    subtitle = target.subtitle,
                                    thumbnailUrl = target.thumbnailUrl,
                                    type = target.type,
                                )
                            }
                        },
                    // The one place a whole release is asked for, from a card and
                    // from the release's own page alike — its header spends that
                    // spot on the search now. Nothing on this device needs
                    // fetching to be on it, so a local page is the exception.
                    // What the target carries that the tracks don't is the
                    // release's own name and cover, which is exactly what the
                    // record wants.
                    onDownloadAll = act { songs ->
                        startDownload(
                            songs,
                            target.browseId
                                ?.takeIf { target.type != BrowseType.ARTIST }
                                ?.let { id ->
                                    DownloadTarget(
                                        id = id,
                                        title = target.title,
                                        subtitle = target.subtitle,
                                        thumbnailUrl = target.thumbnailUrl,
                                        playlist = target.type == BrowseType.PLAYLIST,
                                    )
                                },
                        )
                    }.takeIf { remote },
                    isPinned = pinnableId != null && pinnableId in pinnedPlaylists,
                    onTogglePin = pinnableId?.let { id ->
                        {
                            val nowPinned = AppSettings.togglePinnedPlaylist(id)
                            if (!nowPinned && id !in pinnedPlaylists) {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.pinned_playlist_limit,
                                        AppSettings.MAX_PINNED_PLAYLISTS,
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            browseActions = null
                        }
                    },
                    onRename = playlist?.let { p ->
                        { name: String ->
                            browseActions = null
                            viewModel.renamePlaylist(p, name)
                        }
                    },
                    onDelete = playlist?.let { p ->
                        {
                            browseActions = null
                            viewModel.deletePlaylist(p)
                        }
                    },
                    onDeleteDownload = target.downloadId?.let { id ->
                        {
                            browseActions = null
                            scope.launch { Downloads.deleteCollection(context, id) }
                        }
                    },
                )
            }
        }

        if (songSortMenuOpen) {
            BackHandler { songSortMenuOpen = false }
            FrostedSortMenu(
                hazeState = hazeState,
                selected = songSort,
                onSelect = { option ->
                    detail?.browseId?.let { AppSettings.setDetailSongSort(it, option) }
                    songSortMenuOpen = false
                },
                onFlipDateDirection = {
                    detail?.browseId?.let { browseId ->
                        val next = when (songSort) {
                            SongSort.DATE_ADDED_DESC -> SongSort.DATE_ADDED_ASC
                            SongSort.DATE_ADDED_ASC -> SongSort.DATE_ADDED_DESC
                            else -> SongSort.DATE_ADDED_DESC
                        }
                        AppSettings.setDetailSongSort(browseId, next)
                    }
                },
                onDismiss = { songSortMenuOpen = false },
            )
        }

        if (showAppLanguage) {
            BackHandler { showAppLanguage = false }
            AppLanguageDialog(
                hazeState = hazeState,
                onDismiss = { showAppLanguage = false },
            )
        }

        if (showListenBrainzLogin) {
            var tokenInput by remember { mutableStateOf(listenBrainzToken) }
            ListenBrainzTokenAlert(
                hazeState = hazeState,
                tokenInput = tokenInput,
                onTokenInputChange = { tokenInput = it },
                onSave = {
                    AppSettings.setListenBrainzToken(tokenInput.trim())
                    showListenBrainzLogin = false
                },
                onDismiss = { showListenBrainzLogin = false },
            )
        }

        if (showLastfmLogin) {
            var usernameInput by remember { mutableStateOf("") }
            var passwordInput by remember { mutableStateOf("") }
            var lastfmError by remember { mutableStateOf<String?>(null) }
            var lastfmLoading by remember { mutableStateOf(false) }
            LastfmLoginAlert(
                hazeState = hazeState,
                usernameInput = usernameInput,
                onUsernameInputChange = { usernameInput = it },
                passwordInput = passwordInput,
                onPasswordInputChange = { passwordInput = it },
                error = lastfmError,
                loading = lastfmLoading,
                onSignIn = {
                    lastfmLoading = true
                    lastfmError = null
                    scope.launch {
                        try {
                            LastFM.initialize(
                                apiKey = AppSettings.lastfmApiKey.value,
                                secret = AppSettings.lastfmSecret.value,
                            )
                            LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                                .onSuccess { auth ->
                                    AppSettings.setLastfmSessionKey(auth.session.key)
                                    AppSettings.setLastfmUsername(auth.session.name)
                                    AppSettings.setLastfmEnabled(true)
                                    showLastfmLogin = false
                                }
                                .onFailure { e ->
                                    lastfmError = e.message ?: context.getString(R.string.login_failed)
                                }
                        } catch (e: Exception) {
                            lastfmError = e.message ?: context.getString(R.string.login_failed)
                        } finally {
                            lastfmLoading = false
                        }
                    }
                },
                onDismiss = { if (!lastfmLoading) showLastfmLogin = false },
            )
        }

    }
}

private fun tween(durationMillis: Int) =
    androidx.compose.animation.core.tween<Float>(durationMillis)

/**
 * The track-list sort menu, styled after the account switcher: a full-screen
 * scrim to catch the dismissal tap, and the options on a frosted panel that
 * blurs the page behind it. Composed here in the main hierarchy rather than
 * as a popup window — which is exactly what lets the haze see the content it
 * is blurring.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun FrostedSortMenu(
    hazeState: HazeState,
    selected: SongSort,
    onSelect: (SongSort) -> Unit,
    onFlipDateDirection: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = .48f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(top = 56.dp, end = 20.dp)
                .width(IntrinsicSize.Max)
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(onClick = {}),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                // Date added is one row, Spotify-style: the arrow on it shows
                // the direction — up for newest first, down for oldest — and
                // tapping flips it, the rotation animating the flip. Up is
                // also where a fresh activation lands, newest first being the
                // point of the feature.
                val dateActive = selected == SongSort.DATE_ADDED_ASC ||
                    selected == SongSort.DATE_ADDED_DESC
                val arrowRotation by animateFloatAsState(
                    targetValue = if (selected == SongSort.DATE_ADDED_ASC) 180f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "dateAddedArrow",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { onFlipDateDirection() }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sort_date_added_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (dateActive) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(arrowRotation),
                        )
                    }
                }
                SongSort.entries
                    .filter { it != SongSort.DATE_ADDED_ASC && it != SongSort.DATE_ADDED_DESC }
                    .forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable(role = Role.Button) { onSelect(option) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option.localizedLabel(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (option == selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun SongSort.localizedLabel(): String = when (this) {
    SongSort.DEFAULT -> stringResource(R.string.sort_default)
    SongSort.TITLE_ASC -> stringResource(R.string.sort_title_ascending)
    SongSort.TITLE_DESC -> stringResource(R.string.sort_title_descending)
    SongSort.DATE_ADDED_ASC -> stringResource(R.string.sort_date_added_oldest)
    SongSort.DATE_ADDED_DESC -> stringResource(R.string.sort_date_added)
}

/**
 * Whether this page id is one of the two device folders — `local:downloads` and
 * `local:all`, the tabbed Songs / Artists / Albums view.
 *
 * Asked rather than `startsWith("local:")` because that prefix now covers two
 * unlike pages: a folder, and one downloaded playlist, which is a plain track
 * listing under its own cover and wants the same chrome every other release page
 * gets. See [Downloads.PLAYLIST_PREFIX] for why they share a namespace at all.
 */
private fun String?.isDeviceFolder(): Boolean =
    this == "local:all" || this == "local:downloads"

/** `M:SS`/`H:MM:SS`, the same shape [String?.durationMillis] parses back. */
private fun formatDurationText(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

/**
 * The pane a wide window keeps the player in, down the right-hand edge.
 *
 * It is a fixed [width] rather than a share of the row because the player has a
 * width it wants and a page does not: past a point the sleeve and the transport
 * stop being improved by more room and the feed beside them still is, so the
 * pane takes what it needs and the page has the rest — see [dockedPlayerWidth].
 *
 * The pane is there whether or not anything is playing. A player that appears
 * and disappears would take a third of the page's width with it every time
 * something started or stopped, which is the layout jumping under the finger
 * rather than the app reacting to it; so with nothing to show it says so.
 */
@Composable
private fun DockedPlayer(
    song: Song?,
    width: Dp,
    content: @Composable (Song) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (song != null) {
            content(song)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = BitChordIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.nothing_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.pick_something),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        // The status bar runs across both panes and its glyphs can only be one
        // colour, and that colour follows the page: in a light theme they are
        // dark ink, which over a plain surface is a clock nobody can read. Only
        // painted for the empty state, where the pane really is flat
        // [colorScheme.surface] behind the placeholder copy.
        //
        // A song mounts [NowPlayingScreen] instead, and that already runs its
        // own backdrop — the mesh gradient, and the hero banner's artwork —
        // up behind the inset, with its own scrim once the banner settles (see
        // its [heroT] scrim). Painting flat over that here was covering the
        // player's own backdrop with a solid rectangle every frame, which is
        // the black bar across the top of a playing dock: the artwork stopped
        // at this box instead of running to the edge like it does on a phone.
        if (song == null) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}

/**
 * How far short of the end a seek is allowed to land.
 *
 * Seeking to the final millisecond is indistinguishable from the track running
 * out, so it starts the next song — which is not what anyone dragging to the end
 * of the bar, or tapping the last line of a lyric, is asking for. A second back
 * from the end plays the outro instead.
 */
private const val SEEK_END_GUARD_MS = 1_000L

/**
 * How far a detail page scrolls before its title moves up into the bar.
 *
 * Roughly the height of the sleeve and the credit stacked above the Play pair,
 * so the two titles hand over as the header one leaves rather than sitting on
 * screen together. The bar cross-fades over 220ms, which absorbs the difference
 * between that estimate and a particular page's real header.
 */
private val DETAIL_TITLE_DROP = 320.dp

private const val TAB_SONGS = 0
private const val TAB_ALBUMS = 1
private const val TAB_ARTISTS = 2
private const val TAB_FOLDERS = 3
private const val TAB_SEARCH = 4



/**
 * What a tab's key is prefixed with in the content switcher above.
 *
 * The index is read back off it there rather than off `selectedTab`, so the
 * prefix has to be the one thing both the writing and the reading agree on.
 */
private const val TAB_KEY = "tab:"
