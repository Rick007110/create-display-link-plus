package io.github.rick007110.displaylinkplus.client.ponder;

import com.simibubi.create.AllBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import io.github.rick007110.displaylinkplus.DisplayLinkPlus;

/** Adds our scene to the Display Link's Ponder entry (press W over the item). */
public class DlpPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return DisplayLinkPlus.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(AllBlocks.DISPLAY_LINK.getId())
                .addStoryBoard(DisplayLinkPlus.asResource("multi_link"), MultiLinkScenes::multiLink);
    }
}
