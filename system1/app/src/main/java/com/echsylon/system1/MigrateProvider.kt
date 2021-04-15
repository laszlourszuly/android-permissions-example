package com.echsylon.system1

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.content.edit

/**
 * This is a quick-n-dirty implementation of a content provider backed by a file
 * based key/value store (Android SharedPreferences). Refer to below link for a
 * more comprehensive document on a real ContentProvider implementation:
 *
 *   https://developer.android.com/guide/topics/providers/content-provider-creating
 */
class MigrateProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.echsylon.system1"
        const val MESSAGE = "message"
    }

    private val repository by lazy { context!!.getSharedPreferences("migrate_package", Context.MODE_PRIVATE) }
    private val uriMatcher by lazy { UriMatcher(UriMatcher.NO_MATCH).apply { addURI(AUTHORITY, MESSAGE, 1) } }

    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            1 -> "vnd.android.cursor.item/vnd.$AUTHORITY"
            else -> throw IllegalArgumentException("Unexpected uri: $uri")
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        return when (uriMatcher.match(uri)) {
            1 -> MatrixCursor(arrayOf(MESSAGE), 1).apply { addRow(arrayOf(repository.getString(MESSAGE, ""))) }
            else -> throw IllegalArgumentException("Unexpected uri: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        return when (uriMatcher.match(uri)) {
            1 -> repository.edit { putString(MESSAGE, values?.get(MESSAGE) as? String) }.let { uri }
            else -> throw IllegalArgumentException("Unexpected uri: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            1 -> repository.edit { remove(MESSAGE) }.let { 1 }
            else -> throw IllegalArgumentException("Unexpected uri: $uri")
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            1 -> repository.edit { putString(MESSAGE, values?.get(MESSAGE) as? String) }.let { 1 }
            else -> throw IllegalArgumentException("Unexpected uri: $uri")
        }
    }
}