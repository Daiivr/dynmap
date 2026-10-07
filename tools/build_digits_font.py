# Builds DynmapPixelDigits.woff2 for the web UI theme (css/minecraft_theme.css): digits 0-9 as 5x7 pixel
# glyphs (the same designs as js/timeofdayclock.js), sized to sit in line with Pixelify Sans (UPM 1000,
# digits 586 wide, -12..631 tall).
#
#     pip install fonttools brotli
#     python tools/build_digits_font.py
import os

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen

GLYPHS = {
    '0': ['.###.', '#...#', '#..##', '#.#.#', '##..#', '#...#', '.###.'],
    '1': ['..#..', '.##..', '..#..', '..#..', '..#..', '..#..', '#####'],
    '2': ['.###.', '#...#', '....#', '..##.', '.#...', '#....', '#####'],
    '3': ['.###.', '#...#', '....#', '..##.', '....#', '#...#', '.###.'],
    '4': ['...##', '..#.#', '.#..#', '#...#', '#####', '....#', '....#'],
    '5': ['#####', '#....', '####.', '....#', '....#', '#...#', '.###.'],
    '6': ['..##.', '.#...', '#....', '####.', '#...#', '#...#', '.###.'],
    '7': ['#####', '#...#', '....#', '...#.', '..#..', '..#..', '..#..'],
    '8': ['.###.', '#...#', '#...#', '.###.', '#...#', '#...#', '.###.'],
    '9': ['.###.', '#...#', '#...#', '.####', '....#', '...#.', '.##..'],
}
NAMES = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine']
PX_W, PX_H = 93, 92          # 5 x 93 = 465 wide, 7 x 92 = 644 tall
LEFT, BOTTOM, ADVANCE = 60, -12, 586

def glyph(rows):
    pen = TTGlyphPen(None)
    for r, row in enumerate(rows):
        y1 = BOTTOM + (7 - r) * PX_H
        y0 = y1 - PX_H
        x = 0
        while x < len(row):          # one rectangle per horizontal run of pixels
            if row[x] != '#':
                x += 1
                continue
            start = x
            while x < len(row) and row[x] == '#':
                x += 1
            x0, x1 = LEFT + start * PX_W, LEFT + x * PX_W
            pen.moveTo((x0, y0)); pen.lineTo((x0, y1)); pen.lineTo((x1, y1)); pen.lineTo((x1, y0)); pen.closePath()
    return pen.glyph()

fb = FontBuilder(1000, isTTF=True)
order = ['.notdef'] + NAMES
fb.setupGlyphOrder(order)
fb.setupCharacterMap({ord(str(i)): NAMES[i] for i in range(10)})
glyphs = {'.notdef': TTGlyphPen(None).glyph()}
glyphs.update({NAMES[i]: glyph(GLYPHS[str(i)]) for i in range(10)})
fb.setupGlyf(glyphs)
fb.setupHorizontalMetrics({name: (ADVANCE if name != '.notdef' else 500, LEFT if name != '.notdef' else 0) for name in order})
fb.setupHorizontalHeader(ascent=920, descent=-280)
fb.setupNameTable({
    'familyName': 'Dynmap Pixel Digits',
    'styleName': 'Regular',
    'copyright': 'Pixel digits drawn for this Dynmap fork, Apache License 2.0',
})
fb.setupOS2(sTypoAscender=920, sTypoDescender=-280, sTypoLineGap=0, usWinAscent=922, usWinDescent=289,
            sCapHeight=632, sxHeight=450, achVendID='DYNM', fsType=0)
fb.setupPost()
fb.font.flavor = 'woff2'
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'DynmapCore', 'src', 'main', 'resources',
                   'extracted', 'web', 'css', 'fonts', 'DynmapPixelDigits.woff2')
fb.save(out)

# Round-trip check
from fontTools.ttLib import TTFont
from fontTools.pens.boundsPen import BoundsPen
f = TTFont(out)
gs = f.getGlyphSet(); cmap = f.getBestCmap()
for ch in '0158':
    bp = BoundsPen(gs); gs[cmap[ord(ch)]].draw(bp)
    print(ch, cmap[ord(ch)], 'adv', f['hmtx'][cmap[ord(ch)]][0], 'bounds', bp.bounds)
print('size', os.path.getsize(out), 'bytes')
