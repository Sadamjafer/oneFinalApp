import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

with open('patch_bottom_bar.kt', 'r') as f:
    patch_lines = f.readlines()

out_lines = lines[:108] + patch_lines + lines[322:]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(out_lines)

print("Done")
