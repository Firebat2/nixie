package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import io.github.nightcalls.nixie.repository.MessageCountRepository
import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.service.UserIdService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class MessageCountService(
    private val repository: MessageCountRepository,
    userIdService: UserIdService
) : CommonCountService(userIdService) {

    @Transactional
    fun incrementOrCreateCount(eventDto: MessageEventDto) {
        val record = repository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
        if (record.isPresent) {
            logger.debug { "Счётчик, соответствующий $eventDto, уже есть в базе" }
            val result = repository.incrementMessageCountById(record.get().id)
            logger.debug { "Счётчик, соответствующий $eventDto, увеличен: ${result == 1}" }
            return
        }
        logger.debug { "Счётчик, соответствующий $eventDto, отсутствует в базе" }
        val result = repository.save(MessageCountRecord(eventDto))
        logger.debug { "Счётчик, соответствующий $eventDto, создан: $result" }
    }

    /**
     * Обработать и вывести данные счётчиков сообщений этого сервера в формате списка "порядковый номер + имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    fun showStats(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue()

        val messageViewsList = event.guild!!.let {
            val result = repository.sumCountsForGuildIdGroupByUserId(it.idLong)
            logger.debug { "При сборе статистики сообщений было сформировано ${result.size} записей" }
            result
        }.map { createCountView(it) }

        sendTitleEmbed(event, "Сформирована статистика по количеству отправленных сообщений")
        createTableOutputsAndMultipleReplies(event, messageViewsList)
        logger.info { "Отправлена статистика сообщений на сервер ${event.guild?.name}" }
    }
}