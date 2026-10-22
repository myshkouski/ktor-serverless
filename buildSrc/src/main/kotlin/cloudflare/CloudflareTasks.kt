package cloudflare

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.TaskProvider
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
    val copyAssetsTaskName = "copy${suffix}CloudflareAssets"

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

        // Assets configuration - use the copied assets directory (./assets) for wrangler
        // since that's where files are at runtime (relative to cloudflare/ dir)
        assetsDirectory.set("./assets")
        assetsBinding.set(config.wranglerConfig.assets.binding.getOrElse(""))
        assetsRunWorkerFirst.set(config.wranglerConfig.assets.runWorkerFirst.getOrElse(emptyList<Any>()))
        assetsNotFoundHandling.set(config.wranglerConfig.assets.notFoundHandling.getOrElse(NotFoundHandling.NONE.value))
        dependsOn(generateWorker)
    }

    // Register assets copy task if assets directory is configured
    val copyAssets = project.tasks.register(copyAssetsTaskName, CopyCloudflareAssets::class.java) {
        description = "Copies static assets to the Cloudflare output directory."
        group = "cloudflare"
        assetsSourceDir.set(config.wranglerConfig.assets.directory)
        sourceDir.set(project.objects.directoryProperty().convention(project.layout.projectDirectory.dir(config.wranglerConfig.assets.directory)))
        destinationDir.set(cloudflareDir.map { it.dir("assets") })
        dependsOn(generateWrangler)
        mustRunAfter(generateWrangler)
    }

    project.tasks.matching { it.name == "build" }.configureEach {
        dependsOn(generateWrangler)
        // Also depend on copyAssets if assets are configured
        config.wranglerConfig.assets.directory.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
            dependsOn(copyAssets)
        }
    }

    // Register wranglerDev and wranglerDeploy tasks (only once per project)
    registerWranglerLifecycleTasks(project, binary, packageDir, generateWrangler)
}

/**
 * Registers wranglerDev and wranglerDeploy tasks that run `npx wrangler dev` and `npx wrangler deploy`.
 * These tasks are registered only once per project, when the first Cloudflare binary is configured.
 */
private fun registerWranglerLifecycleTasks(
    project: Project,
    binary: JsIrBinary,
    packageDir: org.gradle.api.provider.Provider<Directory>,
    generateWrangler: TaskProvider<GenerateWranglerConfig>
) {
    val devTaskName = "wranglerDev"
    val deployTaskName = "wranglerDeploy"

    if (project.tasks.findByName(devTaskName) != null) return

    val objects = project.objects

    // Ensure the Kotlin/JS package is built - depend on jsPackageJson task
    // which creates the npmProject directory structure
    val packageJsonTask = project.tasks.named("jsPackageJson")

    project.tasks.register(devTaskName, WranglerDevTask::class.java) {
        description = "Runs wrangler dev for local Cloudflare Workers development."
        group = "cloudflare"
        workingDir.set(objects.directoryProperty().convention(packageDir))
        val envProperty = objects.mapProperty(String::class.java, String::class.java)
        envProperty.convention(emptyMap())
        env.set(envProperty)
        dependsOn(generateWrangler)
        dependsOn(packageJsonTask)
    }

    project.tasks.register(deployTaskName, WranglerDeployTask::class.java) {
        description = "Deploys the Cloudflare Worker using wrangler deploy."
        group = "cloudflare"
        workingDir.set(objects.directoryProperty().convention(packageDir))
        val envProperty = objects.mapProperty(String::class.java, String::class.java)
        envProperty.convention(emptyMap())
        env.set(envProperty)
        val argsProperty = objects.listProperty(String::class.java)
        argsProperty.convention(emptyList())
        args.set(argsProperty)
        dependsOn(generateWrangler)
        dependsOn(packageJsonTask)
    }

    // Also register wranglerDeploy with --env production for convenience
    val deployProdTaskName = "wranglerDeployProduction"
    if (project.tasks.findByName(deployProdTaskName) == null) {
        project.tasks.register(deployProdTaskName, WranglerDeployTask::class.java) {
            description = "Deploys the Cloudflare Worker to production environment."
            group = "cloudflare"
            workingDir.set(objects.directoryProperty().convention(packageDir))
            val envProperty = objects.mapProperty(String::class.java, String::class.java)
            envProperty.convention(emptyMap())
            env.set(envProperty)
            val argsProperty = objects.listProperty(String::class.java)
            argsProperty.convention(listOf("--env", "production"))
            args.set(argsProperty)
            dependsOn(generateWrangler)
            dependsOn(packageJsonTask)
        }
    }
}

internal val JsIrBinary.isProductionMainExecutable: Boolean
    get() = mode == KotlinJsBinaryMode.PRODUCTION && compilation.name == "main"