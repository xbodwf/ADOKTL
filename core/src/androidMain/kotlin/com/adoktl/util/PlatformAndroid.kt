package com.adoktl.util

actual fun currentTimeMillis(): Long = (System.nanoTime() / 1_000_000)
