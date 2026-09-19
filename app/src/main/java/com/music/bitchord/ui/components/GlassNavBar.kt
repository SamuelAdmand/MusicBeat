/*
 * The bar's structure and its inline/expanded behaviour are
 * EchoMusicApp/Echo-Music's AppFloatingNavBar + FloatingMiniPlayer (GPL-3.0),
 * over the FloatingTabBar vendored in [com.music.bitchord.ui.components.floatingtabbar].
 * BitChord's own tabs, song model, transport and haptics are wired through it in
 * place of Echo's Screens routing and PlayerConnection.
 */
@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBar
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarDefaults
import com.music.bitchord.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

/**
 * The liquid glass navigation bar: the iOS 26 shape where the now playing
 * controls and the tabs are one component rather than two stacked bars.
 *
 * Expanded, it is a full width now playing pill sitting over the tab pill and a
 * separate circular Search tab. Scrolling down collapses it inline — the tabs
 * fold down to just the selected one, the now playing controls narrow into the
 * gap between it and Search, and the whole thing becomes a single row the width
 * of the screen. Scrolling back up expands it. The fold is driven by
 * [scrollConnection], which the page's scroll has to be dispatched into for any
 * of this to move; see MainActivity's `nestedScroll`.
 *
 * Every surface here samples the app backdrop through [Modifier.liquidGlass], so
 * this is only ever used where that is supported and switched on — off either,
 * MainActivity draws [MiniPlayer] and [FloatingBottomBar] instead.
 *
 * [song] null means nothing is playing, and the accessory is simply absent: the
 * bar is then the tab pill and Search alone, and the collapse still works.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    useGlass: Boolean = false,
    hazeState: HazeState? = null,
) {
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) GLASS_EDGE_COLOR else Color.Black.copy(alpha = 0.08f)
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val hideNavigationBarLabels by AppSettings.hideNavigationBarLabels.collectAsStateWithLifecycle()

    val contentColor = if (useGlass) glassContentColor() else MaterialTheme.colorScheme.onSurface
    val selectedColor = if (useGlass) contentColor else MaterialTheme.colorScheme.primary
    val unselectedColor = if (useGlass) contentColor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorColor = if (useGlass) glassIndicatorColor().copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val haptics = rememberHaptics()

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val standaloneIndex = tabs.lastIndex

    // Ensure tab bar is expanded whenever no song is playing
    LaunchedEffect(song) {
        if (song == null) {
            scrollConnection.expand()
        }
    }

    val surfaceModifier: @Composable () -> Modifier = if (useGlass) {
        { Modifier.liquidGlass(shape = pillShape) }
    } else {
        {
            if (reduceDynamicBlur || hazeState == null) {
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, pillShape)
                    .border(GLASS_EDGE_WIDTH, borderColor, pillShape)
            } else {
                Modifier
                    .optimizedHazeEffect(
                        state = hazeState,
                        style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                    )
                    .border(GLASS_EDGE_WIDTH, borderColor, pillShape)
            }
        }
    }

    FloatingTabBar(
        selectedTabKey = selectedIndex,
        scrollConnection = scrollConnection,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth(),
        tabBarContentModifier = surfaceModifier,
        inlineAccessory = song?.let { current ->
            { accessoryModifier, _ ->
                GlassNowPlaying(
                    song = current,
                    isInline = true,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    contentColor = contentColor,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onExpand = onExpand,
                    onDismiss = onDismiss,
                    useGlass = useGlass,
                    modifier = accessoryModifier.then(surfaceModifier()),
                )
            }
        },
        expandedAccessory = song?.let { current ->
            { accessoryModifier, _ ->
                GlassNowPlaying(
                    song = current,
                    isInline = false,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    contentColor = contentColor,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onExpand = onExpand,
                    onDismiss = onDismiss,
                    useGlass = useGlass,
                    modifier = accessoryModifier.fillMaxWidth().then(surfaceModifier()),
                )
            }
        },
        // Transparent: the surface underneath is the background, and a
        // colour over it would be the thing you saw instead of the backdrop.
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = Color.Transparent,
            accessoryBackgroundColor = Color.Transparent,
            indicatorColor = indicatorColor,
        ),
        // Flat, because the surface is not. Every surface here already draws its
        // own edge border and shadow pass.
        elevations = FloatingTabBarDefaults.elevations(
            inlineElevation = 0.dp,
            expandedElevation = 0.dp,
        ),
        // Expanded, this is meant to be the plain [FloatingBottomBar] with a
        // different material — same outer width, same pill inset, same tab
        // padding, same 25dp glyph — so the two bars measure identically and the
        // toggle changes the surface rather than the layout. The horizontal tab
        // padding is gone with it: the tabs divide the pill by weight now, the
        // way the plain bar's do, so a per-tab horizontal padding would only
        // inset the ripple.
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(PILL_INSET),
            tabExpandedContentPadding = PaddingValues(vertical = TAB_VERTICAL_PADDING),
        ),
        // Held too: this is declared `Any?`, so a fresh list every pass is a
        // changed argument by identity and defeats skipping on its own.
        contentKey = remember(selectedIndex, tabs, contentColor, hideNavigationBarLabels) {
            listOf(selectedIndex, tabs, contentColor, hideNavigationBarLabels)
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val tint = if (isSelected) selectedColor else unselectedColor
            val onClick = {
                if (!isSelected) haptics.play(Haptic.Select)
                currentOnTabSelected(index)
            }
            if (index == standaloneIndex) {
                standaloneTab(
                    key = index,
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(25.dp),
                        )
                    },
                    onClick = onClick,
                )
            } else {
                tab(
                    key = index,
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(25.dp),
                        )
                    },
                    title = {
                        if (!hideNavigationBarLabels) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                // The library's Tab stacks the glyph and the label
                                // with nothing between them; the plain bar spaces
                                // them, and this is where that gap goes.
                                modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP),
                            )
                        }
                    },
                    onClick = onClick,
                )
            }
        }
    }
}

/**
 * The now playing controls docked into [GlassNavBar] as its accessory.
 *
 * Two densities of the same row rather than two components, so the shared
 * element carrying it between the bar's states has one thing to interpolate:
 * [isInline] narrows the artwork, drops the artist line and drops the skip
 * button, which is what makes it fit the collapsed row's height.
 *
 * The content is [MiniPlayer]'s — same artwork, same transport, same haptics —
 * and not Echo's, which reaches into a player connection this app doesn't have.
 * The press response is Echo's, and belongs to the glass rather than the row:
 * a surface you can push on is the whole point of the material.
 */
