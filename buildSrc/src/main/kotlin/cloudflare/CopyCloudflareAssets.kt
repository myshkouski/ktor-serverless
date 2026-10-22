package cloudflare

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Copies static assets from the source directory to the Cloudflare output directory.
 *
 * This task copies the assets directory configured in the Wrangler extension to the
 * location where Wrangler expects to find them for deployment.
 */
abstract class CopyCloudflareAssets : DefaultTask() {
    @get:Input
    abstract val assetsSourceDir: Property<String>

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @TaskAction
    fun copyAssets() {
        val sourceDirPath = assetsSourceDir.getOrNull()
            ?: return // No assets directory configured, nothing to copy

        val source = sourceDir.get().asFile
        val destination = destinationDir.get().asFile

        if (!source.exists()) {
            logger.lifecycle("Assets source directory does not exist: ${source.path}. Skipping assets copy.")
            return
        }

        if (!source.isDirectory) {
            logger.warn("Assets source path is not a directory: ${source.path}. Skipping assets copy.")
            return
        }

        destination.mkdirs()

        copyDirectory(source, destination)
        logger.lifecycle("Copied assets from ${source.path} to ${destination.path}")
    }

    private fun copyDirectory(source: File, destination: File) {
        source.walkTopDown().forEach { file ->
            val relativePath = source.toPath().relativize(file.toPath())
            val targetFile = destination.toPath().resolve(relativePath).toFile()

            if (file.isDirectory) {
                targetFile.mkdirs()
            } else {
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, true)
            }
        }
    }
}