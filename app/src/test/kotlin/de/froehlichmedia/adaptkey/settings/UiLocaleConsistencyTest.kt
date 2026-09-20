// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.language.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * D-478: keeps the app's own UI translations in sync. English (`values/strings.xml`) is the reference; every
 * `values-xx/strings.xml` must define exactly the same string names, with the same format placeholders, and
 * `res/xml/locales_config.xml` (the Android 13+ per-app language picker) must list exactly English plus one
 * entry per translation directory. Without this the DE/EL files silently drifted (two strings missing from
 * both went unnoticed for months, so German and Greek users saw English in the language-pack screen).
 */
class UiLocaleConsistencyTest {
    
    private val resDir: File = listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }
    
    private val placeholder = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")
    
    private fun stringsOf(file: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
    
    private fun translationDirs(): List<File> =
        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values-") && File(f, "strings.xml").exists() }!!
            .sortedBy { it.name }
    
    private val reference: Map<String, String> by lazy { stringsOf(File(resDir, "values/strings.xml")) }
    
    @Test
    fun `every translation defines exactly the reference string names`() {
        assertTrue(reference.isNotEmpty())
        for (dir in translationDirs()) {
            val translated = stringsOf(File(dir, "strings.xml"))
            val missing = reference.keys - translated.keys
            val extra = translated.keys - reference.keys
            assertTrue(missing.isEmpty(), "${dir.name} is missing ${missing.size} string(s): ${missing.sorted()}")
            assertTrue(extra.isEmpty(), "${dir.name} has ${extra.size} string(s) English does not: ${extra.sorted()}")
        }
    }
    
    @Test
    fun `every translation keeps the reference string's format placeholders`() {
        for (dir in translationDirs()) {
            val translated = stringsOf(File(dir, "strings.xml"))
            for ((name, english) in reference) {
                val expected = placeholder.findAll(english).map { it.value }.sorted().toList()
                val actual = placeholder.findAll(translated[name] ?: continue).map { it.value }.sorted().toList()
                assertEquals(expected, actual, "${dir.name}/$name placeholders differ from English")
            }
        }
    }
    
    @Test
    fun `the per-app language list names English plus exactly the translation directories`() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resDir, "xml/locales_config.xml"))
        val nodes = document.getElementsByTagName("locale")
        val listed = (0 until nodes.length).map { nodes.item(it).attributes.getNamedItem("android:name").nodeValue }
        val expected = listOf("en") + translationDirs().map { it.name.removePrefix("values-") }
        assertEquals(expected.sorted(), listed.sorted())
    }
    
    @Test
    fun `every language with a language pack also has a UI translation`() {
        // D-478: the UI locales to maintain are exactly the languages AdaptKey can type in, so adding a Language
        // entry without a values-xx directory fails here instead of leaving that language's users on English.
        // Filipino is the one case where Android's resource qualifier (`fil`) differs from the pack code (`tl`).
        val uiCodeOverrides = mapOf("tl" to "fil")
        val present = translationDirs().map { it.name.removePrefix("values-") }.toSet()
        val missing = Language.entries
            .filter { it != Language.ENGLISH && it != Language.UNKNOWN }
            .map { uiCodeOverrides[it.code] ?: it.code }
            .filter { it !in present }
        assertTrue(missing.isEmpty(), "languages without a values-xx UI translation: $missing")
    }
}
