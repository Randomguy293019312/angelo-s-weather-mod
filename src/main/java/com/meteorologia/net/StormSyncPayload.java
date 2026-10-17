package com.meteorologia.net;

import com.meteorologia.Meteorologia;
import com.meteorologia.weather.Supercell;
import com.meteorologia.weather.Tornado;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Estado das tempestades enviado do servidor para os clientes. */
public record StormSyncPayload(float rain, List<Supercell> supercells, List<Tornado> tornadoes)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StormSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Meteorologia.MODID, "storm_sync"));

    public static final StreamCodec<FriendlyByteBuf, StormSyncPayload> CODEC =
            new StreamCodec<FriendlyByteBuf, StormSyncPayload>() {
                @Override
                public StormSyncPayload decode(FriendlyByteBuf buf) {
                    float rain = buf.readFloat();
                    int sc = buf.readVarInt();
                    List<Supercell> supercells = new ArrayList<>(sc);
                    for (int i = 0; i < sc; i++) {
                        supercells.add(Supercell.read(buf));
                    }
                    int tc = buf.readVarInt();
                    List<Tornado> tornadoes = new ArrayList<>(tc);
                    for (int i = 0; i < tc; i++) {
                        tornadoes.add(Tornado.read(buf));
                    }
                    return new StormSyncPayload(rain, supercells, tornadoes);
                }

                @Override
                public void encode(FriendlyByteBuf buf, StormSyncPayload payload) {
                    buf.writeFloat(payload.rain);
                    buf.writeVarInt(payload.supercells.size());
                    for (Supercell s : payload.supercells) {
                        s.write(buf);
                    }
                    buf.writeVarInt(payload.tornadoes.size());
                    for (Tornado t : payload.tornadoes) {
                        t.write(buf);
                    }
                }
            };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
