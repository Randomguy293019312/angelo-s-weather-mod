#version 150

// ===========================================================================
//  Meteorologia - ray marching volumetrico de tornados e supercelulas
//
//  Desenha um quad de tela cheia. Para cada pixel reconstroi o raio no espaco
//  do mundo, intersecta com um cilindro limitador e faz o "march" acumulando
//  densidade (funil, poeira, nuvem-parede ou supercelula). O buffer de
//  profundidade do jogo e usado para o terreno ocultar a tempestade.
//
//  Env.z = 0 -> modo tornado     Env.z = 1 -> modo supercelula distante
// ===========================================================================

uniform sampler2D DepthSampler;

uniform mat4 CamRot;   // rotacao view -> mundo
uniform vec4 Proj;     // 1/P00, 1/P11, P22, P32
uniform vec4 Origin;   // xyz = origem da tempestade relativa a camera
uniform vec4 StormA;   // tornado: x raio no chao, y altura ate a base da nuvem, z forca 0..1, w tempo (s)
uniform vec4 StormB;   // x raio da nuvem-mae, y escuridao 0..1, z giro (+1 anti-horario), w semente
uniform vec4 SupA;     // supercelula: x raio, y altura da base, z altura do topo, w direcao do vento (rad)
                       // tornado: x = alcance do funil (0 = so na base da nuvem, 1 = toca o chao)
uniform vec4 Env;      // x luz do dia, y passos, z modo, w distancia de neblina
uniform vec4 SunDir;   // xyz direcao do sol
uniform vec4 SkyCol;   // rgb cor do ceu

in vec2 ndc;
out vec4 fragColor;

// ---------------------------------------------------------------- ruido ----
float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

float vnoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash13(i);
    float b = hash13(i + vec3(1.0, 0.0, 0.0));
    float c = hash13(i + vec3(0.0, 1.0, 0.0));
    float d = hash13(i + vec3(1.0, 1.0, 0.0));
    float e = hash13(i + vec3(0.0, 0.0, 1.0));
    float g = hash13(i + vec3(1.0, 0.0, 1.0));
    float h = hash13(i + vec3(0.0, 1.0, 1.0));
    float k = hash13(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(a, b, f.x), mix(c, d, f.x), f.y),
               mix(mix(e, g, f.x), mix(h, k, f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float amp = 0.5;
    float sum = 0.0;
    for (int i = 0; i < 4; i++) {
        sum += amp * vnoise(p);
        p = p * 2.03 + vec3(17.1, 3.7, 9.2);
        amp *= 0.5;
    }
    return sum;
}

vec2 rot2(vec2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// ------------------------------------------------------- campo: tornado ----
// dF = funil de condensacao, dD = poeira/detritos no chao, dC = nuvem-parede
void tornadoField(vec3 p, bool detail, out float dF, out float dD, out float dC) {
    dF = 0.0;
    dD = 0.0;
    dC = 0.0;

    float t = StormA.w;
    float R0 = StormA.x;
    float H = StormA.y;
    float str = StormA.z;
    float spin = StormB.z;
    float reach = SupA.x;
    float h = p.y;

    // ---- funil: desce da nuvem, afina quando o tornado enfraquece (corda)
    if (h > -4.0 && h < H + 10.0) {
        float hn = clamp(h / H, 0.0, 1.0);
        float tip = pow(1.0 - clamp(reach, 0.0, 1.0), 1.3) * H * 0.92;
        float vis = smoothstep(tip - 4.0, tip + 12.0, h);
        float sway = (0.25 + 1.5 * (1.0 - str)) * R0 * (0.3 + 2.5 * hn * hn);
        vec2 c = vec2(sin(h * 0.045 + t * 0.9), cos(h * 0.037 + t * 0.7)) * sway;
        vec2 q = p.xz - c;
        float rr = length(q);
        float thick = mix(0.30, 1.0, smoothstep(0.0, 0.85, str));
        float rad = R0 * thick * (0.55 + 1.9 * hn * hn) + 0.8;
        if (rr < rad * 1.7 && vis > 0.0) {
            float ang = atan(q.y, q.x + 0.0001) + spin * (t * 2.6 - hn * 2.2);
            vec3 np = vec3(cos(ang) * 2.0 + rr * 0.03, sin(ang) * 2.0, h * 0.035 - t * 1.3);
            float n = detail ? fbm(np) : vnoise(np);
            float edge = rr / rad;
            float body = 1.0 - smoothstep(0.35, 1.0, edge + (n - 0.5) * 0.75);
            dF = body * vis * (0.55 + 0.9 * n) * 5.0 * smoothstep(0.0, 0.06, str);
        }
    }

    // ---- poeira e detritos girando no chao
    float gb = smoothstep(0.05, 0.6, str) * smoothstep(0.7, 1.0, reach);
    if (gb > 0.0 && h > -3.0 && h < 70.0 + R0 * 2.0) {
        float dr = R0 * (1.8 + 1.0 * gb) + h * 0.5;
        vec2 q = p.xz;
        float rr = length(q);
        if (rr < dr * 1.4) {
            vec2 qr = rot2(q, spin * (t * 1.5 - rr * 0.012));
            vec3 np = vec3(qr * 0.045, h * 0.03 - t * 0.35);
            float n = detail ? fbm(np) : vnoise(np);
            float vf = exp(-max(h, 0.0) / (9.0 + R0 * 1.3));
            float rm = 1.0 - smoothstep(0.25, 1.0, rr / dr + (0.5 - n) * 0.6);
            dD = rm * vf * gb * 1.5;
        }
    }

    // ---- nuvem-parede (wall cloud) girando acima do funil
    float Rc = StormB.x;
    if (h > H - 60.0 && h < H + 170.0) {
        vec2 q = p.xz;
        float rr = length(q);
        if (rr < Rc * 1.15) {
            float rn = rr / Rc;
            float base = H - 50.0 * exp(-rn * rn * 8.0) * (0.4 + 0.6 * str);
            float top = H + 40.0 + 50.0 * (1.0 - rn);
            vec2 qr = rot2(q, spin * (t * 0.10 + (1.0 - rn) * 1.3));
            vec3 np = vec3(qr * 0.016, h * 0.018 + 4.0 + StormB.w);
            float n = detail ? fbm(np) : vnoise(np);
            float vert = smoothstep(base - 6.0, base + 22.0, h) * (1.0 - smoothstep(top - 40.0, top, h));
            float rm = 1.0 - smoothstep(0.5, 1.0, rn + (0.5 - n) * 0.5);
            dC = vert * rm * (0.35 + 0.9 * n);
        }
    }
}

// ---------------------------------------------------- campo: supercelula ----
float superField(vec3 p, bool detail) {
    float R = SupA.x;
    float hb = SupA.y;
    float ht = SupA.z;
    float h = p.y;
    if (h < hb - 60.0 || h > ht + 60.0) {
        return 0.0;
    }
    float hn = (h - hb) / (ht - hb);
    vec2 wd = vec2(cos(SupA.w), sin(SupA.w));
    float lean = clamp(hn, 0.0, 1.2);
    vec2 centre = wd * (lean * lean * R * 0.35);
    vec2 q = p.xz - centre;
    float rr = length(q);
    float t = StormA.w;

    // coluna de corrente ascendente (engorda no meio, afina no topo)
    float colR = R * (0.62 + 0.28 * sin(clamp(hn, 0.0, 1.0) * 2.6));
    float col = 1.0 - smoothstep(0.10, 1.0, rr / colR);
    col *= smoothstep(-0.04, 0.06, hn) * (1.0 - smoothstep(0.96, 1.06, hn));

    // prateleira escura da base
    float shelf = 1.0 - smoothstep(0.15, 1.0, length(p.xz - wd * R * 0.2) / (R * 1.35));
    shelf *= smoothstep(-0.14, -0.02, hn) * (1.0 - smoothstep(0.02, 0.16, hn));

    // bigorna: camada fina que se espalha a favor do vento
    float anvil = 1.0 - smoothstep(0.10, 1.0, length(p.xz - wd * R * 1.4) / (R * 2.9));
    anvil *= smoothstep(0.66, 0.74, hn) * (1.0 - smoothstep(0.84, 0.94, hn));

    float shape = max(max(col, shelf * 0.9), anvil * 0.85);
    if (shape <= 0.0) {
        return 0.0;
    }

    // rotacao lenta do mesociclone
    float swirl = StormB.z * t * 0.03 * (1.0 - clamp(rr / (R * 1.5), 0.0, 1.0));
    vec2 pr = centre + rot2(q, swirl);
    vec3 np = vec3(pr.x, p.y * 0.75, pr.y) * 0.0055 + vec3(0.0, -t * 0.015, StormB.w);
    float n = detail ? fbm(np) : vnoise(np * 1.7);
    float d = shape * 1.25 - (1.0 - n) * 0.95;
    return max(d, 0.0) * 1.7;
}

void main() {
    // ---- raio da camera no espaco do mundo
    vec3 vd = vec3(ndc.x * Proj.x, ndc.y * Proj.y, -1.0);
    vec3 rdU = (CamRot * vec4(vd, 0.0)).xyz;
    float rdLen = length(rdU);
    vec3 rd = rdU / rdLen;
    vec3 ro = -Origin.xyz;

    // ---- profundidade do terreno (distancia ao longo do raio)
    vec2 uv = ndc * 0.5 + 0.5;
    float dpt = texture(DepthSampler, uv).r;
    float tScene = 1.0e9;
    if (dpt < 0.99999) {
        float zn = dpt * 2.0 - 1.0;
        float lin = Proj.w / (zn + Proj.z);
        tScene = lin * rdLen;
    }

    // ---- volume limitador (cilindro vertical)
    float rb;
    float y0;
    float y1;
    if (Env.z < 0.5) {
        rb = max(StormB.x * 1.15, StormA.x * 5.0 + 40.0);
        y0 = -6.0;
        y1 = StormA.y + 160.0;
    } else {
        rb = SupA.x * 4.6;
        y0 = SupA.y - 60.0;
        y1 = SupA.z + 70.0;
    }

    float a = dot(rd.xz, rd.xz);
    float b = dot(ro.xz, rd.xz);
    float c = dot(ro.xz, ro.xz) - rb * rb;
    float tmin = 0.0;
    float tmax = tScene;
    if (a > 1.0e-6) {
        float disc = b * b - a * c;
        if (disc < 0.0) {
            discard;
        }
        float sq = sqrt(disc);
        tmin = max(tmin, (-b - sq) / a);
        tmax = min(tmax, (-b + sq) / a);
    } else if (c > 0.0) {
        discard;
    }
    if (abs(rd.y) > 1.0e-5) {
        float t0 = (y0 - ro.y) / rd.y;
        float t1 = (y1 - ro.y) / rd.y;
        tmin = max(tmin, min(t0, t1));
        tmax = min(tmax, max(t0, t1));
    } else if (ro.y < y0 || ro.y > y1) {
        discard;
    }
    if (tmax <= tmin) {
        discard;
    }

    // ---- ray marching
    int steps = int(Env.y);
    float range = tmax - tmin;
    float dtBase = range / float(steps);
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    float t = tmin + dtBase * jitter;

    vec3 col = vec3(0.0);
    float trans = 1.0;
    float tFirst = -1.0;
    vec3 sun = normalize(SunDir.xyz);

    for (int i = 0; i < 128; i++) {
        if (i >= steps || t > tmax || trans < 0.02) {
            break;
        }
        vec3 p = ro + rd * t;

        if (Env.z < 0.5) {
            float dF;
            float dD;
            float dC;
            tornadoField(p, true, dF, dD, dC);
            float dens = dF + dD + dC;
            if (dens > 0.003) {
                float lF;
                float lD;
                float lC;
                tornadoField(p + sun * 14.0, false, lF, lD, lC);
                float shadow = exp(-(lF + lD + lC) * 1.6);
                float wsum = dens + 0.0001;
                vec3 cloudCol = mix(vec3(0.50, 0.53, 0.58), vec3(0.20, 0.24, 0.24), StormB.y);
                vec3 alb = (vec3(0.80, 0.82, 0.86) * dF + vec3(0.46, 0.38, 0.30) * dD + cloudCol * dC) / wsum;
                float amb = mix(0.55, 0.28, StormB.y) * Env.x + 0.035;
                vec3 lit = alb * (amb + vec3(1.0, 0.95, 0.88) * 0.55 * Env.x * shadow);
                float aStep = 1.0 - exp(-dens * 0.075 * dtBase);
                col += trans * aStep * lit;
                trans *= (1.0 - aStep);
                if (tFirst < 0.0) {
                    tFirst = t;
                }
            }
        } else {
            float d = superField(p, true);
            if (d > 0.004) {
                float dl = superField(p + sun * 90.0, false);
                float shadow = exp(-dl * 2.2);
                float hgt = clamp((p.y - SupA.y) / (SupA.z - SupA.y), 0.0, 1.0);
                vec3 baseCol = mix(vec3(0.34, 0.40, 0.42), vec3(0.14, 0.17, 0.18), StormB.y);
                vec3 alb = mix(baseCol, vec3(0.95, 0.96, 1.0), smoothstep(0.0, 0.75, hgt));
                float amb = (0.30 + 0.25 * hgt) * Env.x + 0.04;
                vec3 lit = alb * (amb + vec3(1.0, 0.93, 0.82) * 0.75 * Env.x * shadow * (0.5 + 0.5 * hgt));
                float aStep = 1.0 - exp(-d * 0.012 * dtBase);
                col += trans * aStep * lit;
                trans *= (1.0 - aStep);
                if (tFirst < 0.0) {
                    tFirst = t;
                }
            }
        }
        t += dtBase;
    }

    float alpha = 1.0 - trans;
    if (alpha < 0.003) {
        discard;
    }

    // ---- neblina atmosferica com a distancia
    float tt = tFirst < 0.0 ? tmin : tFirst;
    float hz = 1.0 - exp(-tt / max(Env.w, 1.0));
    vec3 hazeCol = SkyCol.rgb * 0.9;
    col = mix(col, hazeCol * alpha, hz * 0.75);

    fragColor = vec4(col, alpha);
}
