package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.listeners.dto.SlashCommandEventDto
import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.service.count.dto.IdCountPair
import io.github.nightcalls.nixie.service.count.dto.NameValuePair
import io.github.nightcalls.nixie.service.count.dto.StartDateEndDatePair
import io.github.nightcalls.nixie.utils.getCommonEmbedBuilder
import io.github.nightcalls.nixie.utils.toCommonLocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.FileUpload
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

@Service
abstract class CommonCountService(
    protected val userIdService: UserIdService
) {
    /**
     * Сформировать и вывести статистику
     */
    fun showStats(eventDto: SlashCommandEventDto) {
        if (eventDto.startDate != null && eventDto.endDate != null) {
            if (eventDto.name != null) {
                return getStatsForUserAndPeriod(eventDto)
            }
            return getStatsForPeriod(eventDto)
        }
        if (eventDto.startDate != null || eventDto.endDate != null) {
            return sendMissingDateEmbed(eventDto.hook)
        }
        if (eventDto.name != null) {
            return getStatsForUser(eventDto)
        }
        return getStatsDefault(eventDto)
    }

    protected abstract fun getStatsDefault(eventDto: SlashCommandEventDto)

    protected abstract fun getStatsForUser(eventDto: SlashCommandEventDto)

    protected abstract fun getStatsForPeriod(eventDto: SlashCommandEventDto)

    protected abstract fun getStatsForUserAndPeriod(eventDto: SlashCommandEventDto)

    protected fun convertUserNameToUserId(userName: String, hook: InteractionHook): Long? {
        val userId = userIdService.getUserIdByUserName(userName)
        if (userId == null) {
            sendUserNotFoundEmbed(hook)
        }
        return userId
    }

    protected fun validateDates(
        startDateString: String,
        endDateString: String,
        hook: InteractionHook
    ): StartDateEndDatePair? {
        val startDate = validateAndConvertDate(startDateString, hook) ?: return null
        val endDate = validateAndConvertDate(endDateString, hook) ?: return null

        if (endDate.isBefore(startDate)) {
            sendWrongDatesOrderEmbed(hook)
            return null
        }
        return StartDateEndDatePair(startDate, endDate)
    }

    protected fun validateAndConvertDate(date: String, hook: InteractionHook): LocalDate? {
        return try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            sendWrongDateFormatEmbed(hook)
            null
        }
    }

    protected fun sendUserNotFoundEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Пользователь не найден")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о том, что пользователь не найден! $e" }
        }
    }

    protected fun sendMissingDateEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Не заполнена одна из дат")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о том, что не заполнена одна из дат! $e" }
        }
    }

    protected fun sendWrongDateFormatEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Неправильный формат даты. Пример правильного формата: 2024-01-31")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о неправильном формате даты! $e" }
        }
    }

    protected fun sendWrongDatesOrderEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Неправильный порядок дат: end-date должна быть позже или равна start-date")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение о неправильном порядке дат! $e" }
        }
    }

    protected fun sendEmptyDataEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Данные отсутствуют")
        try {
            hook.sendMessageEmbeds(embedBuilder.build()).queue()
        } catch (e: Exception) {
            logger.error { "Не удалось отправить сообщение об отсутствующих данных! $e" }
        }
    }

    protected fun createTitleEmbed(eventDto: SlashCommandEventDto, title: String): MessageEmbed {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription(title)
        return embedBuilder.build()
    }

    protected fun createCountView(userIdAndCount: IdCountPair): NameValuePair {
        return NameValuePair(
            userIdService.getUserNameByUserId(userIdAndCount.id),
            userIdAndCount.count.toString()
        )
    }

    protected fun createCountView(userIdAndCount: IdCountPair, userName: String): NameValuePair {
        return NameValuePair(
            userName,
            userIdAndCount.count.toString()
        )
    }

    protected fun createCountViewWithTimeFormat(userIdAndTimeCount: IdCountPair): NameValuePair {
        val voiceTime = userIdAndTimeCount.count
        return NameValuePair(
            userIdService.getUserNameByUserId(userIdAndTimeCount.id),
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    protected fun createCountViewWithTimeFormat(userIdAndTimeCount: IdCountPair, userName: String): NameValuePair {
        val voiceTime = userIdAndTimeCount.count
        return NameValuePair(
            userName,
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    /**
     * Сформировать однострочную статистику
     */
    protected fun createSingleOutput(
        eventDto: SlashCommandEventDto,
        view: NameValuePair?,
        fileTitle: String
    ): FileUpload? {
        if (view == null) {
            sendEmptyDataEmbed(eventDto.hook)
            return null
        }
        val formationTime = OffsetDateTime.now().toCommonLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        val result = StringBuilder()
        result.append("${view.name} ${view.value}\n")
        return createStatsFile(result, eventDto, formationTime, fileTitle)
    }

    /**
     * Сформировать многострочную статистику
     */
    protected fun createTableOutput(
        eventDto: SlashCommandEventDto,
        viewsList: List<NameValuePair>,
        fileTitle: String
    ): FileUpload? {
        if (viewsList.isEmpty()) {
            sendEmptyDataEmbed(eventDto.hook)
            return null
        }
        val formationTime = OffsetDateTime.now().toCommonLocalDateTime().truncatedTo(ChronoUnit.SECONDS)
        val firstColumnWidth = viewsList.size.toString().length + 3
        val secondColumnWidth = viewsList.maxBy { it.name.length }.name.length + 2
        logger.debug { "Рассчитаны ширины первого ($firstColumnWidth) и второго ($secondColumnWidth) столбцов" }
        var i = 1
        val result = StringBuilder()
        viewsList.forEach {
            val firstColumn = StringUtils.rightPad("${i++}. -", firstColumnWidth, "-")
            val secondColumn = StringUtils.rightPad("${it.name} -", secondColumnWidth, "-")
            result.append("$firstColumn $secondColumn ${it.value}\n")
        }
        return createStatsFile(result, eventDto, formationTime, fileTitle)
    }

    private fun createStatsFile(
        result: StringBuilder,
        eventDto: SlashCommandEventDto,
        formationTime: LocalDateTime?,
        fileTitle: String
    ): FileUpload {
        result.append("\nСервер: ${eventDto.guild?.name}\n")
        result.append("Инициатор: ${eventDto.initiator}\n")

        val periodStart = eventDto.startDate
        if (periodStart == null) {
            val timeJoined =
                eventDto.guild?.selfMember?.timeJoined?.toCommonLocalDateTime()?.truncatedTo(ChronoUnit.SECONDS)
            result.append("Период: $timeJoined - $formationTime")
        } else {
            val periodEnd = eventDto.endDate
            result.append("Период: $periodStart - $periodEnd")
        }
        return FileUpload.fromData(
            result.toString().byteInputStream(StandardCharsets.UTF_8),
            "Stats_${fileTitle}_$formationTime.txt"
        )
    }
}