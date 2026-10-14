"""Draws the Transparent Paintings icon: a framed pixel-art landscape that fades
diagonally into a transparency checkerboard.

Usage: python make_icon.py <output_dir>
Writes icon.png (512x512) and icon-128.png (128x128).
"""
import math
import sys
from pathlib import Path

from PIL import Image

SIZE = 32
CANVAS_MIN, CANVAS_MAX = 4, 27  # inner canvas, inclusive

OUTLINE = (52, 31, 15)
WOOD = (138, 90, 43)
WOOD_LIGHT = (176, 122, 60)
WOOD_DARK = (105, 66, 30)
SKY_TOP = (74, 150, 222)
SKY_HORIZON = (168, 216, 240)
SUN = (255, 216, 74)
SUN_CORE = (255, 242, 168)
HILL_FAR = (92, 168, 76)
HILL_NEAR = (60, 128, 54)
CHECK_LIGHT = (255, 255, 255)
CHECK_DARK = (150, 150, 164)


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def landscape(cx, cy):
    if cy > 17 + 1.6 * math.sin(cx * 0.45 + 2.2):
        return HILL_NEAR
    if cy > 13 + 2.0 * math.sin(cx * 0.32):
        return HILL_FAR
    d = math.hypot(cx - 7.5, cy - 6.5)
    if d < 1.8:
        return SUN_CORE
    if d < 3.4:
        return SUN
    return lerp(SKY_TOP, SKY_HORIZON, cy / 14)


def opacity(cx, cy):
    # Diagonal bands, stepped for a pixel-art look: opaque top-left, see-through bottom-right.
    t = (cx + cy) / 46
    if t < 0.50:
        return 1.0
    if t < 0.62:
        return 0.65
    if t < 0.74:
        return 0.35
    if t < 0.86:
        return 0.12
    return 0.0


def checker(cx, cy):
    return CHECK_LIGHT if (cx // 4 + cy // 4) % 2 == 0 else CHECK_DARK


def frame(x, y):
    ring = min(x - 1, y - 1, 30 - x, 30 - y)
    if ring == 0:
        return OUTLINE
    if ring == 1:
        return WOOD_LIGHT if x + y < 31 else WOOD
    if ring == 2:
        return WOOD if x + y < 31 else WOOD_DARK
    return None


def main():
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    for y in range(1, 31):
        for x in range(1, 31):
            color = frame(x, y)
            if color is None:
                cx, cy = x - CANVAS_MIN, y - CANVAS_MIN
                color = lerp(checker(cx, cy), landscape(cx, cy), opacity(cx, cy))
            img.putpixel((x, y), color + (255,))

    img.resize((512, 512), Image.NEAREST).save(out / "icon.png")
    img.resize((128, 128), Image.NEAREST).save(out / "icon-128.png")


if __name__ == "__main__":
    main()
