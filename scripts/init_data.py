"""
批量初始化测试数据脚本
创建商家、学生用户，并发布美食文章

使用方法：python scripts/init_data.py
前置条件：后端服务已启动（gateway 8080, user 8081, article 8082）
"""

import json
import time
import hashlib
import urllib.request
import urllib.error
import urllib.parse

BASE_URL = "http://localhost:8080"

# ============================================================
# 数据定义
# ============================================================

MERCHANTS = [
    {"username": "chef_wang",   "password": "123456", "nickname": "王大厨",   "shopName": "王记川菜馆",   "category": "川菜", "shopDesc": "正宗四川味道，麻辣鲜香",     "address": "校园东区食堂二楼"},
    {"username": "chef_li",     "password": "123456", "nickname": "李师傅",   "shopName": "李记粤菜轩",   "category": "粤菜", "shopDesc": "广式精致，清淡鲜美",         "address": "校园西区美食街1号"},
    {"username": "chef_zhang",  "password": "123456", "nickname": "张姐",     "shopName": "张姐湘菜馆",   "category": "湘菜", "shopDesc": "湖南口味，香辣过瘾",         "address": "校园南门对面"},
    {"username": "chef_chen",   "password": "123456", "nickname": "陈老板",   "shopName": "陈记甜品屋",   "category": "甜品", "shopDesc": "手工甜品，甜蜜每一天",       "address": "校园北区商业街"},
    {"username": "chef_liu",    "password": "123456", "nickname": "刘姐",     "shopName": "刘姐小吃铺",   "category": "小吃", "shopDesc": "各地特色小吃，物美价廉",     "address": "校园中心广场旁"},
]

STUDENTS = [
    {"username": "student_alice", "password": "123456", "nickname": "Alice"},
    {"username": "student_bob",   "password": "123456", "nickname": "Bob"},
    {"username": "student_carol", "password": "123456", "nickname": "Carol"},
    {"username": "student_dave",  "password": "123456", "nickname": "Dave"},
    {"username": "student_eve",   "password": "123456", "nickname": "Eve"},
]

