"""批次9：西北菜10篇"""
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
{"title":"兰州拉面的五大要素","content":"兰州拉面讲究一清二白三红四绿五黄。清是汤清，牛骨熬制6小时以上，汤色清澈。白是萝卜白，白萝卜片焯水去辣味。红是辣椒油红，菜籽油烧热浇在辣椒面上。绿是香菜蒜苗绿。黄是面条黄亮。\n\n拉面师傅手工拉制，一团面拉出粗细不同的面条。毛细、细、二细、三细、大宽、韭叶，任你选择。一碗牛肉面，汤鲜面劲，辣椒油香而不辣。兰州人每天早上一碗牛肉面，雷打不动。","category":"西北菜"},
{"title":"新疆大盘鸡的皮带面","content":"新疆大盘鸡必须配皮带面。鸡肉斩块焯水，土豆切块。锅中热油炒糖色，放入鸡块翻炒上色。\n\n加干辣椒、花椒、八角、桂皮、啤酒一罐。大火烧开转中火炖20分钟，加土豆再炖10分钟。皮带面宽如皮带，煮熟铺在盘底。大盘鸡连汤带肉浇在面上。鸡肉鲜嫩，土豆软糯，面条吸满汤汁，又宽又劲道。","category":"西北菜"},
{"title":"新疆烤馕的坑炉制作","content":"馕是新疆人的主食。面粉加酵母、盐、牛奶揉成面团，发酵至两倍大。擀成圆形厚饼，用馕针扎出花纹。\n\n传统做法是贴在馕坑壁上烤制。馕坑烧热后扒去炭火，馕贴上去，盖盖焖烤15分钟。烤好的馕外焦内软，表面金黄，芝麻飘香。有大馕、薄馕、油馕、肉馕等多种。馕耐存放，放几天再烤一下依然好吃。","category":"西北菜"},
{"title":"手抓羊肉的蘸料配方","content":"手抓羊肉是西北硬菜。羊排切块冷水下锅，加姜片花椒去腥。大火烧开撇去浮沫，转小火煮1小时。不加任何调料，保留羊肉原味。\n\n蘸料：蒜泥、辣椒面、盐、醋、酱油调匀。羊肉捞出蘸料吃，原味的鲜甜和蘸料的辛辣形成完美搭配。西北人用手抓着吃，豪迈粗犷。羊肉要选滩羊或小尾寒羊，肉质鲜嫩无膻味。","category":"西北菜"},
{"title":"油泼面的辣子怎么泼","content":"油泼面是陕西人的灵魂。宽面煮熟捞入碗中，铺上蒜末、葱花、辣椒面、花椒粉。辣椒面要粗细混合，粗的香细的辣。\n\n菜籽油烧到冒烟（约220度），一勺热油泼在辣椒面上，滋啦一声，辣椒的香气瞬间爆发。加酱油和醋拌匀。面条宽而劲道，辣椒油香而不辣，蒜香扑鼻。一碗油泼面，简单粗暴，却是最治愈的味道。","category":"西北菜"},
{"title":"羊肉泡馍的掰馍学问","content":"羊肉泡馍在西安是门学问。馍要自己掰，掰成黄豆大小。掰馍是社交活动，边掰边聊。掰得越小越入味，厨师越重视。\n\n掰好的馍交给厨师，加羊肉片、粉丝、木耳，浇上滚烫的羊汤。配糖蒜和辣酱。馍吸满汤汁绵软入味，羊肉鲜嫩，汤底醇厚。西安人说：掰馍要耐心，吃馍要趁热。一碗泡馍下肚，浑身暖和。","category":"西北菜"},
{"title":"biangbiang面的面条有多宽","content":"biangbiang面是陕西特色宽面。面条宽约三指，长约一米。和面时加盐增加筋性，醒面后反复拉伸摔打。\n\n一根面条就是一碗面，又宽又厚又劲道。浇头用西红柿鸡蛋、肉臊子、油泼辣子。面条铺在碗里，浇上各种浇头，拌匀吃。一口面条一口蒜，陕西人说：吃面不吃蒜，味道少一半。这个字笔画最多，电脑打不出来。","category":"西北菜"},
{"title":"凉皮的辣椒油怎么熬","content":"陕西凉皮的辣椒油是灵魂。菜籽油烧到七成热，关火降到五成热。辣椒面要三种混合：秦椒面（香）、线椒面（辣）、灯笼椒面（色）。\n\n油温降到五成热时，先放少许芝麻试温。芝麻不焦即可。分三次浇入辣椒面，每次搅匀。第一次激出辣味，第二次激出香味，第三次激出颜色。好的辣椒油红亮飘香，辣而不燥。凉皮拌上这辣椒油，一碗根本不够。","category":"西北菜"},
{"title":"陕西肉夹馍的腊汁肉配方","content":"腊汁肉夹馍的灵魂在腊汁。腊汁用老汤，越老越香。新起卤水：猪骨、鸡架熬汤，加八角、桂皮、花椒、丁香、草果、砂仁等十几种香料。\n\n五花肉放入卤汁中小火卤3小时，关火浸泡过夜。肉酥烂入味，肥肉透明瘦肉深红。剁碎夹入白吉馍中，浇一勺卤汁。馍酥肉烂，一口下去满嘴流油。正宗的不加青椒香菜，纯肉才是王道。","category":"西北菜"},
{"title":"新疆烤包子的羊肉馅","content":"新疆烤包子用羊肉馅。羊腿肉切丁（不剁碎），加洋葱丁、孜然粉、盐、黑胡椒拌匀。肉丁保留颗粒感，吃起来更香。\n\n面团擀成薄皮，包入羊肉馅，捏成方形。贴在馕坑壁上烤至金黄。外皮酥脆，羊肉鲜嫩多汁，洋葱的甜和孜然的香融合。刚出炉的烤包子烫嘴，但忍不住一口接一口。新疆人说：宁可三日无肉，不可一日无馕。","category":"西北菜"},
]

def main():
    print("=== 批次9：西北菜10篇 ===")
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
        r=api_post("/api/coupon",{"articleId":aid,"title":"西北菜专享优惠","description":"凭此券可享受西北菜立减优惠","type":1,"threshold":20.0,"discount":5.0,"originalPrice":0,"totalCount":80,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
