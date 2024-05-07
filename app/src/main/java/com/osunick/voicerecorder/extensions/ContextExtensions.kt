package com.osunick.voicerecorder.extensions

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

fun Context.toast(text: CharSequence) =
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Context.toast(@StringRes stringResId: Int) =
    Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show()