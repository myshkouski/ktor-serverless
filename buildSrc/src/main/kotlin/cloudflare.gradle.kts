import cloudflare.CloudflareWorkerExtension

val cloudflare = extensions.create<CloudflareWorkerExtension>("cloudflareWorker")

cloudflare.wranglerConfig.name.convention(project.name)
cloudflare.wranglerConfig.keepVars.convention(true)
