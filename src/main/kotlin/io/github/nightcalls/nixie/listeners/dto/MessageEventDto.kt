package io.github.nightcalls.nixie.listeners.dto

import io.github.nightcalls.nixie.utils.toCommonLocalDateTime
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.LocalDate

data class MessageEventDto(
    val guildId: Long,
    val userId: Long,
    val date: LocalDate
) {
    constructor(event: MessageReceivedEvent) : this(
        event.guild.idLong,
        event.author.idLong,
        event.message.timeCreated.toCommonLocalDateTime().toLocalDate(),
    )
}