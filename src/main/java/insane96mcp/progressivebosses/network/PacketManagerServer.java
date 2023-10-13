package insane96mcp.progressivebosses.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class PacketManagerServer {
    public static void MessageWitherSync(ServerPlayer player, WitherBoss wither, byte charging) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeInt(wither.getId());
        buf.writeByte(charging);
        ServerPlayNetworking.send(player, Packets.WITHER_SYNC_ID, buf);
    }
}