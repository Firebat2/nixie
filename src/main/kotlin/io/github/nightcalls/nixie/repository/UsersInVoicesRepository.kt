package io.github.nightcalls.nixie.repository

import io.github.nightcalls.nixie.repository.record.UserInVoiceRecord
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface UsersInVoicesRepository : CrudRepository<UserInVoiceRecord, Long> {

    fun findByGuildIdAndUserId(guildId: Long, userId: Long): Optional<UserInVoiceRecord>

    /**
     * Записать время входа в войс определенного пользователя на определенном сервере; не записывать время перехода между войсами одного и того же сервера
     */
    @Transactional
    @Modifying
    @Query(
        value = "INSERT INTO users_in_voices (guild_id, user_id, time) values (:#{#record.guildId}, :#{#record.userId}, :#{#record.time}) ON CONFLICT (guild_id, user_id) DO NOTHING",
        nativeQuery = true
    )
    fun saveIfNotSameGuildJoining(record: UserInVoiceRecord): Int

    fun deleteAllByGuildId(guildId: Long)
}