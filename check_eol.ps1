$filePath = 'C:\Users\Kazutos-PC\IdeaProjects\Grivience\src\main\java\io\papermc\Grivience\GriviencePlugin.java'
$rawBytes = [System.IO.File]::ReadAllBytes($filePath)
$crCount = 0
$lfCount = 0
foreach ($b in $rawBytes[0..2000]) {
    if ($b -eq 13) { $crCount++ }
    if ($b -eq 10) { $lfCount++ }
}
Write-Host "CR: $crCount, LF: $lfCount"
if ($crCount -gt 0) { Write-Host "CRLF" } else { Write-Host "LF only" }
