// Builds the map icons of the Dynmap Waystones add-on (dynmap-waystones/src/main/resources/assets/dynmapwaystones/icons):
// 16x16 pixel art drawn at 2x, so Dynmap shows them at 32x32 with Minecraft-style texels.
//
//     node tools/build_waystone_icons.js
'use strict';
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const OUT = path.join(__dirname, '..', 'dynmap-waystones', 'src', 'main', 'resources', 'assets', 'dynmapwaystones', 'icons');
const SCALE = 2;

// ---- PNG (RGBA) ----
const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    crcTable[n] = c >>> 0;
}
function crc(buf) {
    let c = ~0;
    for (const b of buf) c = crcTable[(c ^ b) & 255] ^ (c >>> 8);
    return (~c) >>> 0;
}
function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length);
    const body = Buffer.concat([Buffer.from(type), data]);
    const sum = Buffer.alloc(4);
    sum.writeUInt32BE(crc(body));
    return Buffer.concat([len, body, sum]);
}
function png(rows, palette) {
    const h = rows.length * SCALE, w = rows[0].length * SCALE;
    const raw = [];
    for (let y = 0; y < h; y++) {
        raw.push(0);
        const row = rows[Math.floor(y / SCALE)];
        for (let x = 0; x < w; x++) {
            const colour = palette[row[Math.floor(x / SCALE)]];
            if (!colour) throw new Error('No colour for "' + row[Math.floor(x / SCALE)] + '"');
            raw.push(...colour);
        }
    }
    const header = Buffer.alloc(13);
    header.writeUInt32BE(w, 0);
    header.writeUInt32BE(h, 4);
    header[8] = 8;  // bit depth
    header[9] = 6;  // RGBA
    return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header),
        chunk('IDAT', zlib.deflateSync(Buffer.from(raw), { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

// ---- Colours ----
const hex = (s, a = 255) => [parseInt(s.slice(1, 3), 16), parseInt(s.slice(3, 5), 16), parseInt(s.slice(5, 7), 16), a];
const mix = (c, d, t) => [0, 1, 2].map(i => Math.round(c[i] + (d[i] - c[i]) * t)).concat([255]);
const WHITE = [255, 255, 255], BLACK = [0, 0, 0];

const STONE = {
    '.': [0, 0, 0, 0],
    O: hex('#17121d'),      // outline
    L: hex('#d4d4dc'),      // lit face
    S: hex('#a5a5b0'),      // stone
    D: hex('#76767f'),      // shaded face
    d: hex('#56565f'),      // seams and undersides
};
// Rune / gem colours from one base colour: bright core, mid, dark, and a see-through glow
function glow(base) {
    const c = hex(base);
    return {
        R: mix(c, WHITE, 0.55),
        r: mix(c, WHITE, 0.15),
        q: mix(c, BLACK, 0.35),
        g: c.slice(0, 3).concat([110]),
    };
}

// Minecraft's dye colours, for sharestones (waystones:<colour>_sharestone)
const DYES = {
    white: '#f9fffe', orange: '#f9801d', magenta: '#c74ebd', light_blue: '#3ab3da',
    yellow: '#fed83d', lime: '#80c71f', pink: '#f38baa', gray: '#8a9396',
    light_gray: '#c4c4bd', cyan: '#169c9c', purple: '#8932b8', blue: '#3c44aa',
    brown: '#835432', green: '#5e7c16', red: '#b02e26', black: '#3a3a42',
};
const WAYSTONE_PURPLE = '#b14cff';

// ---- Art (16x16) ----
// Capped obelisk with a strip of glowing runes and a halo
const WAYSTONE = [
    '.......OO.......',
    '......OLSO......',
    '.....OLLSDO.....',
    '....OddddddO....',
    '....gOLSSDOg....',
    '....gOLRrDOg....',
    '...ggOLqRDOgg...',
    '...ggOLRqDOgg...',
    '....gOLrRDOg....',
    '....gOLRrDOg....',
    '....gOLSSDOg....',
    '....OLSSSSDO....',
    '...OLLSSSSDDO...',
    '...OSSSSSSSDO...',
    '...OddddddddO...',
    '...OOOOOOOOOO...',
];
// Pillar topped with a gem, runes in the same colour
const SHARESTONE = [
    '.......OO.......',
    '......ORrO......',
    '.....gORRqOg....',
    '....gORRrqqOg...',
    '.....gOrqqOg....',
    '......OqqO......',
    '.....OddddO.....',
    '.....OLSSDO.....',
    '....gOLRrDOg....',
    '....gOLrRDOg....',
    '.....OLSSDO.....',
    '....OLSSSSDO....',
    '...OLLSSSSDDO...',
    '...OSSSSSSSDO...',
    '...OddddddddO...',
    '...OOOOOOOOOO...',
];
// Stone pad seen from above with a glowing ring
const WARP_PLATE = [
    '................',
    '..OOOOOOOOOOOO..',
    '.OLLLLLLLLLLLLO.',
    '.OLSSSSSSSSSSDO.',
    '.OLSSSrrrrSSSDO.',
    '.OLSSrRRRRrSSDO.',
    '.OLSrRqqqqRrSDO.',
    '.OLSrRqRRqRrSDO.',
    '.OLSrRqRRqRrSDO.',
    '.OLSrRqqqqRrSDO.',
    '.OLSSrRRRRrSSDO.',
    '.OLSSSrrrrSSSDO.',
    '.OLSSSSSSSSSSDO.',
    '.ODDDDDDDDDDDDO.',
    '..OOOOOOOOOOOO..',
    '................',
];
// Small stone with a gem
const PORTSTONE = [
    '................',
    '................',
    '................',
    '.......OO.......',
    '......ORrO......',
    '.....gORRqOg....',
    '......OrqO......',
    '.....OddddO.....',
    '....OLLSSDDO....',
    '....OLSRrSDO....',
    '....OLSrRSDO....',
    '...OLLSSSSDDO...',
    '...OSSSSSSSDO...',
    '...OddddddddO...',
    '...OOOOOOOOOO...',
    '................',
];

fs.mkdirSync(OUT, { recursive: true });
const write = (name, art, colour) => {
    fs.writeFileSync(path.join(OUT, name + '.png'), png(art, Object.assign({}, STONE, glow(colour))));
    console.log(name + '.png');
};
write('waystone', WAYSTONE, WAYSTONE_PURPLE);
write('warp_plate', WARP_PLATE, WAYSTONE_PURPLE);
write('portstone', PORTSTONE, WAYSTONE_PURPLE);
write('sharestone', SHARESTONE, WAYSTONE_PURPLE);     // Undyed
for (const [dye, colour] of Object.entries(DYES)) {
    write('sharestone_' + dye, SHARESTONE, colour);
}
