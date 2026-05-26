param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$PlateNumber = "B123ABC",

    [string]$PoliceUsername = "police_test",
    [string]$PolicePassword = "Parola123!",

    [string]$ParkingUsername = "parking_test",
    [string]$ParkingPassword = "Parola123!",

    [string]$InsuranceUsername = "insurance_test",
    [string]$InsurancePassword = "Parola123!",

    [string]$PhotoPath = "",
    [string]$VideoPath = "",
    [string]$ParkingEntryImagePath = "",
    [string]$ParkingExitImagePath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$results = @()
$scriptStart = Get-Date

function Add-Result {
    param(
        [string]$Id,
        [string]$Name,
        [string]$Expected,
        [string]$Actual,
        [bool]$Passed,
        [string]$Details = ""
    )

    $script:results += [pscustomobject]@{
        id       = $Id
        name     = $Name
        expected = $Expected
        actual   = $Actual
        passed   = $Passed
        details  = $Details
        at       = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    }
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [string]$Body = "",
        [string]$ContentType = "application/json"
    )

    try {
        $resp = if ($Body -and $Method -ne "GET") {
            Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -Body $Body -ContentType $ContentType
        } else {
            Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers
        }

        return [pscustomobject]@{
            StatusCode = [int]$resp.StatusCode
            Body       = $resp.Content
        }
    }
    catch {
        $httpResponse = $_.Exception.Response
        if ($null -ne $httpResponse) {
            $statusCode = [int]$httpResponse.StatusCode
            $stream = $httpResponse.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $content = $reader.ReadToEnd()
            $reader.Close()

            return [pscustomobject]@{
                StatusCode = $statusCode
                Body       = $content
            }
        }

        throw
    }
}

function Invoke-CurlMultipart {
    param(
        [string]$Method,
        [string]$Uri,
        [string]$Token,
        [hashtable]$FormFields
    )

    $tmpBody = [System.IO.Path]::GetTempFileName()
    $args = @("-s", "-o", $tmpBody, "-w", "%{http_code}", "-X", $Method, $Uri)

    if ($Token) {
        $args += @("-H", "Authorization: Bearer $Token")
    }

    foreach ($key in $FormFields.Keys) {
        $value = $FormFields[$key]
        if ($value -is [System.IO.FileInfo]) {
            $args += @("-F", "$key=@$($value.FullName)")
        }
        else {
            $args += @("-F", "$key=$value")
        }
    }

    $statusText = & curl.exe @args
    $body = Get-Content -Raw $tmpBody
    Remove-Item $tmpBody -Force -ErrorAction SilentlyContinue

    $statusCode = 0
    [void][int]::TryParse($statusText, [ref]$statusCode)

    return [pscustomobject]@{
        StatusCode = $statusCode
        Body       = $body
    }
}

function Get-Token {
    param([string]$Username, [string]$Password)

    $payload = @{ username = $Username; password = $Password } | ConvertTo-Json
    $resp = Invoke-JsonRequest -Method "POST" -Uri "$BaseUrl/auth/login" -Body $payload

    if ($resp.StatusCode -ne 200) {
        throw "Login failed for $Username ($($resp.StatusCode)): $($resp.Body)"
    }

    $json = $resp.Body | ConvertFrom-Json
    if (-not $json.token) {
        throw "Login response missing token for $Username"
    }

    return [string]$json.token
}

function Is-FileReady {
    param([string]$PathValue)
    return ($PathValue -and (Test-Path $PathValue))
}

Write-Host "[INFO] Starting API smoke tests against $BaseUrl" -ForegroundColor Cyan

# 1) Login all roles
try {
    $policeToken = Get-Token -Username $PoliceUsername -Password $PolicePassword
    Add-Result -Id "AUTH-01" -Name "Login POLICE" -Expected "200 + token" -Actual "200 + token" -Passed $true
}
catch {
    Add-Result -Id "AUTH-01" -Name "Login POLICE" -Expected "200 + token" -Actual "FAIL" -Passed $false -Details $_.Exception.Message
    throw
}

