path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

depth = 0
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
    depth += clean.count('{') - clean.count('}')
    if idx + 1 >= 2880:
        print(f"{idx+1} [depth={depth}]: {line.strip()}")
