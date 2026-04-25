$file = 'C:\Users\Kazutos-PC\IdeaProjects\Grivience\src\main\java\io\papermc\Grivience\GriviencePlugin.java'
$content = [System.IO.File]::ReadAllText($file)

# 1. Add field declarations after collectionsManager
$fieldInsertion = "    private CollectionsManager collectionsManager;`r`n"
$fieldAddition = "    private CollectionsManager collectionsManager;`r`n    private MinehubHeartManager minehubHeartManager;`r`n    private MinehubCommissionManager minehubCommissionManager;`r`n"
if ($content.Contains($fieldInsertion)) {
    $content = $content.Replace($fieldInsertion, $fieldAddition, 1)
    Write-Host "Added field declarations"
} else {
    Write-Host "WARNING: Could not find field insertion point"
    # Try to find the line
    $lines = $content -split "`r?`n"
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i].Trim() -eq "private CollectionsManager collectionsManager;") {
            Write-Host "Found at line $($i+1): '$($lines[$i])'"
            if ($i + 1 -lt $lines.Length) {
                Write-Host "Next line: '$($lines[$i+1])'"
            }
            break
        }
    }
}

# 2. Add initialization after collections system init (before enchantment system)
$initSearch = "        // Initialize enchantment system (Skyblock accurate)`r`n        enchantmentManager = new EnchantmentManager(this);"
$initAddition = @"
        // Initialize minehub heart and commission system
        minehubHeartManager = new MinehubHeartManager(this, collectionsManager);
        getServer().getPluginManager().registerEvents(minehubHeartManager, this);
        minehubCommissionManager = new MinehubCommissionManager(this, collectionsManager, minehubHeartManager);
        getServer().getPluginManager().registerEvents(minehubCommissionManager, this);
        MinehubHeartCommand minehubHeartCommand = new MinehubHeartCommand(this, minehubHeartManager, minehubCommissionManager);
        registerCommand("hotm", minehubHeartCommand);
        getLogger().info("Minehub Heart and Commission system enabled.");

        // Initialize enchantment system (Skyblock accurate)
        enchantmentManager = new EnchantmentManager(this);
"@
if ($content.Contains($initSearch)) {
    $content = $content.Replace($initSearch, $initAddition, 1)
    Write-Host "Added initialization code"
} else {
    Write-Host "WARNING: Could not find initialization insertion point"
}

# 3. Add shutdown logic after collectionsManager shutdown (if exists) or after heartOfTheEndMinesManager
$shutdownSearch = "        if (heartOfTheEndMinesManager != null) {`r`n            heartOfTheEndMinesManager.shutdown();`r`n        }"
$shutdownAddition = @"
        if (minehubHeartManager != null) {
            minehubHeartManager.shutdown();
        }
        if (minehubCommissionManager != null) {
            minehubCommissionManager.shutdown();
        }
        if (heartOfTheEndMinesManager != null) {
            heartOfTheEndMinesManager.shutdown();
        }
"@
if ($content.Contains($shutdownSearch)) {
    $content = $content.Replace($shutdownSearch, $shutdownAddition, 1)
    Write-Host "Added shutdown logic"
} else {
    Write-Host "WARNING: Could not find shutdown insertion point"
}

# 4. Add getters after getCollectionsManager()
$getterSearch = "    public CollectionsManager getCollectionsManager() {`r`n        return collectionsManager;`r`n    }"
$getterAddition = @"
    public CollectionsManager getCollectionsManager() {
        return collectionsManager;
    }

    public MinehubHeartManager getMinehubHeartManager() {
        return minehubHeartManager;
    }

    public MinehubCommissionManager getMinehubCommissionManager() {
        return minehubCommissionManager;
    }
"@
if ($content.Contains($getterSearch)) {
    $content = $content.Replace($getterSearch, $getterAddition, 1)
    Write-Host "Added getter methods"
} else {
    Write-Host "WARNING: Could not find getter insertion point"
}

[System.IO.File]::WriteAllText($file, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "File updated successfully"
