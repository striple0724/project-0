param(
    [ValidateSet("gradle", "maven", "both")]
    [string]$Tool = "both"
)

$ErrorActionPreference = "Stop"

$env:JAVA_HOME = (Resolve-Path ".tools/jdk21").Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = "$PSScriptRoot\.tools\gradle-user-home"
$env:GRADLE_OPTS = "-Dorg.gradle.native=false -Dorg.gradle.console=plain"

New-Item -ItemType Directory -Force -Path $env:GRADLE_USER_HOME | Out-Null

function Run-GradleChecks {
    Write-Host "[verify] Gradle compile/test 시작"
    & "$PSScriptRoot\gradlew.bat" --no-daemon --console=plain clean compileJava test
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle verification failed with exit code $LASTEXITCODE"
    }
    Write-Host "[verify] Gradle compile/test 완료"
}

function Run-MavenChecks {
    $mavenBin = (Resolve-Path ".tools/apache-maven-3.9.11/bin").Path
    $env:PATH = "$env:JAVA_HOME\bin;$mavenBin;$env:PATH"
    Write-Host "[verify] Maven compile/test 시작"
    & mvn -DskipTests=false clean test
    if ($LASTEXITCODE -ne 0) {
        throw "Maven verification failed with exit code $LASTEXITCODE"
    }
    Write-Host "[verify] Maven compile/test 완료"
}

switch ($Tool) {
    "gradle" { Run-GradleChecks }
    "maven" { Run-MavenChecks }
    "both" {
        Run-GradleChecks
        Run-MavenChecks
    }
}

Write-Host "[verify] 전체 검증 성공"
