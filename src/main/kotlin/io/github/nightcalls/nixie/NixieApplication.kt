package io.github.nightcalls.nixie

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NixieApplication

fun main(args: Array<String>) {
    runApplication<NixieApplication>(*args)
}