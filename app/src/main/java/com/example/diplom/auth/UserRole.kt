package com.example.diplom.auth

enum class UserRole(val titleRu: String) {
    FELDSHER("Фельдшер"),
    EVAC_DOCTOR("Врач эвакопункта"),
    HOSPITAL_DOCTOR("Врач госпиталя"),
    FUND("Фонд");

    companion object {
        fun fromName(name: String?): UserRole? =
            entries.firstOrNull { it.name == name }
    }
}
