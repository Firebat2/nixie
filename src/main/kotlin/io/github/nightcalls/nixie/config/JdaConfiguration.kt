package io.github.nightcalls.nixie.config

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.security.auth.login.LoginException

private val logger = KotlinLogging.logger {}

@Configuration
class JdaConfiguration(
    private val environment: Environment
) {
    @Bean
    fun jda(): JDA {
        val token = environment.getRequiredProperty("discord.token")
        val builder = createBuilder(token)
        try {
            logger.info { "Производится попытка залогиниться в Discord..." }
            val jda = builder.build()
            jda.awaitReady()
            logger.info { "Успешный логин в Discord" }
            return jda
        } catch (e: LoginException) {
            logger.error { "Не удалось залогиниться в Discord! $e" }
            throw e
        }
    }

    fun createBuilder(token: String): JDABuilder {
        return JDABuilder.create(
            token,
            GatewayIntent.GUILD_MESSAGES, // для сбора статистики сообщений
            GatewayIntent.GUILD_VOICE_STATES, // для сбора статистики входов и выходов из войсов
            GatewayIntent.GUILD_MEMBERS // для удаления участников из кэша, после того как они покинут гильдию
        )
            .disableCache(
                CacheFlag.ACTIVITY,
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ONLINE_STATUS,
                CacheFlag.SCHEDULED_EVENTS
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setActivity(Activity.listening("/nixie"))
    }
}