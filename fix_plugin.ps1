$filePath = 'C:\Users\Kazutos-PC\IdeaProjects\Grivience\src\main\java\io\papermc\Grivience\GriviencePlugin.java'
$content = [System.IO.File]::ReadAllText($filePath)

# Detect line ending
$nl = "`n"
if ($content.Contains("`r`n")) {
    $nl = "`r`n"
}

# 1. Add field declarations
$old = "private HeartOfTheEndMinesManager heartOfTheEndMinesManager;" + $nl + "    private EndMinesConvergenceManager endMinesConvergenceManager;"
$new = "private HeartOfTheEndMinesManager heartOfTheEndMinesManager;" + $nl + "    private MinehubHeartManager minehubHeartManager;" + $nl + "    private MinehubCommissionManager minehubCommissionManager;" + $nl + "    private EndMinesConvergenceManager endMinesConvergenceManager;"
if ($content.Contains($old)) {
    $content = $content.Replace($old, $new)
    Write-Host "Fields added"
} else {
    Write-Host "WARNING: Could not find field insertion point"
}

# 2. Add initialization and command registration before enchantment system
$old2 = "        // Initialize enchantment system (Skyblock accurate)" + $nl + "        enchantmentManager = new EnchantmentManager(this);"
$new2 = "        // Initialize Heart of the Minehub" + $nl + "        minehubHeartManager = new MinehubHeartManager(this, collectionsManager);" + $nl + "        getServer().getPluginManager().registerEvents(minehubHeartManager, this);" + $nl + "        minehubCommissionManager = new MinehubCommissionManager(this, collectionsManager, minehubHeartManager);" + $nl + "        getServer().getPluginManager().registerEvents(minehubCommissionManager, this);" + $nl + "        MinehubHeartCommand minehubHeartCommand = new MinehubHeartCommand(this, minehubHeartManager, minehubCommissionManager);" + $nl + "        registerCommand(`"hotm`", minehubHeartCommand);" + $nl + $nl + "        // Initialize enchantment system (Skyblock accurate)" + $nl + "        enchantmentManager = new EnchantmentManager(this);"
if ($content.Contains($old2)) {
    $content = $content.Replace($old2, $new2)
    Write-Host "Init and registration added"
} else {
    Write-Host "WARNING: Could not find init insertion point"
}

# 3. Add shutdown logic
$old3 = "        if (heartOfTheEndMinesManager != null) {" + $nl + "            heartOfTheEndMinesManager.shutdown();"
$new3 = "        if (minehubHeartManager != null) {" + $nl + "            minehubHeartManager.shutdown();" + $nl + "        }" + $nl + "        if (minehubCommissionManager != null) {" + $nl + "            minehubCommissionManager.shutdown();" + $nl + "        }" + $nl + "        if (heartOfTheEndMinesManager != null) {" + $nl + "            heartOfTheEndMinesManager.shutdown();"
if ($content.Contains($old3)) {
    $content = $content.Replace($old3, $new3)
    Write-Host "Shutdown logic added"
} else {
    Write-Host "WARNING: Could not find shutdown insertion point"
}

# 4. Add getters
$old4 = "    public HeartOfTheEndMinesManager getHeartOfTheEndMinesManager() {" + $nl + "        return heartOfTheEndMinesManager;" + $nl + "    }"
$new4 = "    public MinehubHeartManager getMinehubHeartManager() {" + $nl + "        return minehubHeartManager;" + $nl + "    }" + $nl + $nl + "    public MinehubCommissionManager getMinehubCommissionManager() {" + $nl + "        return minehubCommissionManager;" + $nl + "    }" + $nl + $nl + "    public HeartOfTheEndMinesManager getHeartOfTheEndMinesManager() {" + $nl + "        return heartOfTheEndMinesManager;" + $nl + "    }"
if ($content.Contains($old4)) {
    $content = $content.Replace($old4, $new4)
    Write-Host "Getters added"
} else {
    Write-Host "WARNING: Could not find getter insertion point"
}

[System.IO.File]::WriteAllText($filePath, $content)
Write-Host "File saved"
