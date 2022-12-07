package mindustry.arcModule;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.PixmapRegion;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.game.*;
import mindustry.gen.Call;
import mindustry.type.ItemStack;
import mindustry.type.Weather;
import mindustry.ui.*;
import mindustry.world.Block;

import static arc.graphics.Color.RGBtoHSV;
import static mindustry.Vars.*;
import static mindustry.arcModule.ui.auxilliary.AuxilliaryTable.teamMark;

public class RFuncs {

    static boolean colorized = false;

    public static void colorizeContent(){
        colorized = Core.settings.getBool("colorizedContent");
        content.items().each(c -> c.localizedName = colorized(c.color,c.localizedName));
        content.liquids().each(c -> c.localizedName = colorized(c.color,c.localizedName));
        content.statusEffects().each(c -> c.localizedName = colorized(c.color,c.localizedName));
        content.planets().each(c -> c.localizedName = colorized(c.atmosphereColor,c.localizedName));
        content.blocks().each(c -> {
            if (c.hasColor) c.localizedName = colorized(blockColor(c),c.localizedName);
            else if(c.itemDrop != null) c.localizedName = colorized(c.itemDrop.color,c.localizedName);
        });
        //content.getBy(ContentType.weather).each(  c ->  ((Weather)c).localizedName = "[#" + c..color + "]" + ((Weather)c).localizedName);
        //content.sectors().each(c -> c.localizedName = "[#" + c.planet.atmosphereColor + "]" + c.localizedName);

        //content.units().each(c -> c.localizedName = "[#" + c.outlineColor + "]" + c.localizedName);
    }

    private static String colorized(Color color, String name){
        if (colorized) return  "[#" + color + "]" + name + "[]";
        else return name;
    }

    private static Color blockColor(Block block){
        Color bc = new Color(0, 0, 0, 1);
        Color bestColor = new Color(0, 0, 0, 1);
        int highestS = 0;
        if(!block.synthetic()){
            PixmapRegion image = Core.atlas.getPixmap(block.fullIcon);
            for (int x=0; x<image.width;x++)
                for(int y=0;y<image.height;y++){
                    bc.set(image.get(x, y));
                    int s = RGBtoHSV(bc)[1] * RGBtoHSV(bc)[1] + RGBtoHSV(bc)[2] + RGBtoHSV(bc)[2];
                    if (s > highestS){
                        highestS = s;
                        bestColor = bc;
                    }
                }
        }else{
            return block.mapColor.cpy().mul(1.2f);
        }
        return bestColor;
    }

    public static String arcShareWaveInfo(int waves) {
        if (!state.rules.waves) return " ";
        StringBuilder builder = new StringBuilder(getPrefix("orange", "Wave"));
        builder.append("标记了第").append(waves).append("波");
        if (waves < state.wave) {
            builder.append("。");
        } else {
            if (waves > state.wave) {
                builder.append("，还有").append(waves - state.wave).append("波");
            }
            int timer = (int) (state.wavetime + (waves - state.wave) * state.rules.waveSpacing);
            builder.append("[").append(fixedTime(timer)).append("]。");
        }

        builder.append(arcWaveInfo(waves));
        return builder.toString();
    }

    public static String calWaveTimer(){
        StringBuilder waveTimer = new StringBuilder();
        waveTimer.append("[orange]");
        int m = ((int)state.wavetime / 60) / 60;
        int s = ((int)state.wavetime / 60) % 60;
        int ms = (int)state.wavetime % 60;
        if(m > 0){
            waveTimer.append(m).append("[white]: [orange]");
            if(s < 10){
                waveTimer.append("0");
            }
            waveTimer.append(s).append("[white]min");
        }else{
            waveTimer.append(s).append("[white].[orange]").append(ms).append("[white]s");
        }
        return waveTimer.toString();
    }

    public static String arcWaveInfo(int waves) {
        StringBuilder builder = new StringBuilder();
        if (state.rules.attackMode) {
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1) + Vars.spawner.countSpawns();
            builder.append("包含(×").append(sum).append(")");
        } else {
            builder.append("包含(×").append(Vars.spawner.countSpawns()).append("):");
        }
        for (SpawnGroup group : state.rules.spawns) {
            if (group.getSpawned(waves - 1) > 0) {
                builder.append((char) Fonts.getUnicode(group.type.name)).append("(");
                if (group.effect != StatusEffects.invincible && group.effect != StatusEffects.none && group.effect != null) {
                    builder.append((char) Fonts.getUnicode(group.effect.name)).append("|");
                }
                if (group.getShield(waves - 1) > 0) {
                    builder.append(UI.whiteformatAmount((int) group.getShield(waves - 1))).append("|");
                }
                builder.append(group.getSpawned(waves - 1)).append(")");
            }
        }
        return builder.toString();
    }

    public static String fixedColorTime(int timer) {
        return fixedColorTime(timer, true);
    }
    public static String fixedColorTime(int timer, boolean units) {
        StringBuilder str = new StringBuilder();
        str.append(timer > 0 ? "[orange]":"[acid]");
        timer = Math.abs(timer);
        int m = timer / 60 / 60;
        int s = timer / 60 % 60;
        int ms = timer % 60;
        if (m > 0) {
            str.append(m).append(": ");
            if (s < 10) {
                str.append("0");
            }

            str.append(s);
            if (units) str.append("min");
        } else {
            str.append(s).append(".").append(ms);
            if (units) str.append('s');
        }
        return str.toString();
    }

    public static String fixedTime(int timer, boolean units) {
        StringBuilder str = new StringBuilder();
        int m = timer / 60 / 60;
        int s = timer / 60 % 60;
        int ms = timer % 60;
        if (m > 0) {
            str.append(m).append(": ");
            if (s < 10) {
                str.append("0");
            }

            str.append(s);
            if (units) str.append("min");
        } else {
            str.append(s).append(".").append(ms);
            if (units) str.append('s');
        }
        return str.toString();
    }

    public static String fixedTime(int timer) {
        return fixedTime(timer, true);
    }

    public static StringBuilder getPrefix(Color color, String type) {
        return getPrefix("#" + color, type);
    }

    public static StringBuilder getPrefix(String color, String type) {
        StringBuilder prefix = new StringBuilder();
        if (teamMark) prefix.append("/t ");
        prefix.append(arcVersionPrefix);
        prefix.append("[" + color + "]");
        prefix.append("<" + type + ">");
        prefix.append("[white]");
        return prefix;
    }

    public static String abilitysFormat(String format, Object... values){
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] instanceof Number n ? "[stat]" + Strings.autoFixed(n.floatValue(), 1) + "[]" : "[white]" + values[i] + "[]";
        }
        return Strings.format("[lightgray]" + format.replace("~", "[#" + getThemeColor() + "]~[]"), values);
    }

}
