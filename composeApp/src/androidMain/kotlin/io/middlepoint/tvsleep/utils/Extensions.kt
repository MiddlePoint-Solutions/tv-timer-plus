/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.middlepoint.tvsleep.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.util.fastForEachIndexed
import io.middlepoint.tvsleep.MainActivity
import io.middlepoint.tvsleep.fillWithZeros
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

fun String.toMillis(): Long {
    var timeInMillis = 0L
    this.fillWithZeros().chunked(2).fastForEachIndexed { i, s ->
        when (i) {
            0 -> timeInMillis += TimeUnit.HOURS.toMillis(s.toLong())
            1 -> timeInMillis += TimeUnit.MINUTES.toMillis(s.toLong())
            2 -> timeInMillis += TimeUnit.SECONDS.toMillis(s.toLong())
        }
    }
    return timeInMillis
}

fun Float.roundUp(): Long = this.toBigDecimal().setScale(0, BigDecimal.ROUND_UP).longValueExact()

fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

fun Context.getLaunchIntent() = packageManager.getLaunchIntentForPackage("com.ericktijerou.jettimer")

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.getOpenTimerTabIntent(): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, MainActivity::class.java)
    return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}
