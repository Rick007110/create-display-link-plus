package io.github.rick007110.displaylinkplus;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import io.github.rick007110.displaylinkplus.client.DlpClient;
import io.github.rick007110.displaylinkplus.network.DlpPackets;
import org.slf4j.Logger;

@Mod(DisplayLinkPlus.MOD_ID)
public class DisplayLinkPlus {
    public static final String MOD_ID = "create_displaylinkplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DisplayLinkPlus(IEventBus modBus, ModContainer container) {
        // Server configs are synced to clients, so the GUI knows the entry cap too
        container.registerConfig(ModConfig.Type.SERVER, DlpConfig.SPEC);
        modBus.addListener(DlpPackets::register);
        if (FMLEnvironment.dist.isClient())
            DlpClient.init();
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
