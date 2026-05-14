"""
批次1：新增10篇文章 + 优惠券 + 评论
"""
import json, time, hashlib, urllib.request, urllib.error, urllib.parse

BASE_URL = "http://localhost:8080"

BATCH_ARTICLES = [
    {"title": "鱼香肉丝没有鱼？川菜的调味哲学", "content": "鱼香肉丝是川菜经典，但里面没有鱼。鱼香是四川人烹鱼时用的调料组合：泡辣椒、姜蒜、葱、糖、醋、酱油。这套调料用在肉丝上，酸甜微辣，下饭一绝。\n\n肉丝要切得粗细均匀，用蛋清和淀粉上浆，滑油至变色。木耳、胡萝卜、青椒切丝配色。关键是鱼香汁：糖醋比例2:1，加少许酱油和淀粉水。大火快炒，汁水包裹每根肉丝，酸甜辣香层次分明。", "category": "川菜"},
    {"title": "夫妻肺片的红油怎么熬才香", "content": "夫妻肺片是成都名小吃，牛肉牛杂切薄片，浇上红油，麻辣鲜香。红油是灵魂：菜籽油烧到七成热，关火降到五成热，放入干辣椒面和花椒面，慢慢搅拌，油温让辣椒释放出红色和香气。\n\n牛肉、牛舌、牛肚分别卤制，切薄片码盘。红油加蒜泥、花生碎、芝麻、香菜调匀浇上。每一片都裹满红油，入口先是麻辣，然后是牛肉的醇香，越吃越上头。", "category": "川菜"},
    {"title": "担担面的芝麻酱怎么调", "content": "担担面是四川街头的灵魂小吃。面条用碱水面，细而有弹性。担担面的精髓在酱料：芝麻酱用温水慢慢澥开，加酱油、醋、花椒粉、辣椒油、蒜水、葱花。\n\n肉臊子用猪肉末炒干，加芽菜碎增香。面条煮好沥水，浇上酱料和肉臊子，拌匀后每根面条都裹满酱汁。吃一口，麻辣鲜香，芝麻酱的醇厚和辣椒的刺激完美融合。", "category": "川菜"},
    {"title": "口水鸡为什么叫口水鸡", "content": "口水鸡名字的由来：这道菜麻辣鲜香，让人看了就流口水，所以叫口水鸡。鸡肉用三黄鸡，煮熟后过冰水，皮脆肉嫩。切块码盘，淋上特制酱汁。\n\n酱汁配方：辣椒油、花椒油、酱油、醋、白糖、蒜泥、花生碎、芝麻、葱花。关键是辣椒油要用四川朝天椒和二荆条混合，辣度适中香气足。浇在冰凉的鸡肉上，红油浸透鸡肉，麻辣鲜香，让人欲罢不能。", "category": "川菜"},
    {"title": "清蒸石斑鱼的火候与时间", "content": "粤菜清蒸石斑鱼，讲究的是鲜。石斑鱼处理干净，鱼身划几刀，抹少许盐和料酒腌10分钟。盘底垫葱姜，鱼放上面，大火蒸8分钟。\n\n8分钟是关键！时间短了不熟，时间长了肉老。蒸好后倒掉盘中汁水，铺上葱丝姜丝，浇上蒸鱼豉油。最后一步：烧一勺热油，浇在葱姜上，滋啦一声，香气四溢。鱼肉嫩滑如豆腐，鲜甜无比。", "category": "粤菜"},
    {"title": "广式烧鹅的脆皮秘诀", "content": "广式烧鹅皮脆肉嫩，秘诀在风干和烤制。鹅处理干净，用五香粉、盐、糖、料酒腌制入味。关键步骤：用开水烫皮，让皮收紧，再刷上皮水（麦芽糖、白醋、料酒混合）。\n\n风干4小时以上，皮越干烤出来越脆。烤箱预热220度，先烤背面20分钟，翻面再烤15分钟。皮色金红，油光发亮，切块后皮脆肉嫩，蘸梅子酱吃，酸甜解腻。", "category": "粤菜"},
    {"title": "老火靓汤的煲制时间表", "content": "广东人无汤不欢，老火靓汤是每天的仪式。经典搭配：猪骨+莲藕+花生，鸡+椰子+红枣，排骨+冬瓜+薏米。\n\n煲汤讲究：冷水下料，大火烧开转小火，至少煲2小时。中途不加水，不开盖。盐最后放，早放盐肉会柴。一碗好汤，汤色清澈，味道醇厚，喝一口暖到心里。广东人说：宁可食无菜，不可食无汤。", "category": "粤菜"},
    {"title": "小笼包的汤汁怎么灌进去的", "content": "广式小笼包和上海小笼包不同，广式更注重皮的松软。但汤汁的秘密是共通的：猪皮冻。猪皮煮烂打成冻，切碎拌入肉馅。蒸的时候猪皮冻融化，变成汤汁。\n\n面皮用半发面，擀成中间厚边缘薄。包入肉馅和皮冻，捏出18个褶子。大火蒸10分钟，打开盖子，每个小笼包都鼓鼓的，咬开一个小口，鲜美的汤汁涌出，小心烫嘴。", "category": "粤菜"},
    {"title": "干炒牛河的镬气从哪来", "content": "干炒牛河是检验粤菜厨师功力的标准。河粉要宽而薄，牛肉要嫩，芽菜要脆。最关键的是镬气——大火快炒产生的焦香。\n\n铁锅烧到冒烟，下油，牛肉快速滑炒至变色盛出。再下油，河粉摊平煎至微焦，翻面，加酱油上色。牛肉回锅，加芽菜和葱段，大火翻炒几下出锅。全程不超过3分钟。河粉干身不油腻，牛肉嫩滑，芽菜爽脆，这就是镬气的魅力。", "category": "粤菜"},
    {"title": "潮汕牛肉丸的手打工艺", "content": "潮汕牛肉丸讲究手打。新鲜牛后腿肉，去筋膜，用两根铁棒反复捶打，直到肉浆起胶。这个过程需要30分钟以上，不能偷懒用绞肉机。\n\n打好后挤成丸子，温水定型再煮熟。手打牛肉丸弹牙到什么程度？扔到地上能弹起来。咬一口，牛肉的鲜香在口中爆发，越嚼越香。蘸沙茶酱吃，是潮汕人的标配。", "category": "粤菜"},
]

