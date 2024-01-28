package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.service.count.view.IdCountPair
import io.github.nightcalls.nixie.service.count.view.NameValuePair
import io.github.nightcalls.nixie.utils.getCommonEmbedBuilder
import io.github.nightcalls.nixie.utils.toCommonLocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.FileUpload
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

@Service
class CommonCountService(
    protected val userIdService: UserIdService
) {
    fun createCountView(userIdAndCount: IdCountPair): NameValuePair {
        return NameValuePair(
            userIdService.userIdToUserName(userIdAndCount.id), userIdAndCount.count.toString()
        )
    }

    fun createCountViewWithTimeFormat(userIdAndTimeCount: IdCountPair): NameValuePair {
        val voiceTime = userIdAndTimeCount.count
        return NameValuePair(
            userIdService.userIdToUserName(userIdAndTimeCount.id),
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    fun sendTitleEmbed(event: SlashCommandInteractionEvent, title: String) {
        // Перезапись "Thinking..." сообщения
        val deferEmbedBuilder = getCommonEmbedBuilder()
        deferEmbedBuilder.setDescription("Было запущено формирование статистики")
        event.hook.sendMessageEmbeds(deferEmbedBuilder.build()).queue()

        // С упоминанием пользователя перед выводом результата
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription(title)
        event.member?.asMention?.let { event.hook.sendMessage(it).addEmbeds(embedBuilder.build()).queue() }
    }

    fun sendEmptyDataEmbed(hook: InteractionHook) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Данные отсутствуют")
        hook.sendMessageEmbeds(embedBuilder.build()).queue()
    }

    /**
     * Формирование вывода статистики и отправка его частями в случае превышения установленной в Discord длины в 2000 символов
     */
    fun createTableOutputsAndMultipleReplies(event: SlashCommandInteractionEvent, viewsList: List<NameValuePair>) {
        if (viewsList.isEmpty()) {
            sendEmptyDataEmbed(event.hook)
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
        result.append("\nGuild: ${event.guild?.name}\n")
        result.append("Initiator: ${event.user.name}\n")
        val timeJoined = event.guild?.selfMember?.timeJoined?.toCommonLocalDateTime()?.truncatedTo(ChronoUnit.SECONDS)
        result.append("Period: $timeJoined - $formationTime")
        event.hook.sendFiles(
            FileUpload.fromData(
                result.toString().byteInputStream(StandardCharsets.UTF_8),
                "Statistics_$formationTime.txt"
            )
        ).queue()
    }
}