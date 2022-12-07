package mindustry.arcModule.toolpack;

import arc.Core;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Texture;
import arc.graphics.g2d.*;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.gen.Player;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

public class arcChatPicture {

    public static final String ShareType = "[yellow]<Picture>";

    private static final Table t = new Table(Styles.black3);
    private static final Table pic = new Table();

    private static TextureRegion cache;

    private static Pixmap pix;

    static {
        t.add(pic);
        t.visible = false;
        t.setPosition(Core.graphics.getWidth() / 3f * 2, Core.graphics.getHeight() / 3f * 2, Align.center);
        t.pack();
        t.act(0.1f);
        t.update(() -> t.visible = t.visible && state.isPlaying());
        Core.scene.add(t);
    }


    // * 暂时不做多图片处理，防止过分刷屏
    public static boolean resolveMessage(String text, @Nullable Player playersender) {
        if (!text.contains(ShareType)) {
            return false;
        }

        int Indexer = text.indexOf(ShareType) + ShareType.length();
        Indexer = text.indexOf("http", Indexer);
        String url = text.substring(Indexer);

        clear();

        MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.arcChatPicture, text));

        try {
            Http.get(url, res -> {
                pix = new Pixmap(res.getResult());
                Timer.schedule(() -> {
                    var tex = new Texture(pix);
                    float ratio = Math.max(pix.width, pix.height) / 500f;

                    t.visible = true;
                    cache = new TextureRegion(tex);
                    pic.image(cache).size(pix.width / ratio, pix.height / ratio);
                    pic.row();
                    pic.table(tp -> {
                        if (playersender != null) tp.add("[cyan]来源：" + playersender.name()).padRight(20f);
                        tp.button("\uE879", Styles.cleart, arcChatPicture::saveFig).size(40);
                        tp.button("[red]x", Styles.cleart, arcChatPicture::clear).size(40);
                    });
                    t.addListener(new ElementGestureListener() {
                        @Override
                        public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                            t.setPosition(t.x + deltaX / 2, t.y + deltaY / 2);
                        }
                    });
                }, 0.01f);
            });
        } catch (Exception e) {
            Log.err(e);
            ui.arcInfo("[orange]图片读取失败");
        }
        return true;
    }

    private static void clear(){
        t.visible = false;
        t.clearListeners();
        pic.clear();
    }

    private static void saveFig() {
        platform.export("图片-" + Time.millis(), "png", file -> {
            PixmapIO.writePng(file, pix);
        });
        ui.arcInfo("[cyan]已保存图片");
    }

}