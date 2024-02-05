package io.github.nightcalls.nixie.service

import io.github.nightcalls.nixie.listeners.dto.SlashCommandEventDto
import io.github.nightcalls.nixie.utils.getCommonEmbedBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SlashCommandService {
    /**
     * Вывести информационное сообщение
     */
    fun showInfo(eventDto: SlashCommandEventDto) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setTitle("Инфо")
        embedBuilder.addField(
            "/stats-messages", "Статистика по количеству отправленных сообщений", false
        )
        embedBuilder.addField(
            "/stats-voices", "Статистика по проведенному в голосовых каналах времени", false
        )
        embedBuilder.addField(
            "Параметры команд",
            StringBuilder()
                .append("При заполнении параметра \"name\" будет сформирована статистика по конкретному пользователю\n")
                .append("При заполнении параметров \"start-date\" и \"end-date\" будет сформирована статистика за конкретный период времени\n")
                .append("Формат даты: \"2024-01-31\", оба пограничных дня включаются в период\n")
                .toString(),
            false
        )
        embedBuilder.addField(
            "nixiethebat@gmail.com", "Адрес для обратной связи", false
        )

        val mention = eventDto.member?.asMention
        if (mention != null) {
            mention.let { eventDto.hook.sendMessage(it).addEmbeds(embedBuilder.build()).setEphemeral(true).queue() }
            logger.info { "Отправлена инструкция по использованию бота на сервер ${eventDto.guild?.name}" }
        } else {
            eventDto.hook.sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue()
            logger.info { "Отправлена инструкция по использованию бота в личные сообщения ${eventDto.initiator}" }
        }
    }

    /**
     * Вывести предупреждение о задержке
     */
    fun coolDownReply(eventDto: SlashCommandEventDto) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.addField(
            "Предупреждение", "Команды вывода статистики можно вызывать не чаще, чем раз в 10 секунд", false
        )
        eventDto.member?.asMention?.let {
            eventDto.hook.sendMessage(it).addEmbeds(embedBuilder.build()).setEphemeral(true).queue()
        }
        logger.info { "Отправлено предупреждение о задержке на сервер ${eventDto.guild?.name}" }
    }
}