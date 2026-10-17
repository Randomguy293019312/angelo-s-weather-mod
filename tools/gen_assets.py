import json, os, random, math
from PIL import Image, ImageDraw

ROOT = "/home/claude/meteorologia/src/main"
RES = ROOT + "/resources"
A = RES + "/assets/meteorologia"
D = RES + "/data/meteorologia"
random.seed(7)

def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

# ------------------------------------------------------------ texturas ------
def noisy(img, amt=10):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            n = random.randint(-amt, amt)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), a)

def save(img, rel):
    p = A + "/textures/" + rel
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)

def new(color, size=16):
    return Image.new("RGBA", (size, size), color)

# --- blocos
im = new((58, 64, 72, 255)); d = ImageDraw.Draw(im)
d.rectangle([0, 0, 15, 15], outline=(30, 34, 40, 255))
d.ellipse([3, 3, 12, 12], fill=(8, 40, 22, 255), outline=(60, 220, 110, 255))
d.line([8, 8, 12, 4], fill=(120, 255, 160, 255)); d.point((8, 8), fill=(255, 255, 255, 255))
d.rectangle([1, 1, 3, 2], fill=(200, 60, 50, 255)); noisy(im, 6); save(im, "block/radar.png")

im = new((236, 236, 242, 255)); d = ImageDraw.Draw(im)
for x in range(0, 16, 4): d.rectangle([x, 0, x + 1, 15], fill=(200, 40, 40, 255))
d.rectangle([0, 0, 15, 15], outline=(150, 150, 160, 255)); noisy(im, 5); save(im, "block/anemometer.png")

im = new((172, 128, 62, 255)); d = ImageDraw.Draw(im)
d.ellipse([3, 3, 12, 12], fill=(245, 240, 225, 255), outline=(60, 40, 20, 255))
d.line([8, 8, 11, 5], fill=(190, 30, 30, 255)); d.point((8, 8), fill=(0, 0, 0, 255))
for a in range(0, 360, 45):
    x = 7.5 + 4.6 * math.cos(math.radians(a)); y = 7.5 + 4.6 * math.sin(math.radians(a))
    d.point((int(x), int(y)), fill=(40, 40, 40, 255))
noisy(im, 6); save(im, "block/barometer.png")

im = new((238, 240, 244, 255)); d = ImageDraw.Draw(im)
for y in range(1, 16, 2): d.line([0, y, 15, y], fill=(176, 180, 190, 255))
d.rectangle([0, 0, 15, 15], outline=(120, 124, 134, 255)); noisy(im, 4); save(im, "block/thermometer.png")

im = new((74, 104, 146, 255)); d = ImageDraw.Draw(im)
d.rectangle([0, 0, 15, 15], outline=(36, 52, 80, 255))
for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]: d.point((x, y), fill=(220, 230, 245, 255))
d.rectangle([5, 5, 10, 10], fill=(210, 225, 245, 255), outline=(30, 40, 60, 255))
d.line([6, 9, 9, 6], fill=(200, 40, 40, 255)); noisy(im, 6); save(im, "block/weather_station.png")

im = new((160, 208, 236, 255)); d = ImageDraw.Draw(im)
d.rectangle([0, 0, 15, 15], outline=(80, 130, 170, 255))
for y in range(2, 15, 3): d.line([0, y, 4, y], fill=(30, 60, 100, 255))
d.rectangle([6, 8, 15, 15], fill=(70, 140, 220, 200)); noisy(im, 5); save(im, "block/rain_gauge.png")

im = new((196, 40, 40, 255)); d = ImageDraw.Draw(im)
for y in range(0, 16, 4): d.rectangle([0, y, 15, y + 1], fill=(240, 240, 240, 255))
d.rectangle([0, 0, 15, 15], outline=(110, 20, 20, 255)); noisy(im, 6); save(im, "block/storm_siren.png")

# --- itens
def item(name, fn):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im); fn(d); save(im, "item/" + name + ".png")

def radio(d):
    d.rectangle([3, 6, 12, 14], fill=(54, 58, 66, 255), outline=(24, 26, 30, 255))
    d.line([11, 6, 13, 1], fill=(190, 190, 200, 255))
    d.rectangle([4, 8, 8, 12], fill=(90, 96, 106, 255))
    for y in (9, 11): d.line([5, y, 7, y], fill=(30, 30, 34, 255))
    d.rectangle([10, 8, 11, 9], fill=(60, 230, 110, 255)); d.rectangle([10, 11, 11, 12], fill=(230, 60, 60, 255))
item("weather_radio", radio)

def meter(d):
    d.rectangle([4, 3, 11, 14], fill=(236, 196, 52, 255), outline=(110, 84, 16, 255))
    d.rectangle([5, 4, 10, 8], fill=(16, 40, 28, 255)); d.line([6, 7, 9, 5], fill=(80, 255, 140, 255))
    d.rectangle([6, 10, 9, 12], fill=(90, 90, 96, 255))
item("handheld_meter", meter)

