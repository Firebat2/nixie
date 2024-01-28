package io.github.nightcalls.nixie.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun OffsetDateTime.toCommonLocalDateTime(): LocalDateTime {
    return this.withOffsetSameInstant(ZoneOffset.of("+03:00")).toLocalDateTime()
}