package cloudflare

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Kotlin DSL for the generated `wrangler.json`.
 */
abstract class WranglerExtension @Inject constructor(objects: ObjectFactory) {
    /** The name of the Cloudflare Worker. */
    val name: Property<String> = objects.property(String::class.java)

    /** Wrangler compatibility date in `YYYY-MM-DD` format. */
    val compatibilityDate: Property<String> = objects.property(String::class.java)

    /** Wrangler compatibility flags, for example `nodejs_compat`. */
    val compatibilityFlags: ListProperty<String> = objects.listProperty(String::class.java)

    /** Whether Wrangler should keep variables defined in the dashboard. */
    val keepVars: Property<Boolean> = objects.property(Boolean::class.java)

    /** Plain-text environment variables exposed to the worker. */
    val vars: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    /** Cloudflare KV namespace bindings. */
    val kvNamespaces: ListProperty<String> = objects.listProperty(String::class.java)

    /**
     * Adds a KV namespace binding, for example `kv("CACHE")`.
     */
    fun kv(binding: String) {
        require(binding.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))) {
            "The Cloudflare KV binding '$binding' must be a valid JavaScript identifier."
        }
        require(binding !in kvNamespaces.getOrElse(emptyList())) {
            "The Cloudflare KV binding '$binding' is already registered."
        }
        kvNamespaces.add(binding)
    }

    /** The generated Wrangler config file. Defaults to `wrangler.json` in the generated `cloudflare` directory. */
    val outputFile: RegularFileProperty = objects.fileProperty()

    /** The Wrangler config discovery file. Defaults to `.wrangler/deploy/config.json`. */
    val deployConfigFile: RegularFileProperty = objects.fileProperty()
}
