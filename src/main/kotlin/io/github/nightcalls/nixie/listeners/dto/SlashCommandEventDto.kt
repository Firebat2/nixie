package io.github.nightcalls.nixie.listeners.dto

import io.github.nightcalls.nixie.utils.END_DATE_OPTION
import io.github.nightcalls.nixie.utils.NAME_OPTION
import io.github.nightcalls.nixie.utils.START_DATE_OPTION
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

data class SlashCommandEventDto(
    val command: String,
    val initiator: String,
    val name: String?,
    val startDate: String?,
    val endDate: String?,
    val hook: InteractionHook,
    val guild: Guild?,
    val member: Member?
) {
    constructor(event: SlashCommandInteractionEvent) : this(
        event.name,
        event.user.name,
        event.getOption(NAME_OPTION)?.asString,
        event.getOption(START_DATE_OPTION)?.asString,
        event.getOption(END_DATE_OPTION)?.asString,
        event.hook,
        event.guild,
        event.member
    )

    override fun toString(): String {
        return "SlashCommandEventDto(command=$command, initiator=$initiator, name=$name, startDate=$startDate, endDate=$endDate)"
    }
}