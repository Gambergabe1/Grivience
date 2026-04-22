$file = 'C:\Users\Kazutos-PC\IdeaProjects\Grivience\src\main\java\io\papermc\Grivience\GriviencePlugin.java'

# Read all lines as array
$lines = [System.IO.File]::ReadAllLines($file)
$newLines = [System.Collections.Generic.List[string]]::new()

$fieldAdded = $false
$initAdded = $false
$shutdownAdded = $false
$getterAdded = $false

$i = 0
while ($i -lt $lines.Length) {
    $line = $lines[$i]
    
    # 1. Add fields after collectionsManager declaration
    if (-not $fieldAdded -and $line.Trim() -eq "private CollectionsManager collectionsManager;") {
        $newLines.Add($line)
        $newLines.Add("    private MinehubHeartManager minehubHeartManager;")
        $newLines.Add("    private MinehubCommissionManager minehubCommissionManager;")
        $fieldAdded = $true
        Write-Host "Added field declarations at line $($i+1)"
        $i++
        continue
    }
    
    # 2. Add initialization before enchantment system init
    if (-not $initAdded -and $line.Trim() -eq "// Initialize enchantment system (Skyblock accurate)") {
        $newLines.Add("        // Initialize minehub heart and commission system")
        $newLines.Add("        minehubHeartManager = new MinehubHeartManager(this, collectionsManager);")
        $newLines.Add("        getServer().getPluginManager().registerEvents(minehubHeartManager, this);")
        $newLines.Add("        minehubCommissionManager = new MinehubCommissionManager(this, collectionsManager, minehubHeartManager);")
        $newLines.Add("        getServer().getPluginManager().registerEvents(minehubCommissionManager, this);")
        $newLines.Add("        MinehubHeartCommand minehubHeartCommand = new MinehubHeartCommand(this, minehubHeartManager, minehubCommissionManager);")
        $newLines.Add("        registerCommand(`"hotm`", minehubHeartCommand);")
        $newLines.Add("        getLogger().info(`"Minehub Heart and Commission system enabled.`");")
        $newLines.Add("")
        $initAdded = $true
        Write-Host "Added initialization code at line $($i+1)"
    }
    
    # 3. Add shutdown before heartOfTheEndMinesManager shutdown
    if (-not $shutdownAdded -and $line.Trim() -eq "if (heartOfTheEndMinesManager != null) {") {
        $newLines.Add("        if (minehubHeartManager != null) {")
        $newLines.Add("            minehubHeartManager.shutdown();")
        $newLines.Add("        }")
        $newLines.Add("        if (minehubCommissionManager != null) {")
        $newLines.Add("            minehubCommissionManager.shutdown();")
        $newLines.Add("        }")
        $shutdownAdded = $true
        Write-Host "Added shutdown logic at line $($i+1)"
    }
    
    # 4. Add getters after getCollectionsManager method
    if (-not $getterAdded -and $line.Trim() -eq "return collectionsManager;") {
        $newLines.Add($line)
        # Check if next line is closing brace
        if ($i + 1 -lt $lines.Length -and $lines[$i+1].Trim() -eq "}") {
            $newLines.Add($lines[$i+1])
            $newLines.Add("")
            $newLines.Add("    public MinehubHeartManager getMinehubHeartManager() {")
            $newLines.Add("        return minehubHeartManager;")
            $newLines.Add("    }")
            $newLines.Add("")
            $newLines.Add("    public MinehubCommissionManager getMinehubCommissionManager() {")
            $newLines.Add("        return minehubCommissionManager;")
            $newLines.Add("    }")
            $getterAdded = $true
            Write-Host "Added getter methods at line $($i+1)"
            $i += 2
            continue
        }
    }
    
    $newLines.Add($line)
    $i++
}

if ($fieldAdded -and $initAdded -and $shutdownAdded -and $getterAdded) {
    Write-Host "All additions successful!"
    [System.IO.File]::WriteAllLines($file, $newLines.ToArray())
    Write-Host "File saved successfully"
} else {
    Write-Host "ERROR: Not all additions succeeded"
    Write-Host "Field: $fieldAdded, Init: $initAdded, Shutdown: $shutdownAdded, Getter: $getterAdded"
}
