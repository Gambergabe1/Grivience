package io.papermc.Grivience.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.terrain.MountainProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public final class MountainCommand implements CommandExecutor {
    private static final long MAX_SELECTION_VOLUME = 12_000_000L;
    private static final int EDGE_BUFFER = 2;
    private static final int MIN_MOUNTAIN_FOOTPRINT = 5;
    private static final int MIN_FOOTPRINT = MIN_MOUNTAIN_FOOTPRINT + (EDGE_BUFFER * 2);
    private static final int MIN_HEIGHT_RANGE = 4;

    private final GriviencePlugin plugin;

    public MountainCommand(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!player.hasPermission("grivience.admin")) {
            sender.sendMessage(ChatColor.RED + "You need grivience.admin to use this.");
            return true;
        }
        if (args.length > 1 || (args.length == 1 && "help".equalsIgnoreCase(args[0]))) {
            sendHelp(sender, label);
            return true;
        }

        com.sk89q.worldedit.entity.Player worldEditPlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
        Region region;
        try {
            region = session.getSelection();
        } catch (IncompleteRegionException exception) {
            sender.sendMessage(ChatColor.RED + "Make a complete FAWE/WorldEdit selection first.");
            return true;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int heightRange = max.y() - min.y();
        if (region.getWidth() < MIN_FOOTPRINT || region.getLength() < MIN_FOOTPRINT) {
            sender.sendMessage(ChatColor.RED + "Your selection must be at least " + MIN_FOOTPRINT + "x" + MIN_FOOTPRINT + " blocks to keep a " + EDGE_BUFFER + "-block buffer.");
            return true;
        }
        if (heightRange < MIN_HEIGHT_RANGE) {
            sender.sendMessage(ChatColor.RED + "Your selection needs at least " + MIN_HEIGHT_RANGE + " blocks of vertical range.");
            return true;
        }
        if (region.getVolume() > MAX_SELECTION_VOLUME) {
            sender.sendMessage(ChatColor.RED + "Selection is too large. Keep it under " + MAX_SELECTION_VOLUME + " blocks.");
            return true;
        }

        org.bukkit.World bukkitWorld = region.getWorld() == null ? null : BukkitAdapter.adapt(region.getWorld());
        if (bukkitWorld == null) {
            sender.sendMessage(ChatColor.RED + "The selected WorldEdit world is not available.");
            return true;
        }

        Long seed = resolveSeed(args, bukkitWorld.getSeed(), min, max, sender, label);
        if (seed == null) {
            return true;
        }

        int buildMinX = min.x() + EDGE_BUFFER;
        int buildMaxX = max.x() - EDGE_BUFFER;
        int buildMinZ = min.z() + EDGE_BUFFER;
        int buildMaxZ = max.z() - EDGE_BUFFER;

        MountainProfile profile = new MountainProfile(
                buildMinX,
                buildMaxX,
                buildMinZ,
                buildMaxZ,
                min.y(),
                max.y(),
                seed
        );

        try (EditSession editSession = session.createEditSession(worldEditPlayer)) {
            int changedBlocks = buildMountain(editSession, region, profile);
            session.remember(editSession);
            sender.sendMessage(ChatColor.GREEN + "Generated a mountain in your selection.");
            sender.sendMessage(ChatColor.GRAY + "Peak centered at Y " + ChatColor.YELLOW + profile.peakY()
                    + ChatColor.GRAY + ", changed " + ChatColor.YELLOW + changedBlocks
                    + ChatColor.GRAY + " blocks, seed " + ChatColor.YELLOW + seed + ChatColor.GRAY + ".");
        } catch (MaxChangedBlocksException exception) {
            sender.sendMessage(ChatColor.RED + "WorldEdit's block change limit stopped the command. Increase your limit or use a smaller selection.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate a mountain from a WorldEdit selection.", exception);
            sender.sendMessage(ChatColor.RED + "Mountain generation failed. Check the console for details.");
        }

        return true;
    }

    private int buildMountain(EditSession editSession, Region region, MountainProfile profile) throws MaxChangedBlocksException {
        boolean cuboidSelection = region instanceof CuboidRegion;
        BlockVector3 regionMin = region.getMinimumPoint();
        BlockVector3 regionMax = region.getMaximumPoint();

        BlockState air = BukkitAdapter.adapt(Material.AIR.createBlockData());
        BlockState grass = BukkitAdapter.adapt(Material.GRASS_BLOCK.createBlockData());
        BlockState dirt = BukkitAdapter.adapt(Material.DIRT.createBlockData());
        BlockState coarseDirt = BukkitAdapter.adapt(Material.COARSE_DIRT.createBlockData());
        BlockState stone = BukkitAdapter.adapt(Material.STONE.createBlockData());
        BlockState andesite = BukkitAdapter.adapt(Material.ANDESITE.createBlockData());
        BlockState snowBlock = BukkitAdapter.adapt(Material.SNOW_BLOCK.createBlockData());

        int changedBlocks = 0;
        for (int x = regionMin.x(); x <= regionMax.x(); x++) {
            for (int z = regionMin.z(); z <= regionMax.z(); z++) {
                boolean insideMountain = profile.contains(x, z);
                int surfaceY = insideMountain ? profile.heightAt(x, z) : profile.baseY() - 1;
                double steepness = insideMountain ? profile.steepnessAt(x, z) : 0.0D;

                for (int y = profile.baseY(); y <= profile.peakY(); y++) {
                    BlockVector3 position = BlockVector3.at(x, y, z);
                    if (!cuboidSelection && !region.contains(position)) {
                        continue;
                    }

                    BlockState nextBlock = !insideMountain || y > surfaceY
                            ? air
                            : resolveSolidBlock(profile, surfaceY, y, steepness, x, z, grass, dirt, coarseDirt, stone, andesite, snowBlock);

                    if (editSession.setBlock(position, nextBlock)) {
                        changedBlocks++;
                    }
                }
            }
        }

        return changedBlocks;
    }

    private BlockState resolveSolidBlock(
            MountainProfile profile,
            int surfaceY,
            int y,
            double steepness,
            int x,
            int z,
            BlockState grass,
            BlockState dirt,
            BlockState coarseDirt,
            BlockState stone,
            BlockState andesite,
            BlockState snowBlock
    ) {
        int depthFromSurface = surfaceY - y;
        double relativeHeight = (surfaceY - profile.baseY()) / (double) Math.max(1, profile.heightRange());
        double rockNoise = surfaceNoise(profile.seed(), x, z);
        boolean exposedRock = relativeHeight >= 0.58D || steepness >= 0.06D || (relativeHeight >= 0.42D && rockNoise >= 0.80D);
        boolean snowCap = relativeHeight >= 0.60D || (relativeHeight >= 0.46D && steepness <= 0.08D && rockNoise >= 0.40D);
        boolean windScoured = steepness >= 0.11D && relativeHeight >= 0.62D;
        BlockState rockBlock = rockNoise >= 0.58D ? andesite : stone;

        if (depthFromSurface == 0) {
            if (snowCap && !windScoured) {
                return snowBlock;
            }
            if (relativeHeight >= 0.82D || steepness >= 0.12D) {
                return rockBlock;
            }
            if (exposedRock && rockNoise >= 0.22D) {
                return rockBlock;
            }
            if (relativeHeight >= 0.30D || steepness >= 0.035D) {
                return rockNoise >= 0.72D ? coarseDirt : rockBlock;
            }
            return rockNoise >= 0.55D ? coarseDirt : grass;
        }
        if (depthFromSurface <= 2) {
            if (snowCap && steepness <= 0.07D && relativeHeight >= 0.54D) {
                return snowBlock;
            }
            if (exposedRock && (relativeHeight >= 0.48D || steepness >= 0.045D || rockNoise >= 0.18D)) {
                return rockBlock;
            }
            return rockNoise >= 0.62D ? coarseDirt : dirt;
        }
        if (depthFromSurface <= 4 && (relativeHeight >= 0.36D || steepness >= 0.03D)) {
            return rockBlock;
        }
        return stone;
    }

    private Long resolveSeed(String[] args, long worldSeed, BlockVector3 min, BlockVector3 max, CommandSender sender, String label) {
        if (args.length == 0) {
            long seed = worldSeed;
            seed ^= mix64((long) min.x() * 0x632BE59BD9B4E019L);
            seed ^= mix64((long) max.x() * 0x9E3779B97F4A7C15L);
            seed ^= mix64((long) min.z() * 0x94D049BB133111EBL);
            seed ^= mix64((long) max.z() * 0xBF58476D1CE4E5B9L);
            seed ^= mix64((long) max.y() * 0x369DEA0F31A53F85L);
            return mix64(seed);
        }

        try {
            return Long.parseLong(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Seed must be a whole number.");
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " [seed]");
            return null;
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Mountain Command");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " [seed]" + ChatColor.GRAY + " - Build a mountain inside your FAWE/WorldEdit selection.");
        sender.sendMessage(ChatColor.GRAY + "The selection center becomes the peak, and the selection's highest Y becomes the peak height.");
        sender.sendMessage(ChatColor.GRAY + "The mountain fills almost the full selection, while keeping a " + EDGE_BUFFER + "-block horizontal buffer.");
        sender.sendMessage(ChatColor.GRAY + "Use a taller selection to make a steeper mountain, or pass a seed to vary the shape.");
    }

    private double surfaceNoise(long seed, int x, int z) {
        long hash = seed ^ mix64((long) x * 0x9E3779B97F4A7C15L) ^ mix64((long) z * 0x632BE59BD9B4E019L);
        hash = mix64(hash);
        return ((hash >>> 11) & ((1L << 53) - 1)) * 0x1.0p-53;
    }

    private long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return mixed;
    }
}
