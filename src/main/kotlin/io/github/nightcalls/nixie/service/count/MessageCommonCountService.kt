package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.repository.MessageCountRepository
import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.utils.getLogger
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service

@Service
class MessageCommonCountService(
    private val messageCountRepository: MessageCountRepository,
    userIdService: UserIdService
) : CommonCountService(userIdService) {
    private val logger = getLogger()

    /**
     * Обработать и вывести данные счётчиков сообщений этого сервера в формате списка "порядковый номер + имя пользователя + суммарное кол-во сообщений"
     */
    fun showStats(event: SlashCommandInteractionEvent) {
        val messageViewsList = event.guild!!.let {
            val result = messageCountRepository.sumCountsForGuildIdGroupByUserId(it.idLong)
            logger.debug("При сборе статистики сообщений было сформировано ${result.size} записей")
            result
        }.map { createCountView(it) }

        event.reply(createTableOutput(messageViewsList)).queue()
    }
}