@Composable
private fun GlassNowPlaying(
    song: Song,
    isInline: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    contentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit = {},
    useGlass: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accessoryPressScale",
    )

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 48.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val artSize = if (isInline) 32.dp else 40.dp
    val glyphSlot = if (isInline) 32.dp else 40.dp
    val glyphSize = if (isInline) 24.dp else 32.dp

    Box(
        // Inline, the accessory is stretched to the row's height by the tab bar
        // (`fillMaxHeight` on a row measured at IntrinsicSize.Max), and this row
        // is shorter than that — the tab pill beside it, with a 25dp glyph in
        // 10dp of padding, is the tallest thing in the row and sets the height.
        // A Box defaults to TopStart, so the artwork and the transport sat a
        // couple of dp above the pill's centre line. Expanded the Box wraps its
        // content, so centring is a no-op there.
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = offsetY.value
                alpha = (1f - (offsetY.value / (dismissThresholdPx * 1.5f))).coerceIn(0f, 1f)
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY.value >= dismissThresholdPx) {
                            scope.launch {
                                offsetY.animateTo(dismissThresholdPx * 2f, tween(120))
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                offsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0 || offsetY.value > 0) {
                            change.consume()
                            val next = (offsetY.value + dragAmount).coerceAtLeast(0f)
                            scope.launch { offsetY.snapTo(next) }
                        }
                    },
                )
            }
            .then(modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = pressSource,
                    indication = null,
                    onClick = onExpand,
                )
                .padding(
                    horizontal = if (isInline) 8.dp else 12.dp,
                    vertical = if (isInline) 4.dp else 8.dp,
                ),
        ) {
                AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(if (isInline) 6.dp else 8.dp))
                    .thumbnailBorder(RoundedCornerShape(if (isInline) 6.dp else 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(if (isInline) 8.dp else 10.dp))
            if (isInline) {
                Text(
                    text = song.title,
                    // The same size the expanded row sets it in. Collapsing the
                    // bar drops the artist line and the skip button, not the
                    // title's weight in the row — a title that shrank as well
                    // would read as a different component rather than the same
                    // one folded up, and the shared element carrying it between
                    // the two states has one less thing to interpolate.
                    //
                    // It costs nothing in height: the row is stretched to the
                    // tab pill's 45dp either way, and titleMedium's line box
                    // still clears the 32dp artwork beside it.
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    // The inline row shares its width with the tab pill and the
                    // Search circle, so most titles will not fit at this size.
                    // Cut with an ellipsis rather than wrapped or scaled.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(Modifier.weight(1f)) {
                    ExplicitSongTitle(
                        song = song,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (useGlass) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isLoading) {
                Box(Modifier.size(glyphSlot), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = if (useGlass) contentColor else MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(if (isInline) 18.dp else 22.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                        onPlayPause()
                    },
                    modifier = Modifier.size(glyphSlot),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = contentColor,
                        modifier = Modifier.size(glyphSize),
                    )
                }
            }
            // Dropped inline: the collapsed row is sharing its width with the
            // tab pill and the Search circle, and the title is what has to
            // survive that, not a second transport button.
            if (!isInline) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        haptics.play(Haptic.SkipNext)
                        onNext()
                    },
                    modifier = Modifier.size(glyphSlot),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.widget_next),
                        tint = contentColor,
                        modifier = Modifier.size(glyphSize),
                    )
                }
            }
        }
    }
}