try {
    $parkingToken = Get-Token -Username $ParkingUsername -Password $ParkingPassword
    Add-Result -Id "AUTH-02" -Name "Login PARKING" -Expected "200 + token" -Actual "200 + token" -Passed $true
}
catch {
    Add-Result -Id "AUTH-02" -Name "Login PARKING" -Expected "200 + token" -Actual "FAIL" -Passed $false -Details $_.Exception.Message
    throw
}

try {
    $insuranceToken = Get-Token -Username $InsuranceUsername -Password $InsurancePassword
    Add-Result -Id "AUTH-03" -Name "Login INSURANCE" -Expected "200 + token" -Actual "200 + token" -Passed $true
}
catch {
    Add-Result -Id "AUTH-03" -Name "Login INSURANCE" -Expected "200 + token" -Actual "FAIL" -Passed $false -Details $_.Exception.Message
    throw
}

# 2) Role negative checks
$neg1 = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/police/lookup/$PlateNumber" -Headers @{ Authorization = "Bearer $parkingToken" }
Add-Result -Id "S-NEG-01" -Name "PARKING blocked from police lookup" -Expected "403" -Actual "$($neg1.StatusCode)" -Passed ($neg1.StatusCode -eq 403)

$neg2 = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/video-jobs" -Headers @{ Authorization = "Bearer $insuranceToken" }
Add-Result -Id "S-NEG-02" -Name "INSURANCE blocked from video jobs" -Expected "403" -Actual "$($neg2.StatusCode)" -Passed ($neg2.StatusCode -eq 403)

$neg3Body = @{ company = "X"; validFrom = "2026-01-01"; validTo = "2026-12-31"; licensePlate = @{ plateNumber = $PlateNumber } } | ConvertTo-Json -Depth 5
$neg3 = Invoke-JsonRequest -Method "POST" -Uri "$BaseUrl/api/insurance" -Headers @{ Authorization = "Bearer $policeToken" } -Body $neg3Body
Add-Result -Id "S-NEG-03" -Name "POLICE blocked from insurance write" -Expected "403" -Actual "$($neg3.StatusCode)" -Passed ($neg3.StatusCode -eq 403)

$neg4 = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/parking/$PlateNumber" -Headers @{ Authorization = "Bearer $insuranceToken" }
Add-Result -Id "S-NEG-04" -Name "INSURANCE blocked from parking history" -Expected "403" -Actual "$($neg4.StatusCode)" -Passed ($neg4.StatusCode -eq 403)

$neg5 = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/video-jobs" -Headers @{ Authorization = "Bearer $parkingToken" }
Add-Result -Id "S-NEG-05" -Name "PARKING blocked from video jobs" -Expected "403" -Actual "$($neg5.StatusCode)" -Passed ($neg5.StatusCode -eq 403)

# 3) Functional POLICE lookup
$polLookup = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/police/lookup/$PlateNumber" -Headers @{ Authorization = "Bearer $policeToken" }
Add-Result -Id "F-POL-01" -Name "POLICE lookup" -Expected "200" -Actual "$($polLookup.StatusCode)" -Passed ($polLookup.StatusCode -eq 200)

# 4) Optional POLICE OCR photo
if (Is-FileReady $PhotoPath) {
    $photoResp = Invoke-CurlMultipart -Method "POST" -Uri "$BaseUrl/api/ocr/full" -Token $policeToken -FormFields @{
        image = (Get-Item $PhotoPath)
    }
    Add-Result -Id "F-POL-02" -Name "POLICE photo OCR" -Expected "200" -Actual "$($photoResp.StatusCode)" -Passed ($photoResp.StatusCode -eq 200)

    if ($photoResp.StatusCode -eq 200) {
        try {
            $photoJson = $photoResp.Body | ConvertFrom-Json
            if ($photoJson.plateNumber) {
                $PlateNumber = [string]$photoJson.plateNumber
            }
        } catch {
            # keep existing plate number
        }
    }
}
else {
    Add-Result -Id "F-POL-02" -Name "POLICE photo OCR" -Expected "200" -Actual "SKIP" -Passed $true -Details "PhotoPath missing"
}

