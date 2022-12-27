package mindustry.arcModule.toolpack;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.core.UI;
import mindustry.game.EventType;
import mindustry.game.SpawnGroup;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class arcWaveSpawner {

    public static boolean hasFlyer = true;

    public static float flyerSpawnerRadius = 5f * tilesize;

    static float spawnerMargin = tilesize * 11f;

    public static Seq<waveInfo> arcWave = new Seq<>();

    static {
        Events.on(EventType.WorldLoadEvent.class, event -> {
            hasFlyer = false;
            for (SpawnGroup sg : state.rules.spawns) {
                if (sg.type.flying) {
                    hasFlyer = true;
                    break;
                }
            }
            initArcWave();
        });
    }

    public static void drawSpawner() {
        if (state.hasSpawns()) {
            Lines.stroke(2f);
            Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));

            if (Core.settings.getBool("alwaysshowdropzone")) {
                Draw.alpha(0.8f);
                for (Tile tile : spawner.getSpawns()) {
                    Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                }
            } else {
                for (Tile tile : spawner.getSpawns()) {
                    if (tile.within(player.x, player.y, state.rules.dropZoneRadius + spawnerMargin)) {
                        Draw.alpha(Mathf.clamp(1f - (player.dst(tile) - state.rules.dropZoneRadius) / spawnerMargin));
                        Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                    }
                }
            }
            if (hasFlyer && Core.settings.getBool("showFlyerSpawn") && spawner.countSpawns() < 20) {
                for (Tile tile : spawner.getSpawns()) {
                    float angle = Angles.angle(world.width() / 2f, world.height() / 2f, tile.x, tile.y);
                    float trns = Math.max(world.width(), world.height()) * Mathf.sqrt2 * tilesize;
                    float spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(angle, trns), 0, world.width() * tilesize);
                    float spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(angle, trns), 0, world.height() * tilesize);
                    if (Core.settings.getBool("showFlyerSpawnLine")) {
                        Draw.color(Color.red, 0.5f);
                        Lines.line(tile.worldx(), tile.worldy(), spawnX, spawnY);
                    }
                    Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));
                    Draw.alpha(0.8f);
                    Lines.dashCircle(spawnX, spawnY, flyerSpawnerRadius);

                    Draw.color();
                    Draw.alpha(0.5f);
                    Draw.rect(UnitTypes.zenith.fullIcon, spawnX, spawnY);
                }
            }
            Draw.reset();
        }
    }


    /* 用于检查可能未成功的初始化 */
    public static void checkInit() {
        if (arcWave.size == 0) initArcWave();
    }

    public static void initArcWave() {
        initArcWave((int) (calWinWave() * 1.5f));
    }

    public static void initArcWave(int wave) {
        arcWave = new Seq<>();
        for (int waveN = 0; waveN < wave; waveN++) {
            arcWave.add(new waveInfo(waveN));
        }
    }

    public static int calWinWave() {
        if (state.rules.winWave >= 1) return state.rules.winWave;
        int maxwave = 0;
        for (SpawnGroup group : state.rules.spawns) {
            if (group.end > 99999) continue;
            maxwave = Math.max(maxwave, group.end);
        }
        if (maxwave > 5000) return 200;
        if (maxwave < 2 && state.rules.waveSpacing > 30f) return (int) (1800000 / state.rules.waveSpacing);
        return maxwave + 1;
    }

    /**
     * 单一波次详情
     */
    public static class waveInfo {
        public int waveIndex;
        public Seq<waveGroup> groups = new Seq<>();

        public int amount = 0, amountL = 0;

        public float health = 0, effHealth = 0, dps = 0;
        /**
         * 临时数据记录
         */
        public long healthL = 0, effHealthL = 0, dpsL = 0;

        waveInfo(int waveIndex) {
            this.waveIndex = waveIndex;
            for (SpawnGroup group : state.rules.spawns) {
                int amount = group.getSpawned(waveIndex);
                if (amount == 0) continue;
                groups.add(new waveGroup(waveIndex, group));
            }
            initProperty();
        }

        private void initProperty() {
            groups.each(group -> {
                amount += group.amountT;
                health += group.healthT;
                effHealth += group.effHealthT;
                dps += group.dpsT;
            });
        }

        public void specLoc(int spawn, Boolf<SpawnGroup> pre) {
            amountL = 0;
            healthL = 0;
            effHealthL = 0;
            dpsL = 0;
            groups.each(waveGroup -> (spawn == -1 || waveGroup.group.spawn == -1 ||waveGroup.group.spawn == spawn) && pre.get(waveGroup.group),
                group -> {
                    amountL += group.amountT;
                    healthL += group.healthT;
                    effHealthL += group.effHealthT;
                    dpsL += group.dpsT;
                });
        }

        public Table proTable(boolean doesRow){
            if (amountL == 0) return new Table(t ->t.add("该波次没有敌人"));
            return new Table(t->{
                t.add("\uE86D").width(50f);
                t.add("[accent]" + amountL).growX().padRight(50f);
                if (doesRow) t.row();
                t.add("\uE813").width(50f);
                t.add("[accent]" + UI.formatAmount(healthL, 2)).growX().padRight(50f);
                if (doesRow) t.row();
                if (effHealthL != healthL) {
                    t.add("\uE810").width(50f);
                    t.add("[accent]" + UI.formatAmount(effHealthL, 2)).growX().padRight(50f);
                    if (doesRow) t.row();
                }
                t.add("\uE86E").width(50f);
                t.add("[accent]" + UI.formatAmount(dpsL, 2)).growX();
            });
        }

        public Table unitTable(int spawn, Boolf<SpawnGroup> pre){
            return unitTable(spawn,pre,10);
        }

        public Table unitTable(int spawn, Boolf<SpawnGroup> pre, int perCol){
            int[] count = new int[1];
            return new Table(t->{
                groups.each(waveGroup -> (spawn == -1 || waveGroup.group.spawn == -1 ||waveGroup.group.spawn == spawn) && pre.get(waveGroup.group),wg -> {
                    count[0] ++;
                    if (count[0] % perCol == 0) t.row();
                    StringBuilder groupInfo = new StringBuilder();
                    groupInfo.append(wg.group.type.emoji());
                    if (wg.group.spawn != -1 && spawn == -1) groupInfo.append("*");

                    groupInfo.append(wg.group.type.typeColor());

                    groupInfo.append("\n").append(wg.amount);
                    groupInfo.append("\n");
                    if (wg.shield > 0f)
                        groupInfo.append(UI.formatAmount((long) wg.shield));
                    groupInfo.append("\n[]");
                    if (wg.group.effect != null && wg.group.effect != StatusEffects.none)
                        groupInfo.append(wg.group.effect.emoji());
                    if (wg.group.items != null && wg.group.items.amount > 0)
                        groupInfo.append(wg.group.items.item.emoji());
                    if (wg.group.payloads != null && wg.group.payloads.size > 0)
                        groupInfo.append("\uE87B");
                    t.add(groupInfo.toString()).height(80f).width(70f);
                });
            });
        }

    }

    /**
     * 一种更为详细的spawnGroup
     */
    public static class waveGroup {
        public int waveIndex;
        public SpawnGroup group;
        public int amount,amountT;
        public float shield, health, effHealth, dps;
        public float healthT, effHealthT, dpsT;

        public waveGroup(int waveIndex, SpawnGroup group) {
            this.waveIndex = waveIndex;
            this.group = group;
            this.amount = group.getSpawned(waveIndex);
            this.shield = group.getShield(waveIndex) * amount;   //盾
            this.health = (group.type.health + shield) * amount;   //盾+血
            this.dps = group.type.estimateDps() * amount;
            this.effHealth = health;
            if (group.effect != null) {
                this.effHealth *= group.effect.healthMultiplier;
                this.dps *= group.effect.damageMultiplier * group.effect.reloadMultiplier;
            }

            int multiplier = group.spawn != -1 || spawner.countSpawns() < 2 ? 1 : spawner.countSpawns();
            this.amountT = amount * multiplier;
            this.healthT = health * multiplier;
            this.effHealthT = effHealth * multiplier;
            this.dpsT = dps * multiplier;
        }
    }
}