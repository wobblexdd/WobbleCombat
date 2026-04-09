package com.wobble.wobblecombat.api.service;

import com.wobble.wobblecombat.combat.CombatHistoryEntry;
import com.wobble.wobblecombat.combat.CombatManager;
import org.bukkit.entity.Player;

import java.util.List;

public final class SimpleWobbleCombatService implements WobbleCombatService {

    private final CombatManager combatManager;

    public SimpleWobbleCombatService(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @Override
    public boolean isTagged(Player player) {
        return combatManager.isTagged(player);
    }

    @Override
    public int getRemainingSeconds(Player player) {
        return combatManager.getRemainingSeconds(player);
    }

    @Override
    public String getLastAttackerName(Player player) {
        return combatManager.getLastAttackerName(player);
    }

    @Override
    public List<CombatHistoryEntry> getHistory(Player player) {
        return combatManager.getHistory(player);
    }

    @Override
    public void clearTag(Player player) {
        combatManager.clear(player);
    }

    @Override
    public void forceTag(Player attacker, Player victim) {
        combatManager.tag(attacker, victim);
    }

    @Override
    public int getDurationSeconds() {
        return combatManager.getDurationSeconds();
    }
}
