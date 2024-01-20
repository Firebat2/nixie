package io.github.nightcalls.nixie.service

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service

@Service
class SlashCommandService {
    /**
     * Вывод инструкции по использованию бота
     */
    fun showInfo(event: SlashCommandInteractionEvent) {
        val info = StringBuilder()
        info
            .append("/stats-messages — статистика по сообщениям всех пользователей за последние 30 дней")
            .append("\n/stats-voices — статистика по времени в войсе всех пользователей за последние 30 дней")
            .append("\n\nОбратную связь можно оставить по адресу nixiethebat@gmail.com")

        event.reply(info.toString()).queue()
    }
}