import os
import subprocess
import sys

ponces_sha1 = "faaf6a5a7032aef89e9559b6ceed266e59b2bd29"
matches = []

mnt = "/mnt/scan_img"
for root, dirs, files in os.walk(mnt):
    for f in files:
        if f.endswith(".apk"):
            full = os.path.join(root, f)
            res = subprocess.run(["apksigner", "verify", "--print-certs", full], capture_output=True, text=True)
            if ponces_sha1 in res.stdout:
                matches.append(os.path.relpath(full, mnt))

print(f"Total signed by platform ponces key: {len(matches)}")
for m in matches:
    print(" ", m)
