"""批次5：鲁菜10篇"""
import json,time,hashlib,urllib.request,urllib.error
BASE_URL="http://localhost:8080"

def api_post(path,data,token=None):
    h={"Content-Type":"application/json"}
    if token:h["Authorization"]=f"Bearer {token}"
    r=urllib.request.Request(f"{BASE_URL}{path}",data=json.dumps(data).encode(),headers=h,method="POST")
    try:
        with urllib.request.urlopen(r) as resp:return json.loads(resp.read())
    except urllib.error.HTTPError as e:return json.loads(e.read())

def login(u,p):
    r=api_post("/api/auth/login",{"username":u,"password":p})
    return r["data"]["accessToken"] if r.get("code")==200 else None

def mkcover(t):
    return f"https://picsum.photos/seed/{hashlib.md5(t.encode()).hexdigest()[:8]}/400/300"

COMMENTS=[
    "看完口水直流，必须试试这个做法！","收藏了，这周末就做给家人吃","终于找到正宗做法了，感谢分享",
    "这个配方太靠谱了，做出来跟饭店一样","图片好诱人，食欲大开","原来秘诀在这里，学到了",
    "这家店就在学校旁边，经常去吃","价格实惠分量足，学生党最爱","推荐给了室友，都说好吃",
    "做了好多次了，每次都成功","请问用什么牌子的调料比较好","有没有视频教程啊",
    "夏天吃这个太开胃了","冬天来一碗暖暖的","减肥期间能吃吗","热量高不高啊",
    "适合带便当吗","小朋友能吃吗","老人牙口不好能做吗","素食版本怎么做",
    "可以用空气炸锅吗","电饭煲能做吗","宿舍党能做吗","新手第一次做就成功了",
    "比外卖好吃多了","自己做干净卫生","成本算下来很便宜","聚会做了一桌全吃光",
]

