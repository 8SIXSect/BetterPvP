package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

@Data
public class TremorData {

    private Location origin;
    private double currentRadius;
    private Set<LivingEntity> hitEntities = new HashSet<>();
}
