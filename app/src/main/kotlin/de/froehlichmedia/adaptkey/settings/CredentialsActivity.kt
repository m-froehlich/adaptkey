// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.credential.CredentialEntry
import de.froehlichmedia.adaptkey.credential.CredentialStore

/**
 * D-180: lists every saved username/email (D-142's own [CredentialStore]) so each can be reviewed and
 * removed individually, or all at once - replacing the former top-level "delete everything" settings
 * action, which offered no way to keep some entries while discarding others. Structurally close to
 * [BlacklistActivity] / [LearnedWordsActivity] (list, remove-with-confirmation), but removal is triggered
 * by a long press rather than a plain tap, since a saved credential is more sensitive than an ordinary
 * word and a stray tap should not risk deleting one. The remove dialog also offers a neutral "copy to
 * clipboard" button, for pulling a saved value back out somewhere else.
 */
class CredentialsActivity : AppCompatActivity() {
    
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var deleteAllButton: Button
    private lateinit var adapter: ArrayAdapter<String>
    private val entries = ArrayList<CredentialEntry>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credentials)
        title = getString(R.string.d142_credentials_title)
        
        listView = findViewById(R.id.credentials_list)
        emptyView = findViewById(R.id.credentials_empty)
        deleteAllButton = findViewById(R.id.credentials_delete_all)
        
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        listView.adapter = adapter
        
        deleteAllButton.setOnClickListener { confirmClearAll() }
        
        listView.setOnItemLongClickListener { _, _, position, _ ->
            confirmRemove(entries[position])
            true
        }
        
        refresh()
    }
    
    private fun confirmRemove(entry: CredentialEntry) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.credentials_remove_confirm_title, entry.value))
            .setPositiveButton(R.string.credentials_remove_confirm_action) { _, _ ->
                CredentialStore.forget(this, entry.value)
                Toast.makeText(this, getString(R.string.credentials_removed, entry.value), Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.copy_to_clipboard_action) { _, _ -> copyToClipboard(entry.value) }
            .show()
    }
    
    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.d142_clear_credentials_title)
            .setMessage(R.string.d142_clear_credentials_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                CredentialStore.clear(this)
                Toast.makeText(this, R.string.d142_clear_credentials_done, Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.d142_credentials_title), value))
        Toast.makeText(this, getString(R.string.copy_to_clipboard_done, value), Toast.LENGTH_SHORT).show()
    }
    
    private fun refresh() {
        entries.clear()
        entries.addAll(CredentialStore.all(this))
        adapter.clear()
        adapter.addAll(entries.map { entry -> "${entry.value}  (${entry.kind.name}, ${entry.frequency})" })
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (entries.isEmpty()) TextView.VISIBLE else TextView.GONE
        deleteAllButton.isEnabled = entries.isNotEmpty()
    }
}
