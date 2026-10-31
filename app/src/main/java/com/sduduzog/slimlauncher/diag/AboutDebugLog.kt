package com.sduduzog.slimlauncher.diag

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.sduduzog.slimlauncher.BuildConfig
import com.sduduzog.slimlauncher.R

object AboutDebugLog {
    fun show(fragment: Fragment) {
        val context = fragment.requireContext()
        AlertDialog.Builder(context)
            .setTitle(R.string.options_fragment_about_slim)
            .setMessage(
                context.getString(
                    R.string.about_forge_message,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
            )
            .setPositiveButton(R.string.about_copy_debug_log) { _, _ ->
                DebugLogStore.copyAndShare(context)
                Toast.makeText(context, R.string.about_debug_log_copied, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
