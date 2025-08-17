package com.example.letsgetweddi.utils

import android.app.Activity
import android.view.View

object ViewGate {
    fun hideIfExists(activity: Activity, idName: String) {
        val id = activity.resources.getIdentifier(idName, "id", activity.packageName)
        if (id != 0) activity.findViewById<View>(id)?.visibility = View.GONE
    }

    fun setEnabledIfExists(activity: Activity, idName: String, enabled: Boolean) {
        val id = activity.resources.getIdentifier(idName, "id", activity.packageName)
        if (id != 0) activity.findViewById<View>(id)?.isEnabled = enabled
    }
}
