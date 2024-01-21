package io.github.nightcalls.nixie.repository

import io.github.nightcalls.nixie.repository.record.MessageCountRecord
import io.github.nightcalls.nixie.service.count.view.IdCountPair
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface MessageCountRepository : CrudRepository<MessageCountRecord, Long> {

    fun findByGuildIdAndUserIdAndDate(guildId: Long, userId: Long, date: LocalDate): Optional<MessageCountRecord>

    /**
     * Для записи с этим id увеличить счётчик сообщений на 1
     */
    @Modifying
    @Query("UPDATE MessageCountRecord mcr SET mcr.messageCount = mcr.messageCount + 1 WHERE mcr.id = :id")
    fun incrementMessageCountById(id: Long): Int

    /**
     * Для всех пользователей этого сервера сложить счётчики сообщений, отсортировать по убыванию и сгруппировать пару id пользователя + счётчик сообщений
     */
    @Query("SELECT new io.github.nightcalls.nixie.service.count.view.IdCountPair(mcr.userId, SUM(mcr.messageCount)) FROM MessageCountRecord AS mcr WHERE mcr.guildId = :guildId GROUP BY mcr.userId ORDER BY SUM(mcr.messageCount) DESC")
    fun sumCountsForGuildIdGroupByUserId(guildId: Long): List<IdCountPair>
}