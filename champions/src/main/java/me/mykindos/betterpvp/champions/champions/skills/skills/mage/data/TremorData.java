package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

@Getter
public class TremorData {

    private final Location origin;
    private double currentRadius;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public TremorData(Location origin) {
        this.origin = origin.clone();
    }

    public void increaseRadius(double amount) {
        currentRadius += amount;
    }
}