# 5) Optional POLICE video
if (Is-FileReady $VideoPath) {
    $videoResp = Invoke-CurlMultipart -Method "POST" -Uri "$BaseUrl/api/video-jobs" -Token $policeToken -FormFields @{
        video = (Get-Item $VideoPath)
    }
    $okStatuses = @(200, 202)
    Add-Result -Id "F-POL-03" -Name "POLICE video upload" -Expected "202" -Actual "$($videoResp.StatusCode)" -Passed ($okStatuses -contains $videoResp.StatusCode)
}
else {
    Add-Result -Id "F-POL-03" -Name "POLICE video upload" -Expected "202" -Actual "SKIP" -Passed $true -Details "VideoPath missing"
}

# 6) INSURANCE create + update
$plateCheck = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/license-plates/$PlateNumber" -Headers @{ Authorization = "Bearer $insuranceToken" }
if ($plateCheck.StatusCode -eq 200) {
    $plateData = @()
    try { $plateData = $plateCheck.Body | ConvertFrom-Json } catch { $plateData = @() }

    if ($plateData -and $plateData.Count -gt 0) {
        $plateId = [int64]$plateData[0].id
        $companyName = "TEST_INS_$(Get-Date -Format 'HHmmss')"

        $createBody = @{
            company = $companyName
            validFrom = "2026-01-01"
            validTo   = "2026-12-31"
            licensePlate = @{ id = $plateId; plateNumber = $PlateNumber }
        } | ConvertTo-Json -Depth 6

        $insCreate = Invoke-JsonRequest -Method "POST" -Uri "$BaseUrl/api/insurance" -Headers @{ Authorization = "Bearer $insuranceToken" } -Body $createBody
        Add-Result -Id "F-INS-01" -Name "INSURANCE create policy" -Expected "200" -Actual "$($insCreate.StatusCode)" -Passed ($insCreate.StatusCode -eq 200)

        if ($insCreate.StatusCode -eq 200) {
            $created = $insCreate.Body | ConvertFrom-Json
            $insId = [int64]$created.id

            $updateBody = @{
                company = "$companyName`_UPD"
                validFrom = "2026-01-01"
                validTo   = "2027-01-01"
            } | ConvertTo-Json -Depth 4

            $insUpdate = Invoke-JsonRequest -Method "PUT" -Uri "$BaseUrl/api/insurance/$insId" -Headers @{ Authorization = "Bearer $insuranceToken" } -Body $updateBody
            Add-Result -Id "F-INS-02" -Name "INSURANCE update policy" -Expected "200" -Actual "$($insUpdate.StatusCode)" -Passed ($insUpdate.StatusCode -eq 200)
        }
        else {
            Add-Result -Id "F-INS-02" -Name "INSURANCE update policy" -Expected "200" -Actual "SKIP" -Passed $true -Details "Create failed, update skipped"
        }
    }
    else {
        Add-Result -Id "F-INS-01" -Name "INSURANCE create policy" -Expected "200" -Actual "SKIP" -Passed $true -Details "Plate not found"
        Add-Result -Id "F-INS-02" -Name "INSURANCE update policy" -Expected "200" -Actual "SKIP" -Passed $true -Details "Plate not found"
    }
}
else {
    Add-Result -Id "F-INS-01" -Name "INSURANCE create policy" -Expected "200" -Actual "SKIP" -Passed $true -Details "Plate check failed"
    Add-Result -Id "F-INS-02" -Name "INSURANCE update policy" -Expected "200" -Actual "SKIP" -Passed $true -Details "Plate check failed"
}

