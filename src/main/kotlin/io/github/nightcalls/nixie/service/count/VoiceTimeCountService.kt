package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.SlashCommandEventDto
import io.github.nightcalls.nixie.listeners.dto.VoiceEventDto
import io.github.nightcalls.nixie.repository.UsersInVoicesRepository
import io.github.nightcalls.nixie.repository.VoiceTimeRepository
import io.github.nightcalls.nixie.repository.record.UserInVoiceRecord
import io.github.nightcalls.nixie.repository.record.VoiceTimeRecord
import io.github.nightcalls.nixie.service.UserIdService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

private val logger = KotlinLogging.logger {}

@Service
class VoiceTimeCountService(
    private val usersInVoicesRepository: UsersInVoicesRepository,
    private val voiceTimeRepository: VoiceTimeRepository,
    userIdService: UserIdService,
) : CommonCountService(userIdService) {

    /**
     * Вычислить и записать (создав новый или увеличив существующий счётчик) время, проведенное в голосовом канале, или записать время входа в голосовой канал
     */
    @Transactional
    fun increaseOrCreateCount(eventDto: VoiceEventDto) {
        if (eventDto.isLeaving) {
            val entryTime = returnEntryTimeAndDeleteEntryRecord(eventDto) ?: return
            recordVoiceTimeByDays(eventDto, entryTime)
            return
        }
        val result = usersInVoicesRepository.saveIfNotSameGuildJoining(UserInVoiceRecord(eventDto))
        logger.debug { "Запись о входе в голосовой канал, соответствующая $eventDto, создана: ${result == 1}" }
    }

    /**
     * Обработать время, проведенное в голосовых каналах этого сервера, и вывести в виде списка "порядковый номер + имя пользователя + суммарное время в голосовых каналах"
     */
    @Transactional
    override fun getStatsDefault(eventDto: SlashCommandEventDto) {
        val voiceTimeViewsList = eventDto.guild!!.let {
            val result = voiceTimeRepository.sumAllTimeByGuildIdAndGroupByUserIds(it.idLong)
            logger.debug { "При сборе статистики времени в голосовых каналах было сформировано записей: ${result.size}" }
            result
        }.map { createCountViewWithTimeFormat(it) }

        val file = createTableOutput(eventDto, voiceTimeViewsList, "voice_time") ?: return
        val message = createTitleEmbed(eventDto, "Сформирована статистика по проведенному в голосовых каналах времени")
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика времени в голосовых каналах на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    /**
     * Обработать время, проведенное в голосовых каналах этого сервера конкретным пользователем, и вывести в виде строки "имя пользователя + суммарное время в голосовых каналах"
     */
    @Transactional
    override fun getStatsForUser(eventDto: SlashCommandEventDto) {
        val userName = eventDto.name
        val userId = convertUserNameToUserId(userName!!, eventDto.hook) ?: return

        val voiceTimeView = eventDto.guild!!.let {
            val result = voiceTimeRepository.sumTimeByGuildIdAndUserId(it.idLong, userId)
            logger.debug { "При сборе статистики времени в голосовых каналах конкретного пользователя была сформирована запись: ${result != null}" }
            result
        }?.let { createCountViewWithTimeFormat(it, userName) }

        val file = createSingleOutput(eventDto, voiceTimeView, "voice_time_user") ?: return
        val message = createTitleEmbed(
            eventDto,
            "Сформирована статистика по проведенному в голосовых каналах времени пользователем $userName"
        )
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика времени в голосовых каналах пользователя $userName на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    /**
     * Обработать время, проведенное в голосовых каналах этого сервера времени за конкретный период, и вывести в виде списка "порядковый номер + имя пользователя + суммарное время в голосовых каналах"
     */
    @Transactional
    override fun getStatsForPeriod(eventDto: SlashCommandEventDto) {
        val startDateEndDatePair = validateDates(eventDto.startDate!!, eventDto.endDate!!, eventDto.hook) ?: return
        val startDate = startDateEndDatePair.startDate
        val endDate = startDateEndDatePair.endDate

        val voiceTimeViewsList = eventDto.guild!!.let {
            val result =
                voiceTimeRepository.sumAllTimeByGuildIdAndPeriodAndGroupByUserIds(it.idLong, startDate, endDate)
            logger.debug { "При сборе статистики времени в голосовых каналах за конкретный период было сформировано записей: ${result.size}" }
            result
        }.map { createCountViewWithTimeFormat(it) }

        val file = createTableOutput(eventDto, voiceTimeViewsList, "voice_time_period") ?: return
        val message = createTitleEmbed(
            eventDto,
            "Сформирована статистика по проведенному в голосовых каналах времени за период $startDate - $endDate"
        )
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика времени в голосовых каналах за период $startDate - $endDate на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    /**
     * Обработать время, проведенное в голосовых каналах этого сервера конкретным пользователем за конкретный период, и вывести в виде строки "имя пользователя + суммарное время в голосовых каналах"
     */
    @Transactional
    override fun getStatsForUserAndPeriod(eventDto: SlashCommandEventDto) {
        val userName = eventDto.name
        val userId = convertUserNameToUserId(userName!!, eventDto.hook) ?: return
        val startDateEndDatePair = validateDates(eventDto.startDate!!, eventDto.endDate!!, eventDto.hook) ?: return
        val startDate = startDateEndDatePair.startDate
        val endDate = startDateEndDatePair.endDate

        val voiceTimeView = eventDto.guild!!.let {
            val result = voiceTimeRepository.sumTimeByGuildIdAndUserIdAndPeriod(it.idLong, userId, startDate, endDate)
            logger.debug { "При сборе статистики времени в голосовых каналах конкретного пользователя за конкретный период была сформирована запись: ${result != null}" }
            result
        }?.let { createCountViewWithTimeFormat(it, userName) }

        val file = createSingleOutput(eventDto, voiceTimeView, "voice_time_user_period") ?: return
        val message = createTitleEmbed(
            eventDto,
            "Сформирована статистика по проведенному в голосовых каналах времени пользователем $userName за период $startDate - $endDate"
        )
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика времени в голосовых каналах пользователя $userName за период $startDate - $endDate на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    private fun returnEntryTimeAndDeleteEntryRecord(eventDto: VoiceEventDto): LocalDateTime? {
        val userInVoiceRecord = usersInVoicesRepository.findByGuildIdAndUserId(eventDto.guildId, eventDto.userId)
        if (userInVoiceRecord.isEmpty) {
            /* если бот запустился, когда пользователь был в голосовом канале, и пользователь вышел, когда бот был онлайн
            если бот отключился, когда пользователь был в голосовом канале (была очищена таблица с пользователями в голосовых каналах), и пользователь вышел, когда бот был снова онлайн */
            logger.debug { "Для события выхода из голосового канала не найдены соответствующие данные о событии входа в голосовой канал, время данной сессии не будет учтено в статистике" }
            return null
        }
        val entryTime = userInVoiceRecord.get().entryTime
        usersInVoicesRepository.deleteById(userInVoiceRecord.get().id)
        return entryTime
    }

    private fun recordVoiceTimeByDays(eventDto: VoiceEventDto, entryTime: LocalDateTime) {
        var voiceTimeInSeconds = Duration.between(entryTime, eventDto.time).seconds.toInt()
        var secondsUntilMidnight =
            Duration.between(entryTime, entryTime.toLocalDate().atTime(LocalTime.MAX)).seconds.toInt()
        var syntheticEventDto = VoiceEventDto(eventDto, entryTime.toLocalDate())

        while (voiceTimeInSeconds > secondsUntilMidnight) {
            recordVoiceTime(syntheticEventDto, secondsUntilMidnight)
            syntheticEventDto = VoiceEventDto(syntheticEventDto, syntheticEventDto.date.plusDays(1))
            voiceTimeInSeconds -= secondsUntilMidnight
            secondsUntilMidnight = 86400
        }
        recordVoiceTime(eventDto, voiceTimeInSeconds)
    }

    private fun recordVoiceTime(eventDto: VoiceEventDto, voiceTimeInSeconds: Int) {
        val voiceTimeRecord =
            voiceTimeRepository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
        if (voiceTimeRecord.isPresent) {
            logger.debug { "Счётчик, соответствующий $eventDto, уже есть в базе" }
            val result = voiceTimeRepository.increaseVoiceTimeById(voiceTimeRecord.get().id, voiceTimeInSeconds)
            logger.debug { "Счётчик, соответствующий $eventDto, увеличен: ${result == 1}" }
            return
        }
        logger.debug { "Счётчик, соответствующий $eventDto, отсутствует в базе" }
        val result = voiceTimeRepository.save(VoiceTimeRecord(eventDto, voiceTimeInSeconds))
        logger.debug { "Счётчик, соответствующий $eventDto, создан: $result" }
    }
}