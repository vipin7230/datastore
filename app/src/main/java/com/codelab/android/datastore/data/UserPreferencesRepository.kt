/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codelab.android.datastore.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


private object PreferencesKeys {
    val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
    val SORT_ORDER = stringPreferencesKey("sort_order")
}

data class UserPreferences(
    val showCompleted: Boolean,
    val sortOrder: SortOrder
)

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val SORT_ORDER_KEY = "sort_order"

enum class SortOrder {
    NONE,
    BY_DEADLINE,
    BY_PRIORITY,
    BY_DEADLINE_AND_PRIORITY
}

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception: Throwable ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences: Preferences ->
            val showCompleted = preferences[PreferencesKeys.SHOW_COMPLETED] ?: false
            val sortOrder: SortOrder = SortOrder.valueOf(preferences[PreferencesKeys.SORT_ORDER]?: SortOrder.NONE.name)
            UserPreferences(showCompleted, sortOrder)
        }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[PreferencesKeys.SHOW_COMPLETED] = showCompleted
        }
    }

    suspend fun enableSortByDeadline(enable: Boolean) {

        dataStore.edit { preferences: MutablePreferences ->

            val currentOrder = SortOrder.valueOf(preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.NONE.name)

            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_PRIORITY) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_DEADLINE
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_PRIORITY
                    } else {
                        SortOrder.NONE
                    }
                }

            preferences[PreferencesKeys.SORT_ORDER] = newSortOrder.name
        }

    }

    suspend fun enableSortByPriority(enable: Boolean) {
        dataStore.edit { preferences ->
            // Get the current SortOrder as an enum
            val currentOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_DEADLINE) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_PRIORITY
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_DEADLINE
                    } else {
                        SortOrder.NONE
                    }
                }
            preferences[PreferencesKeys.SORT_ORDER] = newSortOrder.name
        }
    }


}
