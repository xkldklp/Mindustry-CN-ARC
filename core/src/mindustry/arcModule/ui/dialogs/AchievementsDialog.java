package mindustry.arcModule.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.service.Achievement;
import mindustry.service.SStat;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.bundle;
import static mindustry.Vars.*;

public class AchievementsDialog extends BaseDialog {
    private ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();
    private String searchTxt = "";
    private Table browserTable;
    private Table SStatTable;


    public AchievementsDialog() {
        super("ARC-统计与成就");

        cont.pane(t -> {
            SStatTable = t;
            t.row();
            browserTable = t;
        }).scrollX(false);
        addCloseButton();

        shown(this::rebuildBrowser);
        onResize(this::rebuildBrowser);
    }

    private void rebuildBrowser() {
        int cols = (int) Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);
        SStatTable.clear();
        browserTable.clear();

        SStatTable.add("统计").color(getThemeColor()).pad(cols / 2).center().row();
        SStatTable.image().color(getThemeColor()).fillX().row();
        SStatTable.table(t -> {
            for (SStat stat : SStat.values()) {
                String name = Core.bundle.get("sstat." + stat.name() + ".name");
                t.add(name).left();
                if (stat.name() == "arcPlayTime") t.add("                 " + formatTime(stat.get())).left();
                else t.add("                 " + stat.get()).left();
                t.row();
            }
        });

        browserTable.row();
        browserTable.add("成就").color(getThemeColor()).pad(cols / 2).center().row();
        browserTable.image().color(getThemeColor()).fillX().row();
        browserTable.table(t -> {
            int i = 0;
            float s = 64f;

            for (Achievement ach : Achievement.all) {
                String name = Core.bundle.get("achievement." + ach.name() + ".name");
                String desc = (ach.isAchieved() ? "[stat]" : "") + Core.bundle.get("achievement." + ach.name() + ".desc");
                //if( !Strings.matches(searchTxt, name) && !Strings.matches(searchTxt, desc) ) continue;

                t.button(con -> {
                    con.setColor(ach.isAchieved() ? Pal.accent : Color.lightGray);
                    con.margin(0f);
                    con.left();

                    con.add(new BorderImage() {
                        TextureRegion last;

                        {
                            border(ach.isAchieved() ? Pal.accent : Color.lightGray);
                            setDrawable(Tex.nomap);
                            pad = Scl.scl(4f);
                        }

                        @Override
                        public void draw() {
                            super.draw();

                            //TODO draw the sprite of the achievement

                            //textures are only requested when the rendering happens; this assists with culling
                        /*
                        if(!textureCache.containsKey(repo)){
                            textureCache.put(repo, last = Core.atlas.find("nomap"));
                            Http.get("https://raw.githubusercontent.com/Anuken/MindustryMods/master/icons/" + repo.replace("/", "_"), res -> {
                                Pixmap pix = new Pixmap(res.getResult());
                                Core.app.post(() -> {
                                    try{
                                        var tex = new Texture(pix);
                                        tex.setFilter(TextureFilter.linear);
                                        textureCache.put(repo, new TextureRegion(tex));
                                        pix.dispose();
                                    }catch(Exception e){
                                        Log.err(e);
                                    }
                                });
                            }, err -> {});
                        }
                        var next = textureCache.get(repo);
                        if(last != next){
                            last = next;
                            setDrawable(next);
                        }
                        */
                        }
                    }).size(s).pad(4f * 2f);

                    con.add("[accent]" + name +
                            "\n[lightgray]" + desc
                    ).width(358f).wrap().grow().pad(4f, 2f, 4f, 6f).top().left().labelAlign(Align.topLeft);

                }, Styles.flatBordert, () -> {
                }).padRight(4f);

                if (++i % cols == 0) t.row();
            }


        });


        browserTable.row();
        browserTable.table(t->{
            t.button("[red]重置成就",()-> ui.showConfirm("你确定要重置所有成就吗",()->{
                for (Achievement ach : Achievement.all){
                    ach.removeAchieve();
                }
                ui.arcInfo("已移除所有成就");
                rebuildBrowser();
            })).height(50f).width(200f);
            if(Core.settings.getBool("otherCheat"))
                t.button("[red]一键完成",()-> ui.showConfirm("你确定要一键完成所有成就吗",()->{
                    for (Achievement ach : Achievement.all){
                        ach.complete();
                    }
                    ui.arcInfo("已完成所有成就");
                    rebuildBrowser();
                })).height(50f).width(200f);
        });
    }

    private String formatTime(int time) {
        StringBuilder outTime = new StringBuilder();
        int h = time / 3600;
        int m = (time / 60) % 60;
        int s = time % 60;
        outTime.append(h).append(" : ").append(m).append(" : ").append(s);
        return outTime.toString();
    }
}