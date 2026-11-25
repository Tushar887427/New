#!/usr/bin/env python3
"""Apply a JAR signature (v1 scheme) to an APK with a self-signed certificate.

v1-only signing is accepted for installation on all current Android versions
as long as targetSdkVersion < 30 (we ship targetSdk 29, minSdk 18).
PKCS#7 blob is produced with the openssl CLI (python cryptography builder had
a class-identity quirk in this environment).
"""
import sys, os, hashlib, base64, zipfile, subprocess, datetime

from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12

APK_IN   = sys.argv[1] if len(sys.argv) > 1 else '/tmp/worked-unsigned.apk'
APK_OUT  = sys.argv[2] if len(sys.argv) > 2 else '/tmp/worked-signed.apk'
KEYSTORE = sys.argv[3] if len(sys.argv) > 3 else '/tmp/worked-sign.jks'
STORE_PW = sys.argv[4] if len(sys.argv) > 4 else 'worked'
WORK     = '/tmp/tools/signwork'
os.makedirs(WORK, exist_ok=True)

if os.path.exists(KEYSTORE):
    key, cert, _ = pkcs12.load_key_and_certificates(open(KEYSTORE, 'rb').read(), STORE_PW.encode())
    print('[sign] loaded existing keystore')
else:
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    name = x509.Name([
        x509.NameAttribute(NameOID.COMMON_NAME, u"Work'ed"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, u'Worked'),
    ])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder()
            .subject_name(name).issuer_name(name)
            .public_key(key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(days=1))
            .not_valid_after(now + datetime.timedelta(days=365*30))
            .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
            .sign(key, hashes.SHA256()))
    data = pkcs12.serialize_key_and_certificates(b'worked', key, cert, None,
        serialization.BestAvailableEncryption(STORE_PW.encode()))
    open(KEYSTORE, 'wb').write(data)
    print(f'[sign] created keystore {KEYSTORE}')

key_pem  = key.private_bytes(serialization.Encoding.PEM,
                             serialization.PrivateFormat.PKCS8,
                             serialization.NoEncryption())
cert_pem = cert.public_bytes(serialization.Encoding.PEM)
open(f'{WORK}/key.pem', 'wb').write(key_pem)
open(f'{WORK}/cert.pem', 'wb').write(cert_pem)

def jar_digest_lines(entries):
    out = []
    for n, d in entries:
        digest = base64.b64encode(hashlib.sha1(d).digest()).decode()
        raw = f'Name: {n}\nSHA1-Digest: {digest}\n\n'
        for line in raw.split('\n'):
            while len(line.encode()) > 72:
                cut = 72 - 1
                while len(line[:cut].encode()) < cut:
                    cut -= 1
                out.append(line[:cut]); line = ' ' + line[cut:]
            out.append(line)
    return '\n'.join(out) + '\n'

zf = zipfile.ZipFile(APK_IN)
entries = [(n, zf.read(n)) for n in zf.namelist() if not n.startswith('META-INF/')]

mf = 'Manifest-Version: 1.0\nCreated-By: 1.0 (Android)\n\n' + jar_digest_lines(entries)
mf_b = mf.encode()
sf = ('Signature-Version: 1.0\n'
      'Created-By: 1.0 (Android)\n'
      'SHA1-Digest-Manifest: ' + base64.b64encode(hashlib.sha1(mf_b).digest()).decode() + '\n'
      'X-Android-APK-Signed: 2\n'
      '\n') + jar_digest_lines(entries)
sf_b = sf.encode()
open(f'{WORK}/MANIFEST.MF', 'wb').write(mf_b)
open(f'{WORK}/CERT.SF', 'wb').write(sf_b)

subprocess.run(['openssl', 'smime', '-sign',
                '-in', f'{WORK}/CERT.SF', '-out', f'{WORK}/CERT.RSA',
                '-outform', 'DER', '-binary', '-signer', f'{WORK}/cert.pem',
                '-inkey', f'{WORK}/key.pem', '-passin', 'pass:'],
               check=True, capture_output=True)
p7 = open(f'{WORK}/CERT.RSA', 'rb').read()
print(f'[sign] PKCS7 {len(p7)} bytes')

out = zipfile.ZipFile(APK_OUT, 'w', zipfile.ZIP_DEFLATED)
for n, d in entries:
    zi = zipfile.ZipInfo(n, (2026, 1, 1, 0, 0, 0))
    method = zipfile.ZIP_STORED if (n == 'resources.arsc' or n.endswith('.png')) else zipfile.ZIP_DEFLATED
    out.writestr(zi, d, method)
out.writestr(zipfile.ZipInfo('META-INF/MANIFEST.MF', (2026, 1, 1, 0, 0, 0)), mf_b, zipfile.ZIP_DEFLATED)
out.writestr(zipfile.ZipInfo('META-INF/CERT.SF', (2026, 1, 1, 0, 0, 0)), sf_b, zipfile.ZIP_DEFLATED)
out.writestr(zipfile.ZipInfo('META-INF/CERT.RSA', (2026, 1, 1, 0, 0, 0)), p7, zipfile.ZIP_STORED)
out.close()
print(f'[sign] wrote {APK_OUT} ({os.path.getsize(APK_OUT)} bytes)')
