package mindustry.world.meta;

import arc.*;
import arc.struct.*;

import java.util.*;

/** Describes one type of stat for content. */
public class Stat implements Comparable<Stat>{
    public static final Seq<Stat> all = new Seq<>();

    public static final Stat

    health = new Stat("health"),
    armor = new Stat("armor"),
    size = new Stat("size"),
    unitrange = new Stat("unitrange"),
    displaySize = new Stat("displaySize"),
    buildTime = new Stat("buildTime"),
    buildCost = new Stat("buildCost"),
    canOverdrive = new Stat("canOverdrive"),
    memoryCapacity = new Stat("memoryCapacity"),
    explosiveness = new Stat("explosiveness"),
    flammability = new Stat("flammability"),
    radioactivity = new Stat("radioactivity"),
    charge = new Stat("charge"),
    cost = new Stat("cost"),
    healthScaling = new Stat("healthScaling"),
    hardness = new Stat("hardness"),
    buildable = new Stat("buildable"),
    heatCapacity = new Stat("heatCapacity"),
    viscosity = new Stat("viscosity"),
    temperature = new Stat("temperature"),
    boilPoint = new Stat("boilPoint"),
    baseDeflectChance = new Stat("baseDeflectChance"),
    lightningChance = new Stat("lightningChance"),
    lightningDamage = new Stat("lightningDamage"),
    maxUnits = new Stat("maxUnits"),

    damageMultiplier = new Stat("damageMultiplier"),
    healthMultiplier = new Stat("healthMultiplier"),
    speedMultiplier = new Stat("speedMultiplier"),
    reloadMultiplier = new Stat("reloadMultiplier"),
    buildSpeedMultiplier = new Stat("buildSpeedMultiplier"),
    reactive = new Stat("reactive"),
    healing = new Stat("healing"),
    immunities = new Stat("immunities"),

    itemsMoved = new Stat("itemsMoved", StatCat.items),
    itemCapacity = new Stat("itemCapacity", StatCat.items),
    bufferCapacity = new Stat("bufferCapacity", StatCat.items),
    launchTime = new Stat("launchTime", StatCat.items),
    maxConsecutive = new Stat("maxConsecutive", StatCat.items),

    liquidCapacity = new Stat("liquidCapacity", StatCat.liquids),

    powerCapacity = new Stat("powerCapacity", StatCat.power),
    powerUse = new Stat("powerUse", StatCat.power),
    powerDamage = new Stat("powerDamage", StatCat.power),
    powerRange = new Stat("powerRange", StatCat.power),
    powerConnections = new Stat("powerConnections", StatCat.power),
    basePowerGeneration = new Stat("basePowerGeneration", StatCat.power),

    tiles = new Stat("tiles", StatCat.crafting),
    input = new Stat("input", StatCat.crafting),
    output = new Stat("output", StatCat.crafting),
    sepOutput = new Stat("sepOutput", StatCat.crafting),
    productionTime = new Stat("productionTime", StatCat.crafting),
    maxEfficiency = new Stat("maxEfficiency", StatCat.crafting),
    Tier = new Stat("Tier", StatCat.crafting),
    drillTier = new Stat("drillTier", StatCat.crafting),
    drillSpeed = new Stat("drillSpeed", StatCat.crafting),
    drillTime = new Stat("drillTime", StatCat.crafting),
    linkRange = new Stat("linkRange", StatCat.crafting),
    instructions = new Stat("instructions", StatCat.crafting),

    buildSpeed = new Stat("buildSpeed", StatCat.support),
    uniMineTier = new Stat("uniMineTier", StatCat.support),
    mineSpeed = new Stat("mineSpeed", StatCat.support),
    mineTier = new Stat("mineTier", StatCat.support),
    unitItemCapacity = new Stat("unitItemCapacity", StatCat.support),
    payloadCapacity = new Stat("payloadCapacity", StatCat.support),

    abilities = new Stat("abilities", StatCat.combat),
    weapons = new Stat("weapons", StatCat.combat),
    bullet = new Stat("bullet", StatCat.combat),
    estimateDPS = new Stat("estimateDPS", StatCat.combat),
    aiController = new Stat("aiController", StatCat.combat),
    targets = new Stat("targets", StatCat.combat),
    ammoType = new Stat("ammoType", StatCat.combat),
    ammoCapacity = new Stat("ammoCapacity", StatCat.combat),

    flying = new Stat("flying", StatCat.movement),
    speed = new Stat("speed", StatCat.movement),
    rotateSpeed = new Stat("rotateSpeed", StatCat.movement),
    canBoost = new Stat("canBoost", StatCat.movement),
    boostMultiplier = new Stat("boostMultiplier", StatCat.movement),
    drownTimeMultiplier = new Stat("drownTimeMultiplier", StatCat.movement),

    speedIncrease = new Stat("speedIncrease", StatCat.function),
    repairTime = new Stat("repairTime", StatCat.function),
    repairSpeed = new Stat("repairSpeed", StatCat.function),
    range = new Stat("range", StatCat.function),
    shootRange = new Stat("shootRange", StatCat.function),
    inaccuracy = new Stat("inaccuracy", StatCat.function),
    shots = new Stat("shots", StatCat.function),
    reload = new Stat("reload", StatCat.function),
    targetsAir = new Stat("targetsAir", StatCat.function),
    targetsGround = new Stat("targetsGround", StatCat.function),
    damage = new Stat("damage", StatCat.function),
    ammo = new Stat("ammo", StatCat.function),
    ammoUse = new Stat("ammoUse", StatCat.function),
    shieldHealth = new Stat("shieldHealth", StatCat.function),
    cooldownTime = new Stat("cooldownTime", StatCat.function),
    moduleTier = new Stat("moduletier", StatCat.function),
    regenSpeed = new Stat("regenSpeed", StatCat.function),

    booster = new Stat("booster", StatCat.optional),
    boostEffect = new Stat("boostEffect", StatCat.optional),
    affinities = new Stat("affinities", StatCat.optional),
    opposites = new Stat("opposites", StatCat.optional);

    public final StatCat category;
    public final String name;
    public final int id;

    public Stat(String name, StatCat category){
        this.category = category;
        this.name = name;
        id = all.size;
        all.add(this);
    }

    public Stat(String name){
        this(name, StatCat.general);
    }

    public String localized(){
        return Core.bundle.get("stat." + name.toLowerCase(Locale.ROOT));
    }

    @Override
    public int compareTo(Stat o){
        return id - o.id;
    }
}
