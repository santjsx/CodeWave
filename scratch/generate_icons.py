import os
from PIL import Image, ImageDraw, ImageFilter
import numpy as np

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src_path = os.path.join(project_root, "app-icon.png")
res_dir = r"c:\Users\heysa\Documents\Dev\CodeWave\app\src\main\res"

img = Image.open(src_path).convert("RGBA")
arr = np.array(img)
alpha = arr[:, :, 3]

# Find content bounds with alpha > 25
y_indices, x_indices = np.where(alpha > 25)
x_min, x_max = x_indices.min(), x_indices.max()
y_min, y_max = y_indices.min(), y_indices.max()

# Make it a clean square bounding box centered around the content center
center_x = (x_min + x_max) / 2.0
center_y = (y_min + y_max) / 2.0
side = max(x_max - x_min + 1, y_max - y_min + 1)
half_side = side / 2.0

crop_box = (
    int(round(center_x - half_side)),
    int(round(center_y - half_side)),
    int(round(center_x + half_side)),
    int(round(center_y + half_side))
)

# Crop squircle emblem cleanly
cropped_icon = img.crop(crop_box)
print(f"Cropped icon size: {cropped_icon.size}")

# 1. Generate Adaptive Icon Foreground (432x432 px)
# In Android Adaptive Icons, total canvas is 108dp x 108dp, safe circle is 72dp diameter (66.67%).
# 432 * 0.6667 = 288px safe area.
# Placing the emblem within ~290px with smooth anti-aliased resampling.
adaptive_size = 432
target_emblem_size = 290
emblem_resized = cropped_icon.resize((target_emblem_size, target_emblem_size), Image.Resampling.LANCZOS)

adaptive_fg = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
offset = (adaptive_size - target_emblem_size) // 2
adaptive_fg.paste(emblem_resized, (offset, offset), emblem_resized)

adaptive_fg_path = os.path.join(res_dir, "drawable-nodpi", "ic_avatar_art.webp")
adaptive_fg.save(adaptive_fg_path, "WEBP", quality=100)
print(f"Saved adaptive foreground to: {adaptive_fg_path}")

# 2. Generate Legacy Mipmap Icons
# Densities:
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
    
    # Standard squircle icon (scaled to fit size with 2px padding for anti-aliasing)
    pad = max(1, int(round(size * 0.04)))
    icon_inner_size = size - (pad * 2)
    inner_icon = cropped_icon.resize((icon_inner_size, icon_inner_size), Image.Resampling.LANCZOS)
    
    std_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    std_icon.paste(inner_icon, (pad, pad), inner_icon)
    std_path = os.path.join(folder_path, "ic_launcher.webp")
    std_icon.save(std_path, "WEBP", quality=100)
    
    # Round icon (masked circle with dark background fill if needed or cropped circle)
    # The squircle fits beautifully inside a circle
    round_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_canvas.paste(inner_icon, (pad, pad), inner_icon)
    
    # Create high-res circular mask
    mask = Image.new("L", (size * 4, size * 4), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((pad * 4, pad * 4, (size - pad) * 4, (size - pad) * 4), fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    
    round_final = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_final.paste(round_canvas, (0, 0), mask=mask)
    round_path = os.path.join(folder_path, "ic_launcher_round.webp")
    round_final.save(round_path, "WEBP", quality=100)
    
    print(f"Generated {folder}: {size}x{size}")

# 3. High-Res Store/Release Icon (512x512)
release_dir = r"c:\Users\heysa\Documents\Dev\CodeWave\release"
os.makedirs(release_dir, exist_ok=True)
hires_icon = cropped_icon.resize((512, 512), Image.Resampling.LANCZOS)
hires_path = os.path.join(release_dir, "icon.png")
hires_icon.save(hires_path, "PNG")
print(f"Saved 512x512 release icon to: {hires_path}")

print("All icon assets generated successfully!")
