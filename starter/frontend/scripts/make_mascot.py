"""Turn the hand-drawn frog into transparent, theme-tinted mascot PNGs.

usage: python3 make_mascot.py <source.png> <out_dir> [preview_dir]
"""
import sys
import numpy as np
from PIL import Image
from scipy import ndimage

src, out_dir = sys.argv[1], sys.argv[2]
preview_dir = sys.argv[3] if len(sys.argv) > 3 else None

INK = np.array([42, 29, 20], float)      # --frog-ink (light theme)
PAPER = np.array([250, 242, 222], float)  # slightly lighter than --card so fills still read

rgb = np.asarray(Image.open(src).convert('RGB')).astype(float)
h, w, _ = rgb.shape

SEAL = 8          # px — closes the small gaps in hand-drawn outlines (bag, cap, bubble)
MIN_FILL = 2500   # px — enclosed areas smaller than this (e.g. inside the Zs) stay transparent


def border_labels(labels):
    b = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    return b[b != 0]


# 1. Background = near-white pixels reachable from the image border once outline gaps are sealed.
whiteish = rgb.min(axis=2) > 200
sealed_ink = ndimage.binary_dilation(~whiteish, iterations=SEAL)
labels, _ = ndimage.label(~sealed_ink)
exterior = ndimage.binary_dilation(np.isin(labels, border_labels(labels)), iterations=SEAL + 1) & whiteish

interior_labels, n = ndimage.label(whiteish & ~exterior)
sizes = ndimage.sum(np.ones_like(interior_labels), interior_labels, range(1, n + 1))
near_exterior = ndimage.binary_dilation(exterior, iterations=2)
for i in np.flatnonzero(sizes < MIN_FILL) + 1:
    pocket = interior_labels == i
    if (pocket & near_exterior).any():  # open pocket (inside a Z), not a hole inside the bag
        exterior |= pocket

# 2. Soft alpha in the background + a 2px band around it (the anti-aliased edges).
band = ndimage.binary_dilation(exterior, iterations=2)
inkness = (255 - rgb).max(axis=2)
alpha = np.ones((h, w))
alpha[band] = np.clip(inkness[band] / 200.0, 0, 1)
alpha[band & (alpha < 0.08)] = 0  # drop compression noise

# Un-mix white out of semi-transparent edge pixels so the edge color is the true ink color.
a = np.maximum(alpha, 1e-6)[..., None]
fg = np.where(band[..., None], (rgb - (1 - a) * 255) / a, rgb)
fg = np.clip(fg, 0, 255)

# 3. "Printed on aged paper" tint: black -> sepia ink, white -> cream, colors warm slightly.
fg = INK + (fg / 255.0) * (PAPER - INK)

rgba = np.dstack([fg, alpha * 255]).round().astype(np.uint8)
img = Image.fromarray(rgba)


def trim(im, pad):
    x0, y0, x1, y1 = im.getbbox()
    return im.crop((max(x0 - pad, 0), max(y0 - pad, 0), min(x1 + pad, im.width), min(y1 + pad, im.height)))


def fit_height(im, height):
    return im.resize((round(im.width * height / im.height), height), Image.LANCZOS)


scene = trim(img, 6)
fit_height(scene, 360).save(f'{out_dir}/mascot-scene.png', optimize=True)

# Head + nightcap crop for small sizes (navbar, favicon), made square.
head = img.crop((140, 205, 450, 430))
head.paste((0, 0, 0, 0), (200, 0, 310, 25))  # erase the tail of a Z that pokes into the crop
head = trim(head, 4)
side = max(head.size)
square = Image.new('RGBA', (side, side))
square.paste(head, ((side - head.width) // 2, (side - head.height) // 2))
square.resize((192, 192), Image.LANCZOS).save(f'{out_dir}/mascot-head.png', optimize=True)
square.resize((64, 64), Image.LANCZOS).save(f'{out_dir}/favicon.png', optimize=True)
square.save(f'{out_dir}/favicon.ico', sizes=[(16, 16), (32, 32), (48, 48)])

if preview_dir:
    # Checks: magenta reveals leaks through gaps; light/dark show the real theme backgrounds.
    for name, bg in [('magenta', (255, 0, 255)), ('light', (239, 226, 192)), ('dark', (28, 20, 14))]:
        for label, im in [('scene', scene), ('head', square)]:
            canvas = Image.new('RGBA', im.size, bg + (255,))
            canvas.alpha_composite(im)
            canvas.convert('RGB').save(f'{preview_dir}/{label}-{name}.png')
