package com.example.diplom.access

import android.content.Context

object EvacHospitalBindingStorage {
    private const val PREFS = "org_bindings"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(evacPoint: String): String = "evac_to_hospital:${evacPoint.trim()}"

    fun setHospitalForEvacPoint(ctx: Context, evacPoint: String, hospital: String) {
        val e = evacPoint.trim()
        val h = hospital.trim()
        if (e.isBlank() || h.isBlank()) return
        prefs(ctx).edit().putString(key(e), h).apply()
    }

    fun hospitalForEvacPoint(ctx: Context, evacPoint: String): String? {
        val e = evacPoint.trim()
        if (e.isBlank()) return null
        return prefs(ctx).getString(key(e), null)
    }

    fun allBindings(ctx: Context): Map<String, String> {
        val all = prefs(ctx).all
        val out = linkedMapOf<String, String>()
        for ((k, v) in all) {
            if (!k.startsWith("evac_to_hospital:")) continue
            val evacPoint = k.removePrefix("evac_to_hospital:").trim()
            val hospital = (v as? String)?.trim().orEmpty()
            if (evacPoint.isNotBlank() && hospital.isNotBlank()) {
                out[evacPoint] = hospital
            }
        }
        return out
    }
}
