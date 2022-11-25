package mindustry.arcModule;

import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.type.ItemStack;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ui.auxilliary.AuxilliaryTable.teamMark;

public class RFuncs {

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
