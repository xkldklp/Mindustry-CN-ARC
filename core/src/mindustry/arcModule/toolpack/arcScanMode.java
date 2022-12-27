package mindustry.arcModule.toolpack;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.Align;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.liquid.LiquidJunction;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.meta.BlockGroup;


import static mindustry.Vars.*;
import static mindustry.arcModule.RFuncs.calWaveTimer;
import static mindustry.arcModule.toolpack.arcWaveSpawner.*;

public class arcScanMode {

    private static Table st = new Table(Styles.black3);

    private static Table ct = new Table(Styles.none);
    private static Table ctTable = new Table();
    /** spawner */
    private static Table spt, sfpt;
    private static Table spawnerTable = new Table();
    private static Table flyerTable = new Table();
    static float thisAmount, thisHealth, thisEffHealth, thisDps;
    static int totalAmount = 0, totalHealth = 0, totalEffHealth = 0, totalDps = 0;

    static int tableCount = 0;
    /**
     * conveyor
     */
    static final int maxLoop = 200;
    static int forwardIndex = 0;
    static Seq<Building> forwardBuild = new Seq<>();


    static {/*
        {
            st.touchable = Touchable.disabled;
            st.margin(8f).add(">> 扫描详情模式 <<").color(getThemeColor()).style(Styles.outlineLabel).labelAlign(Align.center);
            st.visible = true;
            st.pack();
            st.update(() -> {
                st.visible = control.input.arcScanMode && state.isPlaying();
                st.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() * 0.7f, Align.center);
            });
            Core.scene.add(st);
        }*/
        {
            ct.touchable = Touchable.disabled;
            ct.visible = false;
            ct.add(ctTable).margin(8f);
            ct.pack();
            ct.update(() -> ct.visible = ct.visible && state.isPlaying());
            Core.scene.add(ct);
        }
        {
            spt = new Table();
            spt.touchable = Touchable.disabled;
            spt.visible = false;
            spt.add(spawnerTable).margin(8f);
            spt.pack();
            spt.update(() -> spt.visible = spt.visible && state.isPlaying());
            Core.scene.add(spt);

            sfpt = new Table();
            sfpt.touchable = Touchable.disabled;
            sfpt.visible = false;
            sfpt.add(flyerTable).margin(8f);
            sfpt.pack();
            sfpt.update(() -> sfpt.visible = sfpt.visible && state.isPlaying());
            Core.scene.add(sfpt);
        }
    }

    public static void arcScan() {
        detailCursor();
        detailSpawner();
        //detailTransporter();
        detailTransporter2();
    }

    private static void detailCursor() {
        ct.visible = ct.visible && state.isPlaying();
        ctTable.clear();
        if (!control.input.arcScanMode) {
            ct.visible = false;
            return;
        }
        ct.setPosition(Core.input.mouseX(), Core.input.mouseY());
        ct.visible = true;
        ctTable.table(ctt -> {
            ctt.add((int) (Core.input.mouseWorldX() / 8) + "," + (int) (Core.input.mouseWorldY() / 8));
            ctt.row();
            ctt.add("距离：" + (int) (Mathf.dst(player.x, player.y, Core.input.mouseWorldX(), Core.input.mouseWorldY()) / 8));
        });
    }