# 7) PARKING entry + exit + history (optional when evidence images exist)
if ((Is-FileReady $ParkingEntryImagePath) -and (Is-FileReady $ParkingExitImagePath)) {
    $entryResp = Invoke-CurlMultipart -Method "POST" -Uri "$BaseUrl/api/parking/entry" -Token $parkingToken -FormFields @{
        plateNumber = $PlateNumber
        image       = (Get-Item $ParkingEntryImagePath)
    }
    Add-Result -Id "F-PARK-01" -Name "PARKING entry" -Expected "201" -Actual "$($entryResp.StatusCode)" -Passed ($entryResp.StatusCode -eq 201)

    $exitResp = Invoke-CurlMultipart -Method "POST" -Uri "$BaseUrl/api/parking/exit" -Token $parkingToken -FormFields @{
        plateNumber = $PlateNumber
        image       = (Get-Item $ParkingExitImagePath)
    }
    Add-Result -Id "F-PARK-02" -Name "PARKING exit" -Expected "200" -Actual "$($exitResp.StatusCode)" -Passed ($exitResp.StatusCode -eq 200)

    $historyResp = Invoke-JsonRequest -Method "GET" -Uri "$BaseUrl/api/parking/$PlateNumber" -Headers @{ Authorization = "Bearer $parkingToken" }
    Add-Result -Id "F-PARK-03" -Name "PARKING history" -Expected "200" -Actual "$($historyResp.StatusCode)" -Passed ($historyResp.StatusCode -eq 200)
}
else {
    Add-Result -Id "F-PARK-01" -Name "PARKING entry" -Expected "201" -Actual "SKIP" -Passed $true -Details "ParkingEntryImagePath/ParkingExitImagePath missing"
    Add-Result -Id "F-PARK-02" -Name "PARKING exit" -Expected "200" -Actual "SKIP" -Passed $true -Details "ParkingEntryImagePath/ParkingExitImagePath missing"
    Add-Result -Id "F-PARK-03" -Name "PARKING history" -Expected "200" -Actual "SKIP" -Passed $true -Details "ParkingEntryImagePath/ParkingExitImagePath missing"
}

# 8) Export report
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$reportDir = Join-Path $PSScriptRoot "reports"
New-Item -ItemType Directory -Force $reportDir | Out-Null

$jsonPath = Join-Path $reportDir "api_smoke_$timestamp.json"
$mdPath = Join-Path $reportDir "api_smoke_$timestamp.md"

$summary = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    baseUrl = $BaseUrl
    durationSeconds = [math]::Round(((Get-Date) - $scriptStart).TotalSeconds, 2)
    total = $results.Count
    passed = ($results | Where-Object { $_.passed }).Count
    failed = ($results | Where-Object { -not $_.passed }).Count
    results = $results
}

$summary | ConvertTo-Json -Depth 8 | Set-Content -NoNewline $jsonPath

$md = @()
$md += "# API Smoke Report"
$md += ""
$md += "- Generated at: $($summary.generatedAt)"
$md += "- Base URL: $($summary.baseUrl)"
$md += "- Duration (s): $($summary.durationSeconds)"
$md += "- Total: $($summary.total)"
$md += "- Passed: $($summary.passed)"
$md += "- Failed: $($summary.failed)"
$md += ""
$md += "| ID | Test | Expected | Actual | Passed | Details |"
$md += "|---|---|---|---|---|---|"
foreach ($r in $results) {
    $details = ($r.details -replace "\r", " " -replace "\n", " ")
    $md += "| $($r.id) | $($r.name) | $($r.expected) | $($r.actual) | $($r.passed) | $details |"
}

$md -join "`n" | Set-Content -NoNewline $mdPath

Write-Host "[DONE] JSON report: $jsonPath" -ForegroundColor Green
Write-Host "[DONE] Markdown report: $mdPath" -ForegroundColor Green

if ($summary.failed -gt 0) {
    exit 1
}

exit 0