# 评论模板
COMMENT_TEMPLATES = [
    "看完口水直流，必须试试这个做法！", "收藏了，这周末就做给家人吃", "终于找到正宗做法了，感谢分享",
    "这个配方太靠谱了，做出来跟饭店一样", "图片好诱人，食欲大开", "原来秘诀在这里，学到了",
    "这家店就在学校旁边，经常去吃", "价格实惠分量足，学生党最爱", "推荐给了室友，都说好吃",
    "做了好多次了，每次都成功", "请问用什么牌子的调料比较好", "有没有视频教程啊",
    "夏天吃这个太开胃了", "冬天来一碗暖暖的", "减肥期间能吃吗", "热量高不高啊",
    "适合带便当吗", "小朋友能吃吗", "老人牙口不好能做吗", "素食版本怎么做",
    "可以用空气炸锅吗", "电饭煲能做吗", "宿舍党能做吗", "新手第一次做就成功了",
    "比外卖好吃多了", "自己做干净卫生", "成本算下来很便宜", "聚会做了一桌全吃光",
    "拍照发朋友圈获赞无数", "男朋友说比餐厅还好吃", "妈妈尝了直夸好", "做了两大盘都不够吃",
]

REPLY_TEMPLATES = [
    "确实好吃，强烈推荐！", "我也试了，成功了！", "同问，想知道", "谢谢分享，学到了",
    "这个做法很正宗", "必须安排上", "已经做了，味道绝了", "新手友好，不翻车",
]


