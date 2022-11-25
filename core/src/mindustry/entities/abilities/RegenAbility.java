package mindustry.entities.abilities;

import arc.util.*;
import mindustry.gen.*;
import mindustry.type.UnitType;

import static mindustry.arcModule.RFuncs.abilitysFormat;

public class RegenAbility extends Ability{
    /** Amount healed as percent per tick. */
    public float percentAmount = 0f;
    /** Amount healed as a flat amount per tick. */
    public float amount = 0f;

    @Override
    public String description(UnitType unit){
        if (percentAmount > 0f && amount > 0f) {
            return abilitysFormat( "@%+@/s", percentAmount * 100f, amount);
        } else if (percentAmount > 0f) {
            return abilitysFormat( "@%/s", percentAmount * 100f);
        }
        else return abilitysFormat( "@/s", amount);
    }
    @Override
    public void update(Unit unit){
        unit.heal((unit.maxHealth * percentAmount / 100f + amount) * Time.delta);
    }
}