def kit(d):
    d.rectangle([2, 5, 13, 13], fill=(214, 48, 48, 255), outline=(110, 18, 18, 255))
    d.rectangle([7, 6, 8, 12], fill=(250, 250, 250, 255)); d.rectangle([5, 8, 10, 9], fill=(250, 250, 250, 255))
    d.rectangle([6, 3, 9, 5], fill=(150, 150, 156, 255), outline=(70, 70, 76, 255))
item("emergency_kit", kit)

def tank(d):
    d.rectangle([5, 4, 10, 14], fill=(170, 176, 190, 255), outline=(70, 74, 86, 255))
    d.rectangle([5, 7, 10, 9], fill=(214, 60, 40, 255)); d.rectangle([6, 2, 9, 3], fill=(90, 94, 104, 255))
    d.point((7, 11), fill=(255, 255, 255, 255))
item("propane_tank", tank)

def bar(d):
    d.rectangle([2, 6, 13, 10], fill=(110, 70, 34, 255), outline=(60, 36, 16, 255))
    d.rectangle([2, 6, 5, 10], fill=(220, 60, 50, 255)); d.line([3, 8, 4, 8], fill=(255, 230, 120, 255))
    for x in (7, 9, 11): d.point((x, 8), fill=(200, 150, 80, 255))
item("energy_bar", bar)

def balloon(d):
    d.ellipse([2, 0, 13, 10], fill=(226, 70, 56, 255), outline=(110, 24, 20, 255))
    for x in (5, 8, 11): d.line([x, 1, x, 9], fill=(250, 250, 250, 255))
    d.line([5, 10, 6, 12], fill=(80, 60, 40, 255)); d.line([10, 10, 9, 12], fill=(80, 60, 40, 255))
    d.rectangle([5, 12, 10, 14], fill=(150, 104, 56, 255), outline=(80, 54, 26, 255))
item("hot_air_balloon", balloon)

# --- textura da entidade (256x256)
im = Image.new("RGBA", (256, 256), (0, 0, 0, 0)); px = im.load(); d = ImageDraw.Draw(im)
stripes = [(214, 56, 46), (248, 244, 232), (240, 150, 40), (248, 244, 232), (50, 100, 190), (248, 244, 232)]
for x in range(256):
    c = stripes[(x // 8) % len(stripes)]
    for y in range(0, 96):
        n = random.randint(-6, 6); shade = 1.0 - 0.0 * y
        px[x, y] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n)), max(0, min(255, c[2] + n)), 255)
for y in range(100, 140):
    for x in range(0, 100):
        base = (150, 104, 56) if ((x // 3 + y // 3) % 2 == 0) else (128, 86, 44)
        n = random.randint(-8, 8)
        px[x, y] = (base[0] + n, base[1] + n, base[2] + n, 255)
for y in range(150, 180):
    for x in range(0, 40):
        n = random.randint(-8, 8); px[x, y] = (92 + n, 66 + n, 40 + n, 255)
for y in range(100, 116):
    for x in range(100, 130):
        n = random.randint(-8, 8); px[x, y] = (96 + n, 100 + n, 112 + n, 255)
save(im, "entity/hot_air_balloon.png")

# ------------------------------------------------------------- modelos ------
FACES = ["north", "east", "south", "west", "up", "down"]
def el(f, t): return {"from": f, "to": t, "faces": {k: {"texture": "#t"} for k in FACES}}

BLOCKS = {
    "radar": [[0,0,0,16,5,16],[6,5,6,10,10,10],[2,10,2,14,13,14],[4,13,4,12,16,12]],
    "anemometer": [[7,0,7,9,11,9],[1,10,7,15,11,9],[7,10,1,9,11,15],[0,9,6,3,12,10],[13,9,6,16,12,10],[6,9,0,10,12,3],[6,9,13,10,12,16]],
    "barometer": [[2,0,2,14,2,14],[4,2,6,12,12,10]],
    "thermometer": [[3,0,3,5,5,5],[11,0,3,13,5,5],[3,0,11,5,5,13],[11,0,11,13,5,13],[2,5,2,14,13,14],[1,13,1,15,14,15]],
    "weather_station": [[4,0,4,12,6,12],[7,6,7,9,16,9],[1,14,7,15,15,9]],
    "rain_gauge": [[4,0,4,12,10,12],[3,10,3,13,12,13]],
    "storm_siren": [[3,0,3,13,3,13],[5,3,5,11,9,11],[2,9,2,14,13,14],[5,13,5,11,16,11]],
}
for name, boxes in BLOCKS.items():
    tex = "meteorologia:block/" + name
    wj(A + f"/models/block/{name}.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": tex, "t": tex},
        "elements": [el(b[0:3], b[3:6]) for b in boxes]})
    wj(A + f"/blockstates/{name}.json", {"variants": {"": {"model": "meteorologia:block/" + name}}})
    wj(A + f"/models/item/{name}.json", {"parent": "meteorologia:block/" + name})
    wj(D + f"/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                   "entries": [{"type": "minecraft:item", "name": "meteorologia:" + name}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

ITEMS = ["weather_radio", "handheld_meter", "emergency_kit", "propane_tank", "energy_bar", "hot_air_balloon"]
for name in ITEMS:
    wj(A + f"/models/item/{name}.json", {"parent": "minecraft:item/generated",
                                          "textures": {"layer0": "meteorologia:item/" + name}})

# --------------------------------------------------------------- idiomas ----
pt = {
 "block.meteorologia.radar": "Radar Meteorológico",
 "block.meteorologia.anemometer": "Anemômetro",
 "block.meteorologia.barometer": "Barômetro",
 "block.meteorologia.thermometer": "Termo-higrômetro",
 "block.meteorologia.weather_station": "Estação Meteorológica",
 "block.meteorologia.rain_gauge": "Pluviômetro",
 "block.meteorologia.storm_siren": "Sirene de Tempestade",
 "item.meteorologia.weather_radio": "Rádio Meteorológico",
 "item.meteorologia.handheld_meter": "Medidor Portátil",
 "item.meteorologia.emergency_kit": "Kit de Emergência",
 "item.meteorologia.propane_tank": "Botijão de Propano",
 "item.meteorologia.energy_bar": "Barra Energética",
 "item.meteorologia.hot_air_balloon": "Balão de Ar Quente",
 "entity.meteorologia.hot_air_balloon": "Balão de Ar Quente",
 "itemGroup.meteorologia": "Meteorologia",
}
en = {
 "block.meteorologia.radar": "Weather Radar",
 "block.meteorologia.anemometer": "Anemometer",
 "block.meteorologia.barometer": "Barometer",
 "block.meteorologia.thermometer": "Thermo-hygrometer",
 "block.meteorologia.weather_station": "Weather Station",
 "block.meteorologia.rain_gauge": "Rain Gauge",
 "block.meteorologia.storm_siren": "Storm Siren",
 "item.meteorologia.weather_radio": "Weather Radio",
 "item.meteorologia.handheld_meter": "Handheld Meter",
 "item.meteorologia.emergency_kit": "Emergency Kit",
 "item.meteorologia.propane_tank": "Propane Tank",
 "item.meteorologia.energy_bar": "Energy Bar",
 "item.meteorologia.hot_air_balloon": "Hot Air Balloon",
 "entity.meteorologia.hot_air_balloon": "Hot Air Balloon",
 "itemGroup.meteorologia": "Meteorology",
}
wj(A + "/lang/pt_br.json", pt)
wj(A + "/lang/en_us.json", en)

# -------------------------------------------------------------- receitas ----
def shaped(name, pattern, key, count=1, cat="misc"):
    wj(D + f"/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": cat,
        "pattern": pattern, "key": key, "result": {"id": "meteorologia:" + name, "count": count}})
