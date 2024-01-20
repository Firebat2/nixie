package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.service.SlashCommandService
import io.github.nightcalls.nixie.service.count.MessageCommonCountService
import io.github.nightcalls.nixie.service.count.VoiceTimeCommonCountService
import io.github.nightcalls.nixie.utils.getLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class SlashCommandEventListener(
    private val guild: Guild,
    private val slashCommandService: SlashCommandService,
    private val messageCountService: MessageCommonCountService,
    private val voiceTimeCountService: VoiceTimeCommonCountService
) : ListenerAdapter() {
    private val logger = getLogger()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.guild != guild) {
            return
        }

        logger.info("Получен ${event.javaClass.simpleName}: ${event.name}")
        when (event.name) {
            "nixie" -> slashCommandService.showInfo(event)
            "stats-messages" -> messageCountService.showStats(event)
            "stats-voices" -> voiceTimeCountService.showStats(event)
        }
    }
}