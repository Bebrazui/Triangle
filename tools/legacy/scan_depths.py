import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    lines = f.readlines()

d = 0
for idx, line in enumerate(lines):
    clean = ''
    in_str = False
    in_char = False
    i = 0
    while i < len(line):
        ch = line[i]
        if ch == '"' and not in_char:
            in_str = not in_str
        elif ch == '\'' and not in_str:
            in_char = not in_char
        elif not in_str and not in_char:
            clean += ch
        i += 1
    d_before = d
    d += clean.count('{') - clean.count('}')
    if idx + 1 >= 256 and idx + 1 <= 343:
        print(f"{idx+1} [before={d_before}, after={d}]: {repr(line)}")
