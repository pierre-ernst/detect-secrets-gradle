group = "com.github.pierre-ernst"
version = "1.1-SNAPSHOT"

// Settings for detect-secrets
val detectSecret = mapOf(
        "baselineFile" to ".secrets.baseline.json",
        "exclude" to "package-lock.json"
)

plugins {
    id("groovy")
}

repositories {
    mavenCentral()
}

/**
 * Run external process $git status --porcelain=v1
 * and collect its output to a list of comma-separated file paths
 *
 * This is used to know which local files have changed and should be scanned
 */
fun gitHistory(): String {
    var args = ""

    Runtime.getRuntime().exec(arrayOf(
            "/usr/bin/git", "status", "--porcelain=v1"))
            .inputStream.reader().buffered()
            .lines()
            .forEach { line ->
                if (line.trim().split(" ").size == 2) {
                    val status = line.trim().split(" ")[0].trim()
                    val file = line.trim().split(" ")[1]
                    /**
                     *
                     * ' ' = unmodified
                     * ? = untracked
                     * M = modified
                     * A = added
                     * D = deleted
                     * R = renamed
                     * C = copied
                     * U = updated but unmerged
                     */
                    if (status.startsWith("M") || status.startsWith("A") || status.startsWith("U")) {
                        args += ", '$file'"
                    }
                }
            }

    return args
}

/**
 * Downloads the required docker image
 */
tasks.register<Exec>("dockerPull") {
    commandLine("docker", "pull", "clmdevops/detect-secrets")
}

/**
 * Run main() from detect_secrets.pre_commit_hook.py with:
 *  - output from gitHistory()
 *  - const values settings
 * and propagate its return value to PASS/FAIL the gradle task
 */
tasks.register<Exec>("detectSecrets") {
    dependsOn("dockerPull")
    description = "Run detect-secrets python module to scan modified files for hard-coded secrets (passwords, keys, ...)"

    val cwd = project.rootProject.projectDir.absolutePath
    val pythonBootstrapCode = "import sys; import os; from detect_secrets.pre_commit_hook import main; os.chdir('/source');" +
            "sys.exit(main(['--exclude-files', '${detectSecret["exclude"]}', " +
            "'--no-keyword-scan', '--baseline', '${detectSecret["baselineFile"]}'" +
            "${gitHistory()}]))"

    commandLine("docker", "run",
            "-i", "-a", "stdout", "-a", "stderr",
            "--mount", "type=bind,source=$cwd,target=/source,readonly",
            "clmdevops/detect-secrets"
    )

    doFirst {
        standardInput = pythonBootstrapCode.toByteArray().inputStream()
    }
}
