package com.sduduzog.slimlauncher.ui.home

import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager

object DrawerIme {
    const val TAG = "ForgeDrawer"

    fun applyWindow(window: Window, keyboardOnOpen: Boolean) {
        val prepared = DrawerWindowLogic.flagsAfterPrepare(window.attributes.flags)
        window.clearFlags(DrawerWindowLogic.BLOCKING_FLAGS or DrawerWindowLogic.FLAG_DIM_BEHIND)
        val attrs = window.attributes
        attrs.flags = prepared
        attrs.softInputMode = DrawerWindowLogic.softInputMode(keyboardOnOpen)
        window.attributes = attrs
        window.setSoftInputMode(DrawerWindowLogic.softInputMode(keyboardOnOpen))
        Log.i(
            TAG,
            "window flags=0x${Integer.toHexString(window.attributes.flags)} " +
                "softInput=0x${Integer.toHexString(window.attributes.softInputMode)} " +
                "keyboardOnOpen=$keyboardOnOpen"
        )
    }

    fun show(view: View) {
        if (!view.hasWindowFocus()) {
            Log.i(TAG, "ime show skipped; window not focused")
            return
        }
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.windowInsetsController?.show(WindowInsets.Type.ime())
        }
        val imm = imm(view) ?: return
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        Log.i(TAG, "ime show requested token=${view.windowToken != null}")
    }

    fun hide(view: View?) {
        if (view == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.windowInsetsController?.hide(WindowInsets.Type.ime())
        }
        imm(view)?.hideSoftInputFromWindow(view.windowToken, 0)
        Log.i(TAG, "ime hide")
    }

    private fun imm(view: View): InputMethodManager? {
        return view.context.getSystemService(InputMethodManager::class.java)
    }
}
