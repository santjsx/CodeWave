package com.codewave.player.core.model

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Normalizes audio metadata for canonical album grouping and comparison.
 * Keeps display metadata separate from grouping metadata to preserve human-readable casing and punctuation.
 */
object MetadataNormalizer {

    private val LANGUAGE_TAG_REGEX = Pattern.compile(
        """\s*\[\s*(?:telugu|tamil|hindi|kannada|malayalam)\s*\]\s*""",
        Pattern.CASE_INSENSITIVE
    )

    private val SOUNDTRACK_BRACKET_REGEX = Pattern.compile(
        """\s*\[\s*(?:original motion picture soundtrack|soundtrack|ost)\s*\]\s*""",
        Pattern.CASE_INSENSITIVE
    )

    private val SOUNDTRACK_PAREN_REGEX = Pattern.compile(
        """\s*\(\s*(?:original motion picture soundtrack|soundtrack|ost)\s*\)\s*""",
        Pattern.CASE_INSENSITIVE
    )

    private val RELEASE_TYPE_REGEX = Pattern.compile(
        """\s*-\s*(?:ep|single)\s*$""",
        Pattern.CASE_INSENSITIVE
    )

    private val MULTIPLE_SPACES_REGEX = Pattern.compile("""\s+""")

    /**
     * Normalizes text for album comparison and grouping:
     * - Unicode NFC normalization
     * - Converts non-breaking / special spaces to standard ASCII space
     * - Trims leading/trailing whitespace
     * - Collapses multiple internal spaces to a single space
     * - Removes release / language noise brackets when present (e.g. [Telugu], (Original Motion Picture Soundtrack))
     * - Case-insensitive lowercase
     */
    fun normalizeForGrouping(value: String?): String {
        if (value.isNullOrBlank()) return ""

        // 1. Unicode NFC normalization
        val nfc = Normalizer.normalize(value, Normalizer.Form.NFC)

        // 2. Convert non-breaking and invisible spaces to standard ASCII space
        val cleanSpaces = nfc
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace('\uFEFF', ' ')

        // 3. Remove release / language noise tags for album grouping
        var s = LANGUAGE_TAG_REGEX.matcher(cleanSpaces).replaceAll("")
        s = SOUNDTRACK_BRACKET_REGEX.matcher(s).replaceAll("")
        s = SOUNDTRACK_PAREN_REGEX.matcher(s).replaceAll("")
        s = RELEASE_TYPE_REGEX.matcher(s).replaceAll("")

        // 4. Collapse repeated whitespace and trim
        s = MULTIPLE_SPACES_REGEX.matcher(s.trim()).replaceAll(" ")

        // 5. Lowercase for case-insensitive grouping
        return s.lowercase()
    }

    /**
     * Cleans whitespace and Unicode while preserving original casing and meaningful punctuation for display.
     */
    fun cleanDisplayTitle(value: String?, fallback: String = "Unknown Album"): String {
        if (value.isNullOrBlank()) return fallback
        val nfc = Normalizer.normalize(value, Normalizer.Form.NFC)
        val cleanSpaces = nfc
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace('\uFEFF', ' ')
        val trimmed = MULTIPLE_SPACES_REGEX.matcher(cleanSpaces.trim()).replaceAll(" ")
        return trimmed.ifBlank { fallback }
    }

    /**
     * Computes the Levenshtein distance between two normalized strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        var previous = IntArray(s2.length + 1) { it }
        var current = IntArray(s2.length + 1)

        for (i in s1.indices) {
            current[0] = i + 1
            val c1 = s1[i]
            for (j in s2.indices) {
                val cost = if (c1 == s2[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,        // insertion
                    previous[j + 1] + 1,    // deletion
                    previous[j] + cost      // substitution
                )
            }
            val temp = previous
            previous = current
            current = temp
        }
        return previous[s2.length]
    }
}

/**
 * Canonical domain-level album grouping engine for CodeWave.
 * Establishes one single authoritative source of truth for album identity and track aggregation.
 */
object AlbumGrouping {

    private const val KEY_SEPARATOR = "\u0000"

    private val GENERIC_ALBUM_TITLES = setOf(
        "greatest hits", "best of", "the best of", "live", "collection", "anthology",
        "remix", "remixes", "ep", "singles", "single", "unknown album", "untitled"
    )

