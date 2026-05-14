"""批次4：小吃10篇"""
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
{"title":"肉夹馍的白吉馍怎么做才酥脆","content":"肉夹馍的灵魂在白吉馍。面粉500g、酵母5g、温水260ml揉成面团，发酵至两倍大。分成小剂子，擀成牛舌状卷起，再擀成圆饼。\n\n平底锅不放油，小火烙至两面微黄，再放入烤箱200度烤5分钟。馍外酥内软，层次分明。卤肉切碎夹入，浇一勺卤汁。馍酥肉烂，一口下去满嘴流油。正宗的肉夹馍不需要青椒香菜，纯肉才是王道。","category":"小吃"},
{"title":"凉皮的洗面与蒸制技巧","content":"凉皮的制作分三步：洗面、沉淀、蒸制。面粉加水揉成面团，醒30分钟。放入清水中反复揉洗，洗出的淀粉水静置4小时以上。\n\n倒掉上层清水，底部淀粉浆搅匀。平底盘刷油，舀一勺浆摊平，大火蒸2分钟。取出放凉揭下，刷一层熟油防粘。切条后加黄瓜丝、面筋、蒜水、辣椒油、醋、芝麻酱。酸辣爽滑，夏天来一碗解暑又解馋。","category":"小吃"},
{"title":"炸酱面的酱怎么炸才香","content":"北京炸酱面，酱是灵魂。六必居干黄酱和甜面酱按2:1混合，加少许水澥开。五花肉切小丁。\n\n锅中多放油，先炒肉丁至出油微焦，倒入酱汁。小火不停搅拌，炸20分钟以上，直到酱色深褐、油酱分离。这个过程叫炸酱，火候到了自然香。面条手擀最佳，煮好过凉水。配上黄瓜丝、心里美萝卜丝、豆芽、青豆，浇两大勺炸酱拌匀，每根面条都裹满酱。","category":"小吃"},
{"title":"生煎包的底部怎么煎出冰花","content":"生煎包的冰花底是精华。面团发好包入肉馅，收口朝下放入平底锅。中火煎至底部微黄。\n\n关键步骤：倒入面粉水（面粉和水1:10），水量到包子三分之一高度。盖盖中小火焖8分钟。水分蒸发后，底部形成一层金黄酥脆的冰花。出锅前撒芝麻和葱花。咬开一个小口，先吸汤汁，再吃包子，底部嘎嘣脆。","category":"小吃"},
{"title":"螺蛳粉的酸笋怎么腌制","content":"酸笋是螺蛳粉臭味的来源。新鲜竹笋切条，放入坛中加盐和淘米水，密封腌制15天以上。\n\n腌好的酸笋闻起来臭，但煮在汤里会释放出独特的鲜味。螺蛳粉的汤底用螺蛳和猪骨熬制，加酸笋、酸豆角、木耳、花生、腐竹、青菜。粉用干米粉泡软煮熟。酸辣鲜臭，一碗入魂。第一次吃可能不习惯，但越吃越上瘾。","category":"小吃"},
{"title":"手抓饼的千层酥皮怎么叠","content":"手抓饼的千层效果来自叠酥。面粉加温水揉成面团，醒30分钟。擀成大薄片，刷一层油酥（面粉加油调成糊）。\n\n从一端卷起成长条，再盘成螺旋状，按扁擀圆。这样每一层之间都有油酥，烙的时候自然分层。平底锅小火烙至两面金黄，用筷子从中间挑松，饼层层分明。加鸡蛋、火腿、生菜，挤上番茄酱，就是一份完美的早餐。","category":"小吃"},
{"title":"豆腐脑的卤汁怎么做","content":"豆腐脑分甜咸两派，北方咸卤是经典。内酯豆腐蒸10分钟凝固。卤汁：木耳黄花菜切碎，锅中加油炒香。\n\n加水烧开，放酱油盐调味，勾芡至浓稠。打入蛋花搅散。豆腐脑盛碗，浇上卤汁，加辣椒油、韭菜花、蒜汁、香菜。嫩滑的豆腐配上浓稠的卤汁，每一口都有料。早上来一碗豆腐脑配油条，是北方人的幸福。","category":"小吃"},
{"title":"羊肉泡馍的馍要掰多小","content":"羊肉泡馍的馍要掰成黄豆大小，这是西安人的规矩。馍是半发面的饦饦馍，掰得越小越入味。\n\n羊肉汤用羊骨熬制6小时以上，汤色乳白。掰好的馍放入碗中，厨师加羊肉片、粉丝、木耳，浇上滚烫的羊汤。配糖蒜和辣酱吃。馍吸满汤汁，绵软入味，羊肉鲜嫩。一碗下肚，浑身暖和。西安人说：掰馍是个耐心活。","category":"小吃"},
{"title":"烤红薯为什么比蒸的甜","content":"烤红薯比蒸的甜是有科学道理的。红薯含有淀粉酶，在60-70度时会把淀粉分解成麦芽糖。烤制时红薯内部温度长时间处于这个区间，淀粉大量转化为糖。\n\n蒸制温度太高太快，淀粉酶来不及工作。所以烤红薯又甜又流油，蒸红薯相对清淡。选红薯要选红心的烟薯25号，糖分最高。烤箱200度烤60分钟，中途翻面。冬天捧着一个烤红薯，是幸福的味道。","category":"小吃"},
{"title":"糖炒栗子为什么要放沙子","content":"糖炒栗子为什么要和沙子一起炒？因为沙子是导热介质。栗子是圆的，直接炒受热不均匀。沙子包裹栗子，让热量均匀传递到每一面。\n\n铁锅烧热，倒入粗砂和麦芽糖，翻炒至砂子发烫。放入划了十字口的栗子，不停翻炒20分钟。沙子让栗子均匀受热，麦芽糖让表皮油亮。炒好的栗子壳好剥，肉质粉糯香甜。注意栗子一定要划口，否则会炸。","category":"小吃"},
]

def main():
    print("=== 批次4：小吃10篇 ===")
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
        r=api_post("/api/coupon",{"articleId":aid,"title":"小吃专享优惠","description":"凭此券可享受小吃立减优惠","type":1,"threshold":10.0,"discount":2.0,"originalPrice":0,"totalCount":150,"limitPerUser":2,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
