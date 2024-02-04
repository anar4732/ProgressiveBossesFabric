package insane96mcp.progressivebosses.utils;

import insane96mcp.progressivebosses.module.dragon.entity.AreaEffectCloud3DEntity;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityType;
import net.minecraft.potion.Potions;

@ConfigEntries(includeAll = true)
@Label(name = "Area Effect Cloud 3D", description = "No more boring 2D Area of Effect Clouds")
public class AEC3DFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Replace Vanilla Area of Effect Clouds", comment = "If true, vanilla Area of Effect Clouds will be replaced with 3D versions of them")
	public boolean replaceVanillaAEC = true;

	public AEC3DFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
	}

	public void onSpawn(DummyEvent event) {
		if (!this.replaceVanillaAEC)
			return;

		if (!event.getEntity().getType().equals(EntityType.AREA_EFFECT_CLOUD))
			return;

		AreaEffectCloudEntity areaEffectCloud = (AreaEffectCloudEntity) event.getEntity();
		if (areaEffectCloud.effects.isEmpty() && areaEffectCloud.potion.equals(Potions.EMPTY))
			return;
		
		areaEffectCloud.remove(RemovalReason.DISCARDED);
		AreaEffectCloud3DEntity areaEffectCloud3D = new AreaEffectCloud3DEntity(areaEffectCloud);

		areaEffectCloud3D.getWorld().spawnEntity(areaEffectCloud3D);
	}
}
