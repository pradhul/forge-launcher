package com.sduduzog.slimlauncher.ui.home

import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager

object DrawerIme {
    const val TAG = "ForgeDrawer"

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
