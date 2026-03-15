package voruti.velocityplayerlistquery.hook;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VanishBridgeHook {

    ProxyServer server;
    Logger logger;

    volatile boolean reflectionFailureLogged;

    public VanishBridgeHook(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public Collection<Player> unvanishedPlayers() {
        Collection<Player> allPlayers = new ArrayList<>(this.server.getAllPlayers());

        try {
            Set<UUID> vanishedPlayerIds = this.getVanishedPlayerIds();
            allPlayers.removeIf(player -> vanishedPlayerIds.contains(player.getUniqueId()));
            return allPlayers;
        } catch (ReflectiveOperationException | RuntimeException e) {
            this.logFailureOnce(e);
            return allPlayers;
        }
    }

    public int unvanishedPlayerCount() {
        return this.unvanishedPlayers().size();
    }

    private Set<UUID> getVanishedPlayerIds() throws ReflectiveOperationException {
        Class<?> providerClass = Class.forName("dev.loapu.vanishbridge.api.VanishBridgeProvider");
        Method getMethod = providerClass.getMethod("get");
        Object vanishBridgeApi = getMethod.invoke(null);

        Method vanishedPlayersMethod = vanishBridgeApi.getClass().getMethod("vanishedPlayers");
        Object result = vanishedPlayersMethod.invoke(vanishBridgeApi);

        if (!(result instanceof Collection<?> vanishedPlayers)) {
            throw new IllegalStateException("VanishBridge API returned a non-collection for vanishedPlayers()");
        }

        Set<UUID> vanishedPlayerIds = new HashSet<>();
        for (Object vanishedPlayer : vanishedPlayers) {
            Method uuidMethod = vanishedPlayer.getClass().getMethod("uuid");
            Object uuid = uuidMethod.invoke(vanishedPlayer);
            if (uuid instanceof UUID playerUuid) {
                vanishedPlayerIds.add(playerUuid);
            }
        }

        return vanishedPlayerIds;
    }

    private void logFailureOnce(Throwable throwable) {
        if (!this.reflectionFailureLogged) {
            this.reflectionFailureLogged = true;
            this.logger.warn(
                    "Failed to query VanishBridge API, falling back to all online players. " +
                            "The VanishBridge hook may be incompatible with this version.",
                    throwable
            );
        } else {
            this.logger.debug("Failed to query VanishBridge API", throwable);
        }
    }
}
