package com.adoktl.util

import platform.posix.clock_gettime
import platform.posix.CLOCK_MONOTONIC
import platform.posix.timespec

actual fun currentTimeMillis(): Long {
    val ts = timespec()
    clock_gettime(CLOCK_MONOTONIC, ts)
    return ts.tv_sec * 1000L + ts.tv_nsec / 1_000_000L
}
