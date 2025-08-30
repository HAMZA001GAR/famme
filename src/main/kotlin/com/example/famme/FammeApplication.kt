package com.example.famme

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class FammeApplication

fun main(args: Array<String>) {
	runApplication<FammeApplication>(*args)
}
