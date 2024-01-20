package io.github.nightcalls.nixie.listeners

import io.github.nightcalls.nixie.listeners.dto.VoiceEventDto
import io.github.nightcalls.nixie.repository.UsersInVoicesRepository
import io.github.nightcalls.nixie.repository.VoiceTimeRepository
import io.github.nightcalls.nixie.repository.record.UserInVoiceRecord
import io.github.nightcalls.nixie.repository.record.VoiceTimeRecord
import io.github.nightcalls.nixie.utils.getLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.time.OffsetDateTime
import java.time.ZoneOffset

class VoiceEventListener(
    private val guild: Guild,
    private val voiceTimeRepository: VoiceTimeRepository,
    private val usersInVoicesRepository: UsersInVoicesRepository
) : ListenerAdapter() {
    private val logger = getLogger()

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (event.guild != guild) {
            return
        }

        val eventTime = OffsetDateTime.now()
            .withOffsetSameInstant(ZoneOffset.of("+03:00")) // придется считать это время временем входа/выхода из войса
        val eventDto = VoiceEventDto(event, eventTime)
        logger.info("Получен ${event.javaClass.simpleName}: $eventDto")
        incrementOrCreateCount(eventDto)
    }

    private fun incrementOrCreateCount(eventDto: VoiceEventDto) {
        if (eventDto.isLeaving) {
            val userInVoiceRecord = usersInVoicesRepository.findByGuildIdAndUserId(eventDto.guildId, eventDto.userId)
            if (userInVoiceRecord.isEmpty) {
                // если бот запустился, когда пользователь был в войсе, и пользователь вышел, когда бот был онлайн
                // если бот отключился, когда пользователь был в войсе (была очищена таблица с пользователями в войсах), и пользователь вышел, когда бот был снова онлайн
                logger.debug("Для события \"Выход из голосового канала\" не найдены соответствующие данные о событии входа в голосовой канал, время данной сессии не будет учтено в статистике")
                return
            }
            val voiceTimeInSeconds = eventDto.time.minus(userInVoiceRecord.get().time).toInt()
            usersInVoicesRepository.deleteById(userInVoiceRecord.get().id)

            val voiceTimeRecord =
                voiceTimeRepository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
            if (voiceTimeRecord.isPresent) {
                logger.debug("Счётчик, соответствующий $eventDto, уже есть в базе")
                val result = voiceTimeRepository.incrementVoiceTimeById(voiceTimeRecord.get().id, voiceTimeInSeconds)
                logger.debug("Счётчик, соответствующий $eventDto, увеличен: ${result == 1}")
            } else {
                logger.debug("Счётчик, соответствующий $eventDto, отсутствует в базе")
                val result = voiceTimeRepository.save(VoiceTimeRecord(eventDto, voiceTimeInSeconds))
                logger.debug("Счётчик, соответствующий $eventDto, создан: $result")
            }
        } else {
            val result = usersInVoicesRepository.saveIfNotSameGuildJoining(UserInVoiceRecord(eventDto))
            logger.debug("Запись о входе в голосовой канал, соответствующая $eventDto, создана: ${result == 1}")
        }
    }
}