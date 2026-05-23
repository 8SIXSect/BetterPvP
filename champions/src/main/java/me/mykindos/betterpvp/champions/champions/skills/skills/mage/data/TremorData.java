package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

@Data
public class TremorData {

    @Setter(AccessLevel.NONE)
    private Location origin;
    private double currentRadius;
    @Setter(AccessLevel.NONE)
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public void setOrigin(Location origin) {
        this.origin = origin == null ? null : origin.clone();
    }
}
