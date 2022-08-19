package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.input.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.input.InputHandler.follow;

public class PlayerListFragment{
    public Table content = new Table().marginRight(13f).marginLeft(13f);
    private boolean visible = false;
    private Interval timer = new Interval();
    private TextField search;
    public Seq<Player> players = new Seq<>();

    public void build(Group parent){
        content.name = "players";
        parent.fill(cont -> {
            cont.name = "playerlist";
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!(net.active() && state.isGame())){
                    visible = false;
                    return;
                }

                if(visible && timer.get(20)){
                    rebuild();
                    content.pack();
                    content.act(Core.graphics.getDeltaTime());
                    //hacky
                    Core.scene.act(0f);
                }
            });

            cont.table(Tex.buttonTrans, pane -> {
                pane.label(() -> Core.bundle.format(Groups.player.size() == 1 ? "players.single" : "players", Groups.player.size()));
                pane.row();

                search = pane.field(null, text -> rebuild()).grow().pad(8).name("search").maxTextLength(maxNameLength).get();
                search.setMessageText(Core.bundle.get("players.search"));

                pane.row();
                pane.pane(content).grow().scrollX(false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().growX().height(50f).fillY();
                    menu.name = "menu";

                    menu.button("@server.bans", ui.bans::show).disabled(b -> net.client());
                    menu.button("@server.admins", ui.admins::show).disabled(b -> net.client());
                    menu.button("@close", this::toggle);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f).minWidth(720f);
        });

