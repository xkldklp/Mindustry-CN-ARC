package mindustry.arcModule;

import arc.*;
import arc.audio.Sound;
import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.DesktopInput;

import static mindustry.Vars.*;

public class voiceControl{
    /** 冷却时间 */
    private static float timeInt = 30 * 60;

    public static float lastTime = 0;

    static{
        Events.run(WorldLoadEvent.class, () -> {
            play(voiceType.welcome);
        });

        Events.run(WaveEvent.class,() -> {
            if(state.wavetime < 120f) return;
            play(voiceType.waveCautions);
        });
    }

    public static void voiceControlDialog(){

    }


    public static boolean play(voiceType voiceType){
        //voiceType.sound.play();
        if(Time.time - lastTime > timeInt && Core.settings.getBool("atriVoice")){
            voiceType.sound.play();
            ui.arcInfo("[cyan]亚托莉：[white] " + voiceType.text);
            lastTime = Time.time;
            return true;
        }
        return false;
    }

    public enum voiceType{
        welcome(Sounds.arcWelcome,"欢迎回来"),
        waveCautions(Sounds.wavecautions,"敌人出现了，请小心");

        public Sound sound;
        public String type;
        public String name;
        public String text;

        /** 几级显示  <0:none;1:重要;2：正常；3：丰富>*/
        public int level;
        /** 是否显示*/
        public Boolean show = true;

        voiceType(Sound sound,String type,String name,String text,int level,Boolean show){
            this.sound = sound;
            this.name = name;
            this.type = type;
            this.text = text;
            this.level = level;
            this.show = show;
        }

        voiceType(Sound sound,String text){
            this(sound,"","",text,0,true);
        }

    }
}
