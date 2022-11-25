package mindustry.editor;

import arc.*;
import arc.func.Intc;
import arc.func.Intp;
import arc.graphics.Color;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.ai.types.BuilderAI;
import mindustry.ai.types.MinerAI;
import mindustry.ai.types.RepairAI;
import mindustry.content.*;
import mindustry.core.UI;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.type.unit.ErekirUnitType;
import mindustry.type.unit.MissileUnitType;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.util.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.RFuncs.fixedTime;
import static mindustry.content.UnitTypes.*;
import static mindustry.game.SpawnGroup.*;
import static mindustry.ui.Styles.*;

public class arcWaveInfoDialog extends BaseDialog {
    private int start = 0, displayed = 20, graphSpeed = 1, maxGraphSpeed = 16;
    Seq<SpawnGroup> groups = new Seq<>();
    private SpawnGroup expandedGroup;

    private Table table, nTable, oTable, iTable, eTable, uTable;
    private int search = -1, payLeft, maxVisible = 30;
    private int filterHealth, filterBegin = -1, filterEnd = -1, filterAmount, filterAmountWave;
    private boolean expandPane = false, filterHealthMode = false, filterStrict = false;
    private UnitType lastType = UnitTypes.dagger;
    private StatusEffect filterEffect = StatusEffects.none;
    private Sort sort = Sort.begin;
    private boolean reverseSort = false;
    private float updateTimer, updatePeriod = 1f;
    private TextField amountField = new TextField();
    private boolean checkedSpawns;
    private WaveGraph graph = new WaveGraph();

    public int arcWaveIndex = 0;
    private float handerSize = 40f;
    /**
     * 是否显示波次界面。如果否，则显示原图
     */
    private boolean waveInfo = true;
    private float waveMulti = 1f;

    //波次生成
    Float difficult = 1f;
    Seq<UnitType> spawnUnit = content.units().copy().filter(unitType -> !(unitType instanceof MissileUnitType || unitType.controller instanceof BuilderAI || unitType.controller instanceof MinerAI || unitType.controller instanceof RepairAI));
    Seq<UnitType> allowUnit = content.units().copy().filter(unitType -> !(unitType instanceof MissileUnitType));
    boolean surplusUnit = true, ErekirUnit = true;
    boolean showUnitSelect = true;
    boolean flyingUnit = true, navalUnit = true, supportUnit = true;

