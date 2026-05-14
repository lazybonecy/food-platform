"""批次2：湘菜10篇"""
import json,time,hashlib,urllib.request,urllib.error,urllib.parse
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
    "拍照发朋友圈获赞无数","男朋友说比餐厅还好吃","妈妈尝了直夸好","做了两大盘都不够吃",
]

ARTICLES=[
{"title":"湖南腊肉的烟熏工艺","content":"湖南腊肉是时间的味道。新鲜五花肉抹上盐、花椒、八角，腌制三天后挂起来烟熏。熏料用锯末加橘皮柏树枝，小火慢熏一个月。\n\n熏好的腊肉表皮金黄，肥肉透明，瘦肉深红。切薄片蒸着吃，或者炒蒜苗荷兰豆，烟熏香气扑鼻。湖南人过年必备腊肉，一块腊肉能吃整个正月。切记不要洗掉表面的烟熏层，那是精华。","category":"湘菜"},
{"title":"湖南辣椒炒肉的正确顺序","content":"湖南辣椒炒肉，看似简单实则讲究。肉选五花肉，切薄片用生抽腌10分钟。辣椒选螺丝椒，斜刀切段。\n\n关键顺序：先炒肉！锅烧热不放油，五花肉片下锅煸出油，煸到微焦盛出。用锅里的油炒辣椒，加蒜片豆豉，辣椒炒到虎皮状。肉片回锅，淋生抽翻炒几下出锅。肉香椒香混在一起，配上白米饭，湖南人能吃三碗。","category":"湘菜"},
{"title":"毛氏红烧肉为什么放辣椒","content":"毛氏红烧肉和普通红烧肉的区别：放辣椒。五花肉切大方块，焯水后冰糖炒糖色，肉块翻炒上色。\n\n加干辣椒、八角、桂皮、姜片，倒入料酒和生抽，加水没过肉。大火烧开转小火炖一个半小时。辣椒不是为了辣，而是提鲜增香。成品色泽红亮，肥而不腻，辣中带甜。毛主席最爱这道菜，所以叫毛氏红烧肉。","category":"湘菜"},
{"title":"永州血鸭的制作关键","content":"永州血鸭是湘南名菜。鸭子切块，热油爆炒至出油，加姜蒜辣椒八角桂皮炒香。\n\n关键步骤：杀鸭时接住鸭血，加少许醋防止凝固。鸭肉炒好后，将鸭血淋入锅中，快速翻炒让每块鸭肉裹上血。血遇热凝固，包裹鸭肉形成一层膜，锁住肉汁。成品暗红油亮，鸭肉鲜嫩，血香浓郁。这道菜外地人不敢吃，湖南人爱不释手。","category":"湘菜"},
{"title":"湘西外婆菜的腌制方法","content":"湘西外婆菜是开胃神器。原料：大头菜、萝卜、豆角、辣椒，全部切碎。\n\n腌制方法：切碎的菜加盐揉搓出水，挤干后加蒜末、花椒粉、白酒拌匀。装入坛中密封，阴凉处放一个月。吃的时候取出炒肉末，加辣椒和蒜末爆炒。酸辣咸香，下饭一绝。湘西人出门在外，最想念的就是外婆菜的味道。","category":"湘菜"},
{"title":"湖南糖油粑粑的焦糖化技巧","content":"糖油粑粑是长沙街头小吃。糯米粉加温水揉成团，搓成小圆饼。平底锅多放油，小火煎至两面金黄。\n\n关键在糖浆：白糖加少许水小火熬，不停搅拌到起大泡变琥珀色。粑粑放入糖浆中翻滚，让每个都裹满焦糖。趁热吃，外脆内糯，拉丝效果一流。注意熬糖火候，过了会发苦。冬天来一份热乎乎的糖油粑粑，幸福感爆棚。","category":"湘菜"},
{"title":"常德米粉的牛肉码子做法","content":"常德米粉的灵魂在码子。牛肉码子：牛腩切块焯水，锅中热油炒豆瓣酱和辣椒，加牛肉块翻炒上色。\n\n加八角桂皮香叶，倒入没过牛肉的水，小火炖两小时。炖到牛肉软烂，汤汁浓稠。米粉烫熟捞出，浇上一大勺牛肉码子，再加骨头汤。牛肉软烂入味，汤底浓郁，米粉爽滑。常德人说：一天不嗦粉，浑身不自在。","category":"湘菜"},
{"title":"湘味啤酒鸭的做法","content":"啤酒鸭是湖南家常菜。鸭子斩块焯水，锅中热油，鸭块煸炒至出油微焦。\n\n加姜蒜干辣椒八角炒香，倒入一整瓶啤酒，加生抽老抽冰糖。大火烧开转中火炖30分钟，收汁至浓稠。啤酒去腥增香，鸭肉鲜嫩多汁。配菜可以加土豆块或魔芋，吸满汤汁更好吃。夏天配冰啤，绝了。","category":"湘菜"},
{"title":"湖南擂辣椒皮蛋的正确做法","content":"擂辣椒皮蛋是湖南人的下饭神器。青辣椒在炭火上烤到表皮焦黑，撕去焦皮。皮蛋剥壳。\n\n将烤辣椒和皮蛋放入擂钵中，加蒜瓣盐，用擂棒捣碎。不需要太细，保留一些颗粒感。加少许香油拌匀。辣椒的焦香和皮蛋的独特风味融合，配上白粥或米饭，湖南人一顿能吃两碗。这道菜的灵魂在于炭火烤辣椒，微波炉替代不了。","category":"湘菜"},
{"title":"湖南酱板鸭的风干与卤制","content":"酱板鸭是湖南特产。鸭子处理干净，用盐花椒五香粉腌制一天。挂起来风干三天，让表皮收紧。\n\n卤制：老卤加酱油冰糖八角桂皮干辣椒，放入风干的鸭子小火卤一个半小时。捞出后再风干一天。成品色泽深红，肉质紧实，越嚼越香。切块当零食吃，或者配啤酒，都是绝配。真空包装可以保存一个月。","category":"湘菜"},
]

def main():
    print("=== 批次2：湘菜10篇 ===")
    tk=login("chef_zhang","123456")
    if not tk: print("登录失败"); return
    aids=[]
    for a in ARTICLES:
        r=api_post("/api/article",{"title":a["title"],"content":a["content"],"category":a["category"],"coverImage":mkcover(a["title"]),"status":1},token=tk)
        if r.get("code")==200:
            aids.append(r["data"])
            print(f"  + [{a['category']}] {a['title']} (id={r['data']})")
        else: print(f"  x {a['title']}: {r.get('message')}")
        time.sleep(0.05)

    # 优惠券
    cok=0
    for aid in aids:
        r=api_post("/api/coupon",{"articleId":aid,"title":"湘菜美食专享优惠","description":"凭此券可享受湘菜立减优惠","type":1,"threshold":20.0,"discount":5.0,"originalPrice":0,"totalCount":100,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
        if r.get("code")==200: cok+=1
        time.sleep(0.05)

    # 评论
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
