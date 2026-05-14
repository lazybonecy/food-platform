"""批次6：浙菜10篇"""
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
{"title":"西湖醋鱼的糖醋比例","content":"西湖醋鱼是杭州名菜。草鱼饿养两天去土腥味，从背部剖开。水中加姜葱料酒烧开，鱼放入煮8分钟捞出。\n\n糖醋汁是关键：米醋和糖按2:1.5比例，加酱油和少许盐，烧开后勾薄芡。浇在鱼上，糖醋汁要没过鱼身一半。鱼肉嫩滑，酸甜适口，微微带咸。这道菜不油炸，全靠水煮和浇汁，考验厨师对火候和调味的精准把控。","category":"浙菜"},
{"title":"龙井虾仁的茶叶怎么选","content":"龙井虾仁是杭帮菜的雅菜。虾仁去虾线，加蛋清淀粉上浆。龙井茶要选明前龙井，用80度水泡开。\n\n温油滑虾仁至变色，捞出。锅中加少许油，放入泡开的龙井茶叶和茶水，加盐调味。虾仁回锅快速翻匀，茶叶的清香渗入虾仁。成品虾仁洁白如玉，茶叶碧绿点缀，清香扑鼻。这道菜讲究色香味俱全，是文人雅士的最爱。","category":"浙菜"},
{"title":"叫化鸡的泥巴裹法","content":"叫化鸡是杭州传统名菜。整鸡用酱油料酒腌制，腹中塞入香菇笋丁火腿丁。用荷叶包裹严实。\n\n外层裹上黄泥巴，厚度约2厘米，不能有裂缝。放入炭火中烤2小时。泥巴干透后敲开，荷叶香气扑鼻。鸡肉酥烂脱骨，荷叶和泥土的香气渗入鸡肉。这道菜源自乞丐发明，用泥巴烤鸡充饥，没想到味道绝佳，流传至今。","category":"浙菜"},
{"title":"宋嫂鱼羹的酸辣鲜","content":"宋嫂鱼羹是杭州传统名羹。鳜鱼蒸熟取肉，去骨去刺。锅中加高汤烧开，放入鱼肉、火腿丝、香菇丝、笋丝。\n\n调味：加酱油、醋、胡椒粉。勾芡至浓稠，淋入蛋液搅成蛋花。最后加少许香油和香菜。鱼羹酸辣鲜美，口感滑嫩，配料丰富。据说南宋时宋嫂在西湖边卖鱼羹，宋高宗品尝后赞不绝口，从此闻名天下。","category":"浙菜"},
{"title":"片儿川的雪菜笋片","content":"片儿川是杭州人的日常面食。面条用碱水面，配料三样：雪菜、笋片、瘦肉片。雪菜要选倒笃菜，切碎。\n\n锅中加油炒肉片至变色，加笋片和雪菜翻炒出香。加高汤烧开，放入面条煮熟。面条滑爽，雪菜鲜咸，笋片脆嫩，汤底清鲜。杭州人说：没吃过片儿川，不算来过杭州。一碗面，三种料，简单却让人念念不忘。","category":"浙菜"},
{"title":"宁波汤圆的猪油芝麻馅","content":"宁波汤圆的馅料是精髓。黑芝麻炒香打成粉，加猪油和白糖拌匀。猪油要板油熬的，凝固后搓成小球冷冻。\n\n糯米粉加温水揉成团，取一小块压扁，包入冻好的芝麻球，收口搓圆。水开后下汤圆，浮起后再煮2分钟。咬开一个小口，黑芝麻流心涌出，猪油让馅料丝滑浓郁。宁波人过年必吃汤圆，团团圆圆。","category":"浙菜"},
{"title":"绍兴醉鸡的黄酒选用","content":"绍兴醉鸡用的是绍兴黄酒。三黄鸡煮熟过冰水，切块码入碗中。黄酒加盐、糖、少许水调匀，倒入没过鸡肉。\n\n密封冷藏浸泡24小时以上。黄酒的醇香渗入鸡肉，鸡肉的鲜味融入酒汁。吃的时候鸡肉冰凉，酒香扑鼻，皮爽肉嫩。绍兴黄酒选花雕最佳，年份越久香气越醇。这道菜是冷菜中的极品，夏天吃特别爽口。","category":"浙菜"},
{"title":"杭州酱鸭的酱制配方","content":"杭州酱鸭是冬季时令美食。整鸭用盐腌一天，晾干水分。酱油加冰糖、八角、桂皮、花椒烧开放凉。\n\n鸭子放入酱汁中浸泡3天，每天翻面。取出后挂起来风干一周。成品酱色油亮，肉质紧实。蒸熟切块，鸭皮Q弹，鸭肉咸香，越嚼越有味。杭州人冬天家家户户挂酱鸭，是年味的象征。","category":"浙菜"},
{"title":"温州鱼丸的手打方法","content":"温州鱼丸用鮸鱼或马鲛鱼。鱼去骨取肉，用刀背反复剁成鱼泥。加盐和少许水，沿一个方向搅打上劲。\n\n挤成丸子放入温水中定型，再煮熟。鱼丸洁白弹牙，咬开能吃到鱼肉的纤维。汤底用鱼骨熬制，加米醋和胡椒粉，酸辣开胃。温州鱼丸不加淀粉，纯鱼肉打制，所以特别Q弹。街头小店一碗鱼丸汤，是温州人的乡愁。","category":"浙菜"},
{"title":"嘉兴粽子的五花肉腌制","content":"嘉兴粽子以鲜肉粽闻名。五花肉切块，用酱油、白糖、料酒、五香粉腌制过夜。糯米也用酱油和少许盐拌匀腌2小时。\n\n粽叶泡软，包入糯米和腌好的五花肉。肉要埋在糯米中间。大火煮2小时，小火焖1小时。剥开粽叶，糯米吸收了肉汁，油润鲜香，五花肉肥而不腻，入口即化。嘉兴五芳斋的粽子，每年端午供不应求。","category":"浙菜"},
]

def main():
    print("=== 批次6：浙菜10篇 ===")
    tk=login("chef_liu","123456")
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
        r=api_post("/api/coupon",{"articleId":aid,"title":"浙菜专享优惠","description":"凭此券可享受浙菜立减优惠","type":1,"threshold":25.0,"discount":6.0,"originalPrice":0,"totalCount":80,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
