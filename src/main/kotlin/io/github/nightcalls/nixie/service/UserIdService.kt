package io.github.nightcalls.nixie.service

import net.dv8tion.jda.api.JDA
import org.springframework.stereotype.Service

@Service
class UserIdService(
    private val jda: JDA,
) {
    /**
     * Получить уникальное имя пользователя/имя до хештега по userId или вернуть userId, если пользователь не найден
     */
    fun getUserNameByUserId(userId: Long): String {
        val user = jda.getUserById(userId) ?: jda.retrieveUserById(userId).complete() ?: return userId.toString()
        if (user.isBot) {
            return user.name + " [Бот]"
        }
        return user.name
    }

    /**
     * Получить userId по уникальному имени пользователя/имени до хештега или вернуть null, если пользователь не найден
     */
    fun getUserIdByUserName(name: String): Long? {
        return jda.getUsersByName(name, false).firstOrNull()?.idLong
    }
}