import os
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

src_path = r"c:\Users\heysa\Documents\Dev\CodeWave\ChatGPT Image Sep 16, 2026, 03_39_32 AM.png"
res_dir = r"c:\Users\heysa\Documents\Dev\CodeWave\app\src\main\res"
release_dir = r"c:\Users\heysa\Documents\Dev\CodeWave\release"

# 1. Load source image
img = Image.open(src_path).convert("RGBA")
arr = np.array(img)

# Non-transparent bounds
y_indices, x_indices = np.where(arr[:, :, 3] > 25)
crop = img.crop((x_indices.min(), y_indices.min(), x_indices.max(), y_indices.max()))
w, h = crop.size
square_side = max(w, h)
sq_crop = Image.new("RGBA", (square_side, square_side), (0, 0, 0, 0))
sq_crop.paste(crop, ((square_side - w) // 2, (square_side - h) // 2))

# Inpaint transparent corners so ribbons and blue gradient extend into a full square
arr_sq = np.array(sq_crop)
bgr = cv2.cvtColor(arr_sq, cv2.COLOR_RGBA2BGR)
alpha = arr_sq[:, :, 3]
inpaint_mask = (alpha < 240).astype(np.uint8) * 255
inpainted = cv2.inpaint(bgr, inpaint_mask, inpaintRadius=20, flags=cv2.INPAINT_TELEA)
rgb = cv2.cvtColor(inpainted, cv2.COLOR_BGR2RGB)
full_bleed_sq = Image.fromarray(rgb).convert("RGBA")

# 2. Build full-bleed 432x432 Adaptive Art
# Full 432x432 gradient canvas matching ic_launcher_background
adaptive_size = 432
canvas_bg = Image.new("RGBA", (adaptive_size, adaptive_size), (1, 9, 31, 255))
draw_bg = ImageDraw.Draw(canvas_bg)
for y in range(adaptive_size):
    for x in range(adaptive_size):
        t = (x * 0.4 + y * 0.6) / adaptive_size
        r = int(14 * (1 - t) + 1 * t)
        g = int(52 * (1 - t) + 9 * t)
        b = int(112 * (1 - t) + 31 * t)
        canvas_bg.putpixel((x, y), (r, g, b, 255))

# Radial acoustic glow in center
glow = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
draw_g = ImageDraw.Draw(glow)
draw_g.ellipse((80, 80, 352, 352), fill=(0, 229, 255, 45))
glow = glow.filter(ImageFilter.GaussianBlur(55))
canvas_bg = Image.alpha_composite(canvas_bg, glow)

# Optimal emblem size: 310px gives max distance ~130px from center (safe circle is 144px radius)
emblem_size = 310
emblem_res = full_bleed_sq.resize((emblem_size, emblem_size), Image.Resampling.LANCZOS)

# Soft edge feathering (30px) so emblem seamlessly blends into the full-bleed canvas
em_mask_arr = np.ones((emblem_size, emblem_size), dtype=np.float32) * 255
feather_px = 30
for i in range(feather_px):
    val = (i / float(feather_px)) * 255
    em_mask_arr[i, :] = np.minimum(em_mask_arr[i, :], val)
    em_mask_arr[emblem_size - 1 - i, :] = np.minimum(em_mask_arr[emblem_size - 1 - i, :], val)
    em_mask_arr[:, i] = np.minimum(em_mask_arr[:, i], val)
    em_mask_arr[:, emblem_size - 1 - i] = np.minimum(em_mask_arr[:, emblem_size - 1 - i], val)

emblem_mask = Image.fromarray(em_mask_arr.astype(np.uint8))
emblem_res.putalpha(emblem_mask)

# Composite centered onto full-bleed canvas
offset = (adaptive_size - emblem_size) // 2
adaptive_full = canvas_bg.copy()
adaptive_full.paste(emblem_res, (offset, offset), emblem_res)

# Save adaptive foreground (ic_avatar_art.webp)
# In Android adaptive icon, ic_launcher_foreground uses ic_avatar_art
# Because the foreground has full-bleed blue gradient + feathered emblem,
# when masked by OEM launcher (squircle/circle), it fills 100% of the shape!
# Zero black borders, zero cutoffs!
adaptive_fg_path = os.path.join(res_dir, "drawable-nodpi", "ic_avatar_art.webp")
adaptive_full.save(adaptive_fg_path, "WEBP", quality=100)
print(f"Saved adaptive icon to: {adaptive_fg_path}")

# 3. Generate Legacy Mipmaps
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

for folder, size in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)

    # A) Standard Squircle / Rounded Rect Icon
    pad = max(1, int(round(size * 0.04)))
    inner_size = size - (pad * 2)
    inner_art = adaptive_full.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
    
    # Rounded rect mask with squircle-like radius (22% of size)
    mask_sq = Image.new("L", (inner_size * 4, inner_size * 4), 0)
    draw_sq = ImageDraw.Draw(mask_sq)
    radius_4x = int(inner_size * 4 * 0.22)
    draw_sq.rounded_rectangle((0, 0, inner_size * 4, inner_size * 4), radius=radius_4x, fill=255)
    mask_sq = mask_sq.resize((inner_size, inner_size), Image.Resampling.LANCZOS)

    std_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    std_icon.paste(inner_art, (pad, pad), mask=mask_sq)
    std_path = os.path.join(folder_path, "ic_launcher.webp")
    std_icon.save(std_path, "WEBP", quality=100)

    # B) Round Icon
    mask_round = Image.new("L", (inner_size * 4, inner_size * 4), 0)
    draw_round = ImageDraw.Draw(mask_round)
    draw_round.ellipse((0, 0, inner_size * 4, inner_size * 4), fill=255)
    mask_round = mask_round.resize((inner_size, inner_size), Image.Resampling.LANCZOS)

    round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_icon.paste(inner_art, (pad, pad), mask=mask_round)
    round_path = os.path.join(folder_path, "ic_launcher_round.webp")
    round_icon.save(round_path, "WEBP", quality=100)

    print(f"Generated {folder}: {size}x{size}")

# 4. Release Store Asset (512x512)
os.makedirs(release_dir, exist_ok=True)
hires_icon = adaptive_full.resize((512, 512), Image.Resampling.LANCZOS)
hires_path = os.path.join(release_dir, "icon.png")
hires_icon.save(hires_path, "PNG")
print(f"Saved 512x512 release icon to: {hires_path}")

print("All icon assets generated successfully!")
