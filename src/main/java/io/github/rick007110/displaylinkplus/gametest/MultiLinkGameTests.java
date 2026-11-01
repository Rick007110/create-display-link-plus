package io.github.rick007110.displaylinkplus.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.AllDisplayTargets;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import io.github.rick007110.displaylinkplus.DisplayLinkPlus;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.LabelTemplate;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;
import io.github.rick007110.displaylinkplus.link.RowFormat;

/**
 * Server-side checks for multi-entry Display Links. Run with {@code ./gradlew runGameTestServer}.
 * Layout: target block (redstone power source) <- display link -> sign A (entries 0 and 2), sign B (entry 1),
 * and entry 3 aimed at an empty spot.
 */
@GameTestHolder(DisplayLinkPlus.MOD_ID)
@PrefixGameTestTemplate(false)
public class MultiLinkGameTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1);
    private static final BlockPos LINK = new BlockPos(2, 1, 1);
    private static final BlockPos SIGN_A = new BlockPos(2, 1, 4);
    private static final BlockPos SIGN_B = new BlockPos(4, 1, 4);
    private static final BlockPos NOTHING = new BlockPos(6, 1, 6);

    private static DisplayLinkBlockEntity setUp(GameTestHelper helper) {
        for (int x = 0; x < 7; x++)
            for (int z = 0; z < 7; z++)
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(SOURCE, Blocks.TARGET);
        helper.setBlock(SIGN_A, Blocks.OAK_SIGN);
        helper.setBlock(SIGN_B, Blocks.OAK_SIGN);
        helper.setBlock(LINK, AllBlocks.DISPLAY_LINK.getDefaultState().setValue(DisplayLinkBlock.FACING, Direction.EAST));

        DisplayLinkBlockEntity be = helper.getBlockEntity(LINK);
        DisplaySource source = AllDisplaySources.REDSTONE_POWER.get();
        be.target(helper.absolutePos(SIGN_A));
        be.activeSource = source;
        be.setSourceConfig(label("AAA"));
        be.targetLine = 0;

        MultiLink link = MultiLink.of(be);
        link.dlp$extraEntries().add(new LinkEntry(SIGN_B.subtract(LINK), source, label("BBB"), null, 1));
        link.dlp$extraEntries().add(new LinkEntry(SIGN_A.subtract(LINK), source, label("CCC"), null, 2));
        link.dlp$extraEntries().add(new LinkEntry(NOTHING.subtract(LINK), source, label("DDD"), null, 0));
        return be;
    }

    private static CompoundTag label(String label) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Label", label);
        return tag;
    }

    private static String line(GameTestHelper helper, BlockPos sign, int line) {
        SignBlockEntity be = helper.getBlockEntity(sign);
        return be.getFrontText().getMessage(line, false).getString();
    }

    private static void assertLine(GameTestHelper helper, BlockPos sign, int line, String expected) {
        String actual = line(helper, sign, line);
        helper.assertTrue(actual.contains(expected), "Expected '" + expected + "' on line " + line + " of sign at " + sign + ", got '" + actual + "'");
    }

    @GameTest(template = "empty")
    public static void everyEntryWritesToItsOwnTarget(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        be.updateGatheredData();
        assertLine(helper, SIGN_A, 0, "AAA");
        assertLine(helper, SIGN_A, 2, "CCC");
        assertLine(helper, SIGN_B, 1, "BBB");
        helper.assertTrue(line(helper, SIGN_B, 0).isEmpty(), "Sign B line 0 should be untouched");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void entriesSurviveSaveAndLoad(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        be.updateGatheredData();
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        CompoundTag saved = be.saveWithFullMetadata(registries);
        helper.assertTrue(saved.contains("TargetOffset") && saved.contains("Source"), "Create's own keys must stay");
        helper.assertTrue(saved.contains(MultiLink.NBT_KEY), "Extra entries must be saved");

        BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), saved, registries);
        helper.assertTrue(loaded instanceof DisplayLinkBlockEntity, "Reloaded block entity has the wrong type");
        MultiLink reloaded = MultiLink.of((DisplayLinkBlockEntity) loaded);
        helper.assertTrue(reloaded.dlp$entryCount() == 4, "Expected 4 entries after reload, got " + reloaded.dlp$entryCount());
        LinkEntry second = reloaded.dlp$snapshot(1);
        helper.assertTrue(second.targetOffset.equals(SIGN_B.subtract(LINK)), "Entry 1 target offset changed");
        helper.assertTrue(second.targetLine == 1, "Entry 1 line changed");
        helper.assertTrue(second.activeSource == AllDisplaySources.REDSTONE_POWER.get(), "Entry 1 source changed");
        helper.assertTrue("CCC".equals(reloaded.dlp$snapshot(2).sourceConfig.getString("Label")), "Entry 2 config changed");

        CompoundTag clientTag = be.getUpdateTag(registries);
        CompoundTag clientEntry = clientTag.getList(MultiLink.NBT_KEY, CompoundTag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(clientEntry.contains("TargetType"), "Client sync must include each entry's target type");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extraEntriesRefreshOnTheirOwnTimer(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        be.updateGatheredData();
        // With no source on entry 0, Create's own passive refresh never runs; only ours can refill sign B
        be.activeSource = null;
        SignBlockEntity signB = helper.getBlockEntity(SIGN_B);
        signB.setText(new SignText(), true);
        helper.assertTrue(line(helper, SIGN_B, 1).isEmpty(), "Sign B should have been cleared");
        long start = helper.getTick();
        helper.startSequence()
                .thenWaitUntil(() -> assertLine(helper, SIGN_B, 1, "BBB"))
                .thenExecute(() -> DisplayLinkPlus.LOGGER.info("Sign B refilled after {} ticks", helper.getTick() - start))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void removingFirstEntryPromotesTheNext(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        MultiLink link = MultiLink.of(be);
        helper.assertTrue(link.dlp$removeEntry(0), "Removing entry 0 failed");
        helper.assertTrue(link.dlp$entryCount() == 3, "Expected 3 entries");
        helper.assertTrue(be.getTargetPosition().equals(helper.absolutePos(SIGN_B)), "Entry 1 was not promoted");
        helper.assertTrue(be.targetLine == 1 && "BBB".equals(be.getSourceConfig().getString("Label")), "Promoted entry lost its settings");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void addCopyUsesNextLineAndRespectsCap(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        be.updateGatheredData();
        MultiLink link = MultiLink.of(be);
        int added = EntryOps.addCopy(be, 1);
        helper.assertTrue(added == 4, "Copy should be entry 4, was " + added);
        LinkEntry copy = link.dlp$snapshot(added);
        helper.assertTrue(copy.targetOffset.equals(SIGN_B.subtract(LINK)) && copy.targetLine == 2, "Copy should target the next line of sign B");
        helper.assertTrue("BBB".equals(copy.sourceConfig.getString("Label")), "Copy should keep the source settings");

        // Lines are clamped to the sign's 4 rows
        int last = EntryOps.addCopy(be, EntryOps.addCopy(be, added));
        helper.assertTrue(link.dlp$snapshot(last).targetLine == 3, "Line should clamp to the last row");

        while (EntryOps.addCopy(be, 0) != -1) {
        }
        helper.assertTrue(link.dlp$entryCount() == 8, "Default cap is 8 entries, got " + link.dlp$entryCount());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void labelTemplatePutsValueInPlace(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        be.setSourceConfig(label("Power: {} RS"));
        MultiLink.of(be).dlp$extraEntries().get(1).sourceConfig = label("{}/{}");
        be.updateGatheredData();

        String templated = line(helper, SIGN_A, 0);
        helper.assertTrue(templated.matches("Power: \\d+ RS"), "Expected 'Power: <n> RS', got '" + templated + "'");
        String twice = line(helper, SIGN_A, 2);
        helper.assertTrue(twice.matches("(\\d+)/\\1"), "Every {} should be replaced, got '" + twice + "'");
        // Labels without {} keep Create's "label value" format
        String plain = line(helper, SIGN_B, 1);
        helper.assertTrue(plain.matches("BBB \\d+"), "Expected 'BBB <n>', got '" + plain + "'");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void labelTemplateUsesOneDisplayBoardSection(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        BlockPos boardPos = new BlockPos(5, 1, 1);
        helper.setBlock(boardPos, AllBlocks.DISPLAY_BOARD.getDefaultState());
        FlapDisplayBlockEntity board = helper.getBlockEntity(boardPos);
        DisplaySource source = be.activeSource;
        DisplayTargetStats stats = new DisplayTargetStats(1, board.getMaxCharCount(), AllDisplayTargets.DISPLAY_BOARD.get());
        DisplayLinkContext context = new DisplayLinkContext(helper.getLevel(), be);

        be.setSourceConfig(label("We have {} power"));
        FlapDisplayLayout layout = new FlapDisplayLayout(board.getMaxCharCount());
        source.loadFlapDisplayLayout(context, board, layout, 0);
        helper.assertTrue(layout.getSections().size() == 1, "Template should use one section, got " + layout.getSections().size());
        List<List<MutableComponent>> text = source.provideFlapDisplayText(context, stats);
        helper.assertTrue(text.size() == 1 && text.get(0).size() == 1, "Template should produce one text section");
        String boardText = text.get(0).get(0).getString();
        helper.assertTrue(boardText.matches("We have \\d+ power"), "Expected 'We have <n> power', got '" + boardText + "'");

        // Plain labels keep Create's separate label + value sections (Create only adds the value section if it fits)
        be.setSourceConfig(label("A"));
        FlapDisplayLayout plainLayout = new FlapDisplayLayout(board.getMaxCharCount());
        source.loadFlapDisplayLayout(context, board, plainLayout, 0);
        int expected = "A".length() + 1 < board.getMaxCharCount() ? 2 : 1;
        helper.assertTrue(plainLayout.getSections().size() == expected, "Plain label should keep " + expected
                + " sections on a " + board.getMaxCharCount() + "-char board, got " + plainLayout.getSections().size());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void numberFormats(GameTestHelper helper) {
        assertEquals(helper, "999", LabelTemplate.formatNumber("999", "k"));
        assertEquals(helper, "1.2k", LabelTemplate.formatNumber("1234", "k"));
        assertEquals(helper, "153k", LabelTemplate.formatNumber("153400", "k"));
        assertEquals(helper, "1.5M mB", LabelTemplate.formatNumber("1500000 mB", "k"));
        assertEquals(helper, "1,234,567", LabelTemplate.formatNumber("1234567", ","));
        assertEquals(helper, "12.3 RPM", LabelTemplate.formatNumber("12.345 RPM", ".1"));
        assertEquals(helper, "13", LabelTemplate.formatNumber("12.5", ".0"));
        assertEquals(helper, "no number", LabelTemplate.formatNumber("no number", "k"));
        assertEquals(helper, "Stock: 1.2k items", LabelTemplate.fill("Stock: {k} items", Component.literal("1234")).getString());
        assertEquals(helper, "Stock: 1,234 / 1234", LabelTemplate.fill("Stock: {:,} / {}", Component.literal("1234")).getString());
        helper.assertTrue(LabelTemplate.templateOf("{name}: {count}") == null, "{name}/{count} are not single-line placeholders");
        assertEquals(helper, "Bread: 1.2k mB", LabelTemplate.fillRow("{name}: {count:k}", Component.literal("Bread"),
                Component.literal("1k"), 1234, Component.literal("mB")).getString());
        helper.succeed();
    }

    private static void assertEquals(GameTestHelper helper, String expected, String actual) {
        helper.assertTrue(expected.equals(actual), "Expected '" + expected + "', got '" + actual + "'");
    }

    @GameTest(template = "empty")
    public static void addCopySkipsUsedLinesAndConflictsAreFound(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        // Sign A: entry 0 on line 0, entry 2 on line 2
        int first = EntryOps.addCopy(be, 0);
        helper.assertTrue(MultiLink.of(be).dlp$snapshot(first).targetLine == 1, "Next free line after 0 is 1");
        int second = EntryOps.addCopy(be, 0);
        helper.assertTrue(MultiLink.of(be).dlp$snapshot(second).targetLine == 3, "Lines 1 and 2 are taken, next free is 3");

        helper.assertTrue(EntryOps.conflicts(be, 0).isEmpty(), "Entry 0 has no conflict yet");
        MultiLink.of(be).dlp$extraEntries().get(1).targetLine = 0; // entry 2 -> line 0 of sign A, same as entry 0
        helper.assertTrue(EntryOps.conflicts(be, 0).equals(List.of(2)), "Entry 0 should conflict with entry 2, got " + EntryOps.conflicts(be, 0));
        helper.assertTrue(EntryOps.conflicts(be, 1).isEmpty(), "Entry 1 is on sign B and has no conflict");
        helper.succeed();
    }

    /** A link reading a scoreboard (command block source) with one entry per given line, all on sign A. */
    private static DisplayLinkBlockEntity setUpScoreboard(GameTestHelper helper, String objectiveName, Map<String, Integer> scores) {
        DisplayLinkBlockEntity be = setUp(helper);
        helper.setBlock(SOURCE, Blocks.COMMAND_BLOCK);
        Scoreboard scoreboard = helper.getLevel().getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null)
            objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName),
                    ObjectiveCriteria.RenderType.INTEGER, false, null);
        for (Map.Entry<String, Integer> score : scores.entrySet())
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(score.getKey()), objective).set(score.getValue());
        MultiLink.of(be).dlp$extraEntries().clear();
        be.activeSource = AllDisplaySources.SCOREBOARD.get();
        return be;
    }

    private static CompoundTag scoreboardConfig(String objective, String rowFormat) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Objective", objective);
        if (rowFormat != null)
            tag.putString(RowFormat.CONFIG_KEY, rowFormat);
        return tag;
    }

    @GameTest(template = "empty")
    public static void listStopsAtSiblingEntryLine(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUpScoreboard(helper, "dlp_res", Map.of("Ann", 40, "Ben", 30, "Cat", 20, "Dan", 10));
        DisplaySource scoreboard = AllDisplaySources.SCOREBOARD.get();
        // Entry 0: single row on line 2. Entry 1: a list starting at line 0 that would run over line 2
        be.setSourceConfig(scoreboardConfig("dlp_one", null));
        be.targetLine = 2;
        Scoreboard sb = helper.getLevel().getScoreboard();
        Objective one = sb.getObjective("dlp_one");
        if (one == null)
            one = sb.addObjective("dlp_one", ObjectiveCriteria.DUMMY, Component.literal("one"), ObjectiveCriteria.RenderType.INTEGER, false, null);
        sb.getOrCreatePlayerScore(ScoreHolder.forNameOnly("Zed"), one).set(7);
        MultiLink.of(be).dlp$extraEntries().add(new LinkEntry(SIGN_A.subtract(LINK), scoreboard, scoreboardConfig("dlp_res", null), null, 0));

        be.updateGatheredData();
        assertLine(helper, SIGN_A, 0, "Ann");
        assertLine(helper, SIGN_A, 1, "Ben");
        assertLine(helper, SIGN_A, 2, "Zed");
        helper.assertTrue(line(helper, SIGN_A, 3).isEmpty(), "The list should stop at entry 0's line, got '" + line(helper, SIGN_A, 3) + "'");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void listRowFormat(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUpScoreboard(helper, "dlp_row", Map.of("Alice", 12345));
        be.setSourceConfig(scoreboardConfig("dlp_row", "{name}={count:,}"));
        be.targetLine = 0;
        MultiLink.of(be).dlp$extraEntries().add(new LinkEntry(SIGN_B.subtract(LINK), be.activeSource, scoreboardConfig("dlp_row", "{name}: {count}"), null, 0));
        MultiLink.of(be).dlp$extraEntries().add(new LinkEntry(SIGN_B.subtract(LINK), be.activeSource, scoreboardConfig("dlp_row", ""), null, 2));
        be.updateGatheredData();

        assertEquals(helper, "Alice=12,345", line(helper, SIGN_A, 0));
        assertEquals(helper, "Alice: 12k", line(helper, SIGN_B, 0)); // {count} follows Create's default "Shortened"
        helper.assertTrue(line(helper, SIGN_B, 2).contains("Alice") && !line(helper, SIGN_B, 2).contains("{"),
                "Without a row format Create's own layout is used, got '" + line(helper, SIGN_B, 2) + "'");

        // Display Boards: one full-width section per row, for the shared list base and the fluid list's own layout
        BlockPos boardPos = new BlockPos(5, 1, 1);
        helper.setBlock(boardPos, AllBlocks.DISPLAY_BOARD.getDefaultState());
        FlapDisplayBlockEntity board = helper.getBlockEntity(boardPos);
        DisplayLinkContext context = new DisplayLinkContext(helper.getLevel(), be);
        for (DisplaySource list : List.of(AllDisplaySources.SCOREBOARD.get(), AllDisplaySources.LIST_FLUIDS.get())) {
            FlapDisplayLayout layout = new FlapDisplayLayout(board.getMaxCharCount());
            list.loadFlapDisplayLayout(context, board, layout, 0);
            helper.assertTrue(layout.getSections().size() == 1, list.getClass().getSimpleName() + " should use one section, got " + layout.getSections().size());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clipboardCopiesSettingsAndAddsMissingEntries(GameTestHelper helper) {
        DisplayLinkBlockEntity original = setUp(helper);
        BlockPos otherLink = new BlockPos(2, 1, 0);
        helper.setBlock(new BlockPos(1, 1, 0), Blocks.TARGET);
        helper.setBlock(otherLink, AllBlocks.DISPLAY_LINK.getDefaultState().setValue(DisplayLinkBlock.FACING, Direction.EAST));
        DisplayLinkBlockEntity copy = helper.getBlockEntity(otherLink);
        copy.target(helper.absolutePos(SIGN_B));
        copy.targetLine = 3;

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ClipboardCloneable from = (ClipboardCloneable) original;
        ClipboardCloneable to = (ClipboardCloneable) copy;
        CompoundTag clipboard = new CompoundTag();
        helper.assertTrue(from.writeToClipboard(registries, clipboard, Direction.UP), "Copy failed");
        helper.assertTrue(to.readFromClipboard(registries, clipboard, null, Direction.UP, true), "Paste simulation failed");
        helper.assertTrue(to.readFromClipboard(registries, clipboard, null, Direction.UP, false), "Paste failed");

        MultiLink pasted = MultiLink.of(copy);
        helper.assertTrue(pasted.dlp$entryCount() == 4, "Expected 4 entries after paste, got " + pasted.dlp$entryCount());
        helper.assertTrue(copy.getTargetPosition().equals(helper.absolutePos(SIGN_B)), "Entry 0 must keep its own display");
        helper.assertTrue(copy.targetLine == 0 && "AAA".equals(copy.getSourceConfig().getString("Label")), "Entry 0 should get the copied settings");
        helper.assertTrue(pasted.dlp$snapshot(1).targetOffset.equals(SIGN_B.subtract(LINK)), "Added entries keep the copied relative target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingTargetDoesNotBreakOtherEntries(GameTestHelper helper) {
        DisplayLinkBlockEntity be = setUp(helper);
        // Entry 3 points at air; break sign A as well so entries 0 and 2 lose their target
        helper.setBlock(SIGN_A, Blocks.AIR);
        be.updateGatheredData();
        assertLine(helper, SIGN_B, 1, "BBB");
        helper.succeed();
    }
}
