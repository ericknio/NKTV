/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.vod

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import app.opentv.data.model.Source
import app.opentv.data.parser.displayTitle
import app.opentv.data.repo.MovieVariantGroup
import app.opentv.ui.VodBrowseState
import app.opentv.ui.VodViewModel
import coil.compose.AsyncImage

/**
 * Movies: a modern, row-based home — Continue Watching, Recommended, Recently added and a row per
 * genre, each a horizontally-scrolling shelf of poster cards. A category chip strip along the top
 * keeps whole-category browsing one press away without a permanent rail eating the width. Clicking
 * a film opens its detail page rather than playing straight away, the streaming-app convention.
 */
@Composable
fun MoviesScreen(
    onOpenMovie: (Movie) -> Unit,
    onResume: (mediaKey: String, url: String, title: String, seriesRoomId: Long?) -> Unit,
    onOpenSearch: () -> Unit,
    hasSources: Boolean,
    isSyncing: Boolean,
    viewModel: VodViewModel = viewModel(),
) {
    val pinGate = app.opentv.ui.parental.rememberPinGate()
    val categories by viewModel.movieCategories.collectAsState()
    val lockedMovies by viewModel.lockedMovieCats.collectAsState()
    val movieGroups by viewModel.movieGroups.collectAsState()
    val movieLimit by viewModel.movieLimit.collectAsState()
    val movieLoaded by viewModel.movieLoaded.collectAsState()
    val searchGroups by viewModel.movieSearchGroups.collectAsState()
    val vodLoading by viewModel.vodLoading.collectAsState()
    val continueWatchingItems by viewModel.continueWatching.collectAsState()
    val movieCwItems = remember(continueWatchingItems) { continueWatchingItems.filter { it.mediaKey.startsWith("movie:") } }

    val favouriteGroups by viewModel.favouriteMovieGroups.collectAsState()

    // Kept in the ViewModel so Back from a movie's page returns to the same category and search.
    val browse = viewModel.moviesBrowse
    val railTarget = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf(browse.query) }
    var selectedCategory by remember { mutableStateOf(browse.selected) }
    LaunchedEffect(searchQuery, selectedCategory) {
        browse.query = searchQuery
        browse.selected = selectedCategory
    }

    // Last favourite removed while "Favoritos" is open: fall back to "Todos" instead of a stale list.
    LaunchedEffect(favouriteGroups.isEmpty()) {
        if (favouriteGroups.isEmpty() && selectedCategory == "__fav__") selectedCategory = null
    }
    LaunchedEffect(movieCwItems.isEmpty()) {
        if (movieCwItems.isEmpty() && selectedCategory == "__cw__") selectedCategory = null
    }

    LaunchedEffect(Unit) {
        if (hasSources) viewModel.ensureVodLoaded()
        viewModel.loadHomeFeeds()
    }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
        ) {
            Text(
                stringResource(R.string.nav_movies),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            VodSearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.setMovieSearchQuery(it)
                },
                placeholder = "Buscar filme...",
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (movieCwItems.isNotEmpty()) {
                    item {
                        VodRailEntry(
                            focusWhenSelected = railTarget,
                            label = stringResource(R.string.vod_continue_watching),
                            selected = selectedCategory == "__cw__" && searchQuery.isEmpty(),
                            onClick = {
                                selectedCategory = "__cw__"
                                searchQuery = ""
                                viewModel.setMovieSearchQuery("")
                            },
                        )
                    }
                }
                if (favouriteGroups.isNotEmpty()) {
                    item {
                        VodRailEntry(
                            focusWhenSelected = railTarget,
                            label = stringResource(R.string.vod_favourites),
                            selected = selectedCategory == "__fav__" && searchQuery.isEmpty(),
                            onClick = {
                                selectedCategory = "__fav__"
                                searchQuery = ""
                                viewModel.setMovieSearchQuery("")
                            },
                        )
                    }
                }
                item {
                    VodRailEntry(
                        focusWhenSelected = railTarget,
                        label = stringResource(R.string.vod_all),
                        selected = selectedCategory == null && searchQuery.isEmpty(),
                        onClick = {
                            selectedCategory = null
                            searchQuery = ""
                            viewModel.setMovieSearchQuery("")
                            viewModel.selectMovieCategory(null)
                        },
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    VodRailEntry(
                        focusWhenSelected = railTarget,
                        label = cat.name,
                        selected = selectedCategory == cat.id && searchQuery.isEmpty(),
                        onClick = {
                            pinGate("movie:${cat.id}") {
                                selectedCategory = cat.id
                                searchQuery = ""
                                viewModel.setMovieSearchQuery("")
                                viewModel.selectMovieCategory(cat.id)
                            }
                        },
                        locked = cat.id in lockedMovies,
                    )
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxHeight()) {
            val isSearching = searchQuery.isNotBlank()
            val displayedGroups = if (isSearching) searchGroups else movieGroups
            when {
                selectedCategory == "__cw__" -> ContinueWatchingRow(movieCwItems, onResume)
                selectedCategory == "__fav__" && !isSearching -> {
                    MovieCategoryGrid(
                        groups = favouriteGroups,
                        onOpenMovie = onOpenMovie,
                        browse = browse,
                        leftTarget = railTarget,
                        categoryKey = "__fav__",
                    )
                }
                displayedGroups.isEmpty() && isSearching -> {
                    EmptyVod(
                        "Nenhum filme encontrado",
                        "Tente buscar por outro termo.",
                    )
                }
                displayedGroups.isEmpty() && (vodLoading || isSyncing) -> {
                    LoadingVod(stringResource(R.string.vod_loading_movies))
                }
                displayedGroups.isEmpty() -> {
                    EmptyVod(
                        stringResource(R.string.vod_no_movies),
                        stringResource(R.string.vod_no_movies_provider),
                    )
                }
                else -> {
                    MovieCategoryGrid(
                        groups = displayedGroups,
                        onOpenMovie = onOpenMovie,
                        browse = browse,
                        leftTarget = if (isSearching) null else railTarget,
                        categoryKey = if (isSearching) searchQuery else selectedCategory,
                        canLoadMore = !isSearching && movieLoaded == movieLimit,
                        onLoadMore = { viewModel.loadMoreMovies() },
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesScreen(
    onOpenSeries: (Series) -> Unit,
    onResume: (mediaKey: String, url: String, title: String, seriesRoomId: Long?) -> Unit,
    onOpenSearch: () -> Unit,
    hasSources: Boolean,
    isSyncing: Boolean,
    viewModel: VodViewModel = viewModel(),
) {
    val pinGate = app.opentv.ui.parental.rememberPinGate()
    val categories by viewModel.seriesCategories.collectAsState()
    val lockedSeries by viewModel.lockedSeriesCats.collectAsState()
    val categorySeries by viewModel.series.collectAsState()
    val searchResults by viewModel.seriesSearchResults.collectAsState()
    val vodLoading by viewModel.vodLoading.collectAsState()
    val continueWatchingItems by viewModel.continueWatching.collectAsState()
    val seriesCwItems = remember(continueWatchingItems) { continueWatchingItems.filter { it.mediaKey.startsWith("ep:") } }

    val favouriteSeries by viewModel.favouriteSeries.collectAsState()

    val browse = viewModel.seriesBrowse
    val railTarget = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf(browse.query) }
    var selectedCategory by remember { mutableStateOf(browse.selected) }
    LaunchedEffect(searchQuery, selectedCategory) {
        browse.query = searchQuery
        browse.selected = selectedCategory
    }

    LaunchedEffect(favouriteSeries.isEmpty()) {
        if (favouriteSeries.isEmpty() && selectedCategory == "__fav__") selectedCategory = null
    }
    LaunchedEffect(seriesCwItems.isEmpty()) {
        if (seriesCwItems.isEmpty() && selectedCategory == "__cw__") selectedCategory = null
    }

    LaunchedEffect(Unit) {
        if (hasSources) viewModel.ensureVodLoaded()
        viewModel.loadHomeFeeds()
    }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
        ) {
            Text(
                stringResource(R.string.nav_shows),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            VodSearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.setSeriesSearchQuery(it)
                },
                placeholder = "Buscar série...",
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (seriesCwItems.isNotEmpty()) {
                    item {
                        VodRailEntry(
                            focusWhenSelected = railTarget,
                            label = stringResource(R.string.vod_continue_watching),
                            selected = selectedCategory == "__cw__" && searchQuery.isEmpty(),
                            onClick = {
                                selectedCategory = "__cw__"
                                searchQuery = ""
                                viewModel.setSeriesSearchQuery("")
                            },
                        )
                    }
                }
                if (favouriteSeries.isNotEmpty()) {
                    item {
                        VodRailEntry(
                            focusWhenSelected = railTarget,
                            label = stringResource(R.string.vod_favourites),
                            selected = selectedCategory == "__fav__" && searchQuery.isEmpty(),
                            onClick = {
                                selectedCategory = "__fav__"
                                searchQuery = ""
                                viewModel.setSeriesSearchQuery("")
                            },
                        )
                    }
                }
                item {
                    VodRailEntry(
                        focusWhenSelected = railTarget,
                        label = stringResource(R.string.vod_all),
                        selected = selectedCategory == null && searchQuery.isEmpty(),
                        onClick = {
                            selectedCategory = null
                            searchQuery = ""
                            viewModel.setSeriesSearchQuery("")
                            viewModel.selectSeriesCategory(null)
                        },
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    VodRailEntry(
                        focusWhenSelected = railTarget,
                        label = cat.name,
                        selected = selectedCategory == cat.id && searchQuery.isEmpty(),
                        onClick = {
                            pinGate("series:${cat.id}") {
                                selectedCategory = cat.id
                                searchQuery = ""
                                viewModel.setSeriesSearchQuery("")
                                viewModel.selectSeriesCategory(cat.id)
                            }
                        },
                        locked = cat.id in lockedSeries,
                    )
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxHeight()) {
            val isSearching = searchQuery.isNotBlank()
            val displayedSeries = if (isSearching) searchResults else categorySeries
            when {
                selectedCategory == "__cw__" -> ContinueWatchingRow(seriesCwItems, onResume)
                selectedCategory == "__fav__" && !isSearching ->
                    SeriesCategoryGrid(favouriteSeries, onOpenSeries, browse, railTarget, "__fav__")
                displayedSeries.isEmpty() && isSearching -> {
                    EmptyVod(
                        "Nenhuma série encontrada",
                        "Tente buscar por outro termo.",
                    )
                }
                displayedSeries.isEmpty() && (vodLoading || isSyncing) -> {
                    LoadingVod(stringResource(R.string.vod_loading_shows))
                }
                displayedSeries.isEmpty() -> {
                    EmptyVod(
                        stringResource(R.string.vod_no_shows),
                        stringResource(R.string.vod_no_shows_provider),
                    )
                }
                else -> {
                    SeriesCategoryGrid(
                        displayedSeries,
                        onOpenSeries,
                        browse,
                        if (isSearching) null else railTarget,
                        if (isSearching) searchQuery else selectedCategory,
                    )
                }
            }
        }
    }
}

@Composable
private fun VodSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    // Read-only until the viewer confirms (OK on the remote, or a tap): a text field opens the keyboard
    // the moment it takes focus, which hijacked the screen while d-padding left toward the tab rail.
    var editing by remember { mutableStateOf(false) }
    var cycling by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val fieldFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    // Going from read-only to editable doesn't restart the text input session, so after unlocking the
    // field, cycle its focus: regaining focus while editable is what brings the keyboard up.
    LaunchedEffect(editing) {
        if (editing) {
            cycling = true
            focusManager.clearFocus()
            kotlinx.coroutines.delay(60)
            runCatching { fieldFocus.requestFocus() }
            cycling = false
            kotlinx.coroutines.delay(80)
            keyboardController?.show()
        }
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        readOnly = !editing,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { keyboardController?.hide(); editing = false },
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (focused) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        width = if (focused) 2.dp else 1.dp,
                        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { if (editing) keyboardController?.show() else editing = true }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onQueryChange("") },
                    )
                }
            }
        },
        modifier = modifier
            .focusRequester(fieldFocus)
            .onFocusChanged {
                focused = it.isFocused
                // A finger tapping the field means "type here"; the d-pad only passing through does not.
                if (it.isFocused && inputMode.inputMode == InputMode.Touch) editing = true
                if (!it.isFocused && !cycling) {
                    editing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    if (editing) keyboardController?.show() else editing = true
                    true
                } else {
                    false
                }
            },
    )
}

