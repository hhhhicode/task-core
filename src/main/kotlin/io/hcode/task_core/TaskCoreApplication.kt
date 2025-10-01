package io.hcode.task_core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TaskCoreApplication

fun main(args: Array<String>) {
	runApplication<TaskCoreApplication>(*args)
}
