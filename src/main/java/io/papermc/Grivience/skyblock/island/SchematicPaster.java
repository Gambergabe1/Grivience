package io.papermc.Grivience.skyblock.island;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.WorldEditException;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility to paste WorldEdit schematics bundled with the plugin.
 */
public final class SchematicPaster {

    private SchematicPaster() {
    }

    public static boolean pasteSchematic(World world, Location pasteOrigin, InputStream schematicStream) {
        ClipboardFormat format = ClipboardFormats.findByAlias("schematic");
        if (format == null) {
            return false;
        }
        try (ClipboardReader reader = format.getReader(schematicStream)) {
            Clipboard clipboard = reader.read();
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                var operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(pasteOrigin.getBlockX(), pasteOrigin.getBlockY(), pasteOrigin.getBlockZ()))
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
            return true;
        } catch (IOException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }
}
