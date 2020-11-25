# detect-secrets-gradle

[detect-secrets](https://github.com/Yelp/detect-secrets) is a Python module for identifying hard-coded secrets in source code (think passwords, secret keys, etc...)
This repo is a POC for triggering an incremental detect-secrets scan from a gradle build task.
The implementation relies on docker (allowing to run Python modules from gradle)

## Prerequisite

1. Only git folders can be scanned (`git init` or cloning an existing repo)
1. A baseline scan of your repo is required before you can use this gradle task, and the results must be committed to a file named `.secrets.baseline.json`:
```
$ docker pull clmdevops/detect-secrets
$ docker run -it -a stdout clmdevops/detect-secrets detect-secrets scan --no-keyword-scan --base64-limit 4 --hex-limit 4 . > .secrets.baseline.json
```

## Usage

Once the baseline scan is completed, you can invoke the `detectSecrets` gradle task to scan the code that has changed in the local git history.
For example, if you add secrets to the [src/main/groovy/EachSecret.groovy](src/main/groovy/EachSecret.groovy) file, the output will look like:
```
$ ./gradlew detectSecrets

> Task :checkPython
Using python 3.7.3 from /Users/pierre/Developer/detect-secrets-gradle/.gradle/python (.gradle/python/bin/python)
Using pip 20.0.2 from /Users/pierre/Developer/detect-secrets-gradle/.gradle/python/lib/python3.7/site-packages/pip (python 3.7)

> Task :detectSecrets FAILED
[python] .gradle/python/bin/python -c exec("import sys; from detect_secrets.pre_commit_hook import main; sys.exit(main(['--no-keyword-scan', '--base64-limit', '4', '--hex-limit', '4', '--baseline', '.secrets.baseline.json', 'src/main/groovy/EachSecret.groovy']))")
         Potential secrets about to be committed to git repo! Please rectify or
         explicitly ignore with an inline `pragma: allowlist secret` comment.
         
         Secret Type: AWS Access Key
         Location:    src/main/groovy/EachSecret.groovy:8
         
         Secret Type: Base64 High Entropy String
         Location:    src/main/groovy/EachSecret.groovy:11
         
         Possible mitigations:
         
           - For information about putting your secrets in a safer place,
             please ask in #security
           - Mark false positives with an inline `pragma: allowlist secret`
             comment
           - Commit with `--no-verify` if this is a one-time false positive
         
         If a secret has already been committed, visit
         https://help.github.com/articles/removing-sensitive-data-from-a-
         repository

FAILURE: Build failed with an exception.
```

### Understanding the Source Code
The main file is [build.gradle.kts](build.gradle.kts).
All the other files are used to trigger the scanner since they contain various secrets

### Running with your own repo
Just copy the portions of [build.gradle.kts](build.gradle.kts) applicable to your project
