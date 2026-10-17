package com.meteorologia.weather;

import net.minecraft.network.FriendlyByteBuf;

/** Tornado preso a uma supercelula. */
public class Tornado {
    public int id;
    public int parentId;
    public double x, y, z;
    public double vx, vz;        // blocos/tick
    public int ef;               // 0..5 (escala Fujita melhorada)
    public float strength;       // 0..1 (formando -> maduro -> corda)
    public float dStrength;
    public float radius = 8F;    // raio do funil no chao
    public float height = 120F;  // altura ate a base da nuvem

    // --- apenas servidor
    public int age;
    public int lifetime = 4800;
    public int formTicks = 600;
    public double offAngle;
    public double offDist;

    public void write(FriendlyByteBuf b) {
        b.writeVarInt(id);
        b.writeVarInt(parentId);
        b.writeDouble(x);
        b.writeDouble(y);
        b.writeDouble(z);
        b.writeFloat((float) vx);
        b.writeFloat((float) vz);
        b.writeByte(ef);
        b.writeFloat(strength);
        b.writeFloat(dStrength);
        b.writeFloat(radius);
        b.writeFloat(height);
    }

    public static Tornado read(FriendlyByteBuf b) {
        Tornado t = new Tornado();
        t.id = b.readVarInt();
        t.parentId = b.readVarInt();
        t.x = b.readDouble();
        t.y = b.readDouble();
        t.z = b.readDouble();
        t.vx = b.readFloat();
        t.vz = b.readFloat();
        t.ef = b.readByte();
        t.strength = b.readFloat();
        t.dStrength = b.readFloat();
        t.radius = b.readFloat();
        t.height = b.readFloat();
        return t;
    }
}
