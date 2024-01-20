package io.github.nightcalls.nixie.listeners.dto

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.LocalDate
import java.time.ZoneOffset

data class MessageEventDto(
    val guildId: Long, val userId: Long, val date: LocalDate
) {
    constructor(event: MessageReceivedEvent) : this(
        event.guild.idLong,
        event.author.idLong,
        event.message.timeCreated.withOffsetSameInstant(ZoneOffset.of("+03:00")).toLocalDate(),
    )
}