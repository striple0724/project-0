param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$ErrorActionPreference = "Stop"

$env:JAVA_HOME = (Resolve-Path ".tools/jdk21").Path
$mavenBin = (Resolve-Path ".tools/apache-maven-3.9.11/bin").Path
$env:PATH = "$env:JAVA_HOME\bin;$mavenBin;$env:PATH"

if (-not $Args -or $Args.Count -eq 0) {
    $Args = @("-DskipTests", "compile")
}

& "mvn" @Args
