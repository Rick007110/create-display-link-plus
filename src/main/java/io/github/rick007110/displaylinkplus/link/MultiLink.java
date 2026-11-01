package io.github.rick007110.displaylinkplus.link;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import java.util.List;

/**
 * Duck interface mixed into {@link DisplayLinkBlockEntity}. Entry 0 is Create's own link; entries 1..n are the extra
 * {@link LinkEntry}s. Any Create logic can be run for entry i via {@link #dlp$runWithEntry}, which temporarily swaps
 * that entry's fields into the block entity.
 */
public interface MultiLink {
    String NBT_KEY = "create_displaylinkplus:Entries";

    static MultiLink of(DisplayLinkBlockEntity be) {
        return (MultiLink) be;
    }

    /** Extra entries (entry 1 is index 0 of this list). Mutable. */
    List<LinkEntry> dlp$extraEntries();

    default int dlp$entryCount() {
        return 1 + dlp$extraEntries().size();
    }

    /**
     * Runs the action with entry {@code index} swapped into the block entity's fields. Updates triggered inside only
     * touch that entry. Nested calls run in the already active entry.
     */
    void dlp$runWithEntry(int index, Runnable action);

    /** Index of the entry currently being processed, or -1 when the link acts on all entries. */
    int dlp$activeEntry();

    /** A copy of entry {@code index} (entry 0 is read from Create's fields). */
    LinkEntry dlp$snapshot(int index);

    /** Removes an entry. Removing entry 0 promotes entry 1 into Create's fields. Returns false if it is the last one. */
    boolean dlp$removeEntry(int index);
}
