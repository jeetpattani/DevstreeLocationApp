package com.example.devs.utils

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

inline fun <T> Activity.startNewActivity(
    className: Class<T>,
    isFinish: Boolean = false,
    isClearAllStacks: Boolean = false,
    bundle: Bundle? = null,
    transitionEffectBundle: Bundle? = null
) {
    val intent = Intent(this, className)
    if (isClearAllStacks) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    bundle?.let {
        intent.putExtras(it)
    }
    startActivity(intent, transitionEffectBundle)
    if (isFinish) {
        finish()
    }
}

/**
 * Start an activity with a launcher, allowing for a result callback.
 *
 * @param context The calling activity.
 * @param className The target activity's class.
 * @param bundle Optional data bundle to pass to the new activity.
 * @param onResult Callback to handle the activity result.
 */
inline fun <T> ActivityLauncher<Intent, ActivityResult>.startActivityWithLauncher(
    context: Activity,
    className: Class<T>,
    bundle: Bundle? = null,
    isLaunchedFromHistory: Boolean = false,
    crossinline onResult: (ActivityResult) -> Unit
) {
    val intent = Intent(context, className).apply {
        bundle?.let {
            putExtras(it)
        }
    }
    if (isLaunchedFromHistory) {
        intent.flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
    }
    launch(intent) {
        onResult(ActivityResult(it.resultCode, it.data))
    }
}

/**
 * Finish the activity with a launcher result, optionally passing a bundle.
 *
 * @param bundle Optional data bundle to pass as the result.
 */
inline fun Activity.finishActivityWithLauncherResult(
    /*resultCode: Int = RESULT_OK,*/ // If there is any scenario need to change result code
    bundle: Bundle? = null,
) {
    Intent().apply {
        bundle?.let {
            putExtras(it)
        }
        setResult(AppCompatActivity.RESULT_OK, this)
        finish()
    }
}

fun Context.background(drawable: Int): Drawable? {
    return ContextCompat.getDrawable(this, drawable)
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

inline fun <T : Any, R> T?.withNotNull(block: (T) -> R): R? {
    return this?.let(block)
}

fun formatToOneDecimalPlaces(value: Any): String {
    val doubleValue = when (value) {
        is Int -> value.toDouble()
        is Float -> value.toDouble()
        is Double -> value
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> ""
    }
    return String.format("%.2f", doubleValue)
}