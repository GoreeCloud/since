package com.goreecloud.since.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Temporary Development theme substrate.
 *
 * This does not claim GLAZE UI consumer conformance. The approved GLAZE UI Android mapping will
 * replace or wrap this boundary once the applicable consumable contract is verified for Since.
 */
@Composable
fun SinceTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(content = content)
}
