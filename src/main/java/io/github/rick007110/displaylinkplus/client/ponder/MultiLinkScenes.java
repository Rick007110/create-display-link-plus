package io.github.rick007110.displaylinkplus.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Ponder scene for the Display Link: several entries, adding displays, the overlay, templates and the clipboard. */
public final class MultiLinkScenes {
    private MultiLinkScenes() {
    }

    public static void multiLink(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("multi_link", "Sending to multiple displays");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos source = util.grid().at(2, 1, 3);
        BlockPos link = util.grid().at(2, 2, 3);
        BlockPos signA = util.grid().at(1, 1, 1);
        BlockPos signB = util.grid().at(3, 1, 1);
        Selection signs = util.select().position(signA).add(util.select().position(signB));

        scene.idle(10);
        scene.world().showSection(util.select().position(source), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(link), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(signA), Direction.SOUTH);
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("A single Display Link can now write to several displays and lines")
                .pointAt(util.vector().blockSurface(link, Direction.WEST))
                .placeNearTarget();
        setLine(scene, signA, 0, "Power: 15");
        scene.world().flashDisplayLink(link);
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(link), Pointing.DOWN, 50).rightClick();
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Each entry has its own information, display and line. The interface shows them as numbered tabs")
                .pointAt(util.vector().centerOf(link))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(80)
                .text("Press + to add an entry on the next free line of the same display")
                .pointAt(util.vector().topOf(signA))
                .placeNearTarget();
        setLine(scene, signA, 1, "Speed: 64");
        scene.world().flashDisplayLink(link);
        scene.idle(90);

        scene.world().showSection(util.select().position(signB), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(signB), Pointing.DOWN, 50).rightClick().withItem(AllBlocks.DISPLAY_LINK.asStack());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, signB, new AABB(signB).deflate(0.25, 0, 0.25), 50);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("To add another display, first right-click it with a Display Link...")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().topOf(signB))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(link), Pointing.DOWN, 50).rightClick().withItem(AllBlocks.DISPLAY_LINK.asStack());
        scene.overlay().showText(70)
                .text("...then right-click the existing link. The display is added as a new entry")
                .pointAt(util.vector().centerOf(link))
                .placeNearTarget();
        scene.idle(20);
        scene.effects().indicateSuccess(link);
        setLine(scene, signB, 0, "Power: 15");
        scene.world().flashDisplayLink(link);
        scene.idle(60);

        scene.overlay().showControls(util.vector().topOf(link), Pointing.DOWN, 70).withItem(AllItems.WRENCH.asStack());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, signA, new AABB(signA).deflate(0.25, 0, 0.25), 70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, signB, new AABB(signB).deflate(0.25, 0, 0.25), 70);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Look at a link while holding a Wrench or wearing Goggles to see every display it writes to")
                .pointAt(util.vector().centerOf(link))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Put {} in a label to place the value anywhere, e.g. \"{} RS signal\". {k} shortens large numbers to 1.2k")
                .pointAt(util.vector().topOf(signB))
                .placeNearTarget();
        setLine(scene, signB, 0, "15 RS signal");
        scene.world().flashDisplayLink(link);
        scene.idle(100);

        Vec3 linkTop = util.vector().topOf(link);
        scene.overlay().showControls(linkTop, Pointing.DOWN, 60).withItem(AllBlocks.CLIPBOARD.asStack());
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("A Clipboard copies all entries of a link (right-click) and pastes them onto another link (left-click)")
                .pointAt(linkTop)
                .placeNearTarget();
        scene.idle(100);
        scene.world().hideSection(signs, Direction.UP);
        scene.idle(10);
    }

    private static void setLine(CreateSceneBuilder scene, BlockPos sign, int line, String text) {
        scene.world().modifyBlockEntity(sign, SignBlockEntity.class,
                be -> be.setText(be.getFrontText().setMessage(line, Component.literal(text)), true));
    }
}
