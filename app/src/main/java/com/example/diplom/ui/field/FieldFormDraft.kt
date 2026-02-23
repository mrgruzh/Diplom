package com.example.diplom.ui.field

data class MedicineItemDraft(
    val name: String,
    val qty: String
)

data class FieldFormDraft(
    val status: FieldStatus,
    val filledAt: String = "",
    val fullName: String = "",
    val callsign: String = "",
    val tagNumber: String = "",
    val eventAt: String = "",
    val injuryKind: String = "",
    val diagnosis: String = "",
    val localizationSelected: Set<String> = emptySet(),
    val localizationOther: String = "",
    val evacMethod: String = "",
    val medicines: List<MedicineItemDraft> = emptyList()
)
