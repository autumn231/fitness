#!/usr/bin/env python3
"""把 assets/videos 下的 GIF 动作演示批量转成动态 WebP（同目录、同名、换扩展名）。

用法: python3 convert_videos_to_webp.py <videos_dir>
- 逐帧保留原始时长与无限循环；质量 78、method 4（与包体实测口径一致）。
- 每个文件校验帧数一致；失败文件打印 FAIL 行，进程退出码非 0。
- 已存在的 .webp 跳过（可断点续跑）。
"""
import os
import sys
from multiprocessing import Pool

from PIL import Image


def convert(path: str) -> str:
    out = path[:-4] + ".webp"
    if os.path.exists(out):
        return f"SKIP {os.path.basename(path)}"
    im = Image.open(path)
    frames, durs = [], []
    try:
        while True:
            frames.append(im.convert("RGB").copy())
            durs.append(im.info.get("duration", 40))
            im.seek(im.tell() + 1)
    except EOFError:
        pass
    frames[0].save(
        out, "WEBP", save_all=True, append_images=frames[1:],
        duration=durs, loop=0, quality=78, method=4,
    )
    chk = Image.open(out)
    n = getattr(chk, "n_frames", 1)
    if n != len(frames):
        os.remove(out)
        return f"FAIL {os.path.basename(path)} frames {len(frames)} -> {n}"
    return f"OK {os.path.basename(path)} {os.path.getsize(path)} -> {os.path.getsize(out)}"


def main() -> int:
    videos = sys.argv[1]
    gifs = sorted(
        os.path.join(videos, f) for f in os.listdir(videos) if f.endswith(".gif")
    )
    print(f"total gifs: {len(gifs)}", flush=True)
    fails = 0
    with Pool(3) as pool:
        for i, line in enumerate(pool.imap_unordered(convert, gifs, chunksize=16), 1):
            if line.startswith("FAIL"):
                fails += 1
                print(line, flush=True)
            if i % 200 == 0:
                print(f"progress {i}/{len(gifs)}", flush=True)
    webps = [f for f in os.listdir(videos) if f.endswith(".webp")]
    total_gif = sum(os.path.getsize(p) for p in gifs)
    total_webp = sum(
        os.path.getsize(os.path.join(videos, f)) for f in webps
    )
    print(f"webp count: {len(webps)}")
    print(f"gif total MB: {total_gif / 1e6:.1f}  webp total MB: {total_webp / 1e6:.1f}")
    print(f"fails: {fails}")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
