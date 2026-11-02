package com.avatar.bending.abilities;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class EarthRidge extends EarthAbility implements AddonAbility {

    @Attribute(Attribute.COOLDOWN)
    private long cooldown = 5000;

    @Attribute(Attribute.DAMAGE)
    private double damage = 5.0;

    @Attribute(Attribute.RANGE)
    private double range = 15.0;

    @Attribute(Attribute.SPEED)
    private double speed = 0.8;

    @Attribute("ChargeTime")
    private long chargeTime = 1500; 

    private Location location;
    private Vector direction;
    private double distanceTraveled = 0;
    
    private long startTime;
    private boolean charged = false;
    private boolean fired = false;

    public EarthRidge(Player player) {
        super(player);

        if (!bends()) return;
        if (hasAbility(player, EarthRidge.class)) return;

        this.startTime = System.currentTimeMillis();
        start();
    }

    @Override
    public void progress() {
        if (player == null || !player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        if (!fired) {
            if (!player.isSneaking() && !charged) {
                remove();
                return;
            }

            if (!player.isSneaking() && charged) {
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || !isEarthbendable(targetBlock)) {
                    remove();
                    return;
                }

                this.location = targetBlock.getLocation().add(0.5, 1, 0.5);
                this.direction = player.getLocation().getDirection().setY(0).normalize().multiply(speed);
                this.fired = true;
                
                // Fixed modern cooldown registration
                BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
                if (bPlayer != null) {
                    bPlayer.addCooldown(this);
                }
                return; 
            }

            if (!charged && System.currentTimeMillis() > startTime + chargeTime) {
                charged = true;
                ParticleEffect.BLOCK_DUST.display(player.getLocation(), 10, 0.5, 0.2, 0.5, 0.1, player.getLocation().getBlock().getRelative(0, -1, 0).getType().createBlockData());
            }

            if (!charged) {
                ParticleEffect.SMOKE_NORMAL.display(player.getLocation().add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0.01);
            }
            return; 
        }

        if (distanceTraveled > range) {
            remove();
            return;
        }

        Block floorBlock = location.getBlock().getRelative(0, -1, 0);
        if (isEarthbendable(floorBlock)) {
            // Path is level
        } else if (isEarthbendable(location.getBlock().getRelative(0, -2, 0))) {
            location.add(0, -1, 0); 
        } else if (isEarthbendable(location.getBlock())) {
            location.add(0, 1, 0);  
        } else {
            remove();
            return;
        }

        location.add(direction);
        distanceTraveled += speed;

        if (floorBlock.getType().createBlockData() != null) {
            ParticleEffect.BLOCK_DUST.display(location, 6, 0.3, 0.2, 0.3, 0.1, floorBlock.getType().createBlockData());
        }
        ParticleEffect.CRIT.display(location, 3, 0.2, 0.2, 0.2, 0.05);

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.8)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity target = (LivingEntity) entity;
                DamageHandler.damageEntity(target, player, damage, this);
                target.setVelocity(new Vector(0, 0.6, 0)); 
                remove();
                return;
            }
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return "EarthRidge";
    }

    @Override
    public boolean isHarmless() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "YourGamertag";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}
}
