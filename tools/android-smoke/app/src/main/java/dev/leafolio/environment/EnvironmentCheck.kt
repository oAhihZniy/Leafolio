package dev.leafolio.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Compile Compose code without introducing a product UI before design approval.
@Composable
fun environmentCheck(): String = remember { "Leafolio" }
