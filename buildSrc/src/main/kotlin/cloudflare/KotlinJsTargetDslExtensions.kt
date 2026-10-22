package cloudflare

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

/**
 * Configures Cloudflare Workers for this Kotlin/JS target.
 *
 * The configuration is bound to the production executable binary of the target,
 * so the generated entry point follows the actual Kotlin/JS output location.
 */
fun KotlinJsTargetDsl.cloudflare(action: Action<CloudflareWorkerExtension>) {
    val config = project.extensions.getByType(CloudflareWorkerExtension::class.java)
    action.execute(config)

    binaries.configureEach {
        if (isProductionMainExecutable) {
            registerCloudflareTasks(project, this)
        }
    }
}
