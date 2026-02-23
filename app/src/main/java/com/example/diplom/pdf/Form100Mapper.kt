package com.example.diplom.pdf

import org.json.JSONArray
import org.json.JSONObject

object Form100Mapper {
    fun fromRawText(status: String, rawText: String?, doctorFio: String? = null): Form100Data {
        val o = rawText?.let { safeJson(it) } ?: JSONObject()

        val filledAt = o.optString("filledAt", "")
        val fullName = o.optString("fullName", "")
        val doctor = doctorFio?.trim().takeIf { !it.isNullOrBlank() } ?: o.optString("doctorFio", "")
        val callsign = o.optString("callsign", "")
        val tagNumber = o.optString("tagNumber", "")
        val evacMethod = o.optString("evacMethod", "")

        val eventAt = when (status) {
            "POGIB" -> o.optString("deathAt", "")
            else -> o.optString("injuryAt", "")
        }

        val injuryKind = o.optString("injuryKind", "")
        val diagnosis = o.optString("diagnosis", "")

        val localization = buildString {
            val arr = o.optJSONArray("localization") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(v)
                }
            }
            val other = o.optString("localizationOther", "")
            if (other.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append(other)
            }
        }

        val fromEvacPoint = o.optString("fromEvacPoint", null)

        return Form100Data(
            status = status,
            filledAt = filledAt,
            fullName = fullName,
            doctorFio = doctor,
            callsign = callsign,
            tagNumber = tagNumber,
            eventAt = eventAt,
            injuryKind = injuryKind,
            diagnosis = diagnosis,
            localization = localization,
            evacMethod = evacMethod,
            fromEvacPoint = fromEvacPoint
        )
    }

    private fun safeJson(value: String): JSONObject {
        return try {
            JSONObject(value)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
