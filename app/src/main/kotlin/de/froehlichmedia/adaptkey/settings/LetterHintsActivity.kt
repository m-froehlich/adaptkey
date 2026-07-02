// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R

/**
 * Per-key secondary-symbol editor (C-08 / L-05): lists every letter {@code a}-{@code z} with its
 * current long-press symbol and lets the user pick a symbol from the curated [LetterHints.PALETTE] or
 * remove it - no free-text entry. Changes are persisted via [SettingsStore.saveLetterHints], which the
 * running keyboard observes, so they take effect (corner hint + long-press) on return.
 *
 * Backed directly by the shared preferences, so - like the other Android-facing settings layers - it
 * is covered by instrumented rather than unit tests; the encode/decode/validation logic it relies on
 * lives in the unit-tested [LetterHints].
 */
class LetterHintsActivity : AppCompatActivity() {
    
    private val letters = ('a'..'z').toList()
    private val hints = HashMap<Char, String>()
    private lateinit var adapter: ArrayAdapter<String>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_letter_hints)
        title = getString(R.string.c08_editor_title)
        
        hints.putAll(SettingsStore.loadLetterHints(this))
        
        val listView = findViewById<ListView>(R.id.letter_hints_list)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> pickSymbol(letters[position]) }
        
        findViewById<Button>(R.id.letter_hints_reset).setOnClickListener { resetToDefault() }
        
        refresh()
    }
    
    private fun pickSymbol(letter: Char) {
        val none = getString(R.string.c08_editor_none)
        val items = (listOf(none) + LetterHints.PALETTE).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(letter.uppercaseChar().toString())
            .setItems(items) { _, which ->
                if (which == 0) {
                    hints.remove(letter)
                } else {
                    hints[letter] = LetterHints.PALETTE[which - 1]
                }
                persistAndRefresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun resetToDefault() {
        SettingsStore.resetLetterHints(this)
        hints.clear()
        hints.putAll(SettingsStore.loadLetterHints(this))
        refresh()
        Toast.makeText(this, R.string.c08_reset_done, Toast.LENGTH_SHORT).show()
    }
    
    private fun persistAndRefresh() {
        SettingsStore.saveLetterHints(this, hints)
        refresh()
    }
    
    private fun refresh() {
        val none = getString(R.string.c08_editor_none)
        adapter.clear()
        adapter.addAll(letters.map { letter -> "${letter.uppercaseChar()}      ${hints[letter] ?: none}" })
        adapter.notifyDataSetChanged()
    }
}
