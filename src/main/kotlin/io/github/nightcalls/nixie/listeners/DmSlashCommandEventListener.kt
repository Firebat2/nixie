package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.listeners.dto.SlashCommandEventDto
import io.github.nightcalls.nixie.service.SlashCommandService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

private val logger = KotlinLogging.logger {}

class DmSlashCommandEventListener(
    private val slashCommandService: SlashCommandService,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.guild != null) {
            return
        }
        event.deferReply(true).queue()
        val eventDto = SlashCommandEventDto(event)
        logger.info { "Получен SlashCommandInteractionEvent из личного сообщения: $eventDto" }
        when (eventDto.command) {
            "nixie" -> slashCommandService.showInfo(eventDto)
        }
    }
}