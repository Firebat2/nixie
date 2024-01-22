package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.service.UserIdService
import io.github.nightcalls.nixie.service.count.view.IdCountPair
import io.github.nightcalls.nixie.service.count.view.NameValuePair
import io.github.oshai.kotlinlogging.KotlinLogging
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

    fun createTableOutput(viewsList: List<NameValuePair>): String {
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
        }
        result.append("```")
        return result.toString()
    }
}