    private static void detailSpawner() {
        spt.visible = spt.visible && state.isPlaying();
        sfpt.visible = sfpt.visible && state.isPlaying();
        spawnerTable.clear();
        flyerTable.clear();
        if (!control.input.arcScanMode) {
            spt.visible = false;
            sfpt.visible = false;
            return;
        }
        totalAmount = 0;
        totalHealth = 0;
        totalEffHealth = 0;
        totalDps = 0;
        checkInit();
        waveInfo thisWave = arcWave.get(Math.min(state.wave, arcWave.size - 1));
        for (Tile tile : spawner.getSpawns()) {
            if (Mathf.dst(tile.worldx(), tile.worldy(), Core.input.mouseWorldX(), Core.input.mouseWorldY()) < state.rules.dropZoneRadius) {
                float curve = Mathf.curve(Time.time % 240f, 120f, 240f);
                Draw.z(Layer.effect - 2f);
                Draw.color(state.rules.waveTeam.color);
                Lines.stroke(4f);
                //flyer
                float flyerAngle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
                float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
                float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(flyerAngle, trns), 0, world.width() * tilesize);
                float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(flyerAngle, trns), 0, world.height() * tilesize);

                if (hasFlyer) {
                    Lines.line(tile.worldx(), tile.worldy(), spawnX, spawnY);
                    Tmp.v1.set(spawnX - tile.worldx(), spawnY - tile.worldy());
                    Tmp.v1.setLength(Tmp.v1.len() * curve);
                    Fill.circle(tile.worldx() + Tmp.v1.x, tile.worldy() + Tmp.v1.y, 8f);

                    Vec2 v = Core.camera.project(spawnX, spawnY);
                    sfpt.setPosition(v.x, v.y);
                    sfpt.visible = true;

                    flyerTable.table(Styles.black3, tt -> {
                        tt.add(calWaveTimer()).row();
                        thisWave.specLoc(tile.pos(),group -> group.type.flying);
                        tt.add(thisWave.proTable(false));
                        tt.row();
                        tt.add(thisWave.unitTable(tile.pos(),group -> group.type.flying)).maxWidth(mobile ? 400f : 750f).growX();
                    });
                }
                //ground
                totalAmount = 0;
                totalHealth = 0;
                totalEffHealth = 0;
                totalDps = 0;

                if (curve > 0)
                    Lines.circle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius * Interp.pow3Out.apply(curve));
                Lines.circle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                Lines.arc(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius - 3f, state.wavetime / state.rules.waveSpacing, 90f);
                float angle = Mathf.pi / 2 + state.wavetime / state.rules.waveSpacing * 2 * Mathf.pi;
                Draw.color(state.rules.waveTeam.color);
                Fill.circle(tile.worldx() + state.rules.dropZoneRadius * Mathf.cos(angle), tile.worldy() + state.rules.dropZoneRadius * Mathf.sin(angle), 8f);

