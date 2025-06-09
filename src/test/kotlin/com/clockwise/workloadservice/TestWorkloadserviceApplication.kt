package com.clockwise.workloadservice

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<WorkloadserviceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