        rebuild();
    }

    public void rebuild(){
        content.clear();

        float h = 40f;
        float bs = (h) - 2f;
        boolean found = false;

        players.clear();
        Groups.player.copy(players);

        players.sort(Structs.comps(Structs.comparing(Player::team), Structs.comparingBool(p -> !p.admin)));

        for(var user : players){
            found = true;
            NetConnection connection = user.con;

            if(connection == null && net.server() && !user.isLocal()) return;
            if(search.getText().length() > 0 && !user.name().toLowerCase().contains(search.getText().toLowerCase()) && !Strings.stripColors(user.name().toLowerCase()).contains(search.getText().toLowerCase())) return;

            Table button = new Table();
            button.left();
            button.margin(5).marginBottom(10);

            Table table = new Table(){
                @Override
                public void draw(){
                    super.draw();
                    Draw.color(Pal.gray);
                    Draw.alpha(parentAlpha);
                    Lines.stroke(Scl.scl(4f));
                    Lines.rect(x, y, width, height);
                    Draw.reset();
                }
            };

            var style = new ImageButtonStyle(){{
                down = Styles.none;
                up = Styles.none;
                imageCheckedColor = Pal.accent;
                imageDownColor = Pal.accent;
                imageUpColor = Color.white;
                imageOverColor = Color.lightGray;
            }};

            var ustyle = new ImageButtonStyle(){{
                down = Styles.none;
                up = Styles.none;
                imageDownColor = Pal.accent;
                imageUpColor = Color.white;
                imageOverColor = Color.lightGray;
            }};

            table.margin(4);
            table.add(new Image(user.icon()).setScaling(Scaling.bounded)).grow();
            table.name = user.name();

            if (Core.settings.getBool("arcWayzerServerMode")){
                button.add(table).size(h);
                if (Core.settings.getBool("cheating_mode")){
                    button.labelWrap("[" + user.id + "] ").minWidth(150f);
                }
                button.image(Icon.admin).visible(() -> user.admin && !(!user.isLocal() && net.server())).size(bs).get().updateVisibility();
                button.labelWrap("[#" + user.color().toString().toUpperCase() + "]" + user.name()).width(320f).pad(10);
                button.add().grow();

                button.button(Icon.copy, Styles.clearNonei, () -> {
                    Core.app.setClipboardText(user.name);
                });

                button.button(Icon.link, Styles.clearNonei, () -> {
                    String message = arcAtPlayer(user.name);
                    Call.sendChatMessage(message);
                });

                button.button(Icon.units, Styles.clearNonei,()->{
                    if(control.input instanceof DesktopInput){
                        ((DesktopInput) control.input).panning = true;
                    }
                     Core.camera.position.lerpDelta(user.x, user.y,1f);
                }).visible(()-> !user.isLocal());

                button.button(Icon.lock, Styles.clearTogglei, () -> {
                    if(follow != user){
                        follow = user;
                    }else {
                        follow = null;
                    }
                    if(control.input instanceof DesktopInput){
                        ((DesktopInput) control.input).panning = follow == user;
                    }
                }).checked(b -> {
                    boolean checked = follow == user;
                    b.getStyle().imageUp = checked ? Icon.lock : Icon.lockOpen;
                    b.getStyle().up = Styles.none;
                    return checked;
                });


                button.button(Icon.hammer, ustyle,
                () -> {
                    ui.showConfirm("@confirm", Core.bundle.format("confirmvotekick",  user.name()), () -> {
                        Call.sendChatMessage("/votekick " + user.name());
                    });
                }).size(h);

                t.button(Icon.hammer, ustyle, () -> 
                        ui.showConfirm("@confirm", Core.bundle.format("confirmban",  user.name()), () -> Call.adminRequest(user, AdminAction.ban)));
                t.button(Icon.cancel, ustyle, () -> 
                        ui.showConfirm("@confirm", Core.bundle.format("confirmkick",  user.name()), () -> Call.adminRequest(user, AdminAction.kick)));
                t.button(Icon.zoom, ustyle, () -> Call.adminRequest(user, AdminAction.trace));
                
            }
            //原版模式
            else{
                button.add(table).size(h);
                button.labelWrap("[#" + user.color().toString().toUpperCase() + "]" + user.name()).width(170f).pad(10);
                button.add().grow();

                button.image(Icon.admin).visible(() -> user.admin && !(!user.isLocal() && net.server())).padRight(5).get().updateVisibility();

                if((net.server() || player.admin) && !user.isLocal() && (!user.admin || net.server())){
                    button.add().growY();

                    button.table(t -> {
                        t.defaults().size(bs);

                        t.button(Icon.hammer, ustyle,
                        () -> ui.showConfirm("@confirm", Core.bundle.format("confirmban",  user.name()), () -> Call.adminRequest(user, AdminAction.ban)));
                        t.button(Icon.cancel, ustyle,
                        () -> ui.showConfirm("@confirm", Core.bundle.format("confirmkick",  user.name()), () -> Call.adminRequest(user, AdminAction.kick)));

                        t.row();

                        t.button(Icon.admin, style, () -> {
                            if(net.client()) return;

                            String id = user.uuid();

                            if(user.admin){
                                ui.showConfirm("@confirm", Core.bundle.format("confirmunadmin",  user.name()), () -> {
                                    netServer.admins.unAdminPlayer(id);
                                    user.admin = false;
                                });
                            }else{
                                ui.showConfirm("@confirm", Core.bundle.format("confirmadmin",  user.name()), () -> {
                                    netServer.admins.adminPlayer(id, user.usid());
                                    user.admin = true;
                                });
                            }
                        }).update(b -> b.setChecked(user.admin))
                            .disabled(b -> net.client())
                            .touchable(() -> net.client() ? Touchable.disabled : Touchable.enabled)
                            .checked(user.admin);

                        t.button(Icon.zoom, ustyle, () -> Call.adminRequest(user, AdminAction.trace));

                    }).padRight(12).size(bs + 10f, bs);
                }else if(!user.isLocal() && !user.admin && net.client() && Groups.player.size() >= 3 && player.team() == user.team()){ //votekick
                    button.add().growY();

                    button.button(Icon.hammer, ustyle,
                    () -> {
                        ui.showConfirm("@confirm", Core.bundle.format("confirmvotekick",  user.name()), () -> {
                            Call.sendChatMessage("/votekick " + user.name());
                        });
                    }).size(h);
                }
            }
            content.add(button).padBottom(-6).width(700f).maxHeight(h + 14);
            content.row();
            content.image().height(4f).color(state.rules.pvp|| Core.settings.getBool("arcAlwaysTeamColor") ? user.team().color : Pal.gray).growX();
            content.row();
        }

        if(!found){
            content.add(Core.bundle.format("players.notfound")).padBottom(6).width(350f).maxHeight(h + 14);
        }

        content.marginBottom(5);
    }

    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }else{
            Core.scene.setKeyboardFocus(null);
            search.clearText();
        }
    }

    private String arcAtPlayer(String name){
        StringBuilder builder = new StringBuilder(arcVersionPrefix);
        builder.append("戳了").append(name).append("[white]一下，并提醒你留意对话框");
        return builder.toString();
    }
}