ARTICLES_BY_CATEGORY = {
    "川菜": [
        {"title": "水煮鱼的正宗做法，麻辣鲜香一锅端", "content": "水煮鱼是四川经典名菜。选用新鲜草鱼，片成薄片，用料酒和淀粉腌制。锅中热油，爆香花椒和干辣椒，加入豆芽铺底，鱼片滑入，浇上滚油，麻辣鲜香，让人欲罢不能。\n\n关键技巧：鱼片要逆纹切，腌制时加蛋清更嫩滑。花椒一定要用四川青花椒，香气更浓郁。"},
        {"title": "麻婆豆腐的六种秘诀", "content": "麻婆豆腐看似简单，实则讲究。豆腐要选嫩豆腐，切块后用盐水浸泡。肉末用牛肉末最香。郫县豆瓣酱是灵魂，一定要炒出红油。\n\n六种秘诀：一选嫩豆腐，二用牛肉末，三炒豆瓣酱，四加花椒粉，五勾薄芡，六撒葱花。掌握这六步，你也能做出饭店级别的麻婆豆腐。"},
        {"title": "回锅肉这样做才地道", "content": "回锅肉是川菜之首。五花肉冷水下锅，加姜片料酒煮至八成熟，捞出切薄片。锅中少油，肉片煸至卷曲出油，加入郫县豆瓣酱炒出红油，再放蒜苗翻炒。\n\n记住：肉片要薄，煸到'灯盏窝'状态才正宗。蒜苗要最后放，保持脆嫩。"},
        {"title": "宫保鸡丁的花生什么时候放", "content": "宫保鸡丁的花生一定要最后放！很多人炒宫保鸡丁把花生炒软了，口感全无。正确做法是：鸡丁滑炒至变色，加入调好的宫保汁（醋、糖、酱油、淀粉），快速翻炒均匀后，关火，再放入炸好的花生米拌匀。\n\n这样花生保持酥脆，每一口都是咔嚓脆。"},
    ],
    "粤菜": [
        {"title": "白切鸡的蘸料才是灵魂", "content": "广东白切鸡，鸡肉只是载体，蘸料才是灵魂。经典姜葱蘸料：姜蓉、葱花、盐，浇上滚烫的花生油，滋啦一声，香气四溢。\n\n鸡肉要选三黄鸡，水开后关火浸泡，反复三次，皮爽肉滑。蘸上姜葱料，鲜美无比。"},
        {"title": "煲仔饭的锅巴怎么才能焦脆", "content": "煲仔饭最诱人的就是那层金黄锅巴。秘诀：砂锅要预热，米饭煮到七成熟时沿锅边淋一圈油，小火焖到听见噼啪声。\n\n腊味煲仔饭最经典：腊肠、腊肉切片铺在饭上，淋上特制酱油，搅拌均匀，每一口都有锅巴的焦香。"},
        {"title": "虾饺皮的透明秘诀", "content": "广式虾饺皮要晶莹剔透，关键是用澄面和淀粉的比例。澄面和玉米淀粉按4:1混合，用沸水烫面，快速揉成团。\n\n馅料用鲜虾仁，加少许肥肉丁增香。包好后大火蒸8分钟，皮薄馅大，透过皮能看到粉红的虾仁。"},
    ],
    "湘菜": [
        {"title": "剁椒鱼头的灵魂在于剁椒", "content": "剁椒鱼头是湘菜代表。鱼头要选雄鱼头，对半劈开，铺上自制剁椒，大火蒸12分钟。\n\n自制剁椒：红辣椒剁碎，加蒜末、盐、白酒拌匀，腌制三天以上。好的剁椒鲜辣开胃，配上嫩滑的鱼头，汤汁拌饭能吃三碗。"},
        {"title": "小炒黄牛肉要大火快炒", "content": "小炒黄牛肉讲究镬气。牛肉切薄片，用生抽、蚝油、淀粉腌制15分钟。锅烧到冒烟，下油，牛肉滑入快速翻炒至变色盛出。\n\n再起锅炒青红椒和蒜片，牛肉回锅，淋少许料酒，翻炒几下即出锅。全程不超过2分钟，牛肉才嫩。"},
        {"title": "湖南人的早餐：米粉", "content": "湖南人的一天从一碗米粉开始。汤底用猪骨熬制4小时，米粉烫熟后浇上骨头汤，加码子（浇头）。\n\n经典码子：红烧牛肉、酸辣鸡杂、辣椒炒肉。再加酸豆角、剁辣椒、葱花，一碗下肚，精神一整天。"},
    ],
    "甜品": [
        {"title": "杨枝甘露的完美配比", "content": "杨枝甘露是港式甜品经典。芒果要选台农芒，椰浆用佳乐牌最佳。\n\n完美配比：芒果肉300g打泥，椰浆200ml，牛奶100ml，西米50g煮至透明，西柚剥粒点缀。冷藏2小时后享用，清甜丝滑。"},
        {"title": "双皮奶的两层奶皮怎么形成", "content": "双皮奶的关键是两层奶皮。第一层：全脂牛奶加热到边缘冒泡，倒入碗中放凉，表面结皮。\n\n第二层：牛奶加蛋清、糖搅匀，从碗边缓缓倒回，第一层奶皮浮起。蒸15分钟，第二层奶皮形成。入口即化，奶香浓郁。"},
        {"title": "芋圆怎么做才Q弹", "content": "芋圆Q弹的秘诀：芋头蒸熟压泥，趁热加入木薯淀粉揉成团。芋头和木薯淀粉比例约2:1。\n\n搓成长条切小段，水开后下锅，浮起后再煮2分钟。过冷水让口感更Q弹。搭配红豆汤或奶茶都很好吃。"},
    ],
    "小吃": [
        {"title": "煎饼果子的薄脆怎么做", "content": "煎饼果子的灵魂是薄脆。面粉加少许盐和水揉成面团，擀成极薄的面片，切成长方形，油炸至金黄膨起。\n\n摊煎饼：绿豆面糊摊在平底锅上，打一个鸡蛋摊匀，翻面，刷甜面酱和辣酱，放薄脆和葱花，卷起来。外软内脆，越嚼越香。"},
        {"title": "臭豆腐为什么闻着臭吃着香", "content": "长沙臭豆腐用苋菜梗发酵的卤水浸泡，产生独特的'臭'味。高温油炸后，外皮酥脆，内里嫩滑。\n\n浇上蒜汁、辣椒、香菜调配的酱汁，臭味转化成鲜香。外焦里嫩，一口一个停不下来。"},
        {"title": "烤冷面的酱料配方", "content": "东北烤冷面，酱料是关键。配方：蒜蓉辣酱2勺、甜面酱1勺、番茄酱1勺、白糖半勺、醋少许、水适量调匀。\n\n冷面在铁板上煎至微焦，打鸡蛋摊匀，翻面刷酱，加火腿肠和香菜，卷起来切段。酸甜微辣，Q弹有嚼劲。"},
        {"title": "鸡蛋灌饼的鼓包秘诀", "content": "鸡蛋灌饼要鼓起来，面团是关键。半烫面（一半开水一半冷水和面），擀成圆饼，刷油酥对折再擀。\n\n平底锅烙至一面金黄翻面，饼会自动鼓起大泡。戳开一个口灌入蛋液，两面煎至金黄。外酥内软，蛋香四溢。"},
    ],
    "鲁菜": [
        {"title": "糖醋鲤鱼的浇汁时机", "content": "鲁菜糖醋鲤鱼，鱼要炸到外酥里嫩，浇汁时机最关键。糖醋汁要大火熬到起大泡变浓稠，趁热浇在刚出锅的鱼上。\n\n滋啦一声，酸甜汁渗入酥脆外皮，鱼肉嫩滑多汁。造型要昂首翘尾，寓意鲤鱼跳龙门。"},
    ],
    "浙菜": [
        {"title": "东坡肉的文火慢炖", "content": "东坡肉讲究'慢着火，少着水，火候足时它自美'。五花肉切大块，焯水后放入砂锅。\n\n加黄酒、酱油、冰糖、葱姜，小火炖2小时。肉皮朝下码好，炖到筷子一戳即透。肥而不腻，入口即化。"},
    ],
    "闽菜": [
        {"title": "佛跳墙的十八种食材", "content": "佛跳墙是闽菜之王，汇集十八种珍贵食材：鲍鱼、海参、鱼翅、花胶、瑶柱、鸽蛋、香菇、竹笋等。\n\n所有食材分别处理后放入坛中，加高汤和绍兴酒，小火煨制5小时。开坛那一刻，香气四溢，连佛都跳墙来尝。"},
    ],
    "徽菜": [
        {"title": "臭鳜鱼的腌制时间", "content": "徽州臭鳜鱼，腌制是灵魂。新鲜鳜鱼抹上盐，放入木桶中，压上石头，腌制6-7天。\n\n腌好的鱼肉质紧实，闻起来有特殊气味。热油煎至两面金黄，加笋片、辣椒红烧。蒜瓣状的鱼肉鲜美无比。"},
    ],
}


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
        qs = urllib.parse.urlencode(params)
        url += f"?{qs}"
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def api_put(path, data, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(f"{BASE_URL}{path}", data=body, headers=headers, method="PUT")
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def ensure_user(username, password, nickname, role):
    """确保用户存在，返回 accessToken。幂等：已存在则登录。"""
    # 尝试注册
    res = api_post("/api/auth/register", {
        "username": username,
        "password": password,
        "nickname": nickname,
        "role": role,
    })
    if res.get("code") == 200:
        print(f"  + 注册成功: {nickname}（{username}）")
        return res["data"]["accessToken"]

    # 已存在则登录
    login_res = api_post("/api/auth/login", {
        "username": username,
        "password": password,
    })
    if login_res.get("code") == 200:
        print(f"  ✓ 已存在: {nickname}（{username}），已登录")
        return login_res["data"]["accessToken"]

    print(f"  ✗ 失败: {nickname} - {res.get('message')}")
    return None


def ensure_merchant(token, merchant_info):
    """确保商家入驻成功，返回可能更新的 token。幂等：已入驻则跳过。"""
    res = api_post("/api/merchant/apply", {
        "shopName": merchant_info["shopName"],
        "shopDesc": merchant_info["shopDesc"],
        "category": merchant_info["category"],
        "address": merchant_info["address"],
        "logo": "",
    }, token=token)

    if res.get("code") == 200:
        new_token = res["data"].get("accessToken") if res.get("data") else None
        print(f"  + 入驻成功: {merchant_info['shopName']}")
        return new_token or token

    # 已入驻的情况
    msg = res.get("message", "")
    if "已提交" in msg or "已入驻" in msg:
        # 已入驻，重新登录获取带商家角色的 token
        print(f"  ✓ 已入驻: {merchant_info['shopName']}")
        return token  # token 角色可能已是商家，后续发布文章会验证

    print(f"  ✗ 入驻失败: {merchant_info['shopName']} - {msg}")
    return token


def make_cover(title):
    """根据标题生成固定封面图 URL"""
    seed = hashlib.md5(title.encode()).hexdigest()[:8]
    return f"https://picsum.photos/seed/{seed}/400/300"


def find_article_by_title(token, title):
    """通过关键词搜索文章，返回匹配的文章或 None"""
    res = api_get("/api/article/list", {"keyword": title, "current": 1, "size": 50}, token=token)
    if res.get("code") == 200:
        for a in res["data"]["records"]:
            if a["title"] == title:
                return a
    return None


def ensure_article(token, article_data, category):
    """发布文章，已存在则补封面图。返回 ('created'|'updated'|'exists'|'failed', title)"""
    title = article_data["title"]
    cover = make_cover(title)

    # 先查是否已存在
    existing = find_article_by_title(token, title)
    if existing:
        if not existing.get("coverImage"):
            # 补封面图
            api_put(f"/api/article/{existing['id']}", {"coverImage": cover}, token=token)
            return "updated", title
        return "exists", title

    # 新建
    res = api_post("/api/article", {
        "title": title,
        "content": article_data["content"],
        "category": category,
        "coverImage": cover,
        "status": 1,
    }, token=token)
    return ("created", title) if res.get("code") == 200 else ("failed", title)


def main():
    print("=" * 55)
    print("      美食平台测试数据初始化")
    print("=" * 55)

    # ---- 1. 创建商家并入驻 ----
    print("\n【1/3】创建商家用户并入驻...")
    merchant_tokens = []
    for m in MERCHANTS:
        token = ensure_user(m["username"], m["password"], m["nickname"], 1)
        if token:
            token = ensure_merchant(token, m)
        merchant_tokens.append(token)

    # ---- 2. 创建学生用户 ----
    print("\n【2/3】创建学生用户...")
    student_tokens = []
    for s in STUDENTS:
        token = ensure_user(s["username"], s["password"], s["nickname"], 0)
        student_tokens.append(token)

    # ---- 3. 发布文章 ----
    print("\n【3/3】发布美食文章...")
    stats = {"created": 0, "updated": 0, "exists": 0, "failed": 0}
    for i, m in enumerate(MERCHANTS):
        token = merchant_tokens[i]
        if not token:
            print(f"  跳过 {m['shopName']}（无有效 token）")
            continue

        # 登录获取最新 token（确保角色正确）
        login_res = api_post("/api/auth/login", {
            "username": m["username"],
            "password": m["password"],
        })
        if login_res.get("code") == 200:
            token = login_res["data"]["accessToken"]

        category = m["category"]
        articles = ARTICLES_BY_CATEGORY.get(category, [])
        for art in articles:
            status, title = ensure_article(token, art, category)
            stats[status] += 1
            if status == "created":
                print(f"  + [{category}] {title}")
            elif status == "updated":
                print(f"  ~ [{category}] {title}（已补封面图）")
            elif status == "exists":
                print(f"  ✓ [{category}] {title}")
            else:
                print(f"  ✗ [{category}] {title}")
            time.sleep(0.05)

    # ---- 4. 发表评论 ----
    print("\n【补充】发表测试评论...")
    comment_stats = 0
    # 给前几篇文章添加评论
    sample_comments = [
        "看起来好好吃！必须试试",
        "做法写得很详细，感谢分享",
        "上次做过一次，味道绝了",
        "收藏了，周末就做",
        "这家店在哪里？想去尝尝",
        "图片好诱人啊",
        "学到了，原来秘诀在这里",
        "这个配方靠谱吗？试过的说一下",
    ]
    sample_replies = [
        "同问！",
        "确实好吃，推荐",
        "我也想知道",
        "做过了，很成功！",
    ]
    # 获取一些文章ID
    articles_res = api_get("/api/article/list", {"current": 1, "size": 5}, token=student_tokens[0])
    article_ids = []
    if articles_res.get("code") == 200:
        article_ids = [a["id"] for a in articles_res["data"]["records"][:3]]

    for i, st in enumerate(STUDENTS):
        token = student_tokens[i]
        if not token:
            continue
        for j, article_id in enumerate(article_ids):
            if j % len(STUDENTS) != i:
                continue
            content = sample_comments[(i + j) % len(sample_comments)]
            res = api_post("/api/comment", {
                "articleId": article_id,
                "content": content,
            }, token=token)
            if res.get("code") == 200:
                comment_stats += 1
                print(f"  + {st['nickname']} 评论了文章#{article_id}: {content[:20]}...")
                # 给部分评论添加回复
                if j == 0 and i < len(STUDENTS) - 1:
                    reply_token = student_tokens[i + 1]
                    reply_content = sample_replies[i % len(sample_replies)]
                    parent_id = res["data"]["id"]
                    reply_res = api_post("/api/comment", {
                        "articleId": article_id,
                        "content": reply_content,
                        "parentId": parent_id,
                    }, token=reply_token)
                    if reply_res.get("code") == 200:
                        comment_stats += 1
                        print(f"  + {STUDENTS[i+1]['nickname']} 回复了: {reply_content[:20]}...")
            time.sleep(0.05)

    # ---- 汇总 ----
    print("\n" + "=" * 55)
    print("  初始化完成！")
    print(f"  商家: {sum(1 for t in merchant_tokens if t)} 个")
    print(f"  学生: {sum(1 for t in student_tokens if t)} 个")
    print(f"  文章: 新建 {stats['created']} 篇 | 补图 {stats['updated']} 篇 | 已有 {stats['exists']} 篇")
    print(f"  评论: {comment_stats} 条")
    print()
    print("  所有账号密码: 123456")
    print()
    print("  商家账号:")
    for m in MERCHANTS:
        print(f"    {m['username']:16s}  {m['shopName']}（{m['category']}）")
    print()
    print("  学生账号:")
    for s in STUDENTS:
        print(f"    {s['username']:16s}  {s['nickname']}")
    print("=" * 55)


if __name__ == "__main__":
    main()
