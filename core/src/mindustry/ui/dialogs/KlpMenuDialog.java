package mindustry.ui.dialogs;

import arc.Core;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.input.Binding;

import static arc.Core.input;

public class KlpMenuDialog extends BaseDialog{
    Table settings = new Table();
    ScrollPane pane = new ScrollPane(settings);

    public KlpMenuDialog(){
        super("快捷设置");
        addCloseKey();

        shouldPause = true;

        shown(this::rebuildMenu);
        onResize(this::rebuildMenu);
    }

    void rebuildMenu(){
        cont.clear();
        settings.clear();

        cont.add("[cyan]快捷功能菜单").top().row();
        cont.add("[lightgray]优先级从上到下").top().row();

        settings.add("[yellow]------------------单位转向------------------").row();
        addCheckSetting("角度锁定", "角度锁定" , "使你单位的角度锁定，只能由射击更改角度");
        addCheckSetting("建筑时不会面朝建筑", "建筑时不会面朝建筑" , "船辅拯救者");
        addCheckSetting("瞬间转向", "瞬间转向" , "使单位无需转头时间，瞬间转向");
        addSliderSetting("单位转向速度", "单位转向速度", 0f, 5f, 20, 1f, "单位转向速度倍率" );

        settings.add("[yellow]------------------单位移动------------------").row();
        addCheckSetting("我爱角度移动", "我爱角度移动" , "你是否喜爱anuke的坦克和船只移动？开启他！(优先级大于没有角度移动)");
        addCheckSetting("没有角度移动", "没有角度移动" , "你是否痛恨anuke的坦克和船只移动？开启他！");
        addSliderSetting("单位移动速度", "单位移动速度", 0f, 4f, 20, 1f, "单位移动速度倍率" );

        settings.add("[yellow]------------------单位碰撞------------------").row();
        addCheckSetting("刷怪圈也阻止不了我对你的爱", "刷怪圈也阻止不了我对你的爱" , "乌拉");
        addCheckSetting("单位不碰撞", "单位不碰撞" , "叠叠乐..?你不会用他玩除了叠叠乐以外的东西..对吧?");
        addSliderSetting("地图边界拓展", "地图边界拓展",0f , 250f / 8, 30, 0f, "拓展后单位不会在拓展后边界受到神秘力量(31格外会瞬间死亡！)");
        addSliderSetting("被击退倍率", "被击退倍率",0f , 2f, 8, 1f, "抗击退！");
        cont.add(pane).center();
    }

    private void addCheckSetting(String text, String setting, @Nullable String desc){
        settings.check(text, Core.settings.getBool(setting, false), b -> Core.settings.put(setting, !Core.settings.getBool(setting,false))).tooltip(desc).row();
    }
    private void addSliderSetting(String text, String setting, Float min, Float max, Integer steps, Float def, @Nullable String desc){
        settings.table(table -> {
            table.add(text);
            TextField field = table.field(String.valueOf(Core.settings.getFloat(setting, def)) , t -> {
                if(Strings.canParsePositiveFloat(t)) Core.settings.put(setting, Float.parseFloat(t));
            }).tooltip(desc).valid(Strings::canParsePositiveFloat).get();
            Slider slider = table.slider(min, max, (max - min) / steps, Core.settings.getFloat(setting, def), n -> {
                Core.settings.put(setting, n);
                field.setText(String.valueOf(Core.settings.getFloat(setting, def)));
            }).get();
            table.button("重置", () -> slider.setValue(def));
        });
        settings.row();
    }

    public void addCloseKey(){
        keyDown(key -> {
            if(input.keyDown(Binding.klpMenu) || input.keyDown(Binding.menu)){
                hide();
            }
        });
    }
}

