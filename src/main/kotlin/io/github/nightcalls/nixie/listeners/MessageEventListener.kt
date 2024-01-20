package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import io.github.nightcalls.nixie.repository.MessageCountRepository
import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.utils.getLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MessageEventListener(
    private val guild: Guild,
    private val repository: MessageCountRepository
) : ListenerAdapter() {
    private val logger = getLogger()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.guild != guild) {
            return
        }

        val eventDto = MessageEventDto(event)
        logger.info("Получен ${event.javaClass.simpleName}: $eventDto")
        incrementOrCreateCount(eventDto)
    }

    private fun incrementOrCreateCount(eventDto: MessageEventDto) {
        val record = repository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
        if (record.isPresent) {
            logger.debug("Счётчик, соответствующий $eventDto, уже есть в базе")
            val result = repository.incrementMessageCountById(record.get().id)
            logger.debug("Счётчик, соответствующий $eventDto, увеличен: ${result == 1}")
        } else {
            logger.debug("Счётчик, соответствующий $eventDto, отсутствует в базе")
            val result = repository.save(MessageCountRecord(eventDto))
            logger.debug("Счётчик, соответствующий $eventDto, создан: $result")
        }
    }
}