package com.test.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class MessPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        println("enter mess plugin")
    }
}