import ru.vyarus.gradle.plugin.python.task.PythonTask

group = "com.github.pierre-ernst"
version = "1.0-SNAPSHOT"

// Settings for detect-secrets
val detectSecret = mapOf(
        "baselineFile" to ".secrets.baseline.json",
        "entropyThreshold" to 4
)

dependencies {
    implementation("org.codehaus.groovy:groovy-all:3.0.4") {
        // https://groovy-lang.org/releasenotes/groovy-3.0.html#Groovy3.0releasenotes-3.0.4
        exclude("org.codehaus.groovy:groovy-testng")
    }
}

plugins {
    id("groovy")
    id("ru.vyarus.use-python") version "2.2.0"
}

// Import detect-secrets Python module with PythonTask Gradle plugin
python {
    pip("detect-secrets:0.13.1")
}

repositories {
    mavenCentral()
}

/**
 * Run external process $git diff --name-only
 * and collect its output to a list of comma-separated file paths
 *
 * This is used to know which local files have changed and should be scanned
 */
fun gitHistory(): String {
    var args = ""

    Runtime.getRuntime().exec(arrayOf(
            "/usr/bin/git", "diff", "--name-only")) // --staged
            .inputStream.reader().buffered()
            .lines()
            .forEach { file ->
                args += ", '$file'"
            }

    return args
}

/**
 * Run main() from detect_secrets.pre_commit_hook.py with:
 *  - output from gitHistory()
 *  - const values settings
 * and propagate its return value to PASS/FAIL the gradle task
 */
tasks.register("detectSecrets", PythonTask::class) {
    dependsOn("assemble")
    description = "Run detect-secrets python module to scan modified files for hard-coded secrets (passwords, keys, ...)"

    command = "-c \"import sys; from detect_secrets.pre_commit_hook import main; sys.exit(main([" +
            "'--base64-limit', '${detectSecret["entropyThreshold"]}', '--hex-limit', '${detectSecret["entropyThreshold"]}', " +
            "'--no-keyword-scan', '--baseline', '${detectSecret["baselineFile"]}'" +
            "${gitHistory()}" +
            "]))\""
}
