from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork/source-png"
OUTPUT = ROOT / "composeApp/src/commonMain/composeResources/drawable"

for path in sorted(SOURCE.glob("forest_*.png")):
    image = Image.open(path).convert("RGB")
    destination = OUTPUT / f"{path.stem}.webp"
    image.save(destination, "WEBP", quality=86, method=6)
    print(f"{path.name}: {path.stat().st_size // 1024} KB -> {destination.stat().st_size // 1024} KB")
