package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.effects.EffectType;

public class WeightedEffect extends EffectType {

    @Override
    public String getName() {
        return "Weighted";
    }

    @Override
    public boolean isNegative() {
        return true;
    }
}
