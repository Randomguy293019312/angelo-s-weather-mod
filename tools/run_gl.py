import sys, math, numpy as np
sys.path.insert(0, "/home/claude/tools")
from gl_harness import *
from PIL import Image

D = "/home/claude/meteorologia/src/main/resources/assets/meteorologia/shaders/core/"
vsrc = open(D + "storm.vsh").read(); fsrc = open(D + "storm.fsh").read()
pr = build(vsrc, fsrc)
if pr is None: sys.exit("SHADER NAO COMPILA")

W, H = 640, 360
near, far = 0.05, 1024.0
P22 = -(far + near) / (far - near); P32 = -2 * far * near / (far - near)
fovy = math.radians(70); tanf = math.tan(fovy / 2); aspect = W / H
P00 = 1.0 / (tanf * aspect); P11 = 1.0 / tanf

def scene(cam, look, ground_y):
    fwd = np.array(look, float) - cam; fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, [0, 1, 0]); right /= np.linalg.norm(right); up = np.cross(right, fwd)
    rot = np.eye(4); rot[:3, 0] = right; rot[:3, 1] = up; rot[:3, 2] = -fwd
    ys, xs = np.mgrid[0:H, 0:W]
    ndcx = (xs + 0.5) / W * 2 - 1; ndcy = 1 - (ys + 0.5) / H * 2
    vd = np.stack([ndcx / P00, ndcy / P11, -np.ones_like(ndcx)], -1)
    rdU = vd @ rot[:3, :3].T
    # profundidade do terreno (plano y = ground_y): lin = distancia ao longo do eixo da camera
    with np.errstate(divide="ignore", invalid="ignore"):
        lin = (ground_y - cam[1]) / rdU[..., 1]
    hit = (rdU[..., 1] < -1e-6) & (lin > near) & (lin < far * 4)
    ndc = np.where(hit, (-P22 * lin + P32) / lin, 1.0)
    depth = np.where(hit & (ndc < 1), ndc * 0.5 + 0.5, 1.0)
    return rot, depth, hit, rdU

def background(rdU, hit, cam):
    rd = rdU / np.linalg.norm(rdU, axis=-1, keepdims=True)
    t = np.clip(rd[..., 1] * 1.8 + 0.1, 0, 1)[..., None]
    sky = np.array([0.66, 0.70, 0.76]) * (1 - t) + np.array([0.34, 0.44, 0.62]) * t
    ground = np.array([0.26, 0.36, 0.18])
    return np.where(hit[..., None], ground, sky)

def run(name, cam, look, ground_y, uni):
    rot, depth, hit, rdU = scene(np.array(cam, float), look, ground_y)
    u = dict(uni); u["CamRot"] = rot; u["Proj"] = (1 / P00, 1 / P11, P22, P32)
    px = render(pr, W, H, u, depth).astype(np.float32) / 255.0
    bg = background(rdU, hit, np.array(cam))
    out = px[..., :3] + bg * (1 - px[..., 3:4])
    img = (np.clip(out, 0, 1) ** (1 / 1.6) * 255).astype(np.uint8)
    Image.fromarray(img).save(f"/tmp/gl_{name}.png")
    print(name, "cobertura media do alpha:", round(float(px[..., 3].mean()), 3))

sun = (0.35, 0.8, 0.2, 0)
sky = (0.5, 0.6, 0.8, 1)
# ---- tornado EF3 (modo 0): mesmos valores que o Java envia
cam = (0, 6, -210)
run("tornado", cam, (0, 70, 0), 0.0, {
    "Origin": (0 - cam[0], 0 - cam[1], 0 - cam[2], 0),
    "StormA": (14, 110, 1.0, 3.0), "StormB": (182, 1.0, 1.0, 0.0), "SupA": (0, 0, 0, 0),
    "Env": (0.75, 48, 0, 1500), "SunDir": sun, "SkyCol": sky})
# ---- supercelula distante (modo 1)
cam = (0, 70, -1150)
run("super", cam, (250, 430, 0), 64.0, {
    "Origin": (0 - cam[0], -cam[1], 0 - cam[2], 0),
    "StormA": (0, 0, 0, 3.0), "StormB": (0, 1.0, 1.0, 0.0), "SupA": (380, 180, 950, 0.6),
    "Env": (0.8, 56, 1, 4500), "SunDir": (0.5, 0.75, 0.3, 0), "SkyCol": sky})
