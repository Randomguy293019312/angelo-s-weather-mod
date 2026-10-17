import sys
sys.argv = ["x"]
exec(open("/home/claude/tools/run_gl.py").read().split("sun = (0.35")[0])
from PIL import Image, ImageDraw
sun = (0.35, 0.8, 0.2, 0); sky = (0.5, 0.6, 0.8, 1)
def tor(name, cam, look, R0, H, s, reach=1.0, cloudR=182):
    run(name, cam, look, 0.0, {"Origin": (-cam[0], -cam[1], -cam[2], 0), "StormA": (R0, H, s, 3.0),
        "StormB": (cloudR, 0.55 + 0.45 * s, 1.0, 0.0), "SupA": (reach, 0, 0, 0),
        "Env": (0.75, 48, 0, 1500), "SunDir": sun, "SkyCol": sky})
tor("t_forming", (0, 6, -260), (0, 70, 0), 14, 110, 0.35, 0.407)   # funil descendo
tor("t_close",   (0, 3, -70),  (0, 40, 0), 14, 110, 1.0, 1.0)    # muito perto
tor("t_rope",    (0, 6, -260), (0, 60, 0), 14, 110, 0.12, 1.0)   # fase de corda
tor("t_weak",    (0, 6, -150), (0, 50, 0), 4, 100, 0.9, 1.0)     # EF0 pequeno
names = ["t_forming", "t_close", "t_rope", "t_weak"]
ims = [Image.open(f"/tmp/gl_{n}.png").resize((480, 270)) for n in names]
sheet = Image.new("RGB", (960, 540))
for i, im in enumerate(ims):
    d = ImageDraw.Draw(im); d.text((6, 6), ["formando (0.35)", "perto (80 blocos)", "corda (0.12)", "EF0 pequeno"][i], fill=(255, 255, 255))
    sheet.paste(im, ((i % 2) * 480, (i // 2) * 270))
sheet.save("/tmp/gl_life.png")
# NaN/Inf check na imagem final
a = np.asarray(Image.open("/tmp/gl_t_close.png")); print("pixel min/max:", a.min(), a.max())
