package io.github.nightcalls.nixie.listeners.dto

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import java.time.LocalDate
import java.time.LocalDateTime

data class VoiceEventDto(
    val guildId: Long,
    val userId: Long,
    val date: LocalDate,
    val time: LocalDateTime,
    val isLeaving: Boolean
) {
    constructor(event: GuildVoiceUpdateEvent, time: LocalDateTime) : this(
        event.guild.idLong,
        event.entity.user.idLong,
        time.toLocalDate(),
        time,
        event.channelJoined == null
    )

    constructor(eventDto: VoiceEventDto, date: LocalDate) : this(
        eventDto.guildId,
        eventDto.userId,
        date,
        eventDto.time,
        eventDto.isLeaving
    )
}