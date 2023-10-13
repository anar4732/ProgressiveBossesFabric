package insane96mcp.progressivebosses.network;

import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Strings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class PacketManagerClient {
	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Packets.WITHER_SYNC_ID, (client, handler, buf, responseSender) -> {
			int id = buf.readInt();
			byte charging = buf.readByte();

			client.execute(() -> {
				Entity entity = client.level.getEntity(id);
				if (entity instanceof WitherBoss wither) {
					((IEntityExtraData) wither).getPersistentData().putByte(Strings.Tags.CHARGE_ATTACK, charging);
				}
			});
		});
	}
}