    public arcWaveInfoDialog() {
        super("ARC-波次编辑器");

        shown(() -> {
            checkedSpawns = false;
            setup();
        });
        hidden(() -> state.rules.spawns = groups);

        addCloseListener();

        onResize(this::setup);

        addCloseButton();

        buttons.button("@waves.edit", Icon.pencil, () -> {
            BaseDialog dialog = new BaseDialog("@waves.edit");
            dialog.addCloseButton();
            dialog.setFillParent(false);
            dialog.cont.table(Tex.button, t -> {
                var style = Styles.flatt;
                t.defaults().size(210f, 58f);

                t.button("@waves.copy", Icon.copy, style, () -> {
                    ui.showInfoFade("@waves.copied");
                    Core.app.setClipboardText(maps.writeWaves(groups));
                    dialog.hide();
                }).disabled(b -> groups == null).marginLeft(12f).row();

                t.button("@waves.load", Icon.download, style, () -> {
                    try {
                        groups = maps.readWaves(Core.app.getClipboardText());
                        buildGroups();
                    } catch (Exception e) {
                        e.printStackTrace();
                        ui.showErrorMessage("@waves.invalid");
                    }
                    dialog.hide();
                }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty()).row();

                t.button("@settings.reset", Icon.upload, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups = JsonIO.copy(waves.get());
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f).row();

                t.button("@clear", Icon.cancel, style, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                    groups.clear();
                    buildGroups();
                    dialog.hide();
                })).marginLeft(12f);
            });

            dialog.show();
        }).size(250f, 64f);

        buttons.defaults().width(60f);

        buttons.button("切换显示模式", () -> {
            waveInfo = !waveInfo;
            waveMulti = 1;
            setup();
        }).size(250f, 64f);

        if (true) {
            buttons.button("随机", Icon.refresh, () -> arcSpawner()).width(200f);
        }
    }

    void view(int amount) {
        updateTimer += Time.delta;
        if (updateTimer >= updatePeriod) {
            displayed += amount;
            if (displayed < 5) displayed = 5;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void shift(int amount) {
        updateTimer += Time.delta;
        if (updateTimer >= updatePeriod) {
            start += amount;
            if (start < 0) start = 0;
            updateTimer = 0f;
            updateWaves();
        }
    }

    void setup() {
        groups = JsonIO.copy(state.rules.spawns.isEmpty() ? waves.get() : state.rules.spawns);

        cont.clear();
        cont.stack(new Table(Tex.clear, main -> {
            main.table(s -> {
                s.image(Icon.zoom).padRight(8);
                s.field(search < 0 ? "" : search + "", TextFieldFilter.digitsOnly, text -> {
                    search = !text.isEmpty() ? Math.max(Strings.parseInt(text) - 1, -1) : -1;
                    start = Math.max(search - (displayed / 2) - (displayed % 2), 0);
                    buildGroups();
                }).growX().maxTextLength(8).get().setMessageText("@waves.search");
                s.button(Icon.filter, Styles.emptyi, this::showFilter).size(46f).tooltip("@waves.filter");
            }).fillX().pad(6f).row();
            main.pane(t -> table = t).growX().growY().padRight(8f).scrollX(false);
            main.row();
            main.table(f -> {
                f.button("@add", () -> {
                    if (groups == null) groups = new Seq<>();
                    SpawnGroup newGroup = new SpawnGroup(lastType);
                    groups.add(newGroup);
                    expandedGroup = newGroup;
                    showUpdate(newGroup, false);
                    buildGroups();
                    clearFilter();
                }).growX().height(70f);
                f.button(Icon.filter, () -> {
                    BaseDialog dialog = new BaseDialog("@waves.sort");
                    dialog.setFillParent(false);
                    dialog.cont.table(Tex.button, t -> {
                        for (Sort s : Sort.all) {
                            t.button("@waves.sort." + s, Styles.cleart, () -> {
                                sort = s;
                                dialog.hide();
                                buildGroups();
                            }).size(150f, 60f).checked(s == sort);
                        }
                    }).row();
                    dialog.cont.check("@waves.sort.reverse", b -> {
                        reverseSort = b;
                        buildGroups();
                    }).padTop(4).checked(reverseSort).padBottom(8f);
                    dialog.addCloseButton();
                    dialog.show();
                    buildGroups();
                }).size(64f, 70f).padLeft(4f);
            }).fillX();
        }), new Label("@waves.none") {{
            visible(() -> groups.isEmpty());
            this.touchable = Touchable.disabled;
            setWrap(true);
            setAlignment(Align.center, Align.center);
        }}).width(390f).growY();
        cont.table(tb -> {
            if (waveInfo) {
                tb.table(t -> {
                    t.table(buttons -> {
                        buttons.clear();
                        buttons.button("<<", cleart, () -> {
                            arcWaveIndex -= 10;
                            if (arcWaveIndex < 0) arcWaveIndex = 0;
                            setup();
                        }).size(handerSize);

                        buttons.button("<", cleart, () -> {
                            arcWaveIndex -= 1;
                            if (arcWaveIndex < 0) arcWaveIndex = 1;
                            setup();
                        }).size(handerSize);

                        TextField sField = buttons.field((arcWaveIndex + 1) + "", text -> {
                            if (Strings.canParseInt(text)) {
                                arcWaveIndex = Integer.parseInt(text) - 1;
                                setup();
                            }
                        }).get();

                        buttons.button(">", cleart, () -> {
                            arcWaveIndex += 1;
                            setup();
                        }).size(handerSize);

                        buttons.button(">>", cleart, () -> {
                            arcWaveIndex += 10;
                            setup();
                        }).size(handerSize);

                        buttons.button("×", cleart, () -> {
                            arcWaveIndex = 0;
                            setup();
                        }).size(handerSize);

                        buttons.slider(0, calWinWave(), 1, res -> {
                            arcWaveIndex = (int) res;
                            sField.setText((arcWaveIndex + 1) + "");
                        });

                    }).left().row();
                    t.pane(waveInfo -> {
                        waveInfo.clear();
                        waveInfo.table(wi -> {
                            int curInfoWave = arcWaveIndex;
                            for (SpawnGroup group : state.rules.spawns) {
                                int amount = group.getSpawned(curInfoWave);
                                if (amount > 0) {
                                    StringBuilder groupInfo = new StringBuilder();
                                    groupInfo.append(group.type.emoji()).append("\n");
                                    groupInfo.append(amount).append("\n");
                                    if (group.getShield(curInfoWave) > 0f)
                                        groupInfo.append(UI.formatAmount((long) group.getShield(curInfoWave))).append("\n");
                                    if (group.effect != null && group.effect != StatusEffects.none)
                                        groupInfo.append(group.effect.emoji()).append("\n");
                                    wi.button(groupInfo.toString(), cleart, () -> unitSettingDialog(group)).height(130f).width(50f);
                                }
                            }
                        });
                    }).scrollY(false).left();
                }).growX();
                tb.row();
                tb.add(graph = new WaveGraph()).grow();
                tb.row();
                tb.table(tbt -> {
                    tbt.button("<", () -> {
                    }).update(t -> {
                        if (t.getClickListener().isPressed()) {
                            shift(-graphSpeed);
                        }
                    }).width(150f);
                    tbt.button(">", () -> {
                    }).update(t -> {
                        if (t.getClickListener().isPressed()) {
                            shift(graphSpeed);
                        }
                    }).width(150f);

                    tbt.button("-", () -> {
                    }).update(t -> {
                        if (t.getClickListener().isPressed()) {
                            view(-graphSpeed);
                        }
                    }).width(150f);
                    tbt.button("+", () -> {
                    }).update(t -> {
                        if (t.getClickListener().isPressed()) {
                            view(graphSpeed);
                        }
                    }).width(150f);

                    tbt.button("x" + graphSpeed, () -> {
                        graphSpeed *= 2;
                        if (graphSpeed > maxGraphSpeed) graphSpeed = 1;
                    }).update(b -> b.setText("x" + graphSpeed)).width(150f);
                }).growX();
            } else {
                tb.pane(p -> {
                    p.table(Tex.button, t -> {
                        p.margin(0).defaults().pad(5).growX();
                        t.add("\uE86D 为单位数量；\uE813 为单位血+盾；\uE810 为计算buff的血+盾；\uE86E 为预估DPS。在游戏中时会考虑地图出怪点数目").color(getThemeColor());
                    }).scrollX(false).growX().row();
                    for (int wave = 0; wave < calWinWave() * waveMulti; wave++) {
                        int finalWave = wave;
                        p.table(Tex.button, t -> {
                            t.table(tt -> {
                                tt.add("第[accent]" + (finalWave + 1) + "[]波").fillX();
                                tt.row();
                                tt.add(fixedTime((int) (finalWave * state.rules.waveSpacing + (state.rules.initialWaveSpacing <= 0 ? (2 * state.rules.waveSpacing) : state.rules.initialWaveSpacing)), false));
                            }).width(120f).left();

                            int totalAmount = 0, totalHealth = 0, totalEffHealth = 0, totalDps = 0;
                            float thisAmount, thisHealth, thisEffHealth, thisDps;
                            for (SpawnGroup group : state.rules.spawns) {
                                thisAmount = group.getSpawned(finalWave);

                                if (thisAmount > 0) {
                                    thisHealth = (group.type.health + group.getShield(finalWave)) * thisAmount;
                                    if (group.effect == null) {
                                        thisEffHealth = (group.type.health + group.getShield(finalWave)) * thisAmount;
                                        thisDps = group.type.estimateDps();
                                    } else {
                                        thisEffHealth = group.effect.healthMultiplier * (group.type.health + group.getShield(finalWave)) * thisAmount;
                                        thisDps = group.effect.damageMultiplier * group.effect.reloadMultiplier * group.type.estimateDps();
                                    }
                                    if (spawner.countSpawns() > 1 && group.spawn == -1) {
                                        thisAmount *= spawner.countSpawns();
                                        thisHealth *= spawner.countSpawns();
                                        thisEffHealth *= spawner.countSpawns();
                                        thisDps *= spawner.countSpawns();
                                    }
                                    totalAmount += thisAmount;
                                    totalHealth += thisHealth;
                                    totalEffHealth += thisEffHealth;
                                    totalDps += thisDps;
                                }

                            }
                            if (totalAmount == 0) t.add("该波次没有敌人");
                            else {
                                int finalTotalAmount = totalAmount;
                                int finalTotalHealth = totalHealth;
                                int finalTotalEffHealth = totalEffHealth;
                                int finalTotalDps = totalDps;
                                t.table(tt -> {
                                    tt.add("\uE86D").width(50f);
                                    tt.add("[accent]" + finalTotalAmount).growX().row();
                                    tt.add("\uE813").width(50f);
                                    tt.add("[accent]" + UI.formatAmount(finalTotalHealth, 2)).growX().row();
                                    if (finalTotalEffHealth != finalTotalHealth) {
                                        tt.add("\uE810").width(50f);
                                        tt.add("[accent]" + UI.formatAmount(finalTotalEffHealth, 2)).growX().row();
                                    }
                                    tt.add("\uE86E").width(50f);
                                    tt.add("[accent]" + UI.formatAmount(finalTotalDps, 2)).growX();
                                }).width(180f).left();
                                ;
                                t.pane(wi -> {
                                    int curInfoWave = finalWave;
                                    for (SpawnGroup group : state.rules.spawns) {
                                        int amount = group.getSpawned(curInfoWave);
                                        if (amount > 0) {
                                            StringBuilder groupInfo = new StringBuilder();
                                            groupInfo.append(group.type.emoji());

                                            if (group.spawn != -1) groupInfo.append("*");

                                            groupInfo.append(typeColor(group.type));

                                            groupInfo.append("\n").append(amount);
                                            groupInfo.append("\n");

                                            if (group.getShield(curInfoWave) > 0f)
                                                groupInfo.append(UI.formatAmount((long) group.getShield(curInfoWave)));
                                            groupInfo.append("\n[]");
                                            if (group.effect != null && group.effect != StatusEffects.none)
                                                groupInfo.append(group.effect.emoji());
                                            if (group.items != null && group.items.amount > 0)
                                                groupInfo.append(group.items.item.emoji());
                                            if (group.payloads != null && group.payloads.size > 0)
                                                groupInfo.append("\uE87B");
                                            wi.button(groupInfo.toString(), cleart, () -> unitSettingDialog(group)).height(80f).width(70f).left();
                                        }
                                    }
                                }).scrollX(true).scrollY(false).maxWidth(mobile ? 500f : 1000f).growX();
                            }
                        }).growX().row();
                        p.margin(0).defaults().pad(5).growX();
                    }
                }).scrollX(false).growX().row();
                tb.table(tbb -> {
                    tbb.button("刷新波次显示", () -> setup()).width(200f);
                    TextField sField = tbb.field(calWinWave() * waveMulti + "", text -> {
                        waveMulti = Float.parseFloat(text) / calWinWave();
                        setup();
                    }).valid(Strings::canParsePositiveFloat).width(200f).get();
                    tbb.slider(0.25f, 10f, 0.25f, 1f, t -> {
                        waveMulti = t;
                        sField.setText(calWinWave() * waveMulti + "");
                    }).width(300f);
                });
            }
        }).grow();


        buildGroups();
    }

    void buildGroups() {
        table.clear();
        table.top();
        table.margin(10f);

        if (groups != null) {
            groups.sort(sort.sort);
            if (reverseSort) groups.reverse();

            for (SpawnGroup group : groups) {
                if ((search >= 0 && group.getSpawned(search) <= 0)
                        || (filterHealth != 0 && !(filterHealthMode ? group.type.health * (search >= 0 ? group.getSpawned(search) : 1) > filterHealth : group.type.health * (search >= 0 ? group.getSpawned(search) : 1) < filterHealth))
                        || (filterBegin >= 0 && !(filterStrict ? group.begin == filterBegin : group.begin - 2 <= filterBegin && group.begin + 2 >= filterBegin))
                        || (filterEnd >= 0 && !(filterStrict ? group.end == filterEnd : group.end - 2 <= filterEnd && group.end + 2 >= filterEnd))
                        || (filterAmount != 0 && !(filterStrict ? group.getSpawned(filterAmountWave) == filterAmount : filterAmount - 5 <= group.getSpawned(filterAmountWave) && filterAmount + 5 >= group.getSpawned(filterAmountWave)))
                        || (filterEffect != StatusEffects.none && group.effect != filterEffect)) continue;

                table.table(Tex.button, t -> {
                    t.margin(0).defaults().pad(3).padLeft(5f).growX().left();
                    t.button(b -> {
                        b.left();
                        b.image(group.type.uiIcon).size(32f).padRight(3).scaling(Scaling.fit);
                        //if(group.effect != null && group.effect != StatusEffects.none) b.image(group.effect.uiIcon).size(20f).padRight(3).scaling(Scaling.fit);
                        b.add(typeColor(group.type) + group.type.localizedName);
                        if (group.effect != null && group.effect != StatusEffects.none)
                            b.image(group.effect.uiIcon).size(20f).padRight(3).scaling(Scaling.fit);

                        b.add().growX();

                        b.label(() -> {
                            StringBuilder builder = new StringBuilder();
                            builder.append("[lightgray]").append(group.begin + 1);
                            if (group.begin == group.end) return builder.toString();
                            if (group.end > 999999) builder.append("+");
                            else builder.append("~").append(group.end + 1);
                            if (group.spacing > 1) builder.append("[white]|[lightgray]" + group.spacing);
                            return builder.append("  ").toString();
                        }).minWidth(45f).labelAlign(Align.left).left();

                        b.button(Icon.settingsSmall, Styles.emptyi, () -> unitSettingDialog(group)).pad(-6).size(46f);
                        b.button(Icon.unitsSmall, Styles.emptyi, () -> showUpdate(group, false)).pad(-6).size(46f);
                        b.button(Icon.cancel, Styles.emptyi, () -> {
                            if (expandedGroup == group) expandedGroup = null;
                            groups.remove(group);
                            table.getCell(t).pad(0f);
                            t.remove();
                            buildGroups();
                        }).pad(-6).size(46f).padRight(-12f);
                    }, () -> {
                        expandedGroup = expandedGroup == group ? null : group;
                        buildGroups();
                    }).height(46f).pad(-6f).padBottom(0f).row();

                    if (expandedGroup == group) {
                        t.table(spawns -> {
                            spawns.field("" + (group.begin + 1), TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text)) {
                                    group.begin = Strings.parseInt(text) - 1;
                                    updateWaves();
                                }
                            }).width(100f);
                            spawns.add("@waves.to").padLeft(4).padRight(4);
                            spawns.field(group.end == never ? "" : (group.end + 1) + "", TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text)) {
                                    group.end = Strings.parseInt(text) - 1;
                                    updateWaves();
                                } else if (text.isEmpty()) {
                                    group.end = never;
                                    updateWaves();
                                }
                            }).width(100f).get().setMessageText("∞");
                        }).row();

                        t.table(p -> {
                            p.add("@waves.every").padRight(4);
                            p.field(group.spacing + "", TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0) {
                                    group.spacing = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(100f);
                            p.add("@waves.waves").padLeft(4);
                        }).row();

                        t.table(a -> {
                            a.field(group.unitAmount + "", TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text)) {
                                    group.unitAmount = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);

                            a.add(" + ");
                            a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextFieldFilter.floatsOnly, text -> {
                                if (Strings.canParsePositiveFloat(text)) {
                                    group.unitScaling = 1f / Strings.parseFloat(text);
                                    updateWaves();
                                }
                            }).width(80f);
                            a.add("@waves.perspawn").padLeft(4);
                        }).row();

                        t.table(a -> {
                            a.field(group.max + "", TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text)) {
                                    group.max = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);

                            a.add("@waves.max").padLeft(5);
                        }).row();

                        t.table(a -> {
                            a.field((int) group.shields + "", TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text)) {
                                    group.shields = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);

                            a.add(" + ");
                            a.field((int) group.shieldScaling + "", TextFieldFilter.digitsOnly, text -> {
                                if (Strings.canParsePositiveInt(text)) {
                                    group.shieldScaling = Strings.parseInt(text);
                                    updateWaves();
                                }
                            }).width(80f);
                            a.add("@waves.shields").padLeft(4);
                        }).row();

                        t.table(a -> {
                            a.add("@waves.spawn").padRight(8);

                            a.button("", () -> {
                                if (!checkedSpawns) {
                                    //recalculate waves when changed
                                    spawner.reset();
                                    checkedSpawns = true;
                                }

                                BaseDialog dialog = new BaseDialog("@waves.spawn.select");
                                dialog.cont.pane(p -> {
                                    p.background(Tex.button).margin(10f);
                                    int i = 0;
                                    int cols = 4;
                                    int max = 20;

                                    if (spawner.getSpawns().size >= max) {
                                        p.add("[lightgray](first " + max + ")").colspan(cols).padBottom(4).row();
                                    }

                                    for (var spawn : spawner.getSpawns()) {
                                        p.button(spawn.x + ", " + spawn.y, Styles.flatTogglet, () -> {
                                            group.spawn = Point2.pack(spawn.x, spawn.y);
                                            dialog.hide();
                                        }).size(110f, 45f).checked(spawn.pos() == group.spawn);

                                        if (++i % cols == 0) {
                                            p.row();
                                        }

                                        //only display first 20 spawns, you don't need to see more.
                                        if (i >= 20) {
                                            break;
                                        }
                                    }

                                    p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                                        group.spawn = -1;
                                        dialog.hide();
                                    }).size(110f, 45f).checked(-1 == group.spawn);

                                    if (spawner.getSpawns().isEmpty()) {
                                        p.add("@waves.spawn.none");
                                    }
                                });
                                dialog.setFillParent(false);
                                dialog.addCloseButton();
                                dialog.show();
                            }).width(160f).height(36f).get().getLabel().setText(() -> group.spawn == -1 ? "@waves.spawn.all" : Point2.x(group.spawn) + ", " + Point2.y(group.spawn));

                        }).padBottom(8f).row();
                    }
                }).width(350f).pad(8);

                table.row();
            }
        } else {
            table.add("@editor.default");
        }

        updateWaves();
    }

    void showUpdate(SpawnGroup group, boolean payloads) {
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        if (payloads && group.payloads == null) group.payloads = Seq.with();
        if (payloads) dialog.cont.table(e -> {
            uTable = e;
            updateIcons(group);
        }).padBottom(6f).row();
        dialog.cont.pane(p -> {
            int i = 0;
            for (UnitType type : content.units()) {
                if (type.isHidden() && !(Core.settings.getBool("developmode") && type.hasEmoji())) continue;
                p.button(t -> {
                    t.left();
                    t.image(type.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(type.localizedName);
                }, () -> {
                    if (payloads) {
                        group.payloads.add(type);
                        updateIcons(group);
                    } else {
                        group.type = lastType = type;
                        updateIcons(group);
                        dialog.hide();
                    }
                    if (group.payloads != null && group.type.payloadCapacity <= 8) group.payloads.clear();
                    if (group.items != null)
                        group.items.amount = Mathf.clamp(group.items.amount, 0, group.type.itemCapacity);
                    buildGroups();
                }).pad(2).margin(12f).fillX();
                if (++i % 5 == 0) p.row();
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }

    void showEffect(SpawnGroup group) {
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        dialog.cont.pane(p -> {
            int i = 0;
            for (StatusEffect effect : content.statusEffects()) {
                if (effect != StatusEffects.none && effect.reactive) continue;

                p.button(t -> {
                    t.left();
                    if (effect.uiIcon != null && effect != StatusEffects.none) {
                        t.image(effect.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    } else {
                        t.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    }

                    if (effect != StatusEffects.none) {
                        t.add(effect.localizedName);
                    } else {
                        t.add("@settings.resetKey");
                    }
                }, () -> {
                    if (group == null) {
                        filterEffect = effect;
                    } else {
                        group.effect = effect;
                    }
                    updateIcons(group);
                    dialog.hide();
                    buildGroups();
                }).pad(2).margin(12f).fillX();
                if (++i % 3 == 0) p.row();
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }

    void showItems(SpawnGroup group) {
        BaseDialog dialog = new BaseDialog("");
        dialog.setFillParent(true);
        dialog.cont.table(items -> {
            items.add(Core.bundle.get("filter.option.amount") + ":");
            amountField = items.field(group.items != null ? group.items.amount + "" : "", TextFieldFilter.digitsOnly, text -> {
                if (Strings.canParsePositiveInt(text) && group.items != null) {
                    group.items.amount = Strings.parseInt(text) <= 0 ? group.type.itemCapacity : Mathf.clamp(Strings.parseInt(text), 0, group.type.itemCapacity);
                }
            }).width(120f).pad(2).margin(12f).maxTextLength((group.type.itemCapacity + "").length() + 1).get();
            amountField.setMessageText(group.type.itemCapacity + "");
        }).padBottom(6f).row();
        dialog.cont.pane(p -> {
            int i = 1;
            p.defaults().pad(2).margin(12f).minWidth(200f).fillX();
            p.button(icon -> {
                icon.left();
                icon.image(Icon.none).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                icon.add("@settings.resetKey");
            }, () -> {
                group.items = null;
                updateIcons(group);
                dialog.hide();
                buildGroups();
            });
            for (Item item : content.items()) {
                p.button(t -> {
                    t.left();
                    if (item.uiIcon != null) t.image(item.uiIcon).size(8 * 4).scaling(Scaling.fit).padRight(2f);
                    t.add(item.localizedName);
                }, () -> {
                    group.items = new ItemStack(item, Strings.parseInt(amountField.getText()) <= 0 ? group.type.itemCapacity : Mathf.clamp(Strings.parseInt(amountField.getText()), 0, group.type.itemCapacity));
                    updateIcons(group);
                    dialog.hide();
                    buildGroups();
                });
                if (++i % 3 == 0) p.row();
            }
        });
        dialog.addCloseButton();
        dialog.show();
    }

    void showFilter() {
        BaseDialog dialog = new BaseDialog("@waves.filter");
        dialog.setFillParent(false);
        dialog.cont.defaults().size(210f, 64f);
        dialog.cont.add(Core.bundle.get("waves.sort.health") + ":");
        dialog.cont.table(filter -> {
            filter.button(">", Styles.cleart, () -> {
                filterHealthMode = !filterHealthMode;
                buildGroups();
            }).update(b -> b.setText(filterHealthMode ? ">" : "<")).size(40f).padRight(4f);
            filter.defaults().width(170f);
            numField("", filter, f -> filterHealth = f, () -> filterHealth, 15);
        }).row();

        dialog.cont.add("@waves.filter.begin");
        dialog.cont.table(filter -> {
            filter.defaults().maxWidth(120f);
            numField("", filter, f -> filterBegin = f - 1, () -> filterBegin + 1, 8);
            numField("@waves.to", filter, f -> filterEnd = f - 1, () -> filterEnd + 1, 8);
        }).row();

        dialog.cont.add(Core.bundle.get("waves.filter.amount") + ":");
        dialog.cont.table(filter -> {
            filter.defaults().maxWidth(120f);
            numField("", filter, f -> filterAmount = f, () -> filterAmount, 12);
            numField("@waves.filter.onwave", filter, f -> filterAmountWave = f, () -> filterAmountWave, 8);
        }).row();

        dialog.cont.table(t -> {
            eTable = t;
            updateIcons(null);
        }).row();
        dialog.row();
        dialog.check("@waves.filter.strict", b -> {
            filterStrict = b;
            buildGroups();
        }).checked(filterStrict).padBottom(10f).row();

        dialog.table(p -> {
            p.defaults().size(210f, 64f).padLeft(4f).padRight(4f);
            p.button("@back", Icon.left, dialog::hide);
            p.button("@clear", Icon.refresh, () -> {
                clearFilter();
                buildGroups();
                dialog.hide();
            });
        });
        dialog.addCloseListener();
        dialog.show();
    }

    void unitSettingDialog(SpawnGroup group) {
        BaseDialog dialog = new BaseDialog("设置出怪组");
        dialog.setFillParent(false);
        dialog.cont.table(Tex.button, a -> nTable = a).row();
        dialog.cont.table(Tex.button, a -> oTable = a).row();
        dialog.cont.table(Tex.button, a -> iTable = a).row();
        dialog.cont.table(c -> {
            c.defaults().size(210f, 64f).pad(2f);
            c.button("@waves.duplicate", Icon.copy, () -> {
                SpawnGroup newGroup = group.copy();
                groups.add(newGroup);
                expandedGroup = newGroup;
                buildGroups();
                dialog.hide();
            });
            c.button("@settings.resetKey", Icon.refresh, () -> ui.showConfirm("@confirm", "@settings.clear.confirm", () -> {
                group.effect = StatusEffects.none;
                group.payloads = Seq.with();
                group.items = null;
                buildGroups();
                dialog.hide();
            }));
        });
        buildGroups();
        updateIcons(group);
        dialog.addCloseButton();
        dialog.show();
    }

    void updateIcons(SpawnGroup group) {
        if (nTable != null && group != null) {
            nTable.clear();
            nTable.defaults().size(400f, 50f).pad(2f);
            nTable.table(t -> {
                t.image(group.type.uiIcon).size(32f).padRight(5).scaling(Scaling.fit).get();
                //if(group.effect != null && group.effect != StatusEffects.none) b.image(group.effect.uiIcon).size(20f).padRight(3).scaling(Scaling.fit);
                t.add(group.type.localizedName).padRight(20).color(Pal.accent);
                t.button(Icon.units, Styles.emptyi, () -> showUpdate(group, false)).pad(-6).size(50f);
            }).growX().row();
        }

        if (oTable != null && group != null) {
            oTable.clear();
            oTable.defaults().size(400f, 250f).pad(2f);
            oTable.table(t -> {
                t.table(spawns -> {
                    spawns.field("" + (group.begin + 1), TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text)) {
                            group.begin = Strings.parseInt(text) - 1;
                            updateWaves();
                        }
                    }).width(150f);
                    spawns.add("@waves.to").padLeft(4).padRight(4);
                    spawns.field(group.end == never ? "" : (group.end + 1) + "", TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text)) {
                            group.end = Strings.parseInt(text) - 1;
                            updateWaves();
                        } else if (text.isEmpty()) {
                            group.end = never;
                            updateWaves();
                        }
                    }).width(150f).get().setMessageText("∞");
                }).row();

                t.table(p -> {
                    p.add("@waves.every").padRight(4);
                    p.field(group.spacing + "", TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text) && Strings.parseInt(text) > 0) {
                            group.spacing = Strings.parseInt(text);
                            updateWaves();
                        }
                    }).width(150f);
                    p.add("@waves.waves").padLeft(4);
                }).row();

                t.table(a -> {
                    a.field(group.unitAmount + "", TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text)) {
                            group.unitAmount = Strings.parseInt(text);
                            updateWaves();
                        }
                    }).width(150f);

                    a.add(" + ");
                    a.field(Strings.fixed(Math.max((Mathf.zero(group.unitScaling) ? 0 : 1f / group.unitScaling), 0), 2), TextFieldFilter.floatsOnly, text -> {
                        if (Strings.canParsePositiveFloat(text)) {
                            group.unitScaling = 1f / Strings.parseFloat(text);
                            updateWaves();
                        }
                    }).width(150f);
                    a.add("@waves.perspawn").padLeft(4);
                }).row();

                t.table(a -> {
                    a.field(group.max + "", TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text)) {
                            group.max = Strings.parseInt(text);
                            updateWaves();
                        }
                    }).width(150f);

                    a.add("@waves.max").padLeft(5);
                }).row();

                t.table(a -> {
                    a.field((int) group.shields + "", TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text)) {
                            group.shields = Strings.parseInt(text);
                            updateWaves();
                        }
                    }).width(150f);

                    a.add(" + ");
                    a.field((int) group.shieldScaling + "", TextFieldFilter.digitsOnly, text -> {
                        if (Strings.canParsePositiveInt(text)) {
                            group.shieldScaling = Strings.parseInt(text);
                            updateWaves();
                        }
                    }).width(150f);
                    a.add("@waves.shields").padLeft(4);
                }).row();

                t.table(a -> {
                    a.add("@waves.spawn").padRight(8);

                    a.button("", () -> {
                        if (!checkedSpawns) {
                            //recalculate waves when changed
                            spawner.reset();
                            checkedSpawns = true;
                        }

                        BaseDialog dialog = new BaseDialog("@waves.spawn.select");
                        dialog.cont.pane(p -> {
                            p.background(Tex.button).margin(10f);
                            int i = 0;
                            int cols = 4;
                            int max = 20;

                            if (spawner.getSpawns().size >= max) {
                                p.add("[lightgray](first " + max + ")").colspan(cols).padBottom(4).row();
                            }

                            for (var spawn : spawner.getSpawns()) {
                                p.button(spawn.x + ", " + spawn.y, Styles.flatTogglet, () -> {
                                    group.spawn = Point2.pack(spawn.x, spawn.y);
                                    dialog.hide();
                                }).size(110f, 45f).checked(spawn.pos() == group.spawn);

                                if (++i % cols == 0) {
                                    p.row();
                                }

                                //only display first 20 spawns, you don't need to see more.
                                if (i >= 20) {
                                    break;
                                }
                            }

                            p.button("@waves.spawn.all", Styles.flatTogglet, () -> {
                                group.spawn = -1;
                                dialog.hide();
                            }).size(110f, 45f).checked(-1 == group.spawn);

                            if (spawner.getSpawns().isEmpty()) {
                                p.add("@waves.spawn.none");
                            }
                        });
                        dialog.setFillParent(false);
                        dialog.addCloseButton();
                        dialog.show();
                    }).width(160f).height(36f).get().getLabel().setText(() -> group.spawn == -1 ? "@waves.spawn.all" : Point2.x(group.spawn) + ", " + Point2.y(group.spawn));

                }).padBottom(8f).row();


            }).growX().row();
        }

        if (iTable != null && group != null) {
            iTable.clear();
            iTable.defaults().size(200f, 60f).pad(2f);
            iTable.button(icon -> {
                icon.add("状态");
                if (group.effect != null && group.effect != StatusEffects.none) {
                    icon.image(group.effect.uiIcon).padLeft(6f);
                } else {
                    icon.image(Icon.logic).padLeft(6f);
                }
            }, Styles.cleart, () -> showEffect(group));
            if (group.type.payloadCapacity > 0) iTable.button("添加载荷", Styles.cleart, () -> showUpdate(group, true));
            iTable.button(icon -> {
                icon.add("物品");
                if (group.items != null) {
                    icon.image(group.items.item.uiIcon).padLeft(6f);
                    icon.add("" + group.items.amount).padLeft(6f);
                } else {
                    icon.image(Icon.effect).padLeft(6f);
                }

            }, Styles.cleart, () -> showItems(group));
        }

        if (eTable != null) {
            eTable.clear();
            eTable.add(Core.bundle.get("waves.filter.effect") + ":");
            eTable.button(filterEffect != null && filterEffect != StatusEffects.none ?
                    new TextureRegionDrawable(filterEffect.uiIcon) :
                    Icon.logic, () -> showEffect(null)).padLeft(30f).size(60f);
        }

        if (uTable != null && group != null && group.payloads != null) {
            uTable.clear();
            uTable.left();
            uTable.defaults().pad(3);
            payLeft = (int) group.type.payloadCapacity;
            uTable.table(units -> {
                int i = 0;
                for (UnitType payl : group.payloads) {
                    if (i < maxVisible || expandPane) units.table(Tex.button, s -> {
                        s.image(payl.uiIcon).size(45f);
                        s.button(Icon.cancelSmall, Styles.emptyi, () -> {
                            group.payloads.remove(payl);
                            updateIcons(group);
                            buildGroups();
                        }).size(20f).padRight(-9f).padLeft(-6f);
                    }).pad(2).margin(12f).fillX();
                    payLeft -= payl.hitSize * payl.hitSize;
                    if (++i % 10 == 0) units.row();
                }
            });
            uTable.table(b -> {
                b.defaults().pad(2);
                if (group.payloads.size > 1) b.button(Icon.cancel, () -> {
                    group.payloads.clear();
                    updateIcons(group);
                    buildGroups();
                }).tooltip("@clear").row();
                if (group.payloads.size > maxVisible) b.button(expandPane ? Icon.eyeSmall : Icon.eyeOffSmall, () -> {
                    expandPane = !expandPane;
                    updateIcons(group);
                }).size(45f).tooltip(expandPane ? "@server.shown" : "@server.hidden");
            }).padLeft(6f);
        }
    }

    void numField(String text, Table t, Intc cons, Intp prov, int maxLength) {
        if (!text.isEmpty()) t.add(text);
        t.field(prov.get() + "", TextFieldFilter.digitsOnly, input -> {
            if (Strings.canParsePositiveInt(input)) {
                cons.get(!input.isEmpty() ? Strings.parseInt(input) : 0);
                buildGroups();
            }
        }).maxTextLength(maxLength);
    }

    void clearFilter() {
        filterHealth = filterAmount = filterAmountWave = 0;
        filterStrict = filterHealthMode = false;
        filterBegin = filterEnd = -1;
        filterEffect = StatusEffects.none;
    }

    String typeColor(UnitType unit) {
        if (unit.naval) return "[cyan]";
        else if (unit.allowLegStep) return "[magenta]";
        else if (unit.flying) return "[acid]";
        else if (unit.hovering) return "[sky]";
        else return "[stat]";
    }

    enum Sort {
        begin(Structs.comps(Structs.comparingFloat(g -> g.begin), Structs.comparingFloat(g -> g.type.id))),
        health(Structs.comps(Structs.comparingFloat(g -> g.type.health), Structs.comparingFloat(g -> g.begin))),
        type(Structs.comps(Structs.comparingFloat(g -> g.type.id), Structs.comparingFloat(g -> g.begin)));

        static final Sort[] all = values();

        final Comparator<SpawnGroup> sort;

        Sort(Comparator<SpawnGroup> sort) {
            this.sort = sort;
        }
    }

    void updateWaves() {
        graph.groups = groups;
        graph.from = start;
        graph.to = start + displayed;
        graph.rebuild();
    }

    void arcSpawner() {
        BaseDialog dialog = new BaseDialog("ARC-随机生成器");

        Table table = dialog.cont;
        Runnable[] rebuild = {null};
        rebuild[0] = () -> {

            table.clear();
            table.table(c -> {
                c.table(ct -> {
                    ct.add("难度：").width(100f);
                    ct.field(difficult + "", text -> {
                        difficult = Float.parseFloat(text);
                    }).valid(Strings::canParsePositiveFloat).width(200f);
                }).width(300f);
                c.row();
                c.button("单位设置", showUnitSelect ? Icon.upOpen : Icon.downOpen, Styles.togglet, () -> {
                    showUnitSelect = !showUnitSelect;
                    rebuild[0].run();
                }).fillX().minWidth(400f).row();
                c.row();
                if (showUnitSelect) {
                    c.table(list -> {
                        int i = 0;
                        for (UnitType unit : content.units()) {
                            if (i++ % 8 == 0) list.row();
                            list.button(unit.emoji(), flatToggleMenut, () -> {
                                if (spawnUnit.contains(unit)) spawnUnit.remove(unit);
                                else spawnUnit.add(unit);
                                rebuild[0].run();
                            }).tooltip(unit.localizedName).checked(spawnUnit.contains(unit)).size(50f);
                        }
                    }).row();
                    c.table(ct -> {
                        ct.add("环境").width(50f);
                        ct.button("Surplus", flatToggleMenut, () -> {
                            surplusUnit = !surplusUnit;
                            for (UnitType unit : allowUnit.copy().filter(unitType -> !(unitType instanceof ErekirUnitType))) {
                                filterUnit(unit, surplusUnit);
                            }
                            rebuild[0].run();
                        }).checked(surplusUnit).width(120f);
                        ct.button("Erekir", flatToggleMenut, () -> {
                            ErekirUnit = !ErekirUnit;
                            for (UnitType unit : allowUnit.copy().filter(unitType -> unitType instanceof ErekirUnitType)) {
                                filterUnit(unit, ErekirUnit);
                            }
                            rebuild[0].run();
                        }).checked(ErekirUnit).width(120f);
                    });
                    c.row();
                    c.table(ct -> {
                        ct.add("兵种").width(50f);
                        ct.button("空军", flatToggleMenut, () -> {
                            flyingUnit = !flyingUnit;
                            for (UnitType unit : allowUnit.copy().filter(unitType -> unitType.flying)) {
                                filterUnit(unit, flyingUnit);
                            }
                            rebuild[0].run();
                        }).checked(flyingUnit).width(70f);
                        ct.button("海军", flatToggleMenut, () -> {
                            navalUnit = !navalUnit;
                            for (UnitType unit : allowUnit.copy().filter(unitType -> unitType.naval)) {
                                filterUnit(unit, navalUnit);
                            }
                            rebuild[0].run();
                        }).checked(navalUnit).width(70f);
                        ct.button("支援", flatToggleMenut, () -> {
                            supportUnit = !supportUnit;
                            for (UnitType unit : allowUnit.copy().filter(unitType -> unitType.controller instanceof BuilderAI || unitType.controller instanceof MinerAI || unitType.controller instanceof RepairAI)) {
                                filterUnit(unit, supportUnit);
                            }
                            rebuild[0].run();
                        }).checked(supportUnit).width(70f);
                    });
                }
                c.row();
                c.button("生成！", () -> {
                    groups.clear();
                    groups = Waves.generate(difficult / 10, flyingUnit, navalUnit, supportUnit);
                    updateWaves();
                    buildGroups();
                }).width(300f);
            });

        };
        rebuild[0].run();
        dialog.addCloseButton();
        dialog.show();
        ui.announce("功能制作中..请等待完成\n[orange]目前可调整：难度|空|海|辅，其他功能均无效", 15);
    }

    private void filterUnit(UnitType unit, boolean filter) {
        if (filter && !spawnUnit.contains(unit)) {
            spawnUnit.add(unit);
        } else if (!filter && spawnUnit.contains(unit)) {
            spawnUnit.remove(unit);
        }
    }

    public Seq<SpawnGroup> arcGenerate(float difficulty) {
        Rand rand = new Rand();
        UnitType[][] species = {
                {dagger, mace, fortress, scepter, reign},
                {nova, pulsar, quasar, vela, corvus},
                {crawler, atrax, spiroct, arkyid, toxopid},
                {risso, minke, bryde, sei, omura},
                {retusa, oxynoe, cyerce, aegires, navanax}, //retusa intentionally left out as it cannot damage the core properly
                {flare, horizon, zenith, antumbra, eclipse},
                {mono, poly, mega, quad, oct},
                {stell, locus, precept, vanquish, conquer},
                {merui, cleroi, anthicus, tecta, collaris},
                {elude, avert, obviate, quell, disrupt}
        };


        UnitType[][] fspec = species;

        //required progression:
        //- extra periodic patterns

        Seq<SpawnGroup> out = new Seq<>();

        //max reasonable wave, after which everything gets boring
        int cap = 150;

        float shieldStart = 30, shieldsPerWave = 20 + difficulty * 30f;
        float[] scaling = {1, 2f, 3f, 4f, 5f};

        Intc createProgression = start -> {
            //main sequence
            UnitType[] curSpecies = Structs.random(fspec);
            int curTier = 0;

            for (int i = start; i < cap; ) {
                int f = i;
                int next = rand.random(8, 16) + (int) Mathf.lerp(5f, 0f, difficulty) + curTier * 4;

                float shieldAmount = Math.max((i - shieldStart) * shieldsPerWave, 0);
                int space = start == 0 ? 1 : rand.random(1, 2);
                int ctier = curTier;

                //main progression
                out.add(new SpawnGroup(curSpecies[Math.min(curTier, curSpecies.length - 1)]) {{
                    unitAmount = f == start ? 1 : 6 / (int) scaling[ctier];
                    begin = f;
                    end = f + next >= cap ? never : f + next;
                    max = 13;
                    unitScaling = (difficulty < 0.4f ? rand.random(2.5f, 5f) : rand.random(1f, 4f)) * scaling[ctier];
                    shields = shieldAmount;
                    shieldScaling = shieldsPerWave;
                    spacing = space;
                }});

                //extra progression that tails out, blends in
                out.add(new SpawnGroup(curSpecies[Math.min(curTier, curSpecies.length - 1)]) {{
                    unitAmount = 3 / (int) scaling[ctier];
                    begin = f + next - 1;
                    end = f + next + rand.random(6, 10);
                    max = 6;
                    unitScaling = rand.random(2f, 4f);
                    spacing = rand.random(2, 4);
                    shields = shieldAmount / 2f;
                    shieldScaling = shieldsPerWave;
                }});

                i += next + 1;
                if (curTier < 3 || (rand.chance(0.05) && difficulty > 0.8)) {
                    curTier++;
                }

                //do not spawn bosses
                curTier = Math.min(curTier, 3);

                //small chance to switch species
                if (rand.chance(0.3)) {
                    curSpecies = Structs.random(fspec);
                }
            }
        };

        createProgression.get(0);

        int step = 5 + rand.random(5);

        while (step <= cap) {
            createProgression.get(step);
            step += (int) (rand.random(15, 30) * Mathf.lerp(1f, 0.5f, difficulty));
        }

        int bossWave = (int) (rand.random(50, 70) * Mathf.lerp(1f, 0.5f, difficulty));
        int bossSpacing = (int) (rand.random(25, 40) * Mathf.lerp(1f, 0.5f, difficulty));

        int bossTier = difficulty < 0.6 ? 3 : 4;

        //main boss progression
        out.add(new SpawnGroup(Structs.random(species)[bossTier]) {{
            unitAmount = 1;
            begin = bossWave;
            spacing = bossSpacing;
            end = never;
            max = 16;
            unitScaling = bossSpacing;
            shieldScaling = shieldsPerWave;
            effect = StatusEffects.boss;
        }});

        //alt boss progression
        out.add(new SpawnGroup(Structs.random(species)[bossTier]) {{
            unitAmount = 1;
            begin = bossWave + rand.random(3, 5) * bossSpacing;
            spacing = bossSpacing;
            end = never;
            max = 16;
            unitScaling = bossSpacing;
            shieldScaling = shieldsPerWave;
            effect = StatusEffects.boss;
        }});

        int finalBossStart = 120 + rand.random(30);

        //final boss waves
        out.add(new SpawnGroup(Structs.random(species)[bossTier]) {{
            unitAmount = 1;
            begin = finalBossStart;
            spacing = bossSpacing / 2;
            end = never;
            unitScaling = bossSpacing;
            shields = 500;
            shieldScaling = shieldsPerWave * 4;
            effect = StatusEffects.boss;
        }});

        //final boss waves (alt)
        out.add(new SpawnGroup(Structs.random(species)[bossTier]) {{
            unitAmount = 1;
            begin = finalBossStart + 15;
            spacing = bossSpacing / 2;
            end = never;
            unitScaling = bossSpacing;
            shields = 500;
            shieldScaling = shieldsPerWave * 4;
            effect = StatusEffects.boss;
        }});

        //add megas to heal the base.
        if (supportUnit && difficulty >= 0.5) {
            int amount = Mathf.random(1, 3 + (int) (difficulty * 2));

            for (int i = 0; i < amount; i++) {
                int wave = Mathf.random(3, 20);
                out.add(new SpawnGroup(mega) {{
                    unitAmount = 1;
                    begin = wave;
                    end = wave;
                    max = 16;
                }});
            }
        }

        //shift back waves on higher difficulty for a harder start
        int shift = Math.max((int) (difficulty * 14 - 5), 0);

        for (SpawnGroup group : out) {
            group.begin -= shift;
            group.end -= shift;
        }

        return out;
    }

    public int calWinWave() {
        if (state.rules.winWave >= 1) return state.rules.winWave;
        int maxwave = 0;
        for (SpawnGroup group : state.rules.spawns) {
            if (group.end > 99999) continue;
            maxwave = Math.max(maxwave, group.end);
        }
        if (maxwave > 99999) return 200;
        if (maxwave < 2 && state.rules.waveSpacing > 30f) return (int) (1800000 / state.rules.waveSpacing);
        return maxwave + 1;
    }
}