                Vec2 v = Core.camera.project(tile.worldx(), tile.worldy());
                spt.setPosition(v.x, v.y);
                spt.visible = true;
                spawnerTable.table(Styles.black3, tt -> {
                    tt.add(calWaveTimer()).row();
                    thisWave.specLoc(tile.pos(),group -> !group.type.flying);
                    tt.add(thisWave.proTable(false));
                    tt.row();
                    tt.add(thisWave.unitTable(tile.pos(),group -> !group.type.flying)).maxWidth(mobile ? 400f : 750f).growX();
                });
                return;
            }
        }

        spt.visible = false;
        spawnerTable.clear();
    }

    private static void detailTransporter() {
        if (!control.input.arcScanMode) return;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if (hoverTile == null || hoverTile.build == null || !hoverTile.build.displayable() || hoverTile.build.inFogTo(player.team())) {
            return;
        }
        forwardIndex = 0; forwardBuild.clear();

        forward(hoverTile.build, hoverTile.build);
        //drawBuild();
    }

    private static void forward(Building cur, Building last) {
        forward(cur, last, 0);
    }

    private static void forward(Building cur, Building last, int conduit) {
        /** 检查cur并添加到forwardBuild, 同时迭代下一个的循环 */

        //能否接受
        if (forwardIndex == maxLoop || cur == null || conduit == 3) return;
        if (last.team != cur.team || (cur.team != player.team() && cur.inFogTo(player.team()))) return;
        if (cur.block.itemCapacity == 0 && !cur.block.instantTransfer) return;
        if (cur instanceof LiquidJunction.LiquidJunctionBuild) return;

        //接受成功
        forwardIndex += 1;
        //绘制
        Draw.color(Color.gold);
        Lines.stroke(1.5f);
        float dst = Mathf.dst(last.tile.worldx(), last.tile.worldy(), cur.tile.worldx(), cur.tile.worldy());
        Lines.line(last.tile.worldx(), last.tile.worldy(), cur.tile.worldx(), cur.tile.worldy());
        Fill.circle(cur.tile.worldx(), cur.tile.worldy(), 2f);
        if (dst > 8f) {
            Draw.color(Color.orange);
            Drawf.simpleArrow(last.tile.worldx(), last.tile.worldy(), cur.tile.worldx(), cur.tile.worldy(), dst / 2, 4f);
        }

        if ((cur.block.group != BlockGroup.transportation && canAccept(cur.block)))
            Drawf.selected(cur, Tmp.c1.set(Color.red).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));

        //准备下一迭代
        if (!forwardBuild.addUnique(cur)) return; //循环直接卡出，导致寻路不准，但问题应该不大
        //if (cur.block.group != BlockGroup.transportation) return;

        //处理超传
        conduit = cur.block.instantTransfer ? conduit + 1 : 0;

        //默认的下一个
        int from = cur.relativeToEdge(last.tile);

        if (cur instanceof CoreBlock.CoreBuild || cur instanceof StorageBlock.StorageBuild) {
            Drawf.selected(cur, Tmp.c1.set(Color.red).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));
        } else if (cur instanceof OverflowGate.OverflowGateBuild || cur instanceof Router.RouterBuild
                || cur instanceof DuctRouter.DuctRouterBuild) { // 三向任意口，留意暂不处理路由器回流，即便这是可行的
            dumpForward(cur, last, conduit);
        } else if (cur instanceof Sorter.SorterBuild sb) {  // 分类的特殊情况
            if (sb.sortItem != null || !((Sorter) sb.block).invert) forward(cur.nearby((from + 1) % 4), cur, conduit);
            if (sb.sortItem != null || ((Sorter) sb.block).invert) forward(cur.nearby((from + 2) % 4), cur, conduit);
            if (sb.sortItem != null || !((Sorter) sb.block).invert) forward(cur.nearby((from + 3) % 4), cur, conduit);
        } else if (cur instanceof DuctBridge.DuctBridgeBuild ductBridgeBuild) {
            if (ductBridgeBuild.arcLinkValid()) forward(ductBridgeBuild.findLink(), cur);
            else forward(cur.front(), cur);
        } else if (cur instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild && ! (cur instanceof LiquidBridge.LiquidBridgeBuild)) {
            if (world.tile(itemBridgeBuild.link) != null) forward(world.tile(itemBridgeBuild.link).build, cur);
            else if (itemBridgeBuild.incoming.size == 0) return;
            else dumpNearby(cur, last, conduit);
        } else if (cur instanceof MassDriver.MassDriverBuild massDriverBuild && massDriverBuild.arcLinkValid()) {
            forward(world.tile(massDriverBuild.link).build, cur);
            if (massDriverBuild.state == MassDriver.DriverState.idle || massDriverBuild.state == MassDriver.DriverState.accepting)
                dumpNearby(cur, last, conduit);
        } else if (cur instanceof Conveyor.ConveyorBuild || cur instanceof Duct.DuctBuild || cur instanceof StackConveyor.StackConveyorBuild) {
            forward(cur.front(), cur);
            if (cur instanceof StackConveyor.StackConveyorBuild stackConveyorBuild && stackConveyorBuild.state == 2)
                dumpForward(cur, last, conduit);
        } else if (cur instanceof Junction.JunctionBuild) {
            forward(cur.nearby((from + 2) % 4), cur, conduit);
        } else {
            return;
        }
    }

    private static void dumpForward(Building cur, Building last, int conduit) {
        int from = cur.relativeToEdge(last.tile);
        forward(cur.nearby((from + 1) % 4), cur, conduit);
        forward(cur.nearby((from + 2) % 4), cur, conduit);
        forward(cur.nearby((from + 3) % 4), cur, conduit);
    }

    private static void dumpNearby(Building cur, Building last, int conduit) {
        cur.proximity.each(building -> {
            if (cur.canDump(building, Items.copper) && (building != null && canAccept(building.block)))
                forward(building, cur, conduit);
        });
    }

    private static void drawBuild() {
        forwardBuild.each(building -> Drawf.selected(building, Tmp.c1.set(Color.red).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f)));

        forwardIndex = 0;
        forwardBuild.clear();
    }

    private static boolean canAccept(Block block) {
        if (block.group == BlockGroup.transportation) return true;
        for (Item item : content.items()) {
            if (block.consumesItem(item) || block.itemCapacity > 0) {
                return true;
            }
        }
        return false;
    }



    public static Seq<Point> path = new Seq<>(), source = new Seq<>();
    public static void detailTransporter2() {
        if (!control.input.arcScanMode) return;

        //check tile being hovered over
        Tile hoverTile = world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if (hoverTile == null || hoverTile.build == null || !hoverTile.build.isDiscovered(player.team())) {
            return;
        }

        path.clear();
        source.clear();
        getBackwardPath(new Point(hoverTile.build, null));
        drawPath(false);

        path.clear();
        source.clear();
        getForwardPath(new Point(hoverTile.build, null));
        drawPath(true);
    }

    public static void getBackwardPath(Point point) {
        if (point.build == null || path.size > maxLoop) return;
        if (!point.trans){
            source.add(point);
            return;
        }

        Point same = path.find(other -> point.build == other.build && (other.from == null || point.from.build == other.from.build));
        if (same != null) {
            if (point.conduit >= same.conduit) return;
            else path.replace(same, point);
        }
        else path.add(point);

        Seq<Point> previous = getPrevious(point);
        if (previous.any()) {
            previous.each(arcScanMode::getBackwardPath);
        }
        else {
            source.add(point);
        }
    }

    public static void getForwardPath(Point point) {
        if (point.build == null || path.size > maxLoop) return;
        if (!point.trans){
            source.add(point);
            return;
        }

        Point same = path.find(other -> point.build == other.build && (other.from == null || point.from.build == other.from.build));
        if (same != null) {
            if (point.conduit >= same.conduit) return;
            else path.replace(same, point);
        }
        else path.add(point);

        Seq<Point> next = getNext(point);
        if (next.any()) {
            next.each(arcScanMode::getForwardPath);
        }
        else {
            source.add(point);
        }
    }

    public static Seq<Point> getPrevious(Point point) {
        Building build = point.build;
        if (build == null) return new Seq<>();
        Seq<Point> previous = new Seq<>();
        //质驱
        if (build instanceof MassDriver.MassDriverBuild) {
            //暂时搞不定
        }//桥
        else if (build instanceof ItemBridge.ItemBridgeBuild bridge && ! (build instanceof LiquidBridge.LiquidBridgeBuild)) {
            bridge.incoming.each(pos -> previous.add(new Point(world.tile(pos).build, point)));
        }//导管桥
        else if (build instanceof DirectionBridge.DirectionBridgeBuild bridge) {
            for (Building b : bridge.occupied) {
                if (b != null) {
                    previous.add(new Point(b, point));
                }
            }
        }
        for (Building b : build.proximity) {
            Point from = new Point(b, b.relativeTo(build), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if (canInput(point, b, true) && canOutput(from, build, false)) {
                previous.add(from);
            } else if (canOutput(from, build, false)) {
                from.trans = false;
                previous.add(from);
            }
        }
        return previous;
    }

    public static Seq<Point> getNext(Point point) {
        Building build = point.build;
        if (build == null) return new Seq<>();
        Seq<Point> next = new Seq<>();
        //质驱
        if (build instanceof MassDriver.MassDriverBuild massDriverBuild) {
            if (massDriverBuild.arcLinkValid()) {
                next.add(new Point(world.build(massDriverBuild.link), point));
            }
        }//桥
        else if (build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild && !(build instanceof LiquidBridge.LiquidBridgeBuild)) {
            if (itemBridgeBuild.arcLinkValid()) {
                next.add(new Point(world.build(itemBridgeBuild.link), point));
            }
        }//导管桥
        else if (build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild) {
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            if (link != null) {
                next.add(new Point(link, point));
            }
        }

        for (Building b : build.proximity) {
            Point to = new Point(b, build.relativeTo(b), b.block.instantTransfer ? point.conduit + 1 : 0, point);
            if (canInput(to, build, false) && canOutput(point, b, true)) {
                next.add(to);
            } else if (canInput(to, build, false)) {
                to.trans = false;
                next.add(to);
            }
        }
        return next;
    }

    public static boolean canInput(Point point, Building from, boolean active) {
        Building build = point.build;
        if (build == null || from == null) return false;
        if (from.block.instantTransfer && point.conduit > 2) return false;
        //装甲传送带
        if (build instanceof ArmoredConveyor.ArmoredConveyorBuild) {
            return from != build.front() && (from instanceof Conveyor.ConveyorBuild || from == build.back());
        }//装甲导管
        else if (build instanceof Duct.DuctBuild ductBuild && ((Duct) ductBuild.block).armored) {
            return from != build.front() && (from.block.isDuct || from == build.back());
        }//传送带和导管
        else if (build instanceof Conveyor.ConveyorBuild || build instanceof Duct.DuctBuild) {
            return from != build.front();
        }//塑钢带
        else if (build instanceof StackConveyor.StackConveyorBuild stackConveyorBuild) {
            return switch (stackConveyorBuild.state) {
                case 2 -> from == build.back() && from instanceof StackConveyor.StackConveyorBuild;
                case 1 -> from != build.front();
                default -> from instanceof StackConveyor.StackConveyorBuild;
            };
        }//交叉器
        else if (build instanceof Junction.JunctionBuild) {
            return point.facing == -1 || from.relativeTo(build) == point.facing;
        }//分类
        else if (build instanceof Sorter.SorterBuild sorterBuild) {
            return !active || build.relativeTo(from) != point.facing && (sorterBuild.sortItem != null || (from.relativeTo(build) == point.facing) == ((Sorter) sorterBuild.block).invert);
        }//溢流
        else if (build instanceof OverflowGate.OverflowGateBuild) {
            return !active || build.relativeTo(from) != point.facing;
        }//导管路由器与导管溢流
        else if (build instanceof DuctRouter.DuctRouterBuild || build instanceof OverflowDuct.OverflowDuctBuild) {
            return from == build.back();
        }//桥
        else if (build instanceof ItemBridge.ItemBridgeBuild itemBridgeBuild) {
            return itemBridgeBuild.arcCheckAccept(from);
        }//导管桥
        else if (build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild) {
            return directionBridgeBuild.arcCheckAccept(from);
        }
        else if (build instanceof Router.RouterBuild) {
            return true;
        }else if (canAccept(build.block)) {
            point.trans = false;
            return true;
        }
        return false;
    }

    public static boolean canOutput(Point point, Building to, boolean active) {
        Building build = point.build;
        if (build == null || to == null) return false;
        if (to.block.instantTransfer && point.conduit > 2) return false;
        //传送带和导管
        if (build instanceof Conveyor.ConveyorBuild || build instanceof Duct.DuctBuild) {
            return to == build.front();
        }//塑钢带
        else if (build instanceof StackConveyor.StackConveyorBuild stackConveyor) {
            if (stackConveyor.state == 2 && ((StackConveyor) stackConveyor.block).outputRouter) {
                return to != build.back();
            }
            return to == build.front();
        }//交叉器
        else if (build instanceof Junction.JunctionBuild) {
            return point.facing == -1 || build.relativeTo(to) == point.facing;
        }//分类
        else if (build instanceof Sorter.SorterBuild sorterBuild) {
            return !active || to.relativeTo(build) != point.facing && (sorterBuild.sortItem != null || (build.relativeTo(to) == point.facing) == ((Sorter) sorterBuild.block).invert);
        }//溢流
        else if (build instanceof OverflowGate.OverflowGateBuild) {
            return !active || to.relativeTo(build) != point.facing;
        }//导管路由器与导管溢流
        else if (build instanceof DuctRouter.DuctRouterBuild || build instanceof OverflowDuct.OverflowDuctBuild) {
            return to != build.back();
        }//桥
        else if (build instanceof ItemBridge.ItemBridgeBuild bridge) {
            return bridge.arcCheckDump(to);
        }//导管桥
        else if (build instanceof DirectionBridge.DirectionBridgeBuild directionBridgeBuild) {
            DirectionBridge.DirectionBridgeBuild link = directionBridgeBuild.findLink();
            return link == null && build.relativeTo(to) == build.rotation;
        }
        else if (build instanceof Router.RouterBuild || build instanceof Unloader.UnloaderBuild) {
            return true;
        } else if (build instanceof GenericCrafter.GenericCrafterBuild) {
            point.trans = false;
            return true;
        }
        return false;
    }

    public static void drawPath(boolean forward) {
        Color mainColor = forward ? Color.valueOf("80ff00") : Color.valueOf("ff8000");
        Color highlightColor =  forward ? Color.valueOf("00cc00") : Color.red;
        path.each(p -> {
            if (p.from != null && p.trans) {
                float x1 = p.build.tile.drawx(), y1 = p.build.tile.drawy();
                float x2 = p.from.build.tile.drawx(), y2 = p.from.build.tile.drawy();

                Draw.color(mainColor);
                Draw.color(Tmp.c1.set(mainColor).a(Mathf.absin(4f, 1f) * 0.4f + 0.6f));
                Lines.stroke(1.5f);
                Lines.line(x1, y1, x2, y2);
            }
            else {
                Drawf.selected(p.build, Tmp.c1.set(highlightColor).a(Mathf.absin(4f, 1f) * 0.5f + 0.5f));
            }
            Draw.reset();
        });
        path.each(p -> {
            if (p.from != null && p.trans) {
                float x1 = p.build.tile.drawx(), y1 = p.build.tile.drawy();
                float x2 = p.from.build.tile.drawx(), y2 = p.from.build.tile.drawy();
                float dst = Mathf.dst(x1, y1, x2, y2);

                Draw.color(highlightColor);
                Fill.circle(x1, y1, 1.8f);

                if (dst > tilesize) {
                    Draw.color(highlightColor);
                    if (forward) {
                        Drawf.simpleArrow(x2, y2, x1, y1, dst / 2, 3f);
                    }
                    else {
                        Drawf.simpleArrow(x1, y1, x2, y2, dst / 2, 3f);
                    }
                }
            }
            Draw.reset();
        });
    }
    public static class Point{
        public Building build;
        public byte facing = -1;
        public int conduit = 0;
        //用于记录端点方块
        public boolean trans = true;

        public Point from;
        public Point(Building build, Point from) {
            this.build = build;
            this.from = from;
        }
        public Point(Building build, byte facing, int conduit, Point from) {
            this.build = build;
            this.facing = facing;
            this.conduit = conduit;
            this.from = from;
        }
    }
}