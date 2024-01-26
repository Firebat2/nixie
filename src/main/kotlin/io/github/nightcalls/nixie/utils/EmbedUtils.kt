package io.github.nightcalls.nixie.utils

import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

fun getCommonEmbedBuilder(): EmbedBuilder {
    val embedBuilder = EmbedBuilder()
    embedBuilder.setColor(Color(90, 84, 143))
    return embedBuilder
}