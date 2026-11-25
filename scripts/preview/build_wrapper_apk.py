#!/usr/bin/env python3
"""
Build the Work'ed WebView wrapper APK by patching an open-source WebView template
APK (io.codetic.webview, MIT-friendly sample app published on GitHub rupok/webview).

Patches:
  dex   : io/codetic/webview  -> app/worked/webview   (18 -> 18 chars, in-place)
          const URL           -> file:///android_asset/index.html
  axml  : targetSdkVersion 21 -> 29, package string (same length)
  arsc  : package name (same length), app_name "WebView" -> "Work'ed" (7 -> 7)
  assets: index.html        = the AI Studio HTML preview of Work'ed
  res   : launcher icons replaced with a generated Work'ed icon

Then re-zips and applies a fresh JAR signature (v1) with a self-signed cert,
which is valid for installation on Android 11+ because targetSdk < 30.
"""
import zipfile, re, struct, hashlib, io, os, sys, shutil

TEMPLATE_APK = sys.argv[1] if len(sys.argv) > 1 else '/tmp/tools/webview-tpl/WebView.apk'
HTML_FILE    = sys.argv[2] if len(sys.argv) > 2 else '/home/user/New/public/assets/aistudio/Work_ed_af60d700.html'
OUT_UNSIGNED = sys.argv[3] if len(sys.argv) > 3 else '/tmp/worked-unsigned.apk'

OLD_PKG_PATH = b'io/codetic/webview'   # dex class path fragment (18 chars)
NEW_PKG_PATH = b'app/worked/webview'   # replacement (18 chars)
OLD_URL  = b'http://codetic.net/demo/webview-app/'
NEW_URL  = b'file:///android_asset/index.html'
OLD_PKG  = b'io.codetic.webview'       # axml/arsc (18 chars)
NEW_PKG  = b'app.worked.webview'       # (18 chars)
OLD_NAME = 'WebView'                   # app_name value (7 chars)
NEW_NAME = "Work'ed"                   # (7 chars)

z = zipfile.ZipFile(TEMPLATE_APK)
names = z.namelist()

# ---------------------------------------------------------------- dex patch
dex = bytearray(z.read('classes.dex'))
cnt = dex.count(OLD_PKG_PATH)
assert cnt > 0, 'package path not found in dex'
dex = dex.replace(OLD_PKG_PATH, NEW_PKG_PATH)
# URL string: dex string_data_item = uleb128 utf16_len, utf8 bytes, 0x00.
# Our replacement is SHORTER: rewrite the item, then zero-pad the tail gap.
i = dex.find(OLD_URL)
assert i >= 0, 'URL string not found in dex'
# back up over the uleb128 length prefix
start = i - 1
while dex[start] & 0x80:
    start -= 1
assert start == i - 1, 'multi-byte length unexpected'
old_len = dex[start]
new_span = (i - start) + old_len + 1   # uleb + content + NUL terminator
new_b = NEW_URL
new_need = 1 + len(new_b) + 1       # uleb(1 byte) + content + NUL
assert new_need <= new_span, 'replacement longer than original'
dex[start] = len(new_b)
dex[start+1:start+1+len(new_b)] = new_b
for k in range(start+1+len(new_b), start+new_span):
    dex[k] = 0
# rebuild sha1 (bytes 32..end) + adler32 checksum (bytes 12..end)
body_sha = bytes(dex[32:])
sha = hashlib.sha1(body_sha).digest()
dex[12:32] = sha
A, B = 1, 0
for b in bytes(dex[12:]):
    A = (A + b) % 65521
    B = (B + A) % 65521
adler = (B << 16) | A
dex[8:12] = struct.pack('<I', adler)
dex = bytes(dex)
print(f'[dex] patched {cnt} class-path refs + URL; sha1/adler recomputed')

# --------------------------------------------------------------- axml patch
axml = bytearray(z.read('AndroidManifest.xml'))
cnt = axml.count(OLD_PKG) + axml.count(OLD_PKG.decode().encode('utf-16-le'))
axml = axml.replace(OLD_PKG.decode().encode('utf-16-le'), NEW_PKG.decode().encode('utf-16-le'))
axml = axml.replace(OLD_PKG, NEW_PKG)
# targetSdkVersion: walk AXML chunks. First find the attr's string index via
# the resource-map chunk (0x0180), then patch START_TAG (0x0102) attributes.
def axml_patch_targetsdk(axml, old_val=21, new_val=29, attr_id=0x01010270):
    pos = 8  # skip the outer RES_XML_TYPE chunk header (8 bytes)
    attr_string_idx = None
    # header: type(2) headerSize(2) size(4)
    while pos + 8 <= len(axml):
        ctype, hsize, csize = struct.unpack_from('<HHI', axml, pos)
        if ctype == 0x0180:  # resource map
            n = (csize - 8) // 4
            for i in range(n):
                if struct.unpack_from('<I', axml, pos + 8 + 4*i)[0] == attr_id:
                    attr_string_idx = i
        pos += csize
    assert attr_string_idx is not None, 'targetSdkVersion not in resource map'
    patched = 0
    pos = 8
    want_name = struct.pack('<I', attr_string_idx)
    while pos + 8 <= len(axml):
        ctype, hsize, csize = struct.unpack_from('<HHI', axml, pos)
        if ctype == 0x0102:  # start tag
            ns, name = struct.unpack_from('<II', axml, pos + 16)
            attr_start, attr_size, attr_count = struct.unpack_from('<HHH', axml, pos + 24)
            a = pos + 16 + attr_start  # attrStart is relative to attrExt (ns field)
            for i in range(attr_count):
                base = a + i * attr_size
                aname = axml[base+4:base+8]
                size = struct.unpack_from('<H', axml, base+12)[0]
                typ = axml[base+15]
                if aname == want_name and typ == 0x10:
                    val = struct.unpack_from('<i', axml, base+16)[0]
                    if val == old_val:
                        struct.pack_into('<i', axml, base+16, new_val)
                        patched += 1
        pos += csize
    return patched
