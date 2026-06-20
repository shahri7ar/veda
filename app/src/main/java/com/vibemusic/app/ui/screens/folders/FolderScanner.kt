package com.vibemusic.app.ui.screens.folders

import android.os.Environment
import java.io.File

/**
 * Musicolet-style folder scanner:
 *  - Linear mode: all music folders flattened at root level
 *  - Hierarchical mode: actual directory tree
 */
object FolderScanner {

    data class MusicFolder(
        val path: String,
        val name: String,
        val trackCount: Int = 0,
        val subFolders: List<MusicFolder> = emptyList(),
    )

    /** Scan for folders containing audio files (linear). */
    fun scanLinear(): List<MusicFolder> {
        val results = mutableListOf<MusicFolder>()
        val root = Environment.getExternalStorageDirectory()
        scanRecursive(root, results, depth = 0, maxDepth = 10)
        return results
            .sortedBy { it.name.lowercase() }
    }

    /** Scan hierarchical directory tree. */
    fun scanHierarchical(): List<MusicFolder> {
        val root = Environment.getExternalStorageDirectory()
        return listOfNotNull(buildTree(root, depth = 0, maxDepth = 10))
    }

    // ---------------------------------------------------------------

    private val AUDIO_EXT = setOf(
        "mp3", "flac", "m4a", "aac", "ogg", "opus",
        "wav", "wma", "alac", "ape", "dsf", "dff", "aiff",
    )

    private fun isAudioFile(f: File): Boolean =
        AUDIO_EXT.contains(f.extension.lowercase())

    private fun hasAudioFiles(dir: File): Boolean {
        val children = dir.listFiles() ?: return false
        return children.any { it.isFile && isAudioFile(it) }
    }

    private fun scanRecursive(
        dir: File,
        out: MutableList<MusicFolder>,
        depth: Int,
        maxDepth: Int,
    ) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        val audioCount = children.count { it.isFile && isAudioFile(it) }
        if (audioCount > 0) {
            out.add(MusicFolder(
                path = dir.absolutePath,
                name = dir.name.ifBlank { dir.absolutePath },
                trackCount = audioCount,
            ))
        }
        children.filter { it.isDirectory && !it.name.startsWith(".") }
            .forEach { scanRecursive(it, out, depth + 1, maxDepth) }
    }

    private fun buildTree(
        dir: File,
        depth: Int,
        maxDepth: Int,
    ): MusicFolder? {
        if (depth > maxDepth) return null
        val children = dir.listFiles() ?: return null
        val audioCount = children.count { it.isFile && isAudioFile(it) }
        val subs = children
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .mapNotNull { buildTree(it, depth + 1, maxDepth) }

        if (audioCount == 0 && subs.isEmpty()) return null

        return MusicFolder(
            path = dir.absolutePath,
            name = dir.name.ifBlank { dir.absolutePath },
            trackCount = audioCount + subs.sumOf { it.trackCount },
            subFolders = subs,
        )
    }
}