    /**
     * Holds the canonical album list and an O(1) index of tracks for each album key.
     */
    data class GroupResult(
        val albums: List<Album>,
        val tracksByAlbumKey: Map<String, List<Track>>
    ) {
        fun getTracksForAlbum(album: Album): List<Track> {
            if (album.key.isNotEmpty()) {
                tracksByAlbumKey[album.key]?.let { return it }
            }
            val matched = albums.find { it.id == album.id || it.title.equals(album.title, ignoreCase = true) }
            return matched?.let { tracksByAlbumKey[it.key] } ?: emptyList()
        }

        fun getTracksForAlbumTitle(title: String, artist: String? = null): List<Track> {
            if (artist != null) {
                val normArt = MetadataNormalizer.normalizeForGrouping(artist)
                val cleanTitle = MetadataNormalizer.normalizeForGrouping(title)
                val key = "$cleanTitle$KEY_SEPARATOR$normArt"
                tracksByAlbumKey[key]?.let { return it }
            }
            val cleanTitle = MetadataNormalizer.normalizeForGrouping(title)
            val matched = albums.find {
                val kTitle = it.key.substringBefore(KEY_SEPARATOR)
                kTitle == cleanTitle || it.title.equals(title, ignoreCase = true)
            }
            return matched?.let { tracksByAlbumKey[it.key] } ?: emptyList()
        }
    }

    /**
     * Constructs a canonical string key for an album:
     * canonicalAlbumName + "\u0000" + canonicalAlbumArtist
     */
    fun computeAlbumKey(normalizedAlbum: String, normalizedAlbumArtist: String): String {
        return "$normalizedAlbum$KEY_SEPARATOR$normalizedAlbumArtist"
    }

    /**
     * Generates a deterministic, positive 64-bit Long ID from the canonical string key
     * using FNV-1a 64-bit hashing. Stable across JVM restarts and platforms.
     */
    fun computeStableAlbumId(canonicalKey: String): Long {
        var hash = -0x443f75e30563fee3L // FNV offset basis
        for (i in canonicalKey.indices) {
            hash = hash xor canonicalKey[i].code.toLong()
            hash = hash * 0x100000001b3L // FNV prime
        }
        val positive = hash and Long.MAX_VALUE
        return if (positive == 0L) 1L else positive
    }

    /**
     * Deterministic track comparator for album track ordering:
     * 1. disc number ASC (nulls last)
     * 2. track number ASC (nulls last)
     * 3. title ASC (case-insensitive)
     * 4. track ID ASC
     */
    val TRACK_ORDER_COMPARATOR = Comparator<Track> { t1, t2 ->
        val disc1 = t1.discNumber ?: Int.MAX_VALUE
        val disc2 = t2.discNumber ?: Int.MAX_VALUE
        if (disc1 != disc2) return@Comparator disc1.compareTo(disc2)

        val num1 = t1.trackNumber ?: Int.MAX_VALUE
        val num2 = t2.trackNumber ?: Int.MAX_VALUE
        if (num1 != num2) return@Comparator num1.compareTo(num2)

        val titleComp = t1.title.compareTo(t2.title, ignoreCase = true)
        if (titleComp != 0) return@Comparator titleComp

        t1.id.compareTo(t2.id)
    }

