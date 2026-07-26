#!/usr/bin/env bash
#
# Regenerates the PWA icons from the canonical logo vector.
#
# Every icon is the logo centered on an opaque white canvas, matching the native
# adaptive icon (white background + orange receipt glyph). Two scales are used:
#
#   MASKED   - for surfaces the OS masks (Android adaptive icon, PWA "maskable").
#              These are the exact scale factors from the group transform in
#              androidApp/src/main/res/drawable/ic_launcher_foreground.xml, so the
#              installed PWA icon is pixel-identical to the native launcher icon.
#              The glyph has to stay inside the launcher's circle mask, which is
#              only 72/108 = 66.7% of the canvas; anything larger gets cropped
#              into a solid orange disc.
#
#   UNMASKED - for surfaces that render the full square (desktop install,
#              apple-touch-icon, splash). Matches the Play Store icon
#              (androidApp/src/main/ic_launcher-playstore.png, 304x330 on 512).
#
# favicon-32.png is intentionally not regenerated: at 32px it needs a transparent
# background and a near-full-bleed glyph to stay legible in a browser tab.
#
# Usage: tools/generate-web-icons.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SVG="$REPO_ROOT/docs/images/logo.svg"
OUT_DIR="$REPO_ROOT/composeApp/src/wasmJsMain/resources/icons"

MASKED_W=0.38234186
MASKED_H=0.41555557
UNMASKED_W=0.59375
UNMASKED_H=0.64453

command -v magick >/dev/null 2>&1 || {
    echo "error: ImageMagick 7 ('magick') not found." >&2
    exit 1
}
magick -list format | grep -qE '^\s*SVG\b.*RSVG' || {
    echo "error: ImageMagick has no librsvg delegate; SVG would render incorrectly." >&2
    echo "       Install librsvg2 (Fedora: sudo dnf install librsvg2-tools)." >&2
    exit 1
}
[ -f "$SVG" ] || { echo "error: missing source vector $SVG" >&2; exit 1; }

# render <canvas-px> <scale-w> <scale-h> <output-name>
render() {
    local canvas=$1 scale_w=$2 scale_h=$3 name=$4
    local glyph_w glyph_h
    # bc defaults to scale=0 for division, so "+ 0.5) / 1" rounds half up.
    glyph_w=$(echo "($canvas * $scale_w + 0.5) / 1" | bc)
    glyph_h=$(echo "($canvas * $scale_h + 0.5) / 1" | bc)

    magick -background none "$SVG" -resize "${glyph_w}x${glyph_h}!" MIFF:- |
        magick -size "${canvas}x${canvas}" xc:white - -gravity center -composite \
            -depth 8 -strip "PNG32:$OUT_DIR/$name"

    printf '  %-24s %sx%s canvas, %sx%s glyph\n' "$name" "$canvas" "$canvas" "$glyph_w" "$glyph_h"
}

echo "Generating PWA icons from docs/images/logo.svg:"
render 512 "$MASKED_W"   "$MASKED_H"   icon-maskable-512.png
render 512 "$UNMASKED_W" "$UNMASKED_H" icon-512.png
render 192 "$UNMASKED_W" "$UNMASKED_H" icon-192.png
render 180 "$UNMASKED_W" "$UNMASKED_H" apple-touch-icon.png
echo "Done."