@Composable
private fun VodRailEntry(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    focusWhenSelected: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        focused -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            // Target for "Left" out of the poster grid: the current category, never the search field.
            .then(if (selected && focusWhenSelected != null) Modifier.focusRequester(focusWhenSelected) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (locked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Categoria bloqueada",
                tint = fg,
                modifier = Modifier.padding(start = 6.dp).size(16.dp),
            )
        }
    }
}

// ---- Whole-category browse grids ---------------------------------------------------------------

/** One category's films as a poster grid. Quality variants collapse to one card, badged. */
@Composable
private fun MovieCategoryGrid(
    groups: List<MovieVariantGroup>,
    onOpenMovie: (Movie) -> Unit,
    browse: VodBrowseState,
    leftTarget: FocusRequester?,
    categoryKey: Any? = null,
    canLoadMore: Boolean = false,
    onLoadMore: () -> Unit = {},
) {
    if (groups.isEmpty()) { LoadingVod(stringResource(R.string.vod_loading_movies)); return }
    val gridState = rememberBrowseGridState(browse, categoryKey)
    LaunchedEffect(gridState, canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to total
        }.collect { (lastVisible, total) ->
            if (total > 0 && total - lastVisible <= 30) {
                onLoadMore()
            }
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().exitLeftTo(leftTarget),
    ) {
        gridItems(groups, key = { it.primary.id }) { group ->
            val quality = group.variants.firstOrNull()?.qualityLabel?.takeIf { it.isNotBlank() }
            val badge = quality
                ?: if (group.hasMultipleQualities) {
                    stringResource(R.string.guide_qualities_count, group.variants.size)
                } else {
                    null
                }
            PosterCard(
                title = group.primary.displayTitle,
                posterUrl = group.primary.posterUrl,
                subtitle = group.primary.year?.toString(),
                rating = group.primary.rating,
                qualityBadge = badge,
                modifier = Modifier.restoreFocusIfOpened(group.primary.id, browse),
                onClick = { browse.lastOpenedId = group.primary.id; onOpenMovie(group.primary) },
            )
        }
    }
}

