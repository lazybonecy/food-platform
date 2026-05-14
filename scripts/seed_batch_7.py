"""批次7：闽菜10篇"""
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
{"title":"沙茶面的沙茶酱调配","content":"沙茶面是厦门的灵魂小吃。沙茶酱是关键：花生碎、虾米、蒜蓉、辣椒粉、芝麻、五香粉，用油慢慢熬制融合。\n\n面条用碱水面，配料丰富：猪肝、大肠、鱿鱼、豆腐干、青菜。高汤烧开，放入沙茶酱搅匀。面条煮熟捞入碗中，浇上沙茶汤底，摆上各种配料。沙茶酱的浓郁鲜香渗入每根面条，配料各有风味。厦门人早餐就靠这碗面。","category":"闽菜"},
{"title":"蚵仔煎的海蛎怎么选","content":"蚵仔煎是闽南经典小吃。海蛎要选新鲜小粒的，肉质更鲜甜。海蛎洗净沥干，加地瓜粉和少许水搅成糊。\n\n平底锅多放油，倒入海蛎糊摊平，打入鸡蛋。煎至底部金黄翻面。配上甜辣酱和香菜。外皮Q弹（地瓜粉的功劳），内里海蛎鲜嫩多汁。地瓜粉比淀粉更有嚼劲，是蚵仔煎Q弹口感的关键。路边摊一份十块钱，鲜到掉眉毛。","category":"闽菜"},
{"title":"荔枝肉的酸甜裹汁","content":"荔枝肉是福州名菜。猪瘦肉切块，打十字花刀不切断，受热后卷成荔枝状。裹上淀粉糊油炸至金黄。\n\n酱汁：番茄酱、白醋、白糖、少许盐烧开。炸好的荔枝肉倒入翻匀，加荔枝罐头的荔枝点缀。成品形似荔枝，酸甜可口，外酥内嫩。福州人办酒席必有这道菜，好看又好吃。酱汁比例是关键，酸甜要均衡。","category":"闽菜"},
{"title":"厦门姜母鸭的姜母选用","content":"姜母鸭是闽南滋补名菜。鸭子斩块，姜母（老姜）切片用量要大，半斤姜配一只鸭。\n\n麻油烧热，姜片爆至微焦卷曲。鸭块下锅翻炒至出油。加米酒、酱油、冰糖，倒入砂锅小火炖40分钟。姜母的辛辣渗入鸭肉，驱寒暖胃。米酒的醇香和麻油的浓香融合。冬天吃一锅姜母鸭，全身暖和。闽南人坐月子必吃这道菜。","category":"闽菜"},
{"title":"福州肉燕的燕皮怎么打","content":"肉燕是福州特色，燕皮是精华。猪瘦肉用木槌反复捶打成泥，加地瓜粉擀成薄如纸的皮。这个过程叫打燕皮。\n\n燕皮薄到能透光，却有韧性不破。包入猪肉馅，捏成馄饨状。高汤煮熟，加葱花虾米紫菜。燕皮入口即化，肉馅鲜美，汤底清甜。福州人说：肉燕扁肉燕，太平太平宴。过年必吃肉燕，寓意太平。","category":"闽菜"},
{"title":"土笋冻的制作原理","content":"土笋冻是泉州特色小吃。土笋（沙虫）洗净，加水大火煮沸。沙虫体内的胶原蛋白溶出，冷却后自然凝固成冻。\n\n切块后蘸酱油、醋、蒜泥、辣椒酱食用。土笋冻晶莹剔透，口感Q弹爽滑。蘸料的酸辣和冻的鲜美形成对比。这道小吃外地人不敢吃，但泉州人爱不释手。夏天冰镇后食用更佳，清凉解暑。","category":"闽菜"},
{"title":"闽南卤面的卤汤做法","content":"闽南卤面是宴席必备。卤汤用猪骨、虾壳、香菇熬制2小时。加入扁食（馄饨）、猪肝、大肠、鱿鱼、鸡蛋。\n\n面条用碱水面，煮至七成熟捞入碗中。浇上滚烫的配料和汤底。面条吸收了各种食材的鲜味，汤底浓郁醇厚。闽南人办喜事必须上卤面，寓意长长久久。一碗卤面十几种配料，丰盛热闹。","category":"闽菜"},
{"title":"泉州面线糊的面线怎么煮","content":"面线糊是泉州人的早餐。面线很细，容易烂，所以要快煮。高汤烧开，放入面线，搅拌30秒即关火。\n\n配料自选：醋肉、大肠、猪肝、虾仁、海蛎、豆腐。面线糊要糊而不烂，汤底要鲜。加胡椒粉和葱花提味。醋肉是灵魂配料，猪肉加醋腌制后油炸，酸香酥脆。一碗面线糊配一根油条，泉州人的幸福早餐。","category":"闽菜"},
{"title":"福建佛跳墙的食材处理","content":"佛跳墙食材众多，每种都要单独处理。鲍鱼泡发两天，海参泡发三天。鱼翅去腥，花胶泡软。鸽蛋煮熟剥壳，香菇泡发。\n\n所有食材分别焯水去腥。坛中先放猪蹄鸡块垫底，再层层码放各种食材。加高汤和绍兴酒，密封坛口。小火煨5小时，不能开盖。开坛时酒香混合食材香气扑鼻。每一勺都是精华，汤汁浓稠如琼浆。","category":"闽菜"},
{"title":"闽南醋肉的腌制与炸制","content":"醋肉是泉州特色小吃。猪瘦肉切条，用永春老醋、酱油、五香粉、白糖腌制2小时。醋是灵魂，必须用永春老醋。\n\n腌好的肉条裹上地瓜粉，中火炸至金黄捞出。复炸一次逼油，外皮更酥脆。醋肉外酥内嫩，酸香开胃。可以直接当零食吃，也可以配面线糊。泉州人过年炸一大盆醋肉，来客人就端出来，越放越入味。","category":"闽菜"},
]

def main():
    print("=== 批次7：闽菜10篇 ===")
    tk=login("chef_li","123456")
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
        r=api_post("/api/coupon",{"articleId":aid,"title":"闽菜专享优惠","description":"凭此券可享受闽菜立减优惠","type":1,"threshold":25.0,"discount":6.0,"originalPrice":0,"totalCount":60,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
