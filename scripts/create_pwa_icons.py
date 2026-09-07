from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "composeApp/src/wasmJsMain/resources"

for size in (192, 512):
    image = Image.new("RGB", (size, size), "#19382c")
    draw = ImageDraw.Draw(image)
    for y in range(size):
        t = y / size
        color = tuple(round(a * (1 - t) + b * t) for a, b in zip((39, 83, 68), (13, 35, 29)))
        draw.line((0, y, size, y), fill=color)
    cx = size * .5
    ground = size * .78
    draw.ellipse((size * .18, size * .70, size * .82, size * .86), fill="#102b23")
    draw.rounded_rectangle((size * .465, size * .38, size * .535, ground), radius=size * .025, fill="#d5b17b")
    for x, y, radius in ((.36, .42, .19), (.56, .34, .22), (.68, .47, .17), (.47, .53, .20)):
        draw.ellipse(((x-radius)*size, (y-radius)*size, (x+radius)*size, (y+radius)*size), fill="#86a875")
    image.save(OUT / f"icon-{size}.png", optimize=True)
