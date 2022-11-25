package mindustry.entities.abilities;

import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.tilesize;
import static mindustry.arcModule.RFuncs.abilitysFormat;

/** Spawns a certain amount of units upon death. */
public class SpawnDeathAbility extends Ability{
    public UnitType unit;
    public int amount = 1, randAmount = 0;
    /** Random spread of units away from the spawned. */
    public float spread = 8f;
    /** If true, units spawned face outwards from the middle. */
    public boolean faceOutwards = true;

    public SpawnDeathAbility(UnitType unit, int amount, float spread){
        this.unit = unit;
        this.amount = amount;
        this.spread = spread;
    }

    public SpawnDeathAbility(){
    }

    @Override
    public String description(UnitType unit){
        if (randAmount > 0) {
            return abilitysFormat("(@~@)@@~@格",
                    amount,
                    amount + randAmount,
                    this.unit.localizedName,
                    this.unit.emoji(),
                    spread / tilesize
            );
        }
        return abilitysFormat("@@@~@格",
                amount,
                this.unit.localizedName,
                this.unit.emoji(),
                spread / tilesize
        );
    }
    @Override
    public void death(Unit unit){
        if(!Vars.net.client()){
            int spawned = amount + Mathf.random(randAmount);
            for(int i = 0; i < spawned; i++){
                Tmp.v1.rnd(Mathf.random(spread));
                var u = this.unit.spawn(unit.team, unit.x + Tmp.v1.x, unit.y + Tmp.v1.y);

                u.rotation = faceOutwards ? Tmp.v1.angle() : unit.rotation + Mathf.range(5f);
            }
        }
    }
}
