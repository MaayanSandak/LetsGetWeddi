package com.example.letsgetweddi.model

import androidx.annotation.Keep
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ChecklistItem(
    var id: String? = null,
    var task: String? = null,
    var isDone: Boolean = false
) {
    companion object {
        /**
         * Robust mapper: supports both "isDone" and "done" (and common variants).
         */
        fun fromSnapshot(ds: DataSnapshot): ChecklistItem {
            val id = ds.key

            fun getString(vararg keys: String): String? {
                for (k in keys) {
                    val v = ds.child(k).getValue(String::class.java)?.trim()
                    if (!v.isNullOrEmpty()) return v
                }
                return null
            }

            fun getBool(vararg keys: String): Boolean {
                for (k in keys) {
                    val n = ds.child(k)
                    // Boolean direct
                    n.getValue(Boolean::class.java)?.let { return it }
                    // "true"/"false"
                    n.getValue(String::class.java)?.lowercase()?.let {
                        if (it == "true") return true
                        if (it == "false") return false
                    }
                    // 1/0 as number or string
                    n.getValue(Int::class.java)?.let { return it != 0 }
                }
                return false
            }

            val task = getString("task", "title", "text", "name")
            val done = getBool("isDone", "done", "checked", "complete")

            return ChecklistItem(
                id = id,
                task = task,
                isDone = done
            )
        }
    }
}
