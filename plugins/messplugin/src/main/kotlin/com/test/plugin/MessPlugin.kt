package com.test.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import proguard.obfuscate.MappingProcessor
import proguard.obfuscate.MappingReader
import java.io.File

class MessPlugin : Plugin<Project> {

    class Config(
        val className: String, // 被混淆的类
        val nativePath: String // 对应的c文件路径
    )
    {
        // className被混淆的新类
        var newClassName: String? = null

        // 存储了原方法与混淆方法的对应关系
        var methods: MutableMap<String, String> = mutableMapOf()

        // 源码的备份路径, 比如 native-lib.cpp~
        var backupPath: String? = null
        override fun toString() = "$className, $nativePath, $newClassName, $methods"
    }

    // 存储了所有混淆类的配置
    // [com.test.jnidemo.MainActivity, /xxx/src/main/cpp/native-lib.cpp,
    // com.test.jnidemo.MainActivity, {stringFromJNI=o}]
    private val configs = mutableListOf<Config>()

    override fun apply(project: Project) {
        println("enter mess plugin")

        // 创建用户配置
        val messExtension = project.extensions
            .create("messConfig", MessExtension::class.java)

        project.afterEvaluate {
            // 将app/build.gradle messConfig配置存储起来
            messExtension.classAndNative!!
                .forEach { (className, nativePath) ->
                    configs.add(
                        Config(className, "${projectDir}/${nativePath}")
                    )
                }

            // 获取当前的构建信息
            val releaseVariant = extensions
                .getByType(AppExtension::class.java)
                .applicationVariants.firstOrNull {
                    it.buildType.name.capitalized() == "Release"
                }!!
            // 开启了minifyEnabled后, 会生成mapping.txt
            val mappingFile = releaseVariant.mappingFile
            // 这是编译c代码的task, 不同的gradle版本, 可能不一样, debug模式也不一样
            val nativeBuildTask = tasks
                .findByName("buildCMakeRelWithDebInfo[arm64-v8a]")!!
            // 这是系统混淆的task
            val proguardTask = tasks
                .findByName("minifyReleaseWithR8")!!

            // 使native编译在java类混淆之后运行, 应该需要解析mapping后替换
            nativeBuildTask.dependsOn(proguardTask)

            nativeBuildTask.doFirst {
                // 编译前解析mapping文件, 和替换c源码
                parseMapping(mappingFile)
                replaceNativeSource()
            }
            nativeBuildTask.doLast {
                // 编译完c文件后, 恢复替换的代码
                restoreNativeSource()
            }
        }
    }

    // 解析mapping文件
    private fun parseMapping(mappingFile: File) {
        MappingReader(mappingFile).pump(
            object : MappingProcessor {
                override fun processClassMapping(
                    className: String,
                    newClassName: String
                ): Boolean {
                    // 如果发现配置的类, 则返回true
                    // 如果返回false, processMethodMapping就不会运行
                    return configs.firstOrNull {
                        it.className == className
                    }?.let {
                        it.newClassName = newClassName
                    } != null
                }

                override fun processFieldMapping(
                    className: String,
                    fieldType: String,
                    fieldName: String,
                    newClassName: String,
                    newFieldName: String
                ) {
                }

                override fun processMethodMapping(
                    className: String,
                    firstLineNumber: Int,
                    lastLineNumber: Int,
                    methodReturnType: String,
                    methodName: String,
                    methodArguments: String,
                    newClassName: String,
                    newFirstLineNumber: Int,
                    newLastLineNumber: Int,
                    newMethodName: String
                ) {
                    // 如果混淆前和混淆后一样, 跳过, 比如构造方法
                    if (methodName == newMethodName) return
                    // 记录类的混淆方法对应关系
                    configs.firstOrNull {
                        it.className == className
                    }?.apply {
                        methods[methodName] = newMethodName
                    }
                }
            })
        println("configs: $configs")
    }

    // 在编译c文件前备份和替换
    private fun replaceNativeSource() {
        configs.forEach {
            val nativeFile = File(it.nativePath).apply {
                // 备份文件添加~
                // native-lib.cpp -> native-lib.cpp~
                it.backupPath = "${absolutePath}~"
                copyTo(File(it.backupPath!!), true)
            }

            var source = nativeFile.readText()
            if (it.newClassName != null) {
                // 动态注册的类是"com/test/tokenlib/NativeLib"
                // 这里是放类换成混淆后的字符串
                val realClassName = it.className
                    .replace(".", "/")
                val realNewClassName = it.newClassName!!
                    .replace(".", "/")
                source = source.replace(
                    "\"$realClassName\"",
                    "\"$realNewClassName\""
                )
            }

            it.methods.forEach { (oldMethod, newMethod) ->
                // 这个是替换混淆方法
                source = source.replace(
                    "\"$oldMethod\"",
                    "\"$newMethod\""
                )
            }
            nativeFile.writeText(source)
        }
    }

    // 编译完成后恢复原来的c文件
    private fun restoreNativeSource() {
        configs.filter {
            it.backupPath != null
        }.forEach {
            File(it.backupPath!!).apply {
                // 恢复并删除备份文件
                copyTo(File(it.nativePath), true)
                delete()
            }
        }
    }
}