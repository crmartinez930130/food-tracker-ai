plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
}

// The project version (phone app, watch app, backend) lives in ../version.properties as MAJOR.MINOR.PATCH.
// Parsed once here; the apps take versionName/versionCode from these extras and :shared generates the
// AppVersion constants from them. Keep the code formula in sync with versionCodeOf in :shared.
val appVersionName: String = run {
    val file = rootProject.layout.projectDirectory.file("../version.properties")
    val text = providers.fileContents(file).asText.orNull
        ?: throw GradleException("Missing ${file.asFile}: it must hold the project version, e.g. version=0.2.0")
    val value = Regex("""^\s*version\s*=\s*(\S+)\s*$""", RegexOption.MULTILINE).find(text)?.groupValues?.get(1)
        ?: throw GradleException("${file.asFile} has no `version=` line, e.g. version=0.2.0")
    value
}
val appVersionCode: Int = run {
    val parts = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(appVersionName)?.destructured?.toList()?.map { it.toInt() }
    if (parts == null || parts[1] > 99 || parts[2] > 99) {
        throw GradleException(
            "Invalid version \"$appVersionName\" in version.properties: expected MAJOR.MINOR.PATCH with whole " +
                "numbers and MINOR and PATCH from 0 to 99, e.g. 0.2.0",
        )
    }
    parts[0] * 10000 + parts[1] * 100 + parts[2]
}
extra["appVersionName"] = appVersionName
extra["appVersionCode"] = appVersionCode
