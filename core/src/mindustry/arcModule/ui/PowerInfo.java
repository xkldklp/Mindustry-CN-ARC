package mindustry.arcModule.ui;

import arc.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.ui.*;
import mindustry.core.*;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.power.*;

public class PowerInfo {

    private static PowerInfo info = new PowerInfo();
    public float powerbal;
    public float stored;
    public float capacity;
    public float produced;
    public float need;

    public void add(float powerbal, float stored, float cap, float produced, float need){
        this.powerbal += powerbal;
        this.stored += stored;
        this.capacity += cap;
        this.produced += produced;
        this.need += need;
    }

    public int getPowerBalance(){
        return (int)(powerbal * 60);
    }

    public float getStored(){
        return stored;
    }

    public float getCapacity(){
        return capacity;
    }

    public float getSatisfaction(){
        if(Mathf.zero(produced)){
            return 0f;
        }else if(Mathf.zero(need)){
            return 1f;
        }
        return produced / need;
    }


    public static void update() {
        if (PowerGraph.activeGraphs == null) return;
        PowerInfo newInfo = new PowerInfo();
        PowerGraph.activeGraphs.each(item -> {
            if (item != null) {
                item.updateActive();
                if(item.team == Vars.player.team()){
                    newInfo.add(item.getPowerBalance(), item.getLastPowerStored(), item.getLastCapacity(), item.getLastPowerProduced(), item.getLastPowerNeeded());
                }

            }
        });
        info = newInfo;
    }

    public static Element getBars() {
        Table power = new Table(Tex.wavepane).marginTop(6);

        Bar powerBar = new Bar(
                () -> Core.bundle.format("bar.powerbalance", (info.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount(info.getPowerBalance())) +
                        (info.getSatisfaction() >= 1 ? "" :" [gray]" + (int)(info.getSatisfaction() * 100) + "%"),
                () -> Pal.powerBar,
                () -> info.getSatisfaction());
        Bar batteryBar = new Bar(
                () -> Core.bundle.format("bar.powerstored", UI.formatAmount((long)info.getStored()), UI.formatAmount((long)info.getCapacity())),
                () -> Pal.powerBar,
                () -> info.getStored() / info.getCapacity());

        power.clicked(() -> {
            int arccoreitems = Core.settings.getInt("arccoreitems");
            Core.settings.put("arccoreitems", (arccoreitems+1) % 4);});
        power.margin(0);
        power.add(powerBar).height(18).growX().padBottom(1);
        power.row();
        power.add(batteryBar).height(18).growX().padBottom(1);

		power.update(PowerInfo::update);
        return power;
    }
}