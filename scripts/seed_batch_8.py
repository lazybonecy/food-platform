"""批次8：徽菜10篇"""
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
{"title":"毛豆腐的发酵与煎制","content":"毛豆腐是徽州特色。豆腐切块放在竹匾上，撒上菌种，在15-20度环境中发酵3-5天。豆腐表面长出白色菌丝，像裹了一层白毛。\n\n平底锅多放油，毛豆腐煎至两面金黄。菌丝遇热收缩，形成一层酥脆外皮。蘸辣椒酱吃，外酥内嫩，豆腐的鲜味和菌香融合。黄山屯溪老街上到处都有卖，现煎现吃，一块钱一个。","category":"徽菜"},
{"title":"徽州一品锅的层次摆放","content":"一品锅是徽州传统火锅。铁锅从下往上分层码放：最底层干笋，第二层肉块，第三层豆腐，第四层肉圆，第五层粉丝，最上层鸡蛋饺和青菜。\n\n每层加少许盐和酱油调味。加高汤没过食材，大火烧开转小火炖1小时。越底层越入味。吃的时候从上往下一层层吃，每层风味不同。这道菜是徽州人过年的大菜，一锅上桌，全家围坐。","category":"徽菜"},
{"title":"黄山烧饼的梅干菜馅","content":"黄山烧饼又叫蟹壳黄。梅干菜泡软切碎，五花肉切丁，加酱油、糖、芝麻拌匀成馅。\n\n面团发好分小剂子，包入馅料收口压扁。表面刷饴糖水，撒白芝麻。贴在炭火炉壁上烤至金黄鼓起。外皮酥脆掉渣，梅干菜肉馅咸香适口。黄山人出门远行必带烧饼，耐存放又好吃。刚出炉的最香，隔天回炉烤一下依然酥脆。","category":"徽菜"},
{"title":"徽州刀板香的腌制方法","content":"刀板香是徽州腊肉。新鲜五花肉抹上盐和花椒，腌制一周后挂起来风干。关键在风干环境：要通风但不能暴晒。\n\n风干两周后，肉表面发硬，切开内部红白分明。蒸熟切片，肥肉透明如琥珀，瘦肉深红。放在木板上切（所以叫刀板香），肉香和木香混合。徽州人冬天家家户户做刀板香，是待客的硬菜。","category":"徽菜"},
{"title":"问政山笋的春季时令","content":"问政山笋是徽州春季名菜。问政山在歙县，产的竹笋特别鲜嫩。春笋剥壳切滚刀块。\n\n腊肉切片先煸出油，加笋块翻炒。加酱油、少许糖、高汤，小火焖15分钟。笋吸收了腊肉的油香，腊肉有了笋的清鲜。徽州人说：春天不吃笋，枉在徽州住。这道菜时令性极强，过了春天就吃不到了。","category":"徽菜"},
{"title":"太和板面的摔面技艺","content":"太和板面是安徽名面。面团要硬，醒面30分钟。取一块面团在案板上反复摔打拉伸，面条越摔越韧。\n\n摔好的面条宽而薄，下锅煮熟。浇头用牛肉丁加辣椒、花椒、八角、豆瓣酱炒制。浇头浓香，面条劲道。太和板面的关键在摔面，摔得越久面条越有嚼劲。街头面馆里，师傅摔面的啪啪声就是最好的广告。","category":"徽菜"},
{"title":"淮南牛肉汤的汤底熬制","content":"淮南牛肉汤是安徽人的早餐。牛骨和牛肉冷水下锅焯去血沫，重新加水大火烧开。\n\n加姜片、八角、桂皮、花椒，小火熬4小时以上。汤色乳白，牛肉软烂。粉丝烫熟放入碗中，切几片牛肉铺上，浇上滚烫的牛骨汤。加香菜、蒜苗、辣椒油。一碗牛肉汤配一个烧饼，淮南人每天早上的标配。冬天喝一碗暖到心里。","category":"徽菜"},
{"title":"绩溪一品锅的蛋饺制作","content":"绩溪一品锅里的蛋饺是亮点。鸡蛋打散，平底锅刷薄油，舀一勺蛋液摊成薄皮。\n\n肉馅放蛋皮一侧，对折成饺子状。蛋饺金黄小巧，包着鲜肉馅。一品锅中蛋饺放在最上层，蒸的时候蛋香渗入下层食材。绩溪人过年必须有一品锅，蛋饺形似元宝，寓意招财进宝。一个蛋饺一口肉，满嘴幸福。","category":"徽菜"},
{"title":"徽州圆子的肉馅打制","content":"徽州圆子是宴席必备。猪肉馅肥瘦比3:7，加姜末、盐、少许水沿一个方向搅打上劲。打到肉馅起胶粘稠。\n\n搓成丸子，滚上糯米（提前泡4小时）。放入蒸笼大火蒸20分钟。糯米吸收肉汁变得晶莹，肉圆鲜嫩多汁。咬开糯米壳，肉香四溢。徽州人办酒席，圆子是头道菜，寓意团团圆圆。","category":"徽菜"},
{"title":"五城茶干的卤制入味","content":"五城茶干是休宁特产。豆腐干压紧实，切成方块。卤汁用酱油、八角、桂皮、丁香、甘草、冰糖熬制。\n\n豆腐干放入卤汁中小火卤2小时，关火继续浸泡4小时。卤好的茶干深褐色，紧实有嚼劲。切片当冷菜，或者配稀饭。越嚼越香，豆香和卤香融合。真空包装可保存一个月，是黄山旅游必买的伴手礼。","category":"徽菜"},
]

def main():
    print("=== 批次8：徽菜10篇 ===")
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

    cok=0
    for aid in aids:
        r=api_post("/api/coupon",{"articleId":aid,"title":"徽菜专享优惠","description":"凭此券可享受徽菜立减优惠","type":1,"threshold":20.0,"discount":5.0,"originalPrice":0,"totalCount":60,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