    /**
     * Groups a list of tracks into canonical Album models and maps each album's key to its sorted tracks.
     * Pure, single-pass deterministic function.
     */
    fun groupTracks(tracks: List<Track>): GroupResult {
        if (tracks.isEmpty()) return GroupResult(emptyList(), emptyMap())

        // 1. Group by normalized album title
        val byAlbumTitle = tracks.groupBy {
            val clean = MetadataNormalizer.normalizeForGrouping(it.album)
            clean.ifEmpty { "unknown album" }
        }

        // 2. Cluster near-identical titles (e.g. "aadavari matalaku ardhale verule" vs "...ardhalu veruley")
        val sortedTitles = byAlbumTitle.keys.sortedByDescending { byAlbumTitle[it]?.size ?: 0 }
        val titleClusters = mutableListOf<Pair<String, MutableList<Track>>>()

        for (title in sortedTitles) {
            val groupTracks = byAlbumTitle[title] ?: continue
            var matchedCluster = false

            for ((clusterTitle, clusterTracks) in titleClusters) {
                val dist = MetadataNormalizer.levenshteinDistance(title, clusterTitle)
                val minLen = minOf(title.length, clusterTitle.length)
                // Merge if exact or if Levenshtein distance <= 3 for titles with sufficient length
                if (dist <= 3 && minLen >= 15) {
                    clusterTracks.addAll(groupTracks)
                    matchedCluster = true
                    break
                }
            }

            if (!matchedCluster) {
                titleClusters.add(title to groupTracks.toMutableList())
            }
        }

        val resultAlbums = mutableListOf<Album>()
        val resultTracksMap = mutableMapOf<String, List<Track>>()

        // 3. For each title cluster, determine album artist and partition if needed
        for ((canonicalNormTitle, clusterTracks) in titleClusters) {
            val explicitNormAlbumArtists = clusterTracks
                .mapNotNull { it.albumArtist?.trim() }
                .map { MetadataNormalizer.normalizeForGrouping(it) }
                .filter { it.isNotEmpty() }
                .distinct()

            // Sub-partition tracks within this cluster if needed
            val albumPartitions: List<Pair<String, List<Track>>> = when {
                // Case A: Multiple conflicting explicit album artists (e.g. different artists releasing "Greatest Hits")
                explicitNormAlbumArtists.size > 1 -> {
                    clusterTracks.groupBy {
                        val norm = MetadataNormalizer.normalizeForGrouping(it.albumArtist)
                        norm.ifEmpty { MetadataNormalizer.normalizeForGrouping(it.artist) }
                    }.map { (artistKey, trks) -> artistKey to trks }
                }

                // Case B: Exactly ONE explicit album artist present across cluster tracks
                // ALL tracks in this cluster belong to this album artist (even if individual tracks left it blank)
                explicitNormAlbumArtists.size == 1 -> {
                    listOf(explicitNormAlbumArtists.first() to clusterTracks)
                }

                // Case C: NO explicit album artist present on any track in the cluster
                else -> {
                    val distinctTrackArtists = clusterTracks
                        .map { MetadataNormalizer.normalizeForGrouping(it.artist) }
                        .distinct()

                    if (distinctTrackArtists.size == 1) {
                        // Single-artist album
                        listOf(distinctTrackArtists.first() to clusterTracks)
                    } else if (canonicalNormTitle in GENERIC_ALBUM_TITLES) {
                        // Generic album title without albumArtist -> split by track artist
                        clusterTracks.groupBy {
                            MetadataNormalizer.normalizeForGrouping(it.artist)
                        }.map { (artistKey, trks) -> artistKey to trks }
                    } else {
                        // Multi-artist compilation / soundtrack without albumArtist tag -> unified album
                        listOf("various_artists" to clusterTracks)
                    }
                }
            }

            for ((normArtist, partitionTracks) in albumPartitions) {
                val sortedClusterTracks = partitionTracks.sortedWith(TRACK_ORDER_COMPARATOR)
                val canonicalKey = computeAlbumKey(canonicalNormTitle, normArtist)
                val albumId = computeStableAlbumId(canonicalKey)

                // Pick display title: shortest clean non-blank title among tracks
                val bestTitle = sortedClusterTracks
                    .map { MetadataNormalizer.cleanDisplayTitle(it.album) }
                    .filter { it.isNotBlank() && !it.equals("Unknown Album", ignoreCase = true) }
                    .minByOrNull { it.length }
                    ?: MetadataNormalizer.cleanDisplayTitle(sortedClusterTracks.firstOrNull()?.album)

                // Pick display artist: explicit albumArtist if available, otherwise most common track artist or "Various Artists"
                val explicitAlbumArtist = sortedClusterTracks
                    .mapNotNull { it.albumArtist?.trim() }
                    .firstOrNull { it.isNotBlank() }
                    ?: if (normArtist == "various_artists") {
                        val artistFrequencies = sortedClusterTracks
                            .map { it.artist.trim() }
                            .groupingBy { it }
                            .eachCount()
                        val topArtist = artistFrequencies.maxByOrNull { it.value }
                        if (topArtist != null && topArtist.value >= sortedClusterTracks.size / 2) {
                            topArtist.key
                        } else {
                            "Various Artists"
                        }
                    } else {
                        sortedClusterTracks.firstOrNull {
                            MetadataNormalizer.normalizeForGrouping(it.artist) == normArtist
                        }?.artist?.trim() ?: sortedClusterTracks.first().artist.trim().ifEmpty { "Unknown Artist" }
                    }

                val artworkUri = sortedClusterTracks
                    .mapNotNull { it.albumArtUri?.trim() }
                    .firstOrNull { it.isNotEmpty() }

                val maxYear = sortedClusterTracks
                    .mapNotNull { it.year }
                    .filter { it > 0 }
                    .maxOrNull()

                resultAlbums.add(
                    Album(
                        id = albumId,
                        key = canonicalKey,
                        title = bestTitle,
                        artist = explicitAlbumArtist,
                        albumArtist = if (normArtist == "various_artists") null else explicitAlbumArtist,
                        trackCount = sortedClusterTracks.size,
                        year = maxYear,
                        artworkUri = artworkUri,
                        isHiRes = sortedClusterTracks.any { it.isHiRes },
                        isLossless = sortedClusterTracks.any { it.isLossless }
                    )
                )
                resultTracksMap[canonicalKey] = sortedClusterTracks
            }
        }

        return GroupResult(
            albums = resultAlbums.sortedBy { it.title.lowercase() },
            tracksByAlbumKey = resultTracksMap
        )
    }

    /**
     * Groups a list of tracks into canonical Album models.
     */
    fun groupTracksIntoAlbums(tracks: List<Track>): List<Album> {
        return groupTracks(tracks).albums
    }

    /**
     * Resolves all tracks belonging to the specified album, ordered deterministically.
     * Instant O(1) lookup via GroupResult.
     */
    fun getTracksForAlbum(tracks: List<Track>, album: Album): List<Track> {
        return groupTracks(tracks).getTracksForAlbum(album)
    }

    /**
     * Resolves all tracks belonging to the specified album title, ordered deterministically.
     */
    fun getTracksForAlbum(tracks: List<Track>, albumTitle: String, albumArtist: String? = null): List<Track> {
        return groupTracks(tracks).getTracksForAlbumTitle(albumTitle, albumArtist)
    }
}
