package com.kilovativedesigns.parkaware.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object AppEvents {
    // Fire once per auth change (sign in / sign out)
    private val _authChanged = MutableSharedFlow<Unit>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val authChanged: SharedFlow<Unit> = _authChanged

    fun emitAuthChanged() { _authChanged.tryEmit(Unit) }
}