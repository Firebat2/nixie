package io.github.nightcalls.nixie.service.count

import io.github.nightcalls.nixie.service.UserIdService
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service

@Service
class CommonCountService(
    protected val userIdService: UserIdService
) {
    fun createCountView(userIdAndCount: Pair<Long, Int>): Pair<String, String> {
        return Pair(
            userIdService.userIdToUserName(userIdAndCount.first),
            userIdAndCount.second.toString()
        )
    }

    fun createCountViewWithTimeFormat(userIdAndTimeCount: Pair<Long, Int>): Pair<String, String> {
        val voiceTime = userIdAndTimeCount.second
        return Pair(
            userIdService.userIdToUserName(userIdAndTimeCount.first),
            String.format("%02d:%02d:%02d", voiceTime / 3600, (voiceTime % 3600) / 60, voiceTime % 60)
        )
    }

    fun createTableOutput(viewsList: List<Pair<String, String>>): String {
        val firstColumnWidth = viewsList.size.toString().length + 3
        val secondColumnWidth = viewsList.maxBy { it.first.length }.first.length + 2

        var i = 1
        val result = StringBuilder()
        result.append("```")
        viewsList.forEach {
            val firstColumn = StringUtils.rightPad("${i++}. -", firstColumnWidth, "-")
            val secondColumn = StringUtils.rightPad("${it.first} -", secondColumnWidth, "-")
            result.append("$firstColumn $secondColumn ${it.second}\n")
        }
        result.append("```")

        return result.toString()
    }
}