def i(x): return {"item": "minecraft:" + x}
def m(x): return {"item": "meteorologia:" + x}

shaped("radar", ["IGI", "ICI", "IRI"], {"I": i("iron_ingot"), "G": i("glass"), "C": i("compass"), "R": i("redstone")}, cat="redstone")
shaped("anemometer", [" I ", "ISI", " S "], {"I": i("iron_ingot"), "S": i("stick")}, cat="redstone")
shaped("barometer", ["III", "IGI", "IRI"], {"I": i("iron_ingot"), "G": i("glass_pane"), "R": i("redstone")}, cat="redstone")
shaped("thermometer", ["CGC", "CRC", "CCC"], {"C": i("copper_ingot"), "G": i("glass_pane"), "R": i("redstone")}, cat="redstone")
shaped("weather_station", ["ABT", "III"], {"A": m("anemometer"), "B": m("barometer"), "T": m("thermometer"), "I": i("iron_ingot")}, cat="redstone")
shaped("rain_gauge", ["G G", "G G", "III"], {"G": i("glass"), "I": i("iron_ingot")}, cat="redstone")
shaped("storm_siren", ["IBI", "IRI", "III"], {"I": i("iron_ingot"), "B": i("bell"), "R": i("redstone")}, cat="redstone")
shaped("weather_radio", ["  I", "ICR", "III"], {"I": i("iron_ingot"), "C": i("compass"), "R": i("redstone")}, cat="equipment")
shaped("handheld_meter", [" I ", "IRI", " C "], {"I": i("iron_ingot"), "R": i("redstone"), "C": i("compass")}, cat="equipment")
shaped("propane_tank", ["IBI", "I I", "III"], {"I": i("iron_ingot"), "B": i("blaze_powder")}, count=2)
shaped("hot_air_balloon", ["WWW", "WWW", "SCS"], {"W": {"tag": "minecraft:wool"}, "S": i("string"), "C": i("barrel")}, cat="equipment")
wj(D + "/recipe/emergency_kit.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [i("golden_apple"), i("bread"), i("paper")], "result": {"id": "meteorologia:emergency_kit", "count": 1}})
wj(D + "/recipe/energy_bar.json", {"type": "minecraft:crafting_shapeless", "category": "food",
    "ingredients": [i("wheat"), i("sugar"), i("cocoa_beans")], "result": {"id": "meteorologia:energy_bar", "count": 2}})

# ------------------------------------------------------------------ tags ----
wj(RES + "/data/minecraft/tags/block/mineable/pickaxe.json",
   {"replace": False, "values": ["meteorologia:" + n for n in BLOCKS]})

print("assets ok")
