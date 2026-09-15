package com.codewave.player.core.model

enum class SongSortOption(val displayName: String) {
    DATE_ADDED_DESC("Recently Added"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ALBUM_ASC("Album (A-Z)"),
    DURATION_DESC("Duration (Longest)"),
    DURATION_ASC("Duration (Shortest)"),
    FILE_SIZE_DESC("File Size"),
    YEAR_DESC("Year (Newest)")
}

enum class AlbumSortOption(val displayName: String) {
    DATE_ADDED_DESC("Recently Added"),
    TITLE_ASC("Album (A-Z)"),
    ARTIST_ASC("Artist (A-Z)"),
    YEAR_DESC("Release Year"),
    TRACK_COUNT_DESC("Track Count")
}

enum class ArtistSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    TRACK_COUNT_DESC("Track Count")
}

enum class ViewMode {
    LIST,
    GRID
}

data class SearchResult(
    val query: String,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList()
) {
    val isEmpty: Boolean
        get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()
}
