package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.VoiceEventDto
import io.github.nightcalls.nixie.repository.UsersInVoicesRepository
import io.github.nightcalls.nixie.repository.VoiceTimeRepository
import io.github.nightcalls.nixie.repository.record.UserInVoiceRecord
import io.github.nightcalls.nixie.repository.record.VoiceTimeRecord
import io.github.nightcalls.nixie.service.UserIdService
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class VoiceTimeCountService(
    private val usersInVoicesRepository: UsersInVoicesRepository,
    private val voiceTimeRepository: VoiceTimeRepository,
    userIdService: UserIdService,
) : CommonCountService(userIdService) {

    @Transactional
    fun incrementOrCreateCount(eventDto: VoiceEventDto) {
        if (eventDto.isLeaving) {
            val voiceTimeInSeconds = calculateVoiceTime(eventDto) ?: return
            recordVoiceTime(eventDto, voiceTimeInSeconds)
            return
        }
        val result = usersInVoicesRepository.saveIfNotSameGuildJoining(UserInVoiceRecord(eventDto))
        logger.debug { "Запись о входе в голосовой канал, соответствующая $eventDto, создана: ${result == 1}" }
    }

    /**
     * Обработать и вывести данные о времени в войсах этого сервера в формате списка "порядковый номер + имя пользователя + суммарное время в войсе"
     */
    @Transactional
    fun showStats(event: SlashCommandInteractionEvent) {
        val messageViewsList = event.guild!!.let {
            val result = voiceTimeRepository.sumTimeForGuildIdGroupByUserId(it.idLong)
            logger.debug { "При сборе статистики времени в войсе было сформировано ${result.size} записей" }
            result
        }.map { createCountViewWithTimeFormat(it) }

        event.reply(createTableOutput(messageViewsList)).queue()
    }

    private fun calculateVoiceTime(eventDto: VoiceEventDto): Int? {
        val userInVoiceRecord = usersInVoicesRepository.findByGuildIdAndUserId(eventDto.guildId, eventDto.userId)
        if (userInVoiceRecord.isEmpty) {
            // если бот запустился, когда пользователь был в войсе, и пользователь вышел, когда бот был онлайн
            // если бот отключился, когда пользователь был в войсе (была очищена таблица с пользователями в войсах), и пользователь вышел, когда бот был снова онлайн
            logger.debug { "Для события \"Выход из голосового канала\" не найдены соответствующие данные о событии входа в голосовой канал, время данной сессии не будет учтено в статистике" }
            return null
        }
        val voiceTimeInSeconds = eventDto.time.minus(userInVoiceRecord.get().entryTime).toInt()
        usersInVoicesRepository.deleteById(userInVoiceRecord.get().id)
        return voiceTimeInSeconds
    }

    private fun recordVoiceTime(eventDto: VoiceEventDto, voiceTimeInSeconds: Int) {
        val voiceTimeRecord =
            voiceTimeRepository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
        if (voiceTimeRecord.isPresent) {
            logger.debug { "Счётчик, соответствующий $eventDto, уже есть в базе" }
            val result = voiceTimeRepository.incrementVoiceTimeById(voiceTimeRecord.get().id, voiceTimeInSeconds)
            logger.debug { "Счётчик, соответствующий $eventDto, увеличен: ${result == 1}" }
            return
        }
        logger.debug { "Счётчик, соответствующий $eventDto, отсутствует в базе" }
        val result = voiceTimeRepository.save(VoiceTimeRecord(eventDto, voiceTimeInSeconds))
        logger.debug { "Счётчик, соответствующий $eventDto, создан: $result" }
    }
}