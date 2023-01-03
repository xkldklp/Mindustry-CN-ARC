package mindustry.arcModule;

import arc.Core;
import arc.func.*;
import arc.scene.*;
import arc.scene.event.Touchable;
import arc.scene.ui.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.ui.layout.*;
import mindustry.ui.Styles;

public class ElementUtils{

    public static <T extends Element> T tooltip(T element, String text){
        return tooltip(element, text, true);
    }

    public static <T extends Element> T tooltip(T element, String text, boolean allowMobile){
        Tooltip tooltip = Tooltips.getInstance().create(text);
        tooltip.allowMobile = allowMobile;

        element.addListener(tooltip);
        return element;
    }

    public static <T extends Element> T tooltip(T element, Cons<Table> builder){
        return tooltip(element, builder, true);
    }

    public static <T extends Element> T tooltip(T element, Cons<Table> builder, boolean allowMobile){
        Tooltip tooltip = new Tooltip(builder);
        tooltip.allowMobile = allowMobile;

        element.addListener(tooltip);
        return element;
    }

    public static Table arcSliderTable(Table table, String name, float def, float min, float max, float step, StringProFloat s, Cons<Float> cons) {
        Slider slider = new Slider(min, max, step, false);

        slider.setValue(def);

        Label value = new Label("", Styles.outlineLabel);
        Table content = new Table();
        content.add(name, Styles.outlineLabel).left().growX().wrap();
        content.add(value).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = Touchable.disabled;

        slider.changed(() -> {
            cons.get(slider.getValue());
            value.setText(s.get(slider.getValue()));
        });

        slider.change();

        table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).left().padTop(4f).get();
        table.row();
        return table;
    }

    public static Table arcSliderTableInt(Table table, String name, int def, int min, int max, int step, StringProInt s, Cons<Integer> cons) {
        Slider slider = new Slider(min, max, step, false);

        slider.setValue(def);

        Label value = new Label("", Styles.outlineLabel);
        Table content = new Table();
        content.add(name, Styles.outlineLabel).left().growX().wrap();
        content.add(value).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = Touchable.disabled;

        slider.changed(() -> {
            cons.get((int) slider.getValue());
            value.setText(s.get((int) slider.getValue()));
        });

        slider.change();

        table.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).left().padTop(4f).get();
        table.row();
        return table;
    }

    public interface StringProFloat{
        String get(float i);
    }

    public interface StringProInt{
        String get(int i);
    }

}
