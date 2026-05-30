package com.adoktl.util

import platform.windows.GetTickCount64

actual fun currentTimeMillis(): Long = GetTickCount64()
