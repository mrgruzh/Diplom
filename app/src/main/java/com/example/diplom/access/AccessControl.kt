package com.example.diplom.access

import com.example.diplom.auth.UserProfile
import com.example.diplom.auth.UserRole

object AccessControl {
    fun canSeeSummary(role: UserRole): Boolean =
        role == UserRole.EVAC_DOCTOR || role == UserRole.HOSPITAL_DOCTOR

    fun summaryButtonTitle(role: UserRole): String =
        when (role) {
            UserRole.EVAC_DOCTOR -> "Сводка по эвакопункту"
            UserRole.HOSPITAL_DOCTOR -> "Сводка по госпиталю"
            UserRole.FELDSHER -> ""
            UserRole.FUND -> ""
        }

    fun locationName(profile: UserProfile): String? =
        when (profile.role) {
            UserRole.FELDSHER -> profile.evacPoint
            UserRole.EVAC_DOCTOR -> profile.evacPoint
            UserRole.HOSPITAL_DOCTOR -> profile.hospital
            UserRole.FUND -> null
        }

    fun inboxKind(role: UserRole): InboxKind? =
        when (role) {
            UserRole.EVAC_DOCTOR -> InboxKind.EVAC_POINT
            UserRole.HOSPITAL_DOCTOR -> InboxKind.HOSPITAL
            UserRole.FELDSHER -> null
            UserRole.FUND -> null
        }
}

enum class InboxKind {
    EVAC_POINT,
    HOSPITAL
}
