package cloudflare

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the Cloudflare Workers adapter.
 */
abstract class CloudflareWorkerExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Fully qualified name of the exported Kotlin `fetch` function.
     * Only the last segment is used, because Kotlin/JS exports the function name, not its Kotlin FQN.
     */
    val fetchHandler: Property<String> = objects.property(String::class.java)

    /** The compiled Kotlin/JS module that exports the handler. */
    val moduleFile: RegularFileProperty = objects.fileProperty()

    /** The generated Cloudflare Workers entry point. */
    val outputFile: RegularFileProperty = objects.fileProperty()

    /** Wrangler configuration generated from the Kotlin DSL. */
    val wranglerConfig: WranglerExtension = objects.newInstance(WranglerExtension::class.java)

    fun wrangler(action: Action<WranglerExtension>) {
        action.execute(wranglerConfig)
    }
}
