package mindustry.arcModule.ui.auxilliary;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.arcModule.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.input.DesktopInput;

import static mindustry.Vars.*;
import static mindustry.arcModule.ui.RStyles.*;
import static mindustry.content.UnitTypes.vela;

public class ScriptTable extends BaseToolsTable{

    public ScriptTable(){
        super(UnitTypes.gamma.uiIcon);
    }

    @Override
    protected void setup(){
        defaults().size(40);

        scriptButton(Blocks.buildTower.uiIcon, "在建造列表加入被摧毁建筑", () -> Vars.player.buildDestroyedBlocks());

        scriptButton(Blocks.message.uiIcon, "锁定上个标记点", () -> {
            Marker.lockonLastMark();
        });

        scriptButton(Items.copper.uiIcon, "一键装弹", () -> {
            Vars.player.dropItems();
        });

        scriptButton(Icon.modeAttack, "自动攻击", () -> {
            boolean at = Core.settings.getBool("autotarget");
            Core.settings.put("autotarget", !at);
            ui.arcInfo("已" + (at?"关闭":"开启") + "自动攻击");
        }, b -> Core.settings.getBool("autotarget"));

        scriptButton(vela.uiIcon, "强制助推", () -> {
            boolean ab = Core.settings.getBool("forceBoost");
            Core.settings.put("forceBoost", !ab);
            ui.arcInfo("已" + (ab?"关闭":"开启") + "强制助推");
        }, b -> Core.settings.getBool("forceBoost"));

        if(!mobile)
        scriptButton(Icon.eyeSmall, "取消追踪", () -> {
            boolean ab = Core.settings.getBool("removePan");
            if(control.input instanceof DesktopInput){
                ((DesktopInput) control.input).panning = true;
            }
            Core.settings.put("removePan", !ab);
            ui.arcInfo("已" + (ab?"取消":"开启") + "强制追踪控制单位");
        }, b -> Core.settings.getBool("removePan"));
    }

    private void scriptButton(Drawable region, String describe, Runnable runnable, Boolf<ImageButton> checked){
        scriptButton(region, clearLineNoneTogglei, describe, runnable).checked(checked);
    }

    private void scriptButton(TextureRegion region, String describe, Runnable runnable, Boolf<ImageButton> checked){
        scriptButton(new TextureRegionDrawable(region), clearLineNoneTogglei, describe, runnable).checked(checked);
    }

    private Cell<ImageButton> scriptButton(TextureRegion region, String describe, Runnable runnable){
        return scriptButton(new TextureRegionDrawable(region), clearLineNonei, describe, runnable);
    }

    private Cell<ImageButton> scriptButton(Drawable icon, ImageButtonStyle style, String describe, Runnable runnable){
        Cell<ImageButton> cell = button(icon, style, 30, runnable);

        ElementUtils.tooltip(cell.get(), describe);

        return cell;
    }

}