patched = axml_patch_targetsdk(axml)
assert patched >= 1, 'targetSdkVersion attribute not patched'
# minSdkVersion: bump 11 -> 18 so SHA-256 PKCS#7 v1 signatures verify everywhere
patched_min = axml_patch_targetsdk(axml, old_val=11, new_val=18, attr_id=0x0101020c)
assert patched_min >= 1, 'minSdkVersion attribute not patched'
axml = bytes(axml)
print(f'[axml] package renamed ({cnt} refs), targetSdk -> 29 ({patched} attr)')

# --------------------------------------------------------------- arsc patch
arsc = bytearray(z.read('resources.arsc'))
n1 = arsc.count(OLD_PKG) + arsc.count(OLD_PKG.decode().encode('utf-16-le'))
arsc = arsc.replace(OLD_PKG.decode().encode('utf-16-le'), NEW_PKG.decode().encode('utf-16-le'))
arsc = arsc.replace(OLD_PKG, NEW_PKG)
n2 = 0
# UTF-16 (default arsc strings) app_name
old16 = OLD_NAME.encode('utf-16-le')
new16 = NEW_NAME.encode('utf-16-le')
n2 += arsc.count(old16)
arsc = arsc.replace(old16, new16)
# UTF-8 encoded arsc strings (flagged pools): len-prefixed so the raw bytes
# appear between NUL terminators; patch exact NUL-delimited occurrences.
old8 = b'\x07' + OLD_NAME.encode() + b'\x00'
new8 = b'\x07' + NEW_NAME.encode() + b'\x00'
n2 += arsc.count(old8)
arsc = arsc.replace(old8, new8)
print(f'[arsc] package x{n1}, app_name x{n2}')

# ------------------------------------------------------------------- assets
html = open(HTML_FILE, 'rb').read()

# -------------------------------------------------------------------- icons
from PIL import Image, ImageDraw
def icon(size):
    s = 8
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, size-1, size-1], radius=size//5, fill=(6, 87, 63, 255))
    lw = max(2, size // 11)
    # stylized W: three thick down-strokes
    x0, x1 = size * 0.28, size * 0.72
    y0, y1 = size * 0.34, size * 0.70
    mid = (x0 + x1) / 2
    pts = [(x0, y0), (x0*0.85 + mid*0.15, y1), (mid, y0*0.92 + y1*0.08),
           (x1*0.85 + mid*0.15, y1), (x1, y0)]
    d.line(pts, fill=(255, 255, 255, 255), width=lw, joint='curve')
    return img

icon_replacements = {}
for n in names:
    if re.match(r'^res/(mipmap|drawable)[^/]*/ic_launcher\.png$', n):
        m = re.search(r'(?:mipmap|drawable)-?([a-z]+)?/', n)
        dens = {'ldpi':0.75,'mdpi':1.0,'hdpi':1.5,'xhdpi':2.0,'xxhdpi':3.0,'xxxhdpi':4.0}
        scale = dens.get(m.group(1) or 'mdpi', 1.0)
        size = int(48 * scale)
        buf = io.BytesIO()
        icon(size).save(buf, 'PNG')
        icon_replacements[n] = buf.getvalue()
print(f'[res] regenerating {len(icon_replacements)} launcher icons')

# -------------------------------------------------------------------- build
out = zipfile.ZipFile(OUT_UNSIGNED, 'w', zipfile.ZIP_DEFLATED)
for n in names:
    if n.startswith('META-INF/'):
        continue                       # drop old signature
    data = z.read(n)
    if n == 'AndroidManifest.xml':  data = axml
    elif n == 'resources.arsc':     data = bytes(arsc)
    elif n == 'classes.dex':        data = dex
    elif n in icon_replacements:    data = icon_replacements[n]
    if n == 'resources.arsc':
        out.writestr(zipfile.ZipInfo(n, (2026, 1, 1, 0, 0, 0)), data, zipfile.ZIP_STORED)
    else:
        out.writestr(zipfile.ZipInfo(n, (2026, 1, 1, 0, 0, 0)), data, zipfile.ZIP_DEFLATED)
out.writestr(zipfile.ZipInfo('assets/index.html', (2026, 1, 1, 0, 0, 0)), html, zipfile.ZIP_DEFLATED)
out.close()
print(f'[apk] wrote {OUT_UNSIGNED} ({os.path.getsize(OUT_UNSIGNED)} bytes)')
