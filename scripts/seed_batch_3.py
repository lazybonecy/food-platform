"""批次3：甜品10篇"""
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
    "拍照发朋友圈获赞无数","男朋友说比餐厅还好吃","妈妈尝了直夸好","做了两大盘都不够吃",
]

ARTICLES=[
{"title":"芒果班戟的完美皮薄馅大","content":"芒果班戟是港式甜品店的招牌。面糊配方：低筋面粉50g、玉米淀粉30g、糖粉30g、鸡蛋2个、牛奶250ml、黄油10g融化。搅匀过筛，静置30分钟。\n\n平底锅小火，舀一勺面糊摊成薄饼，凝固后取出放凉。淡奶油加糖打发至硬性发泡。饼皮中间放奶油和芒果块，四面折叠包好。冷藏2小时后食用，皮薄如纸，奶油绵密，芒果清甜。切开时能看到层次分明的横截面。","category":"甜品"},
{"title":"椰汁西米露的西米怎么煮才透明","content":"椰汁西米露最常见的问题：西米煮不透明。诀窍是水要多、火要大。大锅水烧开，倒入西米，大火煮15分钟，期间不停搅拌防粘连。\n\n煮到西米中心还有小白点时关火，盖盖焖10分钟。白点消失，西米全透明。捞出过冷水，冲洗掉表面淀粉。椰浆加牛奶和糖煮开，加入西米和芒果丁。冷藏后食用，椰香浓郁，西米Q弹。","category":"甜品"},
{"title":"蛋挞液的黄金配比","content":"完美蛋挞液配比：淡奶油100ml、牛奶80ml、糖30g、蛋黄3个。糖加入牛奶中加热至融化，放凉后加入蛋黄搅匀，再加淡奶油混合。\n\n关键：过筛两次去除气泡和蛋筋。蛋挞皮提前解冻，倒入八分满的蛋液。烤箱预热220度，中层烤15-18分钟，表面出现焦斑即可。刚出炉的蛋挞外酥内嫩，蛋香浓郁，咬一口会流心。趁热吃最佳。","category":"甜品"},
{"title":"红豆沙的绵密口感怎么做","content":"红豆沙要绵密无颗粒，选红豆很关键。红豆提前浸泡8小时，加水煮至软烂开花。用料理机打成泥，过筛去皮。\n\n红豆泥倒入不粘锅，加白糖和少许麦芽糖，小火不停翻炒。分三次加入猪油，每次炒匀再加。炒到红豆沙能抱团不粘铲，铲起来缓慢滴落。这个过程需要耐心，至少炒30分钟。做好的红豆沙可以做月饼馅、铜锣烧馅、包子馅。","category":"甜品"},
{"title":"桂花酒酿圆子的桂花怎么选","content":"桂花酒酿圆子是江南经典甜品。桂花要选金桂，香气最浓郁。干桂花用温水泡开，酒酿买现成的或自制。\n\n小圆子用糯米粉加温水揉成团，搓成黄豆大小。水烧开下圆子，浮起后加酒酿和冰糖。最后撒泡好的桂花，淋少许桂花蜜。桂花的清香和酒酿的微甜融合，圆子软糯弹牙。冬天来一碗，暖胃又暖心。","category":"甜品"},
{"title":"提拉米苏的手指饼干替代方案","content":"提拉米苏传统用手指饼干，但不好买。替代方案：用海绵蛋糕切片，或者自己做手指饼干。自制配方：蛋白打发加糖，蛋黄打发，混合后筛入低筋面粉，裱成长条形，180度烤12分钟。\n\n组装：马斯卡彭奶酪加糖和蛋黄打匀，蛋白打发拌入。咖啡液加朗姆酒。一层饼干一层奶酪糊，重复两层。冷藏4小时以上，撒可可粉。咖啡的苦、奶酪的甜、可可的香，层次丰富。","category":"甜品"},
{"title":"芒果糯米糍的不破皮技巧","content":"芒果糯米糍的皮要软糯不破。糯米粉100g、玉米淀粉30g、糖40g、牛奶160ml搅匀，盖保鲜膜蒸20分钟。\n\n蒸熟后加15g黄油揉匀，放凉。手上抹椰蓉防粘，取一小块面团压扁，包入芒果块和奶油，收口搓圆。滚上椰蓉。冷藏2小时后食用，皮薄馅大，芒果鲜甜。注意面团不要擀太薄，否则容易破。","category":"甜品"},
{"title":"红糖糍粑的外脆内糯秘诀","content":"红糖糍粑是火锅店的标配甜品。糯米提前泡4小时，蒸熟后放入石臼捣烂，捣到没有米粒。\n\n取出整形切块，平底锅多放油，小火煎至两面金黄酥脆。红糖加少许水熬成糖浆，浇在糍粑上，撒上黄豆粉。外皮焦脆，内里软糯拉丝，红糖的焦香和黄豆粉的醇香完美搭配。在家也能做，关键是要把糯米捣够时间。","category":"甜品"},
{"title":"冰粉的制作与红糖水配方","content":"冰粉是四川夏日消暑神器。冰粉粉按1:40的比例加沸水，搅拌至完全溶解。放凉后冷藏2小时凝固。\n\n红糖水：红糖加水小火熬化，加少许姜片提香，过滤放凉。冰粉用勺子划成小块，浇上红糖水，加花生碎、芝麻、葡萄干、山楂片。冰冰凉凉，甜而不腻。四川街头巷尾都有卖，一碗才几块钱。","category":"甜品"},
{"title":"芋泥波波奶茶的芋泥怎么做","content":"芋泥波波奶茶的灵魂在芋泥。芋头蒸熟压泥，趁热加炼乳和少许牛奶搅匀。不要打太细，保留一些颗粒感更有口感。\n\n波波（珍珠）煮法：水开后下珍珠，大火煮15分钟，焖10分钟，过冷水。杯底放芋泥和珍珠，加冰块，倒入鲜牛奶。喝前搅匀，芋泥的香甜和牛奶的醇厚融合，珍珠Q弹有嚼劲。比奶茶店的还好喝。","category":"甜品"},
]

def main():
    print("=== 批次3：甜品10篇 ===")
    tk=login("chef_chen","123456")
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
        r=api_post("/api/coupon",{"articleId":aid,"title":"甜品专享优惠","description":"凭此券可享受甜品立减优惠","type":1,"threshold":15.0,"discount":3.0,"originalPrice":0,"totalCount":80,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
