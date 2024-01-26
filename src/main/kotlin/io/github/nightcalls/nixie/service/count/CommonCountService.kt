package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.service.count.view.IdCountPair
import io.github.nightcalls.nixie.service.count.view.NameValuePair
import io.github.nightcalls.nixie.utils.getCommonEmbedBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CommonCountService(
    protected val userIdService: UserIdService
) {
    fun createCountView(userIdAndCount: IdCountPair): NameValuePair {
        return NameValuePair(
            userIdService.userIdToUserName(userIdAndCount.id),
            userIdAndCount.count.toString()
        )
    }

    fun createCountViewWithTimeFormat(userIdAndTimeCount: IdCountPair): NameValuePair {
        val voiceTime = userIdAndTimeCount.count
        return NameValuePair(
            userIdService.userIdToUserName(userIdAndTimeCount.id),
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    /**
     * Формирование вывода статистики и отправка его частями в случае превышения установленной в Discord длины в 2000 символов
     */
    fun createTableOutputsAndMultipleReplies(event: SlashCommandInteractionEvent, viewsList: List<NameValuePair>) {
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription("Сформирована статистика")
        event.member?.asMention?.let { event.reply(it).addEmbeds(embedBuilder.build()).queue() }
        val hook = event.hook

        val firstColumnWidth = viewsList.size.toString().length + 3
        val secondColumnWidth = viewsList.maxBy { it.name.length }.name.length + 2
        logger.debug { "Рассчитаны ширины первого ($firstColumnWidth) и второго ($secondColumnWidth) столбцов" }
        var i = 1
        val result = StringBuilder()
        result.append("```")
        viewsList.forEach {
            val firstColumn = StringUtils.rightPad("${i++}. -", firstColumnWidth, "-")
            val secondColumn = StringUtils.rightPad("${it.name} -", secondColumnWidth, "-")
            result.append("$firstColumn $secondColumn ${it.value}\n")

            if (result.length > 1950) {
                sendResultWithHook(hook, result)
                result.setLength(0)
                result.append("```")
            }
        }
        sendResultWithHook(hook, result)
    }

    private fun sendResultWithHook(hook: InteractionHook, result: java.lang.StringBuilder) {
        result.append("```")
        val embedBuilder = getCommonEmbedBuilder()
        embedBuilder.setDescription(result)
        hook.sendMessageEmbeds(embedBuilder.build()).queue()
    }
}