package cloudflare

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.lang.ProcessBuilder.Redirect

/**
 * Runs `wrangler dev` for local development.
 */
abstract class WranglerDevTask : DefaultTask() {

    @get:InputDirectory
    abstract val workingDir: DirectoryProperty

    @get:Input
    abstract val env: MapProperty<String, String>

    @TaskAction
    fun dev() {
        val dir = workingDir.get().asFile
        require(dir.isDirectory) {
            "Working directory not found: ${dir.path}. Run 'generateWranglerConfig' first or ensure the project is built."
        }

        val processBuilder = ProcessBuilder("npx", "wrangler", "dev")
            .directory(dir)
            .redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT)

        env.get().forEach { entry ->
            processBuilder.environment()[entry.key] = entry.value
        }

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        require(exitCode == 0) {
            "wrangler dev exited with code $exitCode"
        }
    }
}