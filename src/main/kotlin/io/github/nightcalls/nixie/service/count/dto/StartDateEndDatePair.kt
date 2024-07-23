package io.github.nightcalls.nixie.service.count.dto

import java.time.LocalDate

data class StartDateEndDatePair(
    val startDate: LocalDate,
    val endDate: LocalDate
)