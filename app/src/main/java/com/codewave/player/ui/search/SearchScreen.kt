package com.codewave.player.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.codewave.player.core.designsystem.component.CWEmptyState
import com.codewave.player.core.designsystem.component.CWTrackRow
import com.codewave.player.core.designsystem.theme.CWColors
import com.codewave.player.core.designsystem.theme.CWShapes
import com.codewave.player.core.designsystem.theme.CWTypography
import com.codewave.player.core.model.Track

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTrackInspect: (Track) -> Unit,
    onTrackOptions: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val result by viewModel.searchResult.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(120)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CWColors.Background)
            .padding(top = 16.dp)
    ) {
        // Search Input Field with terminal prompt aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(CWShapes.RadiusMedium))
                .background(CWColors.SurfaceElevated)
                .border(1.dp, if (query.isNotEmpty()) CWColors.BorderFocus else CWColors.BorderSubtle, RoundedCornerShape(CWShapes.RadiusMedium))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ">",
                    style = CWTypography.TechInspectorHeader,
                    color = CWColors.AccentCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search tracks, artists, albums...",
                            style = CWTypography.AppTypography.bodyLarge,
                            color = CWColors.TextTertiary
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        textStyle = CWTypography.AppTypography.bodyLarge.copy(color = CWColors.TextPrimary),
                        cursorBrush = SolidColor(CWColors.AccentCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearQuery() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = CWColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isEmpty()) {
            CWEmptyState(
                title = "SEARCH LIBRARY",
                message = "Instant Unicode-aware search across all indexed audio files, albums, and artists.",
                icon = Icons.Default.Search
            )
        } else if (result.isEmpty) {
            CWEmptyState(
                title = "NO MATCHES",
                message = "No tracks or albums matched '$query'. Try another term or scan your music folder."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (result.tracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "MATCHING TRACKS (${result.tracks.size})",
                            style = CWTypography.TechBadge,
                            color = CWColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(result.tracks, key = { "search_${it.id}" }) { track ->
                        CWTrackRow(
                            track = track,
                            onTrackClick = { viewModel.playTrack(track) },
                            onFavoriteClick = { viewModel.toggleFavorite(track) },
                            onMoreClick = { onTrackOptions?.invoke(track) ?: onTrackInspect(track) }
                        )
                    }
                }
            }
        }
    }
}
