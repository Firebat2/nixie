package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.listeners.dto.SlashCommandEventDto
import io.github.nightcalls.nixie.service.SlashCommandService
import io.github.nightcalls.nixie.service.count.MessageCountService
import io.github.nightcalls.nixie.service.count.VoiceTimeCountService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

private val logger = KotlinLogging.logger {}

class SlashCommandEventListener(
    private val guild: Guild,
    private val slashCommandService: SlashCommandService,
    private val messageCountService: MessageCountService,
    private val voiceTimeCountService: VoiceTimeCountService
) : ListenerAdapter() {
    private var lastCommandTime: Instant = Clock.System.now().minus(10, DateTimeUnit.SECOND)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.guild != guild) {
            return
        }
        event.deferReply(true).queue()
        val eventDto = SlashCommandEventDto(event)
        logger.info { "Получен SlashCommandInteractionEvent: $eventDto" }
        when (eventDto.command) {
            "nixie" -> slashCommandService.showInfo(eventDto)
            "stats-messages" -> checkCoolDown(eventDto) { messageCountService.showStats(eventDto) }
            "stats-voices" -> checkCoolDown(eventDto) { voiceTimeCountService.showStats(eventDto) }
        }
    }

    private fun checkCoolDown(
        eventDto: SlashCommandEventDto,
        function: (eventDto: SlashCommandEventDto) -> Unit
    ) {
        if ((Clock.System.now() - lastCommandTime).inWholeSeconds < 10) {
            return slashCommandService.coolDownReply(eventDto)
        }
        lastCommandTime = Clock.System.now()
        return function(eventDto)
    }
}