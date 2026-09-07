from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter
import math
import random

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "composeApp/src/commonMain/composeResources/drawable/forest_night.png"
OUTPUT = ROOT / "previews/lake-ambient-loop.webp"
WIDTH, HEIGHT = 1280, 720
FRAMES, FPS = 96, 12


def cover(image: Image.Image, width: int, height: int, zoom: float, pan_x: float) -> Image.Image:
    ratio = max(width / image.width, height / image.height) * zoom
    resized = image.resize((round(image.width * ratio), round(image.height * ratio)), Image.Resampling.LANCZOS)
    left = max(0, round((resized.width - width) / 2 + pan_x))
    top = max(0, round((resized.height - height) / 2))
    return resized.crop((left, top, left + width, top + height))


def fog_layer(phase: float) -> Image.Image:
    small = Image.new("L", (320, 180), 0)
    draw = ImageDraw.Draw(small)
    for index in range(5):
        x = ((phase * (45 + index * 7) + index * 83) % 430) - 70
        y = 103 + index * 6
        draw.ellipse((x - 85, y - 18, x + 85, y + 18), fill=22 + index * 3)
    return small.resize((WIDTH, HEIGHT), Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(24))


def make_frame(index: int) -> Image.Image:
    phase = index / FRAMES
    eased = (1 - math.cos(phase * math.tau)) / 2
    base = cover(source, WIDTH, HEIGHT, 1.018 + eased * .006, math.sin(phase * math.tau) * 4)
    base = ImageEnhance.Brightness(base).enhance(.985 + math.sin(phase * math.tau) * .008)

    atmosphere = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    atmosphere.putalpha(fog_layer(phase))
    white_fog = Image.new("RGBA", (WIDTH, HEIGHT), (205, 218, 225, 0))
    white_fog.putalpha(atmosphere.getchannel("A"))
    frame = Image.alpha_composite(base.convert("RGBA"), white_fog)

    details = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(details)
    for idx, (x, y, strength) in enumerate(stars):
        pulse = .5 + .5 * math.sin(phase * math.tau * 2 + idx)
        alpha = round((18 + pulse * 42) * strength)
        draw.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(235, 241, 243, alpha))
    for idx, (x, y, length) in enumerate(glints):
        shimmer = .5 + .5 * math.sin(phase * math.tau * 3 + idx * .8)
        shift = math.sin(phase * math.tau + idx) * 3
        draw.line((x + shift, y, x + length + shift, y), fill=(205, 224, 235, round(38 * shimmer)), width=1)
    return Image.alpha_composite(frame, details).convert("RGB")


source = Image.open(SOURCE).convert("RGB")
random.seed(42)
stars = [(random.randint(30, 960), random.randint(20, 250), random.uniform(.5, 1.0)) for _ in range(25)]
glints = [(random.randint(20, 830), random.randint(480, 650), random.randint(8, 34)) for _ in range(28)]
frames = [make_frame(index) for index in range(FRAMES)]
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
frames[0].save(
    OUTPUT,
    save_all=True,
    append_images=frames[1:],
    duration=round(1000 / FPS),
    loop=0,
    quality=82,
    method=4,
)
print(OUTPUT)
