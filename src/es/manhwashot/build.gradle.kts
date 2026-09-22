import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "ManhwaShot"
    lang = "es"
    versionCode = 1
    contentWarning = ContentWarning.NSFW
    libVersion = "1.4"

    sources {
        source {
            baseUrl {
                custom("https://manhwashot.lat")
            }
            versionId = 1
        }
    }
}
