package io.github.nightcalls.nixie.listeners.dto

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import java.time.LocalDate
import java.time.OffsetDateTime

data class VoiceEventDto(
    val guildId: Long,
    val userId: Long,
    val date: LocalDate,
    val time: Long,
    val isLeaving: Boolean
) {
    constructor(event: GuildVoiceUpdateEvent, time: OffsetDateTime) : this(
        event.guild.idLong,
        event.entity.user.idLong,
        time.toLocalDate(),
        time.toEpochSecond(),
        event.channelJoined == null
    )
}