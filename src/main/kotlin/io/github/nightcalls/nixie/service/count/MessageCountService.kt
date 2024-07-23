package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.MessageEventDto
import io.github.nightcalls.nixie.listeners.dto.SlashCommandEventDto
import io.github.nightcalls.nixie.repository.MessageCountRepository
import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.service.UserIdService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class MessageCountService(
    private val repository: MessageCountRepository,
    userIdService: UserIdService
) : CommonCountService(userIdService) {

    /**
     * Создать новый или увеличить существующий счётчик отправленных сообщений
     */
    @Transactional
    fun increaseOrCreateCount(eventDto: MessageEventDto) {
        val record = repository.findByGuildIdAndUserIdAndDate(eventDto.guildId, eventDto.userId, eventDto.date)
        if (record.isPresent) {
            logger.debug { "Счётчик, соответствующий $eventDto, уже есть в базе" }
            val result = repository.increaseMessageCountById(record.get().id)
            logger.debug { "Счётчик, соответствующий $eventDto, увеличен: ${result == 1}" }
            return
        }
        logger.debug { "Счётчик, соответствующий $eventDto, отсутствует в базе" }
        val result = repository.save(MessageCountRecord(eventDto))
        logger.debug { "Счётчик, соответствующий $eventDto, создан: $result" }
    }

    /**
     * Обработать счётчики сообщений этого сервера и вывести в виде списка "порядковый номер + имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsDefault(eventDto: SlashCommandEventDto) {
        val messageViewsList = eventDto.guild!!.let {
            val result = repository.sumAllCountsByGuildIdAndGroupByUserIds(it.idLong)
            logger.debug { "При сборе статистики сообщений было сформировано записей: ${result.size}" }
            result
        }.map { createCountView(it) }

        val file = createTableOutput(eventDto, messageViewsList, "messages") ?: return
        val message = createTitleEmbed(eventDto, "Сформирована статистика по количеству отправленных сообщений")
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика сообщений на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    /**
     * Обработать счётчики сообщений конкретного пользователя этого сервера и вывести в виде строки "имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsForUser(eventDto: SlashCommandEventDto) {
        val userName = eventDto.name
        val userId = convertUserNameToUserId(userName!!, eventDto.hook) ?: return

        val messageView = eventDto.guild!!.let {
            val result = repository.sumCountsByGuildIdAndUserId(it.idLong, userId)
            logger.debug { "При сборе статистики сообщений конкретного пользователя была сформирована запись: ${result != null}" }
            result
        }?.let { createCountView(it, userName) }

        val file = createSingleOutput(eventDto, messageView, "messages_user") ?: return
        val message = createTitleEmbed(
            eventDto,
            "Сформирована статистика по количеству отправленных сообщений пользователем $userName"
        )
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика сообщений пользователя $userName на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    /**
     * Обработать счётчики сообщений этого сервера за конкретный период и вывести в виде списка "порядковый номер + имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsForPeriod(eventDto: SlashCommandEventDto) {
        val startDateEndDatePair = validateDates(eventDto.startDate!!, eventDto.endDate!!, eventDto.hook) ?: return
        val startDate = startDateEndDatePair.startDate
        val endDate = startDateEndDatePair.endDate

        val messageViewsList = eventDto.guild!!.let {
            val result = repository.sumAllCountsByGuildIdAndPeriodAndGroupByUserIds(it.idLong, startDate, endDate)
            logger.debug { "При сборе статистики сообщений за конкретный период было сформировано записей: ${result.size}" }
            result
        }.map { createCountView(it) }

        val file = createTableOutput(eventDto, messageViewsList, "messages_period") ?: return
        val message = createTitleEmbed(
            eventDto,
            "Сформирована статистика по количеству отправленных сообщений за период $startDate - $endDate"
        )
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика сообщений за период $startDate - $endDate на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }

    /**
     * Обработать счётчики сообщений конкретного пользователя этого сервера за конкретный период и вывести в виде строки "имя пользователя + суммарное кол-во сообщений"
     */
    @Transactional
    override fun getStatsForUserAndPeriod(eventDto: SlashCommandEventDto) {
        val userName = eventDto.name
        val userId = convertUserNameToUserId(userName!!, eventDto.hook) ?: return
        val startDateEndDatePair = validateDates(eventDto.startDate!!, eventDto.endDate!!, eventDto.hook) ?: return
        val startDate = startDateEndDatePair.startDate
        val endDate = startDateEndDatePair.endDate

        val messageView = eventDto.guild!!.let {
            val result = repository.sumCountsByGuildIdAndUserIdAndPeriod(it.idLong, userId, startDate, endDate)
            logger.debug { "При сборе статистики сообщений конкретного пользователя за конкретный период была сформирована запись: ${result != null}" }
            result
        }?.let { createCountView(it, userName) }

        val file = createSingleOutput(eventDto, messageView, "messages_user_period") ?: return
        val message = createTitleEmbed(
            eventDto,
            "Сформирована статистика по количеству отправленных сообщений пользователем $userName за период $startDate - $endDate"
        )
        try {
            eventDto.member?.asMention?.let {
                eventDto.hook.sendMessage(it).addEmbeds(message).addFiles(file).queue()
            }
            logger.info { "Отправлена статистика сообщений пользователя $userName за период $startDate - $endDate на сервер ${eventDto.guild.name}" }
        } catch (e: Exception) {
            logger.error { "Не удалось отправить статистику! $e" }
        }
    }
}