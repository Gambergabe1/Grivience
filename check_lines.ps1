$file = 'C:\Users\Kazutos-PC\IdeaProjects\Grivience\src\main\java\io\papermc\Grivience\GriviencePlugin.java'
$bytes = [System.IO.File]::ReadAllBytes($file)
$cr = 0
$lf = 0
$limit = [Math]::Min(5000, $bytes.Length)
for ($i = 0; $i -lt $limit; $i++) {
    if ($bytes[$i] -eq 13) { $cr++ }
    if ($bytes[$i] -eq 10) { $lf++ }
}
Write-Host "CR: $cr, LF: $lf"

# Read as text and show exact line endings around collectionsManager
$lines = [System.IO.File]::ReadAllLines($file)
for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($lines[$i] -match 'private CollectionsManager collectionsManager') {
        Write-Host "Line $($i+1): '$($lines[$i])'"
        if ($i + 1 -lt $lines.Length) {
            Write-Host "Line $($i+2): '$($lines[$i+1])'"
        }
        if ($i + 2 -lt $lines.Length) {
            Write-Host "Line $($i+3): '$($lines[$i+2])'"
        }
        break
    }
}
