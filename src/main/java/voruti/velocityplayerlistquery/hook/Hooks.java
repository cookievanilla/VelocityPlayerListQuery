package voruti.velocityplayerlistquery.hook;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hooks {

    VanishBridgeHook vanishBridgeHook;
    SayanVanishHook sayanVanishHook;

    @Inject
    public Hooks(ProxyServer server, Logger logger) {
        this.vanishBridgeHook = server.getPluginManager().isLoaded("vanishbridge")
                ? new VanishBridgeHook(server, logger)
                : null;
        this.sayanVanishHook = server.getPluginManager().isLoaded("sayanvanish")
                ? new SayanVanishHook(server, logger)
                : null;
    }

    public Optional<VanishBridgeHook> vanishBridge() {
        return Optional.ofNullable(this.vanishBridgeHook);
    }

    public Optional<SayanVanishHook> sayanVanish() {
        return Optional.ofNullable(this.sayanVanishHook);
    }
}
