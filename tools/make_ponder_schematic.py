"""Writes the Ponder schematic for the multi-display scene (an original layout, no Create assets).
Run: python -I tools/make_ponder_schematic.py"""
import gzip, struct

def name(n):
    b = n.encode(); return struct.pack('>H', len(b)) + b
def t_int(n, v): return b'\x03' + name(n) + struct.pack('>i', v)
def t_str(n, v):
    b = v.encode(); return b'\x08' + name(n) + struct.pack('>H', len(b)) + b
def t_list(n, typ, payloads): return b'\x09' + name(n) + bytes([typ]) + struct.pack('>i', len(payloads)) + b''.join(payloads)
def t_comp(n, *tags): return b'\x0a' + name(n) + b''.join(tags) + b'\x00'
def comp(*tags): return b''.join(tags) + b'\x00'

SIZE = (5, 3, 5)
palette = []   # (block, {props})
blocks = []    # (x, y, z, state index)

def state(block, **props):
    key = (block, tuple(sorted(props.items())))
    if key not in palette:
        palette.append(key)
    return palette.index(key)

def put(x, y, z, block, **props):
    blocks.append((x, y, z, state(block, **props)))

# Base plate: checkerboard like Create's own scenes (corners are concrete)
for x in range(5):
    for z in range(5):
        put(x, 0, z, 'minecraft:white_concrete' if (x + z) % 2 == 0 else 'minecraft:snow_block')
# Source block with the link on top, two signs in front of it facing the Ponder camera (north)
put(2, 1, 3, 'minecraft:target', power='0')
put(2, 2, 3, 'create:display_link', facing='up', powered='false')
put(1, 1, 1, 'minecraft:oak_sign', rotation='8', waterlogged='false')
put(3, 1, 1, 'minecraft:oak_sign', rotation='8', waterlogged='false')
# Fill the rest with air so the template covers the whole volume
taken = {(x, y, z) for x, y, z, _ in blocks}
for x in range(5):
    for y in range(3):
        for z in range(5):
            if (x, y, z) not in taken:
                put(x, y, z, 'minecraft:air')

pal = []
for block, props in palette:
    tags = [t_str('Name', block)]
    if props:
        tags.append(t_comp('Properties', *[t_str(k, v) for k, v in props]))
    pal.append(comp(*tags))
blk = [comp(t_list('pos', 3, [struct.pack('>i', v) for v in (x, y, z)]), t_int('state', s)) for x, y, z, s in blocks]
root = b'\x0a' + name('') + comp(
    t_int('DataVersion', 3955),
    t_list('size', 3, [struct.pack('>i', v) for v in SIZE]),
    t_list('palette', 10, pal),
    t_list('blocks', 10, blk),
    t_list('entities', 10, []),
)
with gzip.open('src/main/resources/assets/create_displaylinkplus/ponder/multi_link.nbt', 'wb') as f:
    f.write(root)
print('palette', len(palette), 'blocks', len(blocks))
