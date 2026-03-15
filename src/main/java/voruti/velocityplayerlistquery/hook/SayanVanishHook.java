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
public class SayanVanishHook {

    ProxyServer server;
    Logger logger;

    volatile boolean reflectionFailureLogged;

    public SayanVanishHook(ProxyServer server, Logger logger) {
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
        Class<?> apiClass = Class.forName("org.sayandev.sayanvanish.api.SayanVanishAPI");
        Method getInstanceMethod = apiClass.getMethod("getInstance");
        Object api = getInstanceMethod.invoke(null);

        Method getVanishedUsersMethod = api.getClass().getMethod("getVanishedUsers");
        Object result = getVanishedUsersMethod.invoke(api);

        if (!(result instanceof Collection<?> vanishedUsers)) {
            throw new IllegalStateException("SayanVanish API returned a non-collection for getVanishedUsers()");
        }

        Set<UUID> vanishedPlayerIds = new HashSet<>();
        for (Object user : vanishedUsers) {
            Method getUniqueIdMethod = user.getClass().getMethod("getUniqueId");
            Object uuid = getUniqueIdMethod.invoke(user);
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
                    "Failed to query SayanVanish API, falling back to all online players. " +
                            "The SayanVanish hook may be incompatible with this version.",
                    throwable
            );
        } else {
            this.logger.debug("Failed to query SayanVanish API", throwable);
        }
    }
}