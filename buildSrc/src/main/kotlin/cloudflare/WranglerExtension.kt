package cloudflare

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class WranglerExtension @Inject constructor(objects: ObjectFactory) {
    val name: Property<String> = objects.property(String::class.java)
    val compatibilityDate: Property<String> = objects.property(String::class.java)
    val compatibilityFlags: ListProperty<String> = objects.listProperty(String::class.java)
    val keepVars: Property<Boolean> = objects.property(Boolean::class.java)
    val vars: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
    val kvNamespaces: ListProperty<String> = objects.listProperty(String::class.java)

    fun kv(binding: String) {
        val valid = binding.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))
        require(valid) { "The Cloudflare KV binding '$binding' must be a valid JavaScript identifier." }
        val notRegistered = binding !in kvNamespaces.getOrElse(emptyList())
        require(notRegistered) { "The Cloudflare KV binding '$binding' is already registered." }
        kvNamespaces.add(binding)
    }

    val outputFile: RegularFileProperty = objects.fileProperty()
    val deployConfigFile: RegularFileProperty = objects.fileProperty()

    val assets: AssetsExtension = objects.newInstance(AssetsExtension::class.java)

    fun assets(action: Action<AssetsExtension>) {
        action.execute(assets)
    }
}

enum class NotFoundHandling(val value: String) {
    NONE("none"),
    SINGLE_PAGE_APPLICATION("single-page-application"),
    PAGE_404("404-page");
}

abstract class AssetsExtension @Inject constructor(objects: ObjectFactory) {
    val directory: Property<String> = objects.property(String::class.java)
    val binding: Property<String> = objects.property(String::class.java)
    val runWorkerFirst: Property<Any> = objects.property(Any::class.java).convention(null)
    // Internal string property for Gradle compatibility
    private val _notFoundHandling: Property<String> = objects.property(String::class.java).convention(NotFoundHandling.NONE.value)

    fun directory(dir: String) {
        directory.set(dir)
    }

    fun binding(name: String) {
        val valid = name.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))
        require(valid) { "The assets binding '$name' must be a valid JavaScript identifier." }
        binding.set(name)
    }

    fun runWorkerFirst() {
        runWorkerFirst.set(true)
    }

    fun runWorkerFirst(patterns: Collection<String>) {
        runWorkerFirst.set(patterns)
    }

    /** Sets not found handling using enum (type-safe) */
    fun notFoundHandling(handling: NotFoundHandling) {
        _notFoundHandling.set(handling.value)
    }

    /** Sets not found handling using string */
    fun notFoundHandling(handling: String) {
        val enumValue = NotFoundHandling.values().firstOrNull { it.value == handling }
            ?: error("Invalid not_found_handling: '$handling'. Valid: ${NotFoundHandling.values().joinToString { it.value }}")
        _notFoundHandling.set(enumValue.value)
    }

    /** Gets the current not found handling value */
    val notFoundHandling: Property<String> get() = _notFoundHandling
}