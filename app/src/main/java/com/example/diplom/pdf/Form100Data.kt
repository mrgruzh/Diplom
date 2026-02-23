package com.example.diplom.pdf

data class Form100Data(
    val status: String,
    val filledAt: String,
    val fullName: String,
    val doctorFio: String,
    val callsign: String,
    val tagNumber: String,
    val eventAt: String,
    val injuryKind: String,
    val diagnosis: String,
    val localization: String,
    val evacMethod: String,
    val fromEvacPoint: String?
)
