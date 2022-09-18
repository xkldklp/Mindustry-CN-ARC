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

    void rebuildMenu() {
        cont.clear();
        settings.clear();

        cont.add("[cyan]快捷功能菜单").top().row();
        cont.add("[lightgray]优先级从上到下").top().row();

        addCheckSetting("[cyan]单位转向选项", "单位转向选项", null);
        if (getSettingB("单位转向选项")){
            settings.add("[yellow]------------------单位转向------------------").row();
            addCheckSetting("强制看向鼠标", "强制看向鼠标", "我就看着你！");
            addCheckSetting("角度锁定", "角度锁定", "使你单位的角度锁定，只能由射击更改角度");
            addCheckSetting("建筑时不会面朝建筑", "建筑时不会面朝建筑", "船辅拯救者");
            addCheckSetting("瞬间转向", "瞬间转向", "使单位无需转头时间，瞬间转向");
            addCheckSetting("推进转向", "推进转向", "推进时也可转向");
            addSliderSetting("单位转向速度", "单位转向速度", 0f, 5f, 20, 1f, "单位转向速度倍率");
            settings.add("[yellow]------------------单位转向------------------").row();
        }

        addCheckSetting("[cyan]单位移动选项", "单位移动选项", null);
        if (getSettingB("单位移动选项")){
            settings.add("[yellow]------------------单位移动------------------").row();
            addCheckSetting("我爱角度移动", "我爱角度移动", "你是否喜爱anuke的坦克和船只移动？开启他！(优先级大于没有角度移动)");
            addCheckSetting("没有角度移动", "没有角度移动", "你是否痛恨anuke的坦克和船只移动？开启他！");
            addSliderSetting("单位移动速度", "单位移动速度", 0f, 4f, 20, 1f, "单位移动速度倍率");
            settings.add("[yellow]------------------单位移动------------------").row();
        }

        addCheckSetting("[cyan]单位碰撞选项", "单位碰撞选项", null);
        if (getSettingB("单位碰撞选项")){
            settings.add("[yellow]------------------单位碰撞------------------").row();
            addCheckSetting("刷怪圈也阻止不了我对你的爱", "刷怪圈也阻止不了我对你的爱", "刷怪圈不排斥单位");
            addCheckSetting("单位不碰撞", "单位不碰撞", "叠叠乐..?你不会用他玩除了叠叠乐以外的东西..对吧?");
            addCheckSetting("反向击退", "反向击退", "反着来!");
            addSliderSetting("被击退倍率", "被击退倍率", 0f, 2f, 8, 1f, "抗击退！");
            addSliderSetting("地图边界拓展", "地图边界拓展", 0f, 250f / 8, 30, 0f, "拓展后单位不会在拓展后边界受到神秘力量(31格外会瞬间死亡！)");
            settings.add("[yellow]------------------单位碰撞------------------").row();
        }

        addCheckSetting("[cyan]玩家控制选项", "玩家控制选项", null);
        if (getSettingB("玩家控制选项")){
            settings.add("[yellow]------------------玩家控制------------------").row();
            addCheckSetting("强制控制单位", "强制控制单位", "抢我单位？");
            addCheckSetting("一键装填-工厂", "一键装填-工厂", "使一键装填可以向工厂丢入物品");
            addCheckSetting("一键装填-炮台", "一键装填-炮台", "使一键装填可以向炮台丢入物品");
            addCheckSetting("按住一键装填", "按住一键装填", "按住装填按键 然后疯狂装填！");
            addCheckSetting("自由鼠标", "自由鼠标", "开启后单位aim不会跟随鼠标位置,且一键装弹可绕过wz检测");
            addCheckSetting("按住一键装填自由鼠标", "按住一键装填自由鼠标", "开启后单位aim不会跟随鼠标位置,且一键装弹可绕过wz检测");
            addCheckSetting("残血丢下荷载", "残血丢下荷载", "残血会丢下荷载!");
            if (getSettingB("残血丢下荷载")) {
                addSliderSetting("残载-血量阈值", "残载-血量阈值", 0f, 1f, 10, 0.25f, "低于血量阈值会丢下荷载!");
            }
            settings.add("[yellow]------------------玩家控制------------------").row();
        }

        addCheckSetting("[cyan]多人游戏选项", "多人游戏选项", null);
        if (getSettingB("多人游戏选项")){
            settings.add("[yellow]------------------多人游戏------------------").row();
            settings.table(table -> {
                settings.add("存储名字和uuid").row();
                for (int i = 1; i <= 10; i++) {
                    int finalI = i;
                    table.button("Save Slot " + i, () -> {
                                Core.settings.put("nameSlot" + finalI, Core.settings.getString("name", "[lightgray]<None>"));
                                Core.settings.put("uuidSlot" + finalI, Core.settings.getString("uuid", "[lightgray]<None>"));
                                rebuildMenu();
                            }).tooltip(Core.settings.getString("nameSlot" + finalI, "[lightgray]<None>") + "[yellow]:[white]" + Core.settings.getString("uuidSlot" + finalI, "[lightgray]<None>"))
                            .width(128f).height(64f);
                    if (i == 5) table.row();
                }
            }).row();
            settings.table(table -> {
                settings.add("读取名字和uuid").row();
                for (int i = 1; i <= 10; i++) {
                    int finalI = i;
                    table.button("Load Slot " + i, () -> {
                                Core.settings.put("name", Core.settings.getString("nameSlot" + finalI, "[lightgray]<None>"));
                                Core.settings.put("uuid", Core.settings.getString("uuidSlot" + finalI, "[lightgray]<None>"));
                            }).tooltip(Core.settings.getString("nameSlot" + finalI, "[lightgray]<None>") + "[yellow]:[white]" + Core.settings.getString("uuidSlot" + finalI, "[lightgray]<None>"))
                            .width(128f).height(64f);
                    if (i == 5) table.row();
                }
            }).row();
            settings.add("[yellow]------------------多人游戏------------------").row();
        }
        cont.add(pane).center();
    }

    boolean getSettingB(String setting){
        return Core.settings.getBool(setting, false);
    }

    private void addCheckSetting(String text, String setting, @Nullable String desc){
        if (desc == null) desc = text;
         settings.check(text, Core.settings.getBool(setting, false), b -> {
            Core.settings.put(setting, !Core.settings.getBool(setting,false));
            rebuildMenu();
         }).tooltip(desc).row();
    }
    private void addSliderSetting(String text, String setting, Float min, Float max, Integer steps, Float def, @Nullable String desc){
        if (desc == null) desc = text;
        String finalDesc = desc;
        settings.table(table -> {
            table.add(text);
            TextField field = table.field(String.valueOf(Core.settings.getFloat(setting, def)) , t -> {
                if(Strings.canParsePositiveFloat(t)) Core.settings.put(setting, Float.parseFloat(t));
            }).tooltip(finalDesc).valid(Strings::canParsePositiveFloat).get();
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

