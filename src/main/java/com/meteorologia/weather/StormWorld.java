package com.meteorologia.weather;

import java.util.ArrayList;
import java.util.List;

/** Estado das tempestades de uma dimensao (servidor). */
public final class StormWorld {
    public final List<Supercell> supercells = new ArrayList<>();
    public final List<Tornado> tornadoes = new ArrayList<>();
    public int nextId = 1;
    /** 0 = sem chuva forte, 0.35 leve, 0.65 moderada, 1.0 forte. */
    public float rainIntensity = 0F;
    public boolean sentEmpty = false;

    public Supercell findSupercell(int id) {
        for (Supercell s : supercells) {
            if (s.id == id) {
                return s;
            }
        }
        return null;
    }
}