/** Pressing Left out of the grid goes to [target] (the current category), not to whatever is nearest. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun Modifier.exitLeftTo(target: FocusRequester?): Modifier =
    if (target == null) this
    else this
        .focusProperties { exit = { if (it == FocusDirection.Left) target else FocusRequester.Default } }
        .focusGroup()

/**
 * Grid state that survives a trip to a detail page: it starts where the viewer left it for this
 * category, and jumps to the top only when the viewer switches category (or search) themselves.
 */
@Composable
private fun rememberBrowseGridState(browse: VodBrowseState, key: Any?): LazyGridState {
    val restore = browse.scrollKey == key
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = if (restore) browse.scrollIndex else 0,
        initialFirstVisibleItemScrollOffset = if (restore) browse.scrollOffset else 0,
    )
    var shownKey by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (key != shownKey) {
            shownKey = key
            gridState.scrollToItem(0)
        }
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                browse.scrollKey = key
                browse.scrollIndex = index
                browse.scrollOffset = offset
            }
    }
    return gridState
}

/**
 * When the viewer is driving with the d-pad, hands focus back to the poster they opened, so Back
 * lands on it instead of falling to the search field (which pops the keyboard open).
 */
@Composable
private fun Modifier.restoreFocusIfOpened(id: Long, browse: VodBrowseState): Modifier {
    if (browse.lastOpenedId != id) return this
    val usingKeys = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (usingKeys) {
            kotlinx.coroutines.delay(150)
            runCatching { requester.requestFocus() }
            keyboard?.hide()
        }
        browse.lastOpenedId = null
    }
    return this.focusRequester(requester)
}

