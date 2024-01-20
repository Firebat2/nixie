package io.github.nightcalls.nixie.service

import net.dv8tion.jda.api.JDA
import org.springframework.stereotype.Service

@Service
class UserIdService(
    private val jda: JDA,
) {
    /**
     * Получение уникального имени пользователя/имени до хештега по userId
     */
    fun userIdToUserName(userId: Long): String {
        return jda.retrieveUserById(userId).complete()?.name ?: userId.toString()
    }
}