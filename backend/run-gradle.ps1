param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$ErrorActionPreference = "Stop"

$env:JAVA_HOME = (Resolve-Path ".tools/jdk21").Path
$env:GRADLE_USER_HOME = "$PSScriptRoot\.tools\gradle-user-home"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_OPTS = "-Dorg.gradle.native=false -Dorg.gradle.console=plain"

New-Item -ItemType Directory -Force -Path $env:GRADLE_USER_HOME | Out-Null

if (-not $Args -or $Args.Count -eq 0) {
    $Args = @("compileJava")
}

& "$PSScriptRoot\gradlew.bat" --no-daemon --console=plain @Args