/** One category's shows as a poster grid. */
@Composable
private fun SeriesCategoryGrid(
    series: List<Series>,
    onOpenSeries: (Series) -> Unit,
    browse: VodBrowseState,
    leftTarget: FocusRequester?,
    categoryKey: Any?,
) {
    if (series.isEmpty()) { LoadingVod(stringResource(R.string.vod_loading_shows)); return }
    val gridState = rememberBrowseGridState(browse, categoryKey)
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().exitLeftTo(leftTarget),
    ) {
        gridItems(series, key = { it.id }) { item ->
            PosterCard(
                title = item.displayTitle,
                posterUrl = item.posterUrl,
                subtitle = item.year?.toString(),
                rating = item.rating,
                modifier = Modifier.restoreFocusIfOpened(item.id, browse),
                onClick = { browse.lastOpenedId = item.id; onOpenSeries(item) },
            )
        }
    }
}

// ---- Shared shelves ----------------------------------------------------------------------------

/** A titled horizontal shelf of movie poster cards. Shared by the home and the detail's "more like this". */
@Composable
internal fun MoviePosterRow(title: String, movies: List<Movie>, onOpenMovie: (Movie) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                PosterCard(
                    title = movie.displayTitle,
                    posterUrl = movie.posterUrl,
                    subtitle = movie.year?.toString(),
                    rating = movie.rating,
                    onClick = { onOpenMovie(movie) },
                )
            }
        }
    }
}

