package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.repository.VoiceTimeRepository
import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.utils.getLogger
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service

@Service
class VoiceTimeCommonCountService(
    private val voiceTimeRepository: VoiceTimeRepository,
    userIdService: UserIdService,
) : CommonCountService(userIdService) {
    private val logger = getLogger()

    /**
     * Обработать и вывести данные о времени в войсах этого сервера в формате списка "порядковый номер + имя пользователя + суммарное время в войсе"
     */
    fun showStats(event: SlashCommandInteractionEvent) {
        val messageViewsList = event.guild!!.let {
            val result = voiceTimeRepository.sumTimeForGuildIdGroupByUserId(it.idLong)
            logger.debug("При сборе статистики времени в войсе было сформировано ${result.size} записей")
            result
        }.map { createCountViewWithTimeFormat(it) }

        event.reply(createTableOutput(messageViewsList)).queue()
    }
}