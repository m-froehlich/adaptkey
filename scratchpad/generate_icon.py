"""Generator for the AdaptKey icon (real app launcher AND fastlane/metadata/android/*/images/icon.png).

Draws at 4x supersampling and downsamples with LANCZOS for clean anti-aliasing. This script is the
raster proof for a matching VectorDrawable (ic_launcher_foreground.xml) using the same 108-unit
coordinates, so both renditions match exactly. Not part of the app build itself - a one-off asset script
kept here for reproducibility, same convention as build_dict.py / build_emoji_keywords.py.

Design (v6, per user feedback): a single "A" keycap with the T-06 touch-zone visualisation drawn on top
of it, exactly matching AdaptKeyboardView.kt's own real z-order (onDraw() calls drawKeys() - key
background + label - before drawTouchModel(), so the overlay always sits over the label on a real key
too, not the other way around). The dot sits at the keycap's own true geometric centre (54,54) - "central
on the key", not centred in the halo. The halo is a wide, short, semi-transparent OVAL - not a circle -
that bleeds out past the keycap's own left edge, the mistouch scenario T-03/T-05 exist for.

The keycap fills the full official adaptive-icon safe zone (a 66x66dp square centred in the 108x108dp
canvas, per Android's own adaptive-icon spec - the area guaranteed never clipped by any launcher mask
shape), not the much smaller 46x46 square used in earlier drafts - there was significant unused margin
before. Only the halo's own deliberate bleed is allowed to cross past that boundary, since it is a
secondary/decorative element, not core content - some minor clipping of just that sliver on an aggressive
launcher mask is an accepted, common trade-off, unlike the keycap+letter which must stay fully safe.

Colours are not invented for the icon; they are the same hue as the exact Paint values
AdaptKeyboardView.kt already uses on-device for this overlay (touchModelFillPaint/touchModelStrokePaint/
touchModelDotPaint, D-24), boosted in alpha for contrast on this icon's own dark blue background - the
real overlay is only ever drawn over a light keyboard background (key_background/keyboard_background are
white/light grey), never this dark brand blue, and teal/this-blue are close enough in luminance that the
real in-app alpha values (0x33/0x88) would nearly disappear once bled onto it. The background blue itself
is intentionally left unchanged (it's colors.xml's real ic_launcher_background, also used for
link_text/suggestion_verbatim_text in the real UI) - contrast is fixed via the halo's own alpha, not by
touching the brand colour:
  - halo fill:   (0,121,107) @ alpha 150/255 (in-app: 0x3300796B, alpha 51/255)
  - halo stroke: (0,121,107) @ alpha 230/255 (in-app: 0x8800796B, alpha 136/255)
  - centre dot:  (211,47,47) @ alpha 224/255 (in-app: 0xE0D32F2F, unchanged - red already read fine)

Every translucent shape is drawn on its own transparent layer and composited with
Image.alpha_composite() rather than drawn straight onto the canvas - PIL's ImageDraw does not itself
alpha-blend against existing pixels, it overwrites them, so a direct draw of a translucent fill would
bake genuine (and wrong) partial transparency into the saved PNG instead of the intended opaque blended
colour.
"""

from PIL import Image, ImageDraw, ImageFilter

BRAND_BLUE = (21, 101, 192, 255)   # #1565C0, colors.xml ic_launcher_background / link_text
WHITE = (255, 255, 255, 255)
# Same hue as the real on-device touchModelFillPaint/touchModelStrokePaint (0,121,107, D-24), but with
# boosted alpha versus the in-app 0x33/0x88: the real overlay is only ever drawn over a light keyboard
# background (key_background/keyboard_background are white/light grey), never this dark brand blue, so
# the in-app alpha values are too low to read once the halo bleeds off the white keycap onto the icon's
# own blue background - teal and this blue are close in luminance, so plain alpha-51 nearly disappears.
HALO_FILL = (0, 121, 107, 150)
HALO_STROKE = (0, 121, 107, 230)
STRIKE_DOT = (211, 47, 47, 224)    # touchModelDotPaint, 0xE0D32F2F - unchanged, red already reads fine

UNIT = 108           # matches the Android adaptive-icon 108dp coordinate space
SUPERSAMPLE = 2048    # render canvas size before downsampling
SCALE = SUPERSAMPLE / UNIT


def pt(x: float, y: float) -> tuple[float, float]:
    return (x * SCALE, y * SCALE)


def box(x0: float, y0: float, x1: float, y1: float) -> list[float]:
    return [x0 * SCALE, y0 * SCALE, x1 * SCALE, y1 * SCALE]