def api_post(path, data, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(f"{BASE_URL}{path}", data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def api_get(path, params=None, token=None):
    url = f"{BASE_URL}{path}"
    if params:
        url += f"?{urllib.parse.urlencode(params)}"
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def make_cover(title):
    seed = hashlib.md5(title.encode()).hexdigest()[:8]
    return f"https://picsum.photos/seed/{seed}/400/300"


def login(username, password):
    res = api_post("/api/auth/login", {"username": username, "password": password})
    if res.get("code") == 200:
        return res["data"]["accessToken"]
    return None


def main():
    print("=" * 50)
    print("  批次1：创建10篇文章 + 优惠券 + 评论")
    print("=" * 50)

    # 登录所有商家
    merchants = [
        ("chef_wang", "王记川菜馆", "川菜"),
        ("chef_li", "李记粤菜轩", "粤菜"),
        ("chef_zhang", "张姐湘菜馆", "湘菜"),
        ("chef_chen", "陈记甜品屋", "甜品"),
        ("chef_liu", "刘姐小吃铺", "小吃"),
    ]
    merchant_tokens = {}
    for username, shop, cat in merchants:
        token = login(username, "123456")
        if token:
            merchant_tokens[cat] = token
            print(f"  ✓ 登录: {shop}")
        else:
            print(f"  ✗ 登录失败: {shop}")

    # 登录学生（用于评论）
    students = ["student_alice", "student_bob", "student_carol", "student_dave", "student_eve"]
    student_tokens = []
    for s in students:
        t = login(s, "123456")
        student_tokens.append(t)
        if t:
            print(f"  ✓ 登录: {s}")

    # 创建文章
    print("\n【创建文章】")
    created_articles = []
    for art in BATCH_ARTICLES:
        cat = art["category"]
        token = merchant_tokens.get(cat)
        if not token:
            print(f"  ✗ 无{cat}商家token，跳过: {art['title']}")
            continue

        res = api_post("/api/article", {
            "title": art["title"],
            "content": art["content"],
            "category": cat,
            "coverImage": make_cover(art["title"]),
            "status": 1,
        }, token=token)

        if res.get("code") == 200:
            article_id = res["data"]
            created_articles.append({"id": article_id, "title": art["title"], "category": cat})
            print(f"  + [{cat}] {art['title']} (id={article_id})")
        else:
            print(f"  ✗ [{cat}] {art['title']}: {res.get('message')}")
        time.sleep(0.05)

    # 创建优惠券（给每篇文章创建优惠券）
    print("\n【创建优惠券】")
    coupon_count = 0
    for a in created_articles:
        token = merchant_tokens.get(a["category"])
        if not token:
            continue
        res = api_post("/api/coupon", {
            "articleId": a["id"],
            "title": f"{a['category']}美食专享优惠",
            "discountAmount": 5.0,
            "minOrderAmount": 20.0,
            "totalCount": 100,
            "startTime": "2026-05-14 00:00:00",
            "endTime": "2026-06-14 23:59:59",
        }, token=token)
        if res.get("code") == 200:
            coupon_count += 1
            print(f"  + 优惠券: {a['title']}")
        else:
            print(f"  ✗ 优惠券失败: {a['title']}: {res.get('message')}")
        time.sleep(0.05)

    # 创建评论（每篇20+条）
    print("\n【创建评论】")
    total_comments = 0
    for a in created_articles:
        article_id = a["id"]
        article_comments = 0
        for i in range(22):
            student_idx = i % len(student_tokens)
            token = student_tokens[student_idx]
            if not token:
                continue

            comment_text = COMMENT_TEMPLATES[i % len(COMMENT_TEMPLATES)]
            res = api_post("/api/comment", {
                "articleId": article_id,
                "content": comment_text,
            }, token=token)

            comment_id = None
            if res.get("code") == 200:
                comment_id = res.get("data", {}).get("id") if isinstance(res.get("data"), dict) else res.get("data")
                article_comments += 1
                total_comments += 1

                # 部分评论添加回复
                if i < 6 and comment_id:
                    reply_idx = (student_idx + 1) % len(student_tokens)
                    reply_token = student_tokens[reply_idx]
                    if reply_token:
                        reply_text = REPLY_TEMPLATES[i % len(REPLY_TEMPLATES)]
                        api_post("/api/comment", {
                            "articleId": article_id,
                            "content": reply_text,
                            "parentId": comment_id,
                        }, token=reply_token)
                        total_comments += 1

            time.sleep(0.03)
        print(f"  + {a['title'][:20]}...: {article_comments}条评论")

    # 汇总
    print("\n" + "=" * 50)
    print(f"  批次1完成！")
    print(f"  文章: {len(created_articles)} 篇")
    print(f"  优惠券: {coupon_count} 张")
    print(f"  评论: {total_comments} 条")
    print("=" * 50)


if __name__ == "__main__":
    main()
