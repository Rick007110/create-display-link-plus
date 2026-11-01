package io.github.rick007110.displaylinkplus.client;

import net.createmod.ponder.foundation.PonderIndex;
import io.github.rick007110.displaylinkplus.client.ponder.DlpPonderPlugin;

/** Client-only setup, kept out of the main class so servers never load client classes. */
public final class DlpClient {
    private DlpClient() {
    }

    public static void init() {
        PonderIndex.addPlugin(new DlpPonderPlugin());
    }
}
