/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(val database: SleepDatabaseDao, application: Application) :
    AndroidViewModel(application) {

    private var tonight = MutableLiveData<SleepNight?>()
    private val nights = database.getAllNights()
    var nightString = Transformations.map(nights) {
        formatNights(nights = it, application.resources)
    }
    private val _navigateToQuality = MutableLiveData<SleepNight?>()
    val navigateToQuality: LiveData<SleepNight?>
        get() = _navigateToQuality

    private val _showSnakeBarEvent = MutableLiveData<Boolean>()
    val showSnakeBarEvent: LiveData<Boolean>
        get() = _showSnakeBarEvent

    val enableStartButton = Transformations.map(tonight) {
        null == it
    }
    val enableStopButton = Transformations.map(tonight) {
        null != it
    }
    val enableClearButton = Transformations.map(nights) {
        it.isNotEmpty()
    }

    init {
        initTonight()
    }

    private fun initTonight() {
        viewModelScope.launch {
            tonight.value = getTonightDatabase()
        }
    }

    private suspend fun getTonightDatabase(): SleepNight? {
        var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    private suspend fun insertNewNight(night: SleepNight) {
        database.insert(night)
    }

    private suspend fun updateNight(night: SleepNight) {
        database.update(night)
    }

    fun clear() {
        viewModelScope.launch {
            clearNight()
            tonight.value = null
            _showSnakeBarEvent.value = true
        }
    }

    fun onStartTracking() {
        viewModelScope.launch {
            val newNight = SleepNight()
            insertNewNight(newNight)
            tonight.value = getTonightDatabase()
        }
    }

    fun onStopTracking() {
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            updateNight(oldNight)
            _navigateToQuality.value = oldNight
        }
    }

    private suspend fun clearNight() {
        database.clear()
    }

    fun doneNavigating() {
        _navigateToQuality.value = null
    }
}
