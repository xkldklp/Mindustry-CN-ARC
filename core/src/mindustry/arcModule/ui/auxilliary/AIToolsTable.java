package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.util.Strings;
import mindustry.ai.types.*;
import mindustry.arcModule.*;
import mindustry.arcModule.ai.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;

import static mindustry.Vars.*;
import static mindustry.Vars.getThemeColor;
import static mindustry.arcModule.ai.ArcMinerAI.*;
import static mindustry.arcModule.ui.RStyles.*;
import static mindustry.content.UnitTypes.*;
import static mindustry.ui.Styles.flatToggleMenut;

public class AIToolsTable extends BaseToolsTable{
    private AIController selectAI;

    public AIToolsTable(){
        super(Icon.android);

        Events.run(EventType.Trigger.update, () -> {
            if(selectAI != null){
                selectAI.unit(player.unit());
                selectAI.updateUnit();
            }
        });
    }

    @Override
    public void setup(){
        clear();
        button(Icon.settingsSmall, clearLineNoneTogglei, 30, this::arcAISettingDialog).checked(t -> false);
        aiButton(new ArcMinerAI(), mono, "矿机AI");
        aiButton(new ArcBuilderAI(), poly, "重建AI");
        aiButton(new ArcRepairAI(), mega, "修复AI");
        aiButton(new DefenderAI(), oct, "保护AI");
    }

    private void aiButton(AIController ai, UnitType symbol, String describe){
        var button = button(new TextureRegionDrawable(symbol.uiIcon), clearLineNoneTogglei, 30, () -> selectAI(ai))
        .checked(b -> selectAI == ai).size(40).get();

        ElementUtils.tooltip(button, describe);
    }

    private void selectAI(AIController ai){
        selectAI = selectAI == ai ? null : ai;
    }

    private void arcAISettingDialog(){
        int cols = (int)Math.max(Core.graphics.getWidth() / Scl.scl(480), 1);
        
        BaseDialog dialog = new BaseDialog("ARC-AI设定器");

        dialog.cont.table(t -> {
            t.add("minerAI-矿物筛选器").color(getThemeColor()).pad(cols / 2f).center().row();

            t.image().color(getThemeColor()).fillX().row();

            t.table(c -> {
                c.add("地表矿").row();

                c.table(list -> {
                    int i = 0;
                    for(Block block : ArcMinerAI.oreAllList){
                        if(indexer.floorOresCount[block.id] == 0) continue;
                        if(i++ % 3 == 0) list.row();
                        list.button(block.emoji() + "\n" + indexer.floorOresCount[block.id], flatToggleMenut, () -> {
                            if(oreList.contains(block)) oreList.remove(block);
                            else if(!oreList.contains(block)) oreList.add(block);
                        }).tooltip(block.localizedName).checked(k -> oreList.contains(block)).width(100f).height(50f);
                    }
                }).row();

                c.add("墙矿").row();

                c.table(list -> {
                    int i = 0;
                    for(Block block : oreAllWallList){
                        if(indexer.wallOresCount[block.id] == 0) continue;
                        if(i++ % 3 == 0) list.row();
                        list.button(block.emoji() + "\n" + indexer.wallOresCount[block.id], flatToggleMenut, () -> {
                            if(oreWallList.contains(block)) oreWallList.remove(block);
                            else if(!oreWallList.contains(block)) oreWallList.add(block);
                        }).tooltip(block.localizedName).checked(k -> oreWallList.contains(block)).width(100f).height(50f);
                    }
                }).row();

            }).growX();
        }).growX();

        dialog.cont.row();

        dialog.cont.table(t -> {
            t.add("builderAI").color(getThemeColor()).pad(cols / 2f).center().row();

            t.image().color(getThemeColor()).fillX().row();

            t.table(tt -> {
                tt.add("重建冷却时间: ");

                TextField sField = tt.field(ArcBuilderAI.rebuildTime + "", text -> {
                    ArcBuilderAI.rebuildTime = Math.max(5f, Float.parseFloat(text));
                    setup();
                }).valid(Strings::canParsePositiveFloat).width(200f).get();

                tt.slider(5, 200, 5, i -> {
                    ArcBuilderAI.rebuildTime = i;
                    sField.setText(ArcBuilderAI.rebuildTime + "");
                }).width(200f);
            }).growX();
        }).growX();

        dialog.addCloseButton();
        dialog.show();
    }

}
