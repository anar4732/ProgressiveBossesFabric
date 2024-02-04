package insane96mcp.progressivebosses.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class PacketManagerServer {
    public static void MessageWitherSync(ServerPlayerEntity player, WitherEntity wither, byte charging) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(wither.getId());
        buf.writeByte(charging);
        ServerPlayNetworking.send(player, Packets.WITHER_SYNC_ID, buf);
    }
}