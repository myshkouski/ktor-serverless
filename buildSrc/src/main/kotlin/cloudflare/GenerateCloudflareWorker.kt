package cloudflare

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generates a Cloudflare Workers entry point with a default `fetch` export.
 */
abstract class GenerateCloudflareWorker : DefaultTask() {
    @get:Input
    abstract val fetchHandler: Property<String>

    @get:InputFile
    abstract val moduleFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val handler = fetchHandler.get()
        val exportName = handler.substringAfterLast('.')
        require(exportName.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))) {
            "The fetch handler '$handler' does not end with a valid JavaScript export name."
        }

        val module = moduleFile.get().asFile
        require(module.isFile) {
            "The compiled Kotlin/JS module was not found at ${module.path}."
        }

        val output = outputFile.get().asFile
        val importPath = output.parentFile.toPath()
            .relativize(module.toPath())
            .toString()
            .replace('\\', '/')
            .let { if (it.startsWith(".")) it else "./$it" }

        output.parentFile.mkdirs()
        output.writeText(
            """
            import * as entryPoint from '$importPath';

            const handler = entryPoint['$exportName'];
            if (typeof handler !== 'function') {
                throw new Error(
                    "The Kotlin/JS module '${module.name}' does not export a function named '$exportName'."
                );
            }

            export default {
                async fetch(request, env, ctx) {
                    return handler(request, env, ctx);
                },
            };
            """.trimIndent() + "\n"
        )
    }
}
