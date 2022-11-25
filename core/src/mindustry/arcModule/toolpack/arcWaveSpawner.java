package mindustry.arcModule.toolpack;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.UnitTypes;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class arcWaveSpawner {

    static float spawnerMargin = tilesize * 11f;

    public static void drawSpawner() {
        if(state.hasSpawns()){
            Lines.stroke(2f);
            Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));

            if (Core.settings.getBool("alwaysshowdropzone")) {
                Draw.alpha(0.8f);
                for(Tile tile : spawner.getSpawns()) {
                    Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                }
            }
            else {
                for(Tile tile : spawner.getSpawns()) {
                    if (tile.within(player.x, player.y, state.rules.dropZoneRadius + spawnerMargin)) {
                        Draw.alpha(Mathf.clamp(1f - (player.dst(tile) - state.rules.dropZoneRadius) / spawnerMargin));
                        Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                    }
                }
            }
            if (Core.settings.getBool("showFlyerSpawn") && spawner.countSpawns() < 20) {
                for(Tile tile : spawner.getSpawns()) {
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
                    Lines.dashCircle(spawnX, spawnY, 5f * tilesize);

                    Draw.color();
                    Draw.alpha(0.5f);
                    Draw.rect(UnitTypes.zenith.fullIcon, spawnX, spawnY);
                }
            }
            Draw.reset();
        }
    }
}