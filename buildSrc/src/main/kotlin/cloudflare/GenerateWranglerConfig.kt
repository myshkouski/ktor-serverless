package cloudflare

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Generates the Wrangler config along with the `.wrangler/deploy/config.json`
 * discovery file that points Wrangler at it.
 */
abstract class GenerateWranglerConfig : DefaultTask() {
    @get:Input
    abstract val workerName: Property<String>

    @get:Input
    abstract val compatibilityDate: Property<String>

    @get:Input
    abstract val compatibilityFlags: ListProperty<String>

    @get:Input
    abstract val keepVars: Property<Boolean>

    @get:Input
    abstract val vars: MapProperty<String, String>

    @get:Input
    abstract val kvNamespaces: ListProperty<String>

    @get:Input
    abstract val assetsDirectory: Property<String>

    @get:Input
    abstract val assetsBinding: Property<String>

    @get:Input
    abstract val assetsRunWorkerFirst: Property<Any>

    @get:Input
    abstract val assetsNotFoundHandling: Property<String>

    @get:InputFile
    abstract val workerFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val deployConfigFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val configFile = outputFile.get().asFile
        val worker = workerFile.get().asFile
        val configDir = configFile.parentFile.toPath()

        fun relativePath(file: File): String {
            val path = configDir.relativize(file.toPath()).toString().replace('\\', '/')
            return when {
                path.isEmpty() -> "."
                path.startsWith(".") -> path
                else -> "./$path"
            }
        }

        val wranglerConfig = linkedMapOf<String, Any>(
            "compatibility_date" to compatibilityDate.get(),
            "name" to workerName.get()
        )

        compatibilityFlags.get().takeIf { it.isNotEmpty() }?.let {
            wranglerConfig["compatibility_flags"] = it
        }
        wranglerConfig["main"] = relativePath(worker)
        wranglerConfig["base_dir"] = relativePath(worker.parentFile)
        vars.get().takeIf { it.isNotEmpty() }?.let {
            wranglerConfig["vars"] = it
        }
        kvNamespaces.get().takeIf { it.isNotEmpty() }?.let { bindings ->
            wranglerConfig["kv_namespaces"] = bindings.map { mapOf("binding" to it) }
        }
        keepVars.orNull?.let {
            wranglerConfig["keep_vars"] = it
        }

        // Add assets configuration if directory is set
        val assetsDir = assetsDirectory.getOrNull()
        if (assetsDir != null && assetsDir.isNotEmpty()) {
            val assetsConfig = linkedMapOf<String, Any>()
            assetsConfig["directory"] = assetsDir

            assetsBinding.getOrNull()?.let { binding ->
                if (binding.isNotEmpty()) {
                    assetsConfig["binding"] = binding
                }
            }

            assetsRunWorkerFirst.getOrNull()?.let { runWorkerFirst ->
                when (runWorkerFirst) {
                    is List<*> -> if (runWorkerFirst.isNotEmpty()) assetsConfig["run_worker_first"] = runWorkerFirst
                    is Boolean -> if (runWorkerFirst) assetsConfig["run_worker_first"] = true
                }
            }

            assetsNotFoundHandling.getOrNull()?.let { handling ->
                if (handling.isNotEmpty() && handling != NotFoundHandling.NONE.value) {
                    val validValues = NotFoundHandling.values().map { it.value }
                    require(handling in validValues) {
                        "Invalid not_found_handling value: '$handling'. Must be one of: ${validValues.joinToString(", ")}"
                    }
                    assetsConfig["not_found_handling"] = handling
                }
            }

            if (assetsConfig.isNotEmpty()) {
                wranglerConfig["assets"] = assetsConfig
            }
        }

        configFile.parentFile.mkdirs()
        configFile.writeText(
            JsonOutput.prettyPrint(JsonOutput.toJson(wranglerConfig)) + "\n"
        )

        val deployConfig = deployConfigFile.get().asFile
        val configPath = deployConfig.parentFile.toPath()
            .relativize(configFile.toPath())
            .toString()
            .replace('\\', '/')

        deployConfig.parentFile.mkdirs()
        deployConfig.writeText(
            JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("configPath" to configPath))) + "\n"
        )
    }
}