ARTICLES=[
{"title":"葱烧海参的葱油熬制","content":"葱烧海参是鲁菜代表。海参泡发好，大葱切段。关键在葱油：大葱白切段，冷油下锅，小火慢慢炸至金黄焦香。捞出葱段，葱油留用。\n\n海参焯水，锅中加葱油烧热，放入海参和炸过的葱段。加高汤、酱油、料酒、白糖，小火煨10分钟让海参入味。勾芡收汁，淋葱油出锅。海参软糯弹滑，葱香浓郁，汤汁醇厚。这道菜考验厨师对火候的掌控。","category":"鲁菜"},
{"title":"九转大肠的五味调和","content":"九转大肠是鲁菜功夫菜。大肠反复清洗去异味，焯水后切段。锅中加油炸至表面微焦。\n\n五味调和是精髓：酸（醋）、甜（糖）、苦（砂仁）、辣（胡椒）、咸（酱油）。锅中加油炒糖色，放入大肠翻炒。加五味调料和高汤，小火煨20分钟。大火收汁，撒香菜末。成品色泽红润，五味俱全，大肠软嫩不腻。这道菜工序复杂，是考验厨师的试金石。","category":"鲁菜"},
{"title":"油焖大虾的虾油提取","content":"油焖大虾是胶东名菜。大虾剪去虾须虾脚，挑出虾线。虾头不要扔，虾油从虾头来。\n\n锅中多放油，虾头放入小火慢炸，按压虾头挤出红色虾油。捞出虾头，虾油留用。大虾放入虾油中煎至两面变红，加料酒、酱油、白糖、少许醋。盖盖焖3分钟，大火收汁。虾壳红亮，虾肉鲜嫩，虾油的鲜味渗入每一寸虾肉。","category":"鲁菜"},
{"title":"拔丝地瓜的糖浆拉丝技巧","content":"拔丝地瓜是鲁菜经典甜菜。地瓜切滚刀块，油炸至金黄熟透。关键在拔丝糖浆：白糖加少许水，中火熬制。\n\n不能搅拌！只能晃动锅。糖浆从大泡变小泡，颜色从白变黄，用筷子挑起能拉出细丝时立即关火。地瓜倒入快速翻匀，蘸冷水定型。上桌后趁热夹起，能拉出长长的金丝。蘸冷水吃，外脆内糯。动作要快，糖浆冷了就拔不出丝了。","category":"鲁菜"},
{"title":"德州扒鸡的卤制秘方","content":"德州扒鸡是山东名吃。整鸡造型：双腿交叉插入腹腔，双翅从脖子处交叉别好。抹酱油炸至金黄上色。\n\n卤制秘方：老汤加八角、桂皮、花椒、丁香、砂仁、白芷、草果等十几种香料。鸡放入卤锅，大火烧开转小火卤4小时。关火后继续浸泡2小时。成品色泽金黄，骨酥肉烂，轻轻一抖骨肉分离。冷吃热吃皆宜，越嚼越香。","category":"鲁菜"},
{"title":"爆炒腰花的去腥与火候","content":"爆炒腰花是鲁菜快炒代表。猪腰对半切开，去掉白色腰臊（腥味来源）。打花刀：斜切不切断，再交叉切，受热后卷成麦穗状。\n\n腌制去腥：料酒、姜片、淀粉抓匀腌10分钟。锅烧到冒烟，宽油快炒，腰花入锅10秒即捞出。另起锅炒蒜片木耳笋片，腰花回锅，淋酱油醋翻匀出锅。全程不超过1分钟，腰花才嫩。火候过了就老了嚼不动。","category":"鲁菜"},
{"title":"锅包肉的两次油炸法","content":"锅包肉是东北鲁菜融合的经典。猪里脊切薄片，用盐和料酒腌制。土豆淀粉加水调成浓浆，肉片挂浆。\n\n两次油炸：第一次中火炸至定型捞出。第二次大火复炸30秒，逼出多余油分，外皮更酥脆。糖醋汁：白醋、白糖、少许盐熬至起泡。炸好的肉片倒入快速翻匀，撒香菜胡萝卜丝。外酥里嫩，酸甜可口，咬开咔嚓响。","category":"鲁菜"},
{"title":"四喜丸子的松软不散秘诀","content":"四喜丸子是鲁菜宴席大菜。猪肉馅肥瘦比3:7，加葱姜水、鸡蛋、淀粉、盐、胡椒粉。关键：沿一个方向搅打上劲，让肉馅产生黏性。\n\n团成四个大丸子，油炸至表面定型金黄。砂锅底部垫白菜叶，丸子放入，加高汤、酱油、料酒、冰糖。大火烧开转小火炖1小时。丸子松软不散，入口即化，汤汁浓郁。逢年过节必做，寓意四季平安。","category":"鲁菜"},
{"title":"糟溜鱼片的糟卤调制","content":"糟溜鱼片是鲁菜清雅之作。鱼片用鲈鱼或鳜鱼，斜刀切薄片，蛋清淀粉上浆。温油滑熟至白色。\n\n糟卤是灵魂：香糟加料酒泡出糟汁，过滤取汁。锅中加高汤、糟汁、盐、糖烧开，放入鱼片轻轻推匀。勾薄芡，淋明油出锅。鱼片洁白如玉，糟香幽雅，口感嫩滑。这道菜不辣不酸，是鲁菜中难得的清淡口味。","category":"鲁菜"},
{"title":"糖醋里脊的面糊配方","content":"糖醋里脊的外皮要酥脆，面糊是关键。配方：淀粉和面粉2:1，加一个鸡蛋，少许水调成浓稠面糊。能挂在筷子上缓慢滴落。\n\n里脊切条腌制，挂糊后油炸。两次炸：中火炸熟，大火复炸逼油。糖醋汁：番茄酱、白醋、白糖、少许盐，大火熬至起泡。里脊倒入快速翻匀。外皮酥脆裹满酸甜汁，里脊肉嫩多汁。趁热吃，凉了皮会软。","category":"鲁菜"},
]

def main():
    print("=== 批次5：鲁菜10篇 ===")
    tk=login("chef_wang","123456")
    if not tk: print("登录失败"); return
    aids=[]
    for a in ARTICLES:
        r=api_post("/api/article",{"title":a["title"],"content":a["content"],"category":a["category"],"coverImage":mkcover(a["title"]),"status":1},token=tk)
        if r.get("code")==200:
            aids.append(r["data"])
            print(f"  + [{a['category']}] {a['title']} (id={r['data']})")
        else: print(f"  x {a['title']}: {r.get('message')}")
        time.sleep(0.05)

    cok=0
    for aid in aids:
        r=api_post("/api/coupon",{"articleId":aid,"title":"鲁菜美食专享优惠","description":"凭此券可享受鲁菜立减优惠","type":1,"threshold":30.0,"discount":8.0,"originalPrice":0,"totalCount":60,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
        if r.get("code")==200: cok+=1
        time.sleep(0.05)

    students=[login(f"student_{n}","123456") for n in ["alice","bob","carol","dave","eve"]]
    tcom=0
    for aid in aids:
        for i in range(22):
            st=students[i%5]
            if not st:continue
            r=api_post("/api/comment",{"articleId":aid,"content":COMMENTS[i%len(COMMENTS)]},token=st)
            cid=None
            if r.get("code")==200:
                cid=r.get("data",{}).get("id") if isinstance(r.get("data"),dict) else r.get("data")
                tcom+=1
                if i<6 and cid:
                    api_post("/api/comment",{"articleId":aid,"content":"确实好吃，强烈推荐！","parentId":cid},token=students[(i+1)%5])
                    tcom+=1
            time.sleep(0.03)
    print(f"\n文章:{len(aids)} 优惠券:{cok} 评论:{tcom}")

if __name__=="__main__": main()
