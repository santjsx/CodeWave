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
     * Groups a list of tracks into canonical Album models.
     * Pure, deterministic function used identically across Library, Search, and Statistics.
     */
    fun groupTracksIntoAlbums(tracks: List<Track>): List<Album> {
        if (tracks.isEmpty()) return emptyList()

        // Phase 1: Partition tracks by presence of non-blank albumArtist
        val tracksWithAlbumArtist = mutableListOf<Track>()
        val tracksWithoutAlbumArtist = mutableListOf<Track>()

        for (track in tracks) {
            val normArt = MetadataNormalizer.normalizeForGrouping(track.albumArtist)
            if (normArt.isNotEmpty()) {
                tracksWithAlbumArtist.add(track)
            } else {
                tracksWithoutAlbumArtist.add(track)
            }
        }

        val resultAlbums = mutableListOf<Album>()

        // Phase 2: Group tracks that have explicit albumArtist
        val byAlbumArtist = tracksWithAlbumArtist.groupBy {
            MetadataNormalizer.normalizeForGrouping(it.albumArtist)
        }

        for ((normArtist, artistTracks) in byAlbumArtist) {
            // Group by normalized album title within the same albumArtist
            val byAlbumTitle = artistTracks.groupBy {
                val clean = MetadataNormalizer.normalizeForGrouping(it.album)
                clean.ifEmpty { "unknown album" }
            }

            // Cluster near-identical titles under the same albumArtist (e.g. "aadavari matalaku ardhale verule" vs "...ardhalu veruley")
            // Sort title groups by track count descending so the primary release name forms the cluster anchor
            val sortedTitles = byAlbumTitle.keys.sortedByDescending { byAlbumTitle[it]?.size ?: 0 }
            val clusters = mutableListOf<Pair<String, MutableList<Track>>>()

            for (title in sortedTitles) {
                val groupTracks = byAlbumTitle[title] ?: continue
                var matchedCluster = false

                for ((clusterTitle, clusterTracks) in clusters) {
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
                    clusters.add(title to groupTracks.toMutableList())
                }
            }

            for ((canonicalNormTitle, clusterTracks) in clusters) {
                val sortedClusterTracks = clusterTracks.sortedWith(TRACK_ORDER_COMPARATOR)
                val canonicalKey = computeAlbumKey(canonicalNormTitle, normArtist)
                val albumId = computeStableAlbumId(canonicalKey)

                // Pick display title: choose shortest clean non-blank title among tracks
                val bestTitle = sortedClusterTracks
                    .map { MetadataNormalizer.cleanDisplayTitle(it.album) }
                    .filter { it.isNotBlank() && !it.equals("Unknown Album", ignoreCase = true) }
                    .minByOrNull { it.length }
                    ?: MetadataNormalizer.cleanDisplayTitle(sortedClusterTracks.firstOrNull()?.album)

                // Pick display artist: explicit albumArtist from the first track that has it
                val explicitAlbumArtist = sortedClusterTracks
                    .mapNotNull { it.albumArtist?.trim() }
                    .firstOrNull { it.isNotBlank() }
                    ?: normArtist

                // Deterministic artwork: first valid non-blank artwork in sorted order
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
                        albumArtist = explicitAlbumArtist,
                        trackCount = sortedClusterTracks.size,
                        year = maxYear,
                        artworkUri = artworkUri,
                        isHiRes = sortedClusterTracks.any { it.isHiRes },
                        isLossless = sortedClusterTracks.any { it.isLossless }
                    )
                )
            }
        }

        // Phase 3: Group tracks that do NOT have explicit albumArtist
        val byMissingArtistAlbum = tracksWithoutAlbumArtist.groupBy {
            val clean = MetadataNormalizer.normalizeForGrouping(it.album)
            clean.ifEmpty { "unknown album" }
        }

        for ((normTitle, albumTracks) in byMissingArtistAlbum) {
            // Check if all tracks in this album share the exact same track artist
            val distinctNormTrackArtists = albumTracks.map {
                MetadataNormalizer.normalizeForGrouping(it.artist)
            }.distinct()

            if (distinctNormTrackArtists.size == 1) {
                // Single-artist album without albumArtist tag
                val normArtist = distinctNormTrackArtists.first()
                val sortedAlbumTracks = albumTracks.sortedWith(TRACK_ORDER_COMPARATOR)
                val canonicalKey = computeAlbumKey(normTitle, normArtist)
                val albumId = computeStableAlbumId(canonicalKey)

                val displayTitle = sortedAlbumTracks
                    .map { MetadataNormalizer.cleanDisplayTitle(it.album) }
                    .minByOrNull { it.length }
                    ?: MetadataNormalizer.cleanDisplayTitle(sortedAlbumTracks.firstOrNull()?.album)

                val displayArtist = sortedAlbumTracks.first().artist.trim().ifEmpty { "Unknown Artist" }
                val artworkUri = sortedAlbumTracks.mapNotNull { it.albumArtUri?.trim() }.firstOrNull { it.isNotEmpty() }
                val maxYear = sortedAlbumTracks.mapNotNull { it.year }.filter { it > 0 }.maxOrNull()

                resultAlbums.add(
                    Album(
                        id = albumId,
                        key = canonicalKey,
                        title = displayTitle,
                        artist = displayArtist,
                        albumArtist = displayArtist,
                        trackCount = sortedAlbumTracks.size,
                        year = maxYear,
                        artworkUri = artworkUri,
                        isHiRes = sortedAlbumTracks.any { it.isHiRes },
                        isLossless = sortedAlbumTracks.any { it.isLossless }
                    )
                )
            } else {
                // Multi-artist compilation / soundtrack without albumArtist tag
                // Keep tracks together as ONE album; display artist is "Various Artists"
                val sortedAlbumTracks = albumTracks.sortedWith(TRACK_ORDER_COMPARATOR)
                val canonicalKey = computeAlbumKey(normTitle, "various_artists")
                val albumId = computeStableAlbumId(canonicalKey)

                val displayTitle = sortedAlbumTracks
                    .map { MetadataNormalizer.cleanDisplayTitle(it.album) }
                    .minByOrNull { it.length }
                    ?: MetadataNormalizer.cleanDisplayTitle(sortedAlbumTracks.firstOrNull()?.album)

                val artworkUri = sortedAlbumTracks.mapNotNull { it.albumArtUri?.trim() }.firstOrNull { it.isNotEmpty() }
                val maxYear = sortedAlbumTracks.mapNotNull { it.year }.filter { it > 0 }.maxOrNull()

                resultAlbums.add(
                    Album(
                        id = albumId,
                        key = canonicalKey,
                        title = displayTitle,
                        artist = "Various Artists",
                        albumArtist = null,
                        trackCount = sortedAlbumTracks.size,
                        year = maxYear,
                        artworkUri = artworkUri,
                        isHiRes = sortedAlbumTracks.any { it.isHiRes },
                        isLossless = sortedAlbumTracks.any { it.isLossless }
                    )
                )
            }
        }

        // Return sorted by album title ascending (case-insensitive)
        return resultAlbums.sortedBy { it.title.lowercase() }
    }

    /**
     * Resolves all tracks belonging to the specified album, ordered deterministically.
     */
    fun getTracksForAlbum(tracks: List<Track>, album: Album): List<Track> {
        if (tracks.isEmpty()) return emptyList()

        // Match by canonical key if available
        if (album.key.isNotEmpty()) {
            val allAlbumsWithTracks = groupTracksIntoAlbumMap(tracks)
            return allAlbumsWithTracks[album.key] ?: emptyList()
        }

        // Fallback: match by normalized album title and albumArtist
        return getTracksForAlbum(tracks, album.title, album.albumArtist ?: album.artist)
    }

    /**
     * Resolves all tracks belonging to the specified album title, ordered deterministically.
     */
    fun getTracksForAlbum(tracks: List<Track>, albumTitle: String, albumArtist: String? = null): List<Track> {
        if (tracks.isEmpty()) return emptyList()

        val normTitle = MetadataNormalizer.normalizeForGrouping(albumTitle)
        val normArtist = albumArtist?.let { MetadataNormalizer.normalizeForGrouping(it) }?.ifEmpty { null }

        val allAlbumsWithTracks = groupTracksIntoAlbumMap(tracks)

        // 1. Exact canonical key match if artist provided
        if (normArtist != null) {
            val key = computeAlbumKey(normTitle, normArtist)
            allAlbumsWithTracks[key]?.let { return it }
        }

        // 2. Match by normalized album title in keys
        for ((key, albumTracks) in allAlbumsWithTracks) {
            val keyTitle = key.substringBefore(KEY_SEPARATOR)
            if (keyTitle == normTitle || MetadataNormalizer.levenshteinDistance(keyTitle, normTitle) <= 2) {
                if (normArtist == null || key.substringAfter(KEY_SEPARATOR) == normArtist) {
                    return albumTracks
                }
            }
        }

        // 3. Fallback: filter tracks directly with relaxed title matching
        return tracks.filter {
            val tNorm = MetadataNormalizer.normalizeForGrouping(it.album)
            tNorm == normTitle || MetadataNormalizer.levenshteinDistance(tNorm, normTitle) <= 2
        }.sortedWith(TRACK_ORDER_COMPARATOR)
    }

    /**
     * Internal helper that groups tracks and maps canonical keys to their sorted tracks.
     */
    private fun groupTracksIntoAlbumMap(tracks: List<Track>): Map<String, List<Track>> {
        val albums = groupTracksIntoAlbums(tracks)
        val albumKeySet = albums.map { it.key }.toSet()

        // Map each track to its matching album key
        val result = mutableMapOf<String, MutableList<Track>>()
        for (key in albumKeySet) {
            result[key] = mutableListOf()
        }

        // For fast lookup, group by albumArtist then albumTitle
        for (album in albums) {
            val normAlbum = album.key.substringBefore(KEY_SEPARATOR)
            val normArtist = album.key.substringAfter(KEY_SEPARATOR)

            for (track in tracks) {
                val tNormAlbum = MetadataNormalizer.normalizeForGrouping(track.album)
                val tNormArtist = MetadataNormalizer.normalizeForGrouping(track.albumArtist)

                val artistMatches = if (normArtist == "various_artists") {
                    tNormArtist.isEmpty()
                } else if (tNormArtist.isNotEmpty()) {
                    tNormArtist == normArtist
                } else {
                    MetadataNormalizer.normalizeForGrouping(track.artist) == normArtist
                }

                if (artistMatches) {
                    val dist = MetadataNormalizer.levenshteinDistance(tNormAlbum, normAlbum)
                    val minLen = minOf(tNormAlbum.length, normAlbum.length)
                    if (tNormAlbum == normAlbum || (dist <= 3 && minLen >= 15)) {
                        result[album.key]?.add(track)
                    }
                }
            }
        }

        // Sort all track lists deterministically
        return result.mapValues { (_, list) ->
            list.distinctBy { it.id }.sortedWith(TRACK_ORDER_COMPARATOR)
        }
    }
}
