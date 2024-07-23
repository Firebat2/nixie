package io.github.nightcalls.nixie.service

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class BotInfoService(
    private val jda: JDA,
) : ListenerAdapter() {

    /**
     * Вывести в лог информацию о серверах, к которым подключён бот
     */
    @EventListener
    fun onContextRefreshedEvent(event: ContextRefreshedEvent) {
        val joinedGuilds = jda.guilds
        logger.info {
            "Бот подключён к серверам (${joinedGuilds.size}): ${
                joinedGuilds.stream().map { c -> c.name }.toList().joinToString()
            }"
        }
    }
}