def composited(canvas: Image.Image, draw_fn) -> Image.Image:
    """Draws translucent shapes on a fresh transparent layer, then alpha-blends it onto canvas -
    ImageDraw itself would otherwise just overwrite pixels with the raw (unblended) fill colour."""
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    return Image.alpha_composite(canvas, layer)


def build(size: int, out_path: str) -> None:
    canvas = Image.new("RGBA", (SUPERSAMPLE, SUPERSAMPLE), BRAND_BLUE)

    # Keycap fills the full 66x66dp adaptive-icon safe zone (21,21)-(87,87), centred in the 108x108
    # canvas - corner radius scaled up from the old 46-wide card in the same proportion (9/46 of width).
    CARD_LO, CARD_HI = 21.0, 87.0
    CARD_RADIUS = 9.0 * (CARD_HI - CARD_LO) / 46.0

    # Keycap drop shadow (soft, offset down) for a little physical depth - blur the shadow shape on its
    # own transparent layer first, then composite that blurred layer onto the canvas once. Vertical
    # offset scaled up in the same proportion as the card itself (was +2.5 on a 46-wide card).
    shadow_dy = 2.5 * (CARD_HI - CARD_LO) / 46.0
    shadow_layer = Image.new("RGBA", (SUPERSAMPLE, SUPERSAMPLE), (0, 0, 0, 0))
    ImageDraw.Draw(shadow_layer).rounded_rectangle(
        box(CARD_LO, CARD_LO + shadow_dy, CARD_HI, CARD_HI + shadow_dy), radius=CARD_RADIUS * SCALE,
        fill=(0, 0, 0, 90),
    )
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(radius=SUPERSAMPLE * 0.012))
    canvas = Image.alpha_composite(canvas, shadow_layer)

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(box(CARD_LO, CARD_LO, CARD_HI, CARD_HI), radius=CARD_RADIUS * SCALE, fill=WHITE)

    # Slimmer "A" glyph, scaled up from the same design used against the smaller card (uniform scale
    # around the shared centre (54,54), so the letter keeps the exact same proportions relative to the
    # card as before) - single simple polygon; the open wedge above the crossbar is left unfilled
    # (keycap-coloured) purely by the point ordering.
    letter_scale = (CARD_HI - CARD_LO) / 46.0

    def scaled(x: float, y: float) -> tuple[float, float]:
        return pt(54 + (x - 54) * letter_scale, 54 + (y - 54) * letter_scale)

    a_points = [
        scaled(57, 38),   # apex
        scaled(74, 70),   # bottom-right outer
        scaled(69, 70),   # bottom-right inner
        scaled(59, 58),   # crossbar right
        scaled(55, 58),   # crossbar left
        scaled(45, 70),   # bottom-left inner
        scaled(40, 70),   # bottom-left outer
    ]
    draw.polygon(a_points, fill=BRAND_BLUE)

    # Touch-zone overlay, drawn on top of the key/label like the real onDraw() order. Halo is a wide,
    # short oval bleeding left past the card's own left edge (CARD_LO) by a fixed ~9-unit amount (not
    # scaled up with the card - the bleed is a deliberate, modest accent, not core content, and scaling
    # it up with the card would push it into real launcher-mask clipping risk). Unlike drawTouchModel()
    # (which always puts the dot at the same cx,cy as the oval), the dot here is deliberately decoupled
    # and sits at the keycap's own true geometric centre (54,54) - "central on the key" - while the
    # oval's own centre sits further left so the bleed is meaningful.
    zone_cx, zone_cy, zone_rx, zone_ry = 45.0, 54.0, 33.0, 19.0
    halo_box = box(zone_cx - zone_rx, zone_cy - zone_ry, zone_cx + zone_rx, zone_cy + zone_ry)
    canvas = composited(canvas, lambda d: (
        d.ellipse(halo_box, fill=HALO_FILL),
        d.ellipse(halo_box, outline=HALO_STROKE, width=max(1, round(2.4 * SCALE))),
    ))
    dot_cx, dot_cy, dot_r = 54.0, 54.0, 6.0
    dot_box = box(dot_cx - dot_r, dot_cy - dot_r, dot_cx + dot_r, dot_cy + dot_r)
    canvas = composited(canvas, lambda d: d.ellipse(dot_box, fill=STRIKE_DOT))

    final = canvas.resize((size, size), Image.LANCZOS)
    final.save(out_path, "PNG")
    print(f"wrote {out_path} ({size}x{size})")


if __name__ == "__main__":
    build(512, "scratchpad/icon_draft_512.png")
