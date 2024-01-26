package io.github.nightcalls.nixie.service

import io.github.nightcalls.nixie.utils.getCommonEmbedBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SlashCommandService {
    /**
     * Вывод инструкции по использованию бота
     */
    fun showInfo(event: SlashCommandInteractionEvent) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setTitle("Инфо")
        embedBuilder.addField(
            "/stats-messages", "Статистика по количеству отправленных сообщений", false
        )
        embedBuilder.addField(
            "/stats-voices", "Статистика по проведенному в голосовых каналах времени", false
        )
        embedBuilder.addField(
            "nixiethebat@gmail.com", "Адрес для обратной связи", false
        )

        event.member?.asMention?.let { event.reply(it).addEmbeds(embedBuilder.build()).queue() }
        logger.info { "Отправлена инструкция по использованию бота на сервер ${event.guild?.name}" }
    }

    /**
     * Вывод предупреждения о задержке
     */
    fun coolDownReply(event: SlashCommandInteractionEvent) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.addField(
            "Предупреждение", "Команды вывода статистики можно вызывать не чаще, чем раз в 10 секунд", false
        )

        event.member?.asMention?.let { event.reply(it).addEmbeds(embedBuilder.build()).setEphemeral(true).queue() }
        logger.info { "Отправлено предупреждение о задержке на сервер ${event.guild?.name}" }
    }
}