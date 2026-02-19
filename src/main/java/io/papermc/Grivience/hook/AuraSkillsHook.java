package io.papermc.Grivience.hook;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AuraSkillsHook {
    public enum StatKey {
        STRENGTH,
        CRIT_CHANCE,
        CRIT_DAMAGE
    }

    private final JavaPlugin plugin;
    private final Map<StatKey, Object> statConstants = new HashMap<>();

    private boolean available;
    private Object api;
    private Method getUserMethod;
    private Method getStatLevelMethod;
    private Method consumeManaMethod;
    private Method isLoadedMethod;
    private Class<? extends Enum<?>> statsEnumClass;

    public AuraSkillsHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        available = false;
        api = null;
        getUserMethod = null;
        getStatLevelMethod = null;
        consumeManaMethod = null;
        isLoadedMethod = null;
        statsEnumClass = null;
        statConstants.clear();

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled("AuraSkills")) {
            return;
        }

        try {
            Class<?> auraApiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
            Class<?> skillsUserClass = Class.forName("dev.aurelium.auraskills.api.user.SkillsUser");
            Class<?> statClass = Class.forName("dev.aurelium.auraskills.api.stat.Stat");
            Class<?> statsClass = Class.forName("dev.aurelium.auraskills.api.stat.Stats");

            Method getApiMethod = auraApiClass.getMethod("get");
            api = getApiMethod.invoke(null);
            getUserMethod = auraApiClass.getMethod("getUser", UUID.class);
            getStatLevelMethod = skillsUserClass.getMethod("getStatLevel", statClass);
            consumeManaMethod = skillsUserClass.getMethod("consumeMana", double.class);
            isLoadedMethod = skillsUserClass.getMethod("isLoaded");

            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) statsClass;
            statsEnumClass = enumClass;

            for (StatKey key : StatKey.values()) {
                Object constant = parseStatConstant(key.name());
                if (constant != null) {
                    statConstants.put(key, constant);
                }
            }

            available = true;
            plugin.getLogger().info("AuraSkills hook enabled.");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("AuraSkills was found but API hook failed: " + ex.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public double getStat(Player player, StatKey key) {
        if (!available || player == null) {
            return 0.0D;
        }
        Object stat = statConstants.get(key);
        if (stat == null) {
            return 0.0D;
        }
        Object user = getUser(player);
        if (user == null) {
            return 0.0D;
        }
        try {
            Object value = getStatLevelMethod.invoke(user, stat);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return 0.0D;
    }

    public boolean consumeMana(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (!available || player == null) {
            return false;
        }
        Object user = getUser(player);
        if (user == null) {
            return false;
        }
        try {
            Object result = consumeManaMethod.invoke(user, amount);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return false;
    }

    private Object getUser(Player player) {
        if (!available || api == null) {
            return null;
        }
        try {
            Object value = getUserMethod.invoke(api, player.getUniqueId());
            if (value instanceof Optional<?> optional) {
                value = optional.orElse(null);
            }
            if (value == null) {
                return null;
            }
            if (isLoadedMethod != null) {
                Object loaded = isLoadedMethod.invoke(value);
                if (loaded instanceof Boolean bool && !bool) {
                    return null;
                }
            }
            return value;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private Object parseStatConstant(String rawName) {
        if (statsEnumClass == null || rawName == null || rawName.isBlank()) {
            return null;
        }
        String targetName = rawName.toUpperCase(Locale.ROOT);
        Object[] constants = statsEnumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(targetName)) {
                return constant;
            }
        }
        return null;
    }
}
