package mindustry.arcModule.toolpack;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Texture;
import arc.graphics.g2d.*;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.arcModule.ui.dialogs.MessageDialog;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;
import static mindustry.arcModule.RFuncs.getPrefix;

public class arcChatPicture {

    public static final String ShareType = "[yellow]<Picture>";
    private static Pixmap oriImage;
    static Table tTable = new Table(Tex.button);
    static Fi figureFile;
    static TextField figureLink;

    static final int maxPicture = 10;
    static int curPicture = 0;

    public static boolean resolveMessage(String text, @Nullable Player playersender) {
        if (!text.contains(ShareType) || !text.contains("http")) {
            return false;
        }

        if (!checkPic()) return true;

        int Indexer = text.indexOf(ShareType) + ShareType.length();
        Indexer = text.indexOf("http", Indexer);
        String url = text.substring(Indexer);

        MessageDialog.addMsg(new MessageDialog.advanceMsg(MessageDialog.arcMsgType.arcChatPicture, text));

        Http.get(url, res -> {
            try {
                Pixmap pix = new Pixmap(res.getResult());
                Timer.schedule(() -> new floatFigure(pix, playersender), 0.01f);
            } catch (Exception e) {
                Log.err(e);
                ui.arcInfo("[orange]图片读取失败");
            }
        });

        return true;
    }

    public static void arcSharePicture() {

        Dialog dialog = new BaseDialog("图片分享器");
        dialog.cont.table(t -> {
            t.button("[cyan]选择图片[white](png)", () -> platform.showFileChooser(false, "png", file -> {
                figureFile = file;
                try {
                    byte[] bytes = file.readBytes();
                    oriImage = new Pixmap(bytes);
                    rebuildShare();
                    if (oriImage.width > 500 || oriImage.height > 500)
                        ui.arcInfo("[orange]警告：图片可能过大，请尝试压缩图片", 5);
                } catch (Throwable e) {
                    ui.arcInfo("读取图片失败，请尝试更换图片\n" + e);
                }
            })).size(240, 50).padBottom(20f).row();
            t.table(a -> tTable = a);
            t.row();
            figureLink = t.field("在此输入图片网址api", text -> {
            }).width(400f).get();
            t.button("♐", () -> {
                Call.sendChatMessage(getPrefix("yellow", "Picture").append(figureLink.getText()).toString().replace(" ",""));
                figureLink.clear();
            }).disabled(disable -> !figureLink.getText().startsWith("http"));
            t.row();
            t.button("[orange]随机二次元(大雾)", () -> {
                try {
                    Http.get("https://api.ixiaowai.cn/api/api.php", res -> {
                        Pixmap pix = new Pixmap(res.getResult());
                        Timer.schedule(() -> new floatFigure(pix, player), 0.01f);
                    });
                } catch (Exception e) {
                    Log.err(e);
                    ui.arcInfo("[orange]图片读取失败");
                }

            }).padTop(30f).width(400f);
            t.row();
            t.add("关闭识别方法：中央监控室——设置" + (mobile ? "" : "\nPC端可通过[cyan]扫描模式[white]来隐藏附属信息"));
        });

        dialog.addCloseButton();
        dialog.show();
    }

    private static void rebuildShare() {
        tTable.clear();
        tTable.table(t -> {
            t.add("名称").color(getThemeColor()).padRight(25f).padBottom(10f);
            t.add(figureFile.name()).padBottom(10f).row();
            t.add("大小").color(getThemeColor()).padRight(25f);
            t.add(oriImage.width + "\uE815" + oriImage.height);
        });
        tTable.row();
        tTable.add("操作图片").pad(25f);
        tTable.row();
        tTable.table(t -> {
            t.button("添加到本地", () -> new floatFigure(oriImage, player)).width(300f).row();
            t.button("上传到云以分享", () -> {
                ui.arcInfo("上传中，请等待...");
                var post = Http.post("http://squirrel.gq/api/upload");
                post.contentStream = figureFile.read();
                post.header("filename", figureFile.name());
                post.header("size", String.valueOf(figureFile.length()));
                post.header("token", "3ab6950d5970c57f938673911f42fd32");
                post.timeout=10000;
                post.error(e -> ui.arcInfo("发生了一个错误:"+e.toString()));
                post.submit(r -> figureLink.setText("http://squirrel.gq/api/get?id=" + r.getResultAsString()));
            }).width(300f);

        });
    }

    private static boolean checkPic() {
        if (curPicture >= maxPicture) {
            ui.arcInfo("当前图片已达上限，仅允许自己添加图片", 10);
            return false;
        } else
            return true;
    }

    public static class floatFigure {
        private final Table t;
        private final Table pic = new Table();
        private final Pixmap pix;
        private float sizeM = 1f;
        private final @Nullable Player sender;

        floatFigure(Pixmap pixmap, @Nullable Player playersender) {
            curPicture += 1;

            pix = pixmap;
            t = new Table(Styles.black3);
            sender = playersender;

            t.add(pic);
            t.visible = false;
            t.setPosition(Core.graphics.getWidth() / 3f * 2, Core.graphics.getHeight() / 3f * 2, Align.center);
            t.pack();
            t.act(0.1f);
            t.update(() -> {
                if (!state.isGame()) clear();
            });
            Core.scene.add(t);
            buildTable();
            ui.arcInfo("已收到图片!，来源：" + (playersender.isNull() ? "" : playersender.name) + "\n[gray]使用参考中央监控室-图片分享器");
        }

        private void buildTable() {
            pic.clear();
            float ratio = Math.max(pix.width, pix.height) / 500f / sizeM;
            t.visible = true;
            TextureRegion cache = new TextureRegion(new Texture(pix));

            pic.image(cache).size(pix.width / ratio, pix.height / ratio).get();
            pic.row();
            pic.table(tp -> {
                if (sender != null) tp.add("[cyan]来源：" + sender.name()).fontScale(sizeM).row();
                tp.table(tpa -> {
                    tpa.button("\uE879", Styles.cleart, this::saveFig).size(40);
                    tpa.button("-", Styles.cleart, () -> {
                        sizeM = sizeM / 1.2f;
                        buildTable();
                    }).size(40);
                    tpa.button("+", Styles.cleart, () -> {
                        sizeM = sizeM * 1.2f;
                        buildTable();
                    }).size(40);
                    tpa.button("[red]x", Styles.cleart, this::clear).size(40);
                });
            }).visible(() -> mobile || control.input.arcScanMode);
            t.addListener(new ElementGestureListener() {
                @Override
                public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                    t.setPosition(t.x + deltaX / 2, t.y + deltaY / 2);
                }
            });
        }

        private void clear() {
            t.visible = false;
            t.clearListeners();
            t.remove();
            curPicture -= 1;
        }

        private void saveFig() {
            platform.export("图片-" + Time.millis(), "png", file -> PixmapIO.writePng(file, pix));
            ui.arcInfo("[cyan]已保存图片");
        }
    }

}
