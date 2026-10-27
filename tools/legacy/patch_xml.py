p = '/mnt/c/Users/ttt79/.gemini/antigravity/brain/65f2cd39-5686-4280-bbf5-3f9f970f9585/settings_global.xml'
data = bytearray(open(p, 'rb').read())
target = b'debug.force_rtl/\x00\x05\x00\x011'
rep = b'debug.force_rtl/\x00\x05\x00\x010'
idx = data.find(target)
print('Target index:', idx)
if idx != -1:
    data[idx:idx+len(target)] = rep
    open('/mnt/c/Users/ttt79/.gemini/antigravity/brain/65f2cd39-5686-4280-bbf5-3f9f970f9585/settings_global_fixed.xml', 'wb').write(data)
    print('Successfully patched!')
else:
    print('Target not found')
