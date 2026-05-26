param(
  [string]$ProjectDir = "c:/Users/asus/Desktop/alpro/ocrbackend",
  [string]$Image = "maven:3.9.6-eclipse-temurin-21",
  [switch]$Quiet
)

$cmd = @("run", "--rm", "-v", "${ProjectDir}:/workspace", "-w", "/workspace", $Image)
if ($Quiet) {
  $cmd += @("mvn", "-q", "test")
} else {
  $cmd += @("mvn", "test")
}

Write-Host "Running JUnit in Docker (Java 21)..." -ForegroundColor Cyan
& docker @cmd
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
  Write-Host "[OK] Tests passed." -ForegroundColor Green
  Write-Host "Reports: $ProjectDir/target/surefire-reports" -ForegroundColor Green
} else {
  Write-Host "[FAIL] Tests failed (exit code $exitCode)." -ForegroundColor Red
  Write-Host "Inspect: $ProjectDir/target/surefire-reports" -ForegroundColor Yellow
}

exit $exitCode
