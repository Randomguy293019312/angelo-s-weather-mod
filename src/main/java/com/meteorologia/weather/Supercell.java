package com.meteorologia.weather;

import net.minecraft.network.FriendlyByteBuf;

/** Supercelula: tempestade rotativa de grande escala que pode gerar um tornado. */
public class Supercell {
    public int id;
    public double x, z;          // centro (blocos)
    public double vx, vz;        // velocidade (blocos/tick)
    public float radius = 300F;  // raio da corrente ascendente
    public float baseY = 180F;   // altura da base da nuvem (Y do mundo)
    public float topY = 520F;    // altura do topo
    public float strength;       // 0..1 (maturidade)
    public float dStrength;      // variacao por tick (para o cliente interpolar)
    public float windAngle;      // direcao da bigorna (rad)

    // --- apenas servidor
    public int age;
    public int lifetime = 15000;
    public int maxEf = -1;       // potencial de tornado (-1 = nenhum)
    public boolean hasTornado;

    public void write(FriendlyByteBuf b) {
        b.writeVarInt(id);
        b.writeDouble(x);
        b.writeDouble(z);
        b.writeFloat((float) vx);
        b.writeFloat((float) vz);
        b.writeFloat(radius);
        b.writeFloat(baseY);
        b.writeFloat(topY);
        b.writeFloat(strength);
        b.writeFloat(dStrength);
        b.writeFloat(windAngle);
        b.writeByte(maxEf);
    }

    public static Supercell read(FriendlyByteBuf b) {
        Supercell s = new Supercell();
        s.id = b.readVarInt();
        s.x = b.readDouble();
        s.z = b.readDouble();
        s.vx = b.readFloat();
        s.vz = b.readFloat();
        s.radius = b.readFloat();
        s.baseY = b.readFloat();
        s.topY = b.readFloat();
        s.strength = b.readFloat();
        s.dStrength = b.readFloat();
        s.windAngle = b.readFloat();
        s.maxEf = b.readByte();
        return s;
    }
}
