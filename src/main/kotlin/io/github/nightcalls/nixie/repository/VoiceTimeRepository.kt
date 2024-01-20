package io.github.nightcalls.nixie.repository

import io.github.nightcalls.nixie.repository.record.VoiceTimeRecord
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Repository
interface VoiceTimeRepository : CrudRepository<VoiceTimeRecord, Long> {

    fun findByGuildIdAndUserIdAndDate(guildId: Long, userId: Long, date: LocalDate): Optional<VoiceTimeRecord>

    /**
     * Для записи с этим id увеличить время в войсе на величину времени последней сессии в войсе
     */
    @Transactional
    @Modifying
    @Query("UPDATE VoiceTimeRecord vtr SET vtr.time = vtr.time + :voiceTime WHERE vtr.id = :id")
    fun incrementVoiceTimeById(id: Long, voiceTime: Int): Int

    /**
     * Для всех пользователей этого сервера сложить время в войсе, отсортировать по убыванию и сгруппировать пару id пользователя + время в войсе
     */
    @Query("SELECT new kotlin.Pair(vtr.userId, SUM(vtr.time)) FROM VoiceTimeRecord AS vtr WHERE vtr.guildId = :guildId GROUP BY vtr.userId ORDER BY SUM(vtr.time) DESC")
    fun sumTimeForGuildIdGroupByUserId(guildId: Long): List<Pair<Long, Int>>
}