Normally no local JARs are required because build.gradle resolves dependencies from Maven.
If CurseMaven is unavailable, put the Re-Avaritia 1.20.1 JAR here and replace the
CurseMaven dependency in build.gradle with:

compileOnly fg.deobf(files('libs/Re-Avaritia-forge-1.20.1-1.4.1-release.jar'))
runtimeOnly fg.deobf(files('libs/Re-Avaritia-forge-1.20.1-1.4.1-release.jar'))
