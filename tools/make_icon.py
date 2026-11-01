"""Builds the mod icon from the images in reference/icon/ (local only, not in the repo): the Display Link on the Create-style round background,
with a brass "+" badge. Drawn at 4x and downscaled for smooth edges. Run: python -I tools/make_icon.py [size] [output]"""
import sys

from PIL import Image, ImageDraw, ImageFilter

SIZE = int(sys.argv[1]) if len(sys.argv) > 1 else 256
SS = 4  # supersampling factor
W = SIZE * SS

background = Image.open('reference/icon/New_Create_Logo_Background.webp').convert('RGBA')
background = background.crop(background.getbbox()).resize((W, W), Image.LANCZOS)

link = Image.open('reference/icon/Display_Link.webp').convert('RGBA')
link = link.crop(link.getbbox())
link_w = int(W * 0.66)
link = link.resize((link_w, int(link_w * link.height / link.width)), Image.LANCZOS)
link_x = (W - link.width) // 2 - int(W * 0.02)
link_y = (W - link.height) // 2 + int(W * 0.02)

icon = background.copy()

# Soft contact shadow under the block
shadow = Image.new('RGBA', (W, W), (0, 0, 0, 0))
ImageDraw.Draw(shadow).ellipse([link_x + link.width * 0.08, link_y + link.height * 0.80,
                                link_x + link.width * 0.92, link_y + link.height * 1.02], fill=(10, 30, 60, 120))
icon = Image.alpha_composite(icon, shadow.filter(ImageFilter.GaussianBlur(W * 0.02)))
icon.alpha_composite(link, (link_x, link_y))

# Brass "+" badge, bottom right
badge = Image.new('RGBA', (W, W), (0, 0, 0, 0))
d = ImageDraw.Draw(badge)
cx, cy, r = W * 0.78, W * 0.78, W * 0.15
d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(74, 44, 18, 255))  # dark outline
r2 = r * 0.86
d.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], fill=(232, 176, 70, 255))  # brass
r3 = r * 0.70
d.ellipse([cx - r3, cy - r3 - r * 0.08, cx + r3, cy + r3 - r * 0.08], fill=(246, 206, 112, 255))  # highlight
arm, thick = r * 0.55, r * 0.20
for (x0, y0, x1, y1), color in [((-arm, -thick, arm, thick), (74, 44, 18, 255))]:
    pad = r * 0.07
    d.rounded_rectangle([cx + x0 - pad, cy + y0 - pad, cx + x1 + pad, cy + y1 + pad], radius=thick, fill=color)
    d.rounded_rectangle([cx - thick - pad, cy - arm - pad, cx + thick + pad, cy + arm + pad], radius=thick, fill=color)
d.rounded_rectangle([cx - arm, cy - thick, cx + arm, cy + thick], radius=thick * 0.6, fill=(255, 250, 235, 255))
d.rounded_rectangle([cx - thick, cy - arm, cx + thick, cy + arm], radius=thick * 0.6, fill=(255, 250, 235, 255))
badge_shadow = Image.new('RGBA', (W, W), (0, 0, 0, 0))
ImageDraw.Draw(badge_shadow).ellipse([cx - r, cy - r + W * 0.012, cx + r, cy + r + W * 0.012], fill=(0, 0, 0, 90))
icon = Image.alpha_composite(icon, badge_shadow.filter(ImageFilter.GaussianBlur(W * 0.01)))
icon = Image.alpha_composite(icon, badge)

out = sys.argv[2] if len(sys.argv) > 2 else 'src/main/resources/icon.png'
icon.resize((SIZE, SIZE), Image.LANCZOS).save(out, optimize=True)
print('icon written', SIZE)
