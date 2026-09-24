extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.anondev.patches.zotero.extension"
}

dependencies {
    // Compile-only: the app this extension is merged into already ships both.
    compileOnly(libs.gson)
    compileOnly(libs.okhttp)
}
