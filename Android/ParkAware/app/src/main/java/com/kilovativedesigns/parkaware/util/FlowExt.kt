package com.kilovativedesigns.parkaware.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T> Flow<T>.collectWithLifecycle(fragment: Fragment, block: (T) -> Unit) {
    fragment.viewLifecycleOwner.lifecycleScope.launch {
        fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { block(it) }
        }
    }
}