/** A titled horizontal shelf of series poster cards. */
@Composable
internal fun SeriesPosterRow(title: String, series: List<Series>, onOpenSeries: (Series) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(series, key = { it.id }) { item ->
                PosterCard(
                    title = item.displayTitle,
                    posterUrl = item.posterUrl,
                    subtitle = item.year?.toString(),
                    rating = item.rating,
                    onClick = { onOpenSeries(item) },
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

/**
 * The reusable poster card: art, title and an optional year, with a rating chip, a quality badge and
 * a resume progress bar drawn over the art where the data is there. The focused card scales up and
 * gains a primary border — the app's established focus cue — and, being focusable, the lazy row
 * brings it into view on its own.
 */
@Composable
internal fun PosterCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    rating: Double? = null,
    qualityBadge: String? = null,
    progress: Float? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "posterScale")
    Column(
        modifier
            .width(POSTER_WIDTH)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (focused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    else Modifier,
                ),
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            rating?.takeIf { it > 0.0 }?.let {
                Badge(
                    text = "★ ${formatRating(it)}",
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                )
            }
            qualityBadge?.let {
                Badge(
                    text = it,
                    highlight = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            progress?.let {
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** A small rounded chip drawn over poster art — a rating or a quality label. */
@Composable
private fun Badge(text: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val bg = if (highlight) MaterialTheme.colorScheme.primary
    else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.66f)
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary else androidx.compose.ui.graphics.Color.White
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ---- Continue watching -------------------------------------------------------------------------

@Composable
internal fun ContinueWatchingRow(
    items: List<VodViewModel.ResumeItem>,
    onResume: (mediaKey: String, url: String, title: String, seriesRoomId: Long?) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.vod_continue_watching))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.mediaKey }) { item ->
                ResumeCard(item) { onResume(item.mediaKey, item.streamUrl, item.title, item.seriesRoomId) }
            }
        }
    }
}

/** A landscape resume thumbnail with a progress fill — a movie or an episode part-way through. */
@Composable
private fun ResumeCard(item: VodViewModel.ResumeItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "resumeScale")
    Column(
        Modifier
            .width(190.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(107.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (focused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    else Modifier,
                ),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomStart),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- Category chips ----------------------------------------------------------------------------

/**
 * A horizontal chip strip along the top of the home: an "All" chip returns to the curated rows, and
 * each following chip opens that category's full grid. Keeps whole-category browsing reachable on a
 * d-pad without a permanent side rail taking the width.
 */
@Composable
private fun CategoryChips(
    entries: List<Pair<String, String>>,
    selected: String?,
    onSelectHome: () -> Unit,
    onSelectCategory: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "label") {
            Text(
                stringResource(R.string.vod_categories),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        item(key = "all") { Chip(stringResource(R.string.vod_all), selected == null, onSelectHome) }
        items(entries, key = { it.first }) { (id, name) ->
            Chip(name, selected == id) { onSelectCategory(id) }
        }
    }
}

/**
 * A provider filter above the category chips, shown only when more than one source is configured —
 * pick a provider to browse just its Movies/Shows categories, or "All sources" to fold them.
 */
@Composable
private fun ProviderChips(
    sources: List<Source>,
    selected: Long?,
    onSelectAll: () -> Unit,
    onSelectSource: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "plabel") {
            Text(
                stringResource(R.string.channels_manager_source_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        item(key = "pall") {
            Chip(stringResource(R.string.channels_manager_all_sources), selected == null, onSelectAll)
        }
        items(sources, key = { it.id }) { source ->
            Chip(source.name, selected == source.id) { onSelectSource(source.id) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        focused -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ---- Search / loading / empty ------------------------------------------------------------------

/**
 * The search entry at the top of the Movies and Shows home. The rail's global search already covers
 * movies and series; this makes it reachable without leaving the tab. Focusable for d-pad on TV and
 * tappable on touch, it just opens the existing search screen.
 */
@Composable
private fun SearchAffordance(onOpenSearch: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onOpenSearch)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (focused) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(Icons.Filled.Search, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.vod_search_hint),
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

@Composable
private fun EmptyVod(title: String, body: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingVod(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.vod_large_provider_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Poster shelf card width; the grid uses an adaptive min size close to this. */
private val POSTER_WIDTH = 140.dp

/** Rating to one decimal place, locale-independent (the "★" is drawn beside it). */
internal fun formatRating(rating: Double): String = String.format(java.util.Locale.US, "%.1f", rating)
