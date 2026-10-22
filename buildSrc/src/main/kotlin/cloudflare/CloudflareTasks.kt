package cloudflare

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

/**
 * Registers the adapter tasks for the given production executable binary.
 *
 * The extension is bound to the binary rather than to a hard-coded build directory,
 * so the generated files follow the target's actual output location.
 */
internal fun registerCloudflareTasks(project: Project, binary: JsIrBinary) {
    val config = project.extensions.getByType(CloudflareWorkerExtension::class.java)

    val suffix = if (binary.name == "productionExecutable") {
        ""
    } else {
        binary.name.replaceFirstChar { it.uppercase() }
    }
    val workerTaskName = "generate${suffix}CloudflareWorker"
    val wranglerTaskName = "generate${suffix}WranglerConfig"

    if (project.tasks.findByName(workerTaskName) != null) return

    config.moduleFile.convention(binary.mainFileSyncPath)
    val packageDir = binary.compilation.npmProject.dir
    val cloudflareDir = packageDir.map { it.dir("cloudflare") }
    val wranglerDir = packageDir.map { it.dir(".wrangler").dir("deploy") }
    config.outputFile.convention(cloudflareDir.map { it.file("worker.mjs") })
    config.wranglerConfig.outputFile.convention(cloudflareDir.map { it.file("wrangler.json") })
    config.wranglerConfig.deployConfigFile.convention(wranglerDir.map { it.file("config.json") })

    val generateWorker = project.tasks.register(workerTaskName, GenerateCloudflareWorker::class.java) {
        description = "Generates a Cloudflare Workers module with a default fetch handler."
        group = "cloudflare"
        fetchHandler.set(config.fetchHandler)
        moduleFile.set(config.moduleFile)
        outputFile.set(config.outputFile)
        dependsOn(binary.linkSyncTask)
    }

    val generateWrangler = project.tasks.register(wranglerTaskName, GenerateWranglerConfig::class.java) {
        description = "Generates the Wrangler config and the .wrangler config discovery file."
        group = "cloudflare"
        workerName.set(config.wranglerConfig.name)
        compatibilityDate.set(config.wranglerConfig.compatibilityDate)
        compatibilityFlags.set(config.wranglerConfig.compatibilityFlags)
        keepVars.set(config.wranglerConfig.keepVars)
        vars.set(config.wranglerConfig.vars)
        kvNamespaces.set(config.wranglerConfig.kvNamespaces)
        workerFile.set(config.outputFile)
        outputFile.set(config.wranglerConfig.outputFile)
        deployConfigFile.set(config.wranglerConfig.deployConfigFile)
        dependsOn(generateWorker)
    }

    project.tasks.matching { it.name == "build" }.configureEach {
        dependsOn(generateWrangler)
    }
}

internal val JsIrBinary.isProductionMainExecutable: Boolean
    get() = mode == KotlinJsBinaryMode.PRODUCTION && compilation.name == "main"
