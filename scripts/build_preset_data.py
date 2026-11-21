#!/usr/bin/env python3
"""生成 assets/plan_templates.json 与 assets/recipes.json，并逐项校验引用的
动作 ID / 食物 ID 真实存在于 exercises.json / foods.json，顺带打印食谱营养汇总。
在仓库根目录运行: python3 scripts/build_preset_data.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")

exercises = {e["id"]: e for e in json.load(open(os.path.join(ASSETS, "exercises.json")))}
foods = {f["id"]: f for f in json.load(open(os.path.join(ASSETS, "foods.json")))}


def item(eid, sets, reps):
    assert eid in exercises, f"exercise not found: {eid}"
    return {"exerciseId": eid, "sets": sets, "reps": reps}


TEMPLATES = [
    {
        "id": "beginner-fullbody",
        "title": "新手全身计划",
        "desc": "适合刚开始力量训练的新手，A/B 两套全身动作交替练，建立基础力量与动作模式。",
        "level": "新手",
        "daysPerWeek": "每周 3 练（A/B 交替）",
        "days": [
            {"name": "全身 A", "items": [
                item("0043", 4, "6"), item("0025", 4, "8"),
                item("0027", 4, "10"), item("0464", 3, "40秒"),
            ]},
            {"name": "全身 B", "items": [
                item("0032", 3, "5"), item("0091", 3, "8"),
                item("2330", 3, "12"), item("0054", 3, "每侧 10"),
            ]},
        ],
    },
    {
        "id": "ppl-3day",
        "title": "推拉腿 · 一周三练",
        "desc": "经典推/拉/腿分化，一周各练一次，适合时间有限又想系统增肌的人。",
        "level": "进阶",
        "daysPerWeek": "每周 3 练",
        "days": [
            {"name": "推（胸肩三头）", "items": [
                item("0025", 4, "6"), item("0047", 3, "10"),
                item("0426", 3, "10"), item("0334", 3, "15"),
                item("0201", 3, "12"),
            ]},
            {"name": "拉（背二头）", "items": [
                item("0032", 3, "5"), item("0652", 4, "力竭"),
                item("0180", 3, "12"), item("0031", 3, "10"),
                item("0298", 3, "12"),
            ]},
            {"name": "腿", "items": [
                item("0043", 4, "6"), item("0085", 3, "10"),
                item("0739", 3, "12"), item("0599", 3, "12"),
                item("1372", 4, "15"),
            ]},
        ],
    },
    {
        "id": "ppl-6day",
        "title": "推拉腿 · 一周六练",
        "desc": "推拉腿循环两轮，B 日换哑铃与器械变式补容量，适合恢复能力强的进阶者。",
        "level": "进阶",
        "daysPerWeek": "每周 6 练",
        "days": [
            {"name": "推 A（杠铃）", "items": [
                item("0025", 4, "6"), item("0047", 3, "10"),
                item("0426", 3, "10"), item("0334", 3, "15"),
                item("0201", 3, "12"),
            ]},
            {"name": "拉 A（杠铃）", "items": [
                item("0032", 3, "5"), item("0652", 4, "力竭"),
                item("0180", 3, "12"), item("0031", 3, "10"),
                item("0298", 3, "12"),
            ]},
            {"name": "腿 A", "items": [
                item("0043", 4, "6"), item("0085", 3, "10"),
                item("0739", 3, "12"), item("0599", 3, "12"),
                item("1372", 4, "15"),
            ]},
            {"name": "推 B（哑铃）", "items": [
                item("0289", 4, "10"), item("0319", 3, "12"),
                item("0091", 3, "10"), item("0178", 3, "15"),
                item("0060", 3, "12"),
            ]},
            {"name": "拉 B（器械）", "items": [
                item("0027", 4, "8"), item("2330", 3, "12"),
                item("0215", 3, "15"), item("0406", 3, "15"),
                item("0165", 3, "12"),
            ]},
            {"name": "腿 B", "items": [
                item("0042", 3, "8"), item("1409", 3, "12"),
                item("0585", 3, "15"), item("0586", 3, "12"),
                item("0088", 4, "15"),
            ]},
        ],
    },
    {
        "id": "upper-lower",
        "title": "上下肢分化 · 一周四练",
        "desc": "上肢与下肢各两练，主项打力量、副项补容量，是最均衡的四日分化。",
        "level": "进阶",
        "daysPerWeek": "每周 4 练",
        "days": [
            {"name": "上肢 A", "items": [
                item("0025", 4, "6"), item("0027", 4, "8"),
                item("0091", 3, "10"), item("0652", 3, "力竭"),
                item("0031", 2, "12"), item("0201", 2, "12"),
            ]},
            {"name": "下肢 A", "items": [
                item("0043", 4, "6"), item("0085", 3, "10"),
                item("0054", 3, "每侧 10"), item("1372", 4, "15"),
            ]},
            {"name": "上肢 B", "items": [
                item("0047", 3, "10"), item("0180", 3, "12"),
                item("0289", 3, "10"), item("2330", 3, "12"),
                item("0334", 3, "15"), item("0060", 2, "12"),
            ]},
            {"name": "下肢 B", "items": [
                item("0032", 3, "5"), item("0739", 3, "12"),
                item("0585", 3, "15"), item("0599", 3, "12"),
                item("0088", 4, "15"),
            ]},
        ],
    },
    {
        "id": "five-split",
        "title": "五分化 · 一天一肌群",
        "desc": "胸、背、腿、肩、手臂各占一天，单次专注一个肌群，健美式经典分化。",
        "level": "进阶",
        "daysPerWeek": "每周 5 练",
        "days": [
            {"name": "胸", "items": [
                item("0025", 4, "8"), item("0047", 3, "10"),
                item("0308", 3, "12"), item("0251", 3, "力竭"),
            ]},
            {"name": "背", "items": [
                item("0032", 4, "5"), item("0652", 4, "力竭"),
                item("0027", 3, "10"), item("2330", 3, "12"),
                item("0095", 3, "15"),
            ]},
            {"name": "腿", "items": [
                item("0043", 4, "8"), item("0739", 3, "12"),
                item("0085", 3, "10"), item("0585", 3, "15"),
                item("0599", 3, "12"), item("1372", 4, "15"),
            ]},
            {"name": "肩", "items": [
                item("0091", 4, "8"), item("0334", 4, "15"),
                item("0310", 3, "12"), item("0215", 3, "15"),
                item("0406", 3, "15"),
            ]},
            {"name": "手臂", "items": [
                item("0031", 3, "10"), item("0060", 3, "12"),
                item("0298", 3, "12"), item("0201", 3, "12"),
                item("0165", 2, "15"), item("0030", 2, "10"),
            ]},
        ],
    },
    {
        "id": "home-bodyweight",
        "title": "徒手居家计划",
        "desc": "全程零器械，在家就能练。引体向上需要一根单杠，没有单杠时用反向划船替代。",
        "level": "新手",
        "daysPerWeek": "每周 3 练",
        "days": [
            {"name": "居家 A", "items": [
                item("0662", 4, "力竭"), item("0514", 3, "12"),
                item("0499", 3, "12"), item("3013", 3, "15"),
                item("0464", 3, "40秒"),
            ]},
            {"name": "居家 B", "items": [
                item("0283", 3, "力竭"), item("3769", 3, "每侧 10"),
                item("0652", 3, "力竭"), item("0489", 3, "15"),
                item("0687", 3, "每侧 12"),
            ]},
            {"name": "居家 C", "items": [
                item("0279", 3, "力竭"), item("1160", 3, "10"),
                item("0630", 3, "40秒"), item("1373", 3, "20"),
                item("0276", 3, "每侧 10"),
            ]},
        ],
    },
]


def ing(fid, gram, note=""):
    assert fid in foods, f"food not found: {fid}"
    return {"foodId": fid, "gram": gram, "note": note}


RECIPES = [
    {
        "id": "chicken-breast-rice", "title": "香煎鸡胸肉配米饭", "category": "增肌",
        "minutes": 25, "tags": ["高蛋白", "经典增肌餐"],
        "ingredients": [
            ing("091112", 200, "切片，用盐和黑胡椒腌 10 分钟"),
            ing("012401x", 250, "约一碗半"),
            ing("045217", 150, "掰小朵"),
        ],
        "steps": [
            "鸡胸肉切片，加盐、黑胡椒、生抽少许腌制 10 分钟。",
            "平底锅少油，中火每面煎 3-4 分钟至表面金黄、内部全熟。",
            "西兰花焯水 2 分钟捞出，加少许盐调味。",
            "米饭盛碗，摆上鸡胸肉和西兰花即可。",
        ],
    },
    {
        "id": "beef-onion", "title": "牛肉炒洋葱配米饭", "category": "增肌",
        "minutes": 20, "tags": ["高蛋白", "下饭"],
        "ingredients": [
            ing("082118", 180, "逆纹切片"),
            ing("044301", 100, "切丝"),
            ing("012401x", 200, "约一碗"),
        ],
        "steps": [
            "牛肉逆纹切片，加生抽、淀粉少许抓匀腌 10 分钟。",
            "热锅少油，下牛肉大火快炒至变色盛出。",
            "同锅下洋葱丝炒软，倒回牛肉翻炒均匀，加盐调味。",
            "盖在米饭上即可。",
        ],
    },
    {
        "id": "tuna-egg-rice", "title": "金枪鱼鸡蛋拌饭", "category": "增肌",
        "minutes": 15, "tags": ["免开火多", "快手"],
        "ingredients": [
            ing("121405", 120, "沥干水分"),
            ing("111101x", 100, "约 2 个，煮熟"),
            ing("012401x", 250, "约一碗半"),
            ing("043208", 80, "切丁"),
        ],
        "steps": [
            "鸡蛋冷水下锅，水开后煮 8 分钟，剥壳切块。",
            "金枪鱼沥干，与黄瓜丁、鸡蛋一起铺在米饭上。",
            "淋少许生抽或低脂蛋黄酱拌匀即可。",
        ],
    },
    {
        "id": "shrimp-egg", "title": "虾仁滑蛋配米饭", "category": "增肌",
        "minutes": 15, "tags": ["高蛋白", "嫩滑"],
        "ingredients": [
            ing("122206", 150, "去虾线"),
            ing("111101x", 150, "约 3 个"),
            ing("012401x", 200, "约一碗"),
        ],
        "steps": [
            "虾仁加盐、胡椒和少许淀粉抓匀。",
            "鸡蛋打散加一小勺水和少许盐。",
            "热锅少油先炒虾仁至变色，倒入蛋液，小火推炒至刚凝固关火。",
            "盖在米饭上，撒葱花（可选）。",
        ],
    },
    {
        "id": "oat-protein-breakfast", "title": "燕麦牛奶蛋白早餐", "category": "增肌",
        "minutes": 10, "tags": ["早餐", "快手"],
        "ingredients": [
            ing("019012", 60, ""),
            ing("101101x", 250, "一盒"),
            ing("111101x", 100, "约 2 个，煮熟"),
            ing("103002", 100, "拌入燕麦"),
        ],
        "steps": [
            "燕麦加牛奶小火煮 3-5 分钟至浓稠（或微波炉高火 3 分钟）。",
            "关火后拌入酸奶增加蛋白质和口感。",
            "配水煮蛋一起吃，10 分钟搞定一顿高蛋白早餐。",
        ],
    },
    {
        "id": "mapo-tofu-rice", "title": "麻婆豆腐盖饭（增肌版）", "category": "增肌",
        "minutes": 20, "tags": ["下饭", "肉末加量"],
        "ingredients": [
            ing("031306", 200, "切块"),
            ing("081110", 80, "剁成肉末"),
            ing("012401x", 250, "约一碗半"),
        ],
        "steps": [
            "北豆腐切块，焯水 1 分钟去豆腥，捞出备用。",
            "热锅少油下肉末煸炒出油，加豆瓣酱少许炒出红油。",
            "加半碗水和豆腐小火炖 5 分钟，勾薄芡收汁。",
            "浇在米饭上，撒花椒粉和葱花（可选）。",
        ],
    },
    {
        "id": "steamed-cod", "title": "清蒸鳕鱼配时蔬", "category": "减脂",
        "minutes": 20, "tags": ["低脂", "清淡"],
        "ingredients": [
            ing("121239", 200, ""),
            ing("045217", 150, "掰小朵"),
            ing("041201", 80, "切滚刀块"),
        ],
        "steps": [
            "鳕鱼加少许盐和姜丝腌 10 分钟。",
            "水开后上锅蒸 8-10 分钟，倒掉盘中腥水。",
            "西兰花和胡萝卜焯水 2 分钟摆盘。",
            "鱼身淋少许蒸鱼豉油即可，尽量不额外加油。",
        ],
    },
    {
        "id": "chicken-salad", "title": "鸡胸肉蔬菜沙拉", "category": "减脂",
        "minutes": 20, "tags": ["低卡", "饱腹"],
        "ingredients": [
            ing("091112", 150, "整块煮熟后撕条"),
            ing("045333", 100, "撕片"),
            ing("043119", 100, "切块"),
            ing("043208", 100, "切片"),
            ing("013101", 80, "煮熟剥粒"),
        ],
        "steps": [
            "鸡胸肉冷水下锅，加姜片煮 12 分钟，关火焖 5 分钟后撕成条。",
            "玉米粒煮熟；生菜、番茄、黄瓜洗净切好。",
            "全部食材拌匀，用油醋汁或低脂沙拉汁调味（汁的量控制在 1 勺内）。",
        ],
    },
    {
        "id": "tomato-tofu-soup", "title": "番茄豆腐鸡蛋汤", "category": "减脂",
        "minutes": 15, "tags": ["暖胃", "低卡"],
        "ingredients": [
            ing("031307", 200, "切块"),
            ing("043119", 150, "切块"),
            ing("111101x", 50, "约 1 个，打散"),
            ing("045301", 80, "洗净"),
        ],
        "steps": [
            "番茄块下锅少油翻炒出汁，加 600ml 水煮开。",
            "下豆腐块煮 3 分钟，再下菠菜烫软。",
            "淋入蛋液形成蛋花，加盐调味即可。",
        ],
    },
    {
        "id": "broccoli-shrimp", "title": "蒜蓉西兰花炒虾仁", "category": "减脂",
        "minutes": 15, "tags": ["低脂高蛋白", "快手"],
        "ingredients": [
            ing("122206", 120, "去虾线"),
            ing("045217", 200, "掰小朵"),
        ],
        "steps": [
            "西兰花焯水 1 分钟捞出（保持翠绿和脆感）。",
            "热锅少油爆香蒜末，下虾仁炒至变色。",
            "倒入西兰花大火快炒 1 分钟，加盐调味出锅。",
        ],
    },
    {
        "id": "salmon-asparagus", "title": "香煎三文鱼配芦笋土豆", "category": "减脂",
        "minutes": 25, "tags": ["优质脂肪", "饱腹"],
        "ingredients": [
            ing("121228", 150, "带皮"),
            ing("045415", 120, "去老根"),
            ing("021101", 150, "切块蒸熟"),
        ],
        "steps": [
            "三文鱼用厨房纸吸干水分，两面撒盐和黑胡椒。",
            "平底锅不用油，鱼皮面朝下中火煎 4 分钟，翻面再煎 2 分钟。",
            "芦笋同锅煎 2 分钟；土豆蒸熟压块撒少许盐。",
            "摆盘，挤柠檬汁（可选）。",
        ],
    },
    {
        "id": "chicken-leg-sweet-potato", "title": "香煎鸡腿配红薯菠菜", "category": "减脂",
        "minutes": 30, "tags": ["饱腹", "粗粮主食"],
        "ingredients": [
            ing("091113", 180, "去皮"),
            ing("021205", 200, "蒸熟"),
            ing("045301", 100, "焯水"),
        ],
        "steps": [
            "红薯上锅蒸 25 分钟至软糯。",
            "去皮鸡腿加盐、黑胡椒腌 10 分钟，平底锅少油两面煎至金黄熟透。",
            "菠菜焯水 1 分钟捞出，加少许蒜末和盐拌匀。",
            "三样摆盘即可，红薯代替米饭做主食。",
        ],
    },
]

with open(os.path.join(ASSETS, "plan_templates.json"), "w", encoding="utf-8") as f:
    json.dump({"templates": TEMPLATES}, f, ensure_ascii=False, indent=1)
with open(os.path.join(ASSETS, "recipes.json"), "w", encoding="utf-8") as f:
    json.dump({"recipes": RECIPES}, f, ensure_ascii=False, indent=1)

print(f"templates: {len(TEMPLATES)}  days: {sum(len(t['days']) for t in TEMPLATES)}")
for r in RECIPES:
    e = sum(foods[i["foodId"]]["energy"] * i["gram"] / 100 for i in r["ingredients"])
    p = sum(foods[i["foodId"]]["protein"] * i["gram"] / 100 for i in r["ingredients"])
    print(f"{r['category']} {r['title']}: {e:.0f} kcal, 蛋白 {p:.1f} g")
print("OK - all ids validated")
