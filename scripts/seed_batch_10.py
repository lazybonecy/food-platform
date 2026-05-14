"""批次10：东北菜10篇"""
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
{"title":"东北铁锅炖大鹅的做法","content":"铁锅炖大鹅是东北硬菜。大鹅斩块焯水去血沫。铁锅烧热，鹅块下锅煸炒至出油微焦。\n\n加姜蒜八角桂皮干辣椒炒香，倒入酱油料酒。加水没过鹅肉，大火烧开转中火炖1小时。锅边贴上玉米面饼子，盖盖焖15分钟。鹅肉软烂入味，饼子一面焦脆一面吸满汤汁。东北人说：铁锅炖大鹅，越吃越暖和。冬天围着大铁锅吃，热气腾腾。","category":"东北菜"},
{"title":"酸菜白肉的酸菜怎么腌","content":"东北酸菜用大白菜腌。白菜晒两天去水分，一层白菜一层盐码入缸中。压上大石头，加凉水没过。密封腌制一个月以上。\n\n腌好的酸菜金黄透亮，酸味纯正。切丝后和五花肉片一起炖。五花肉切薄片，酸菜丝铺在砂锅底部，肉片铺上面。加高汤炖30分钟。酸菜吸满肉汁，肥而不腻。配蒜泥酱油蘸肉吃，东北人的冬季必备。","category":"东北菜"},
{"title":"小鸡炖蘑菇的榛蘑泡发","content":"小鸡炖蘑菇必须用榛蘑。干榛蘑提前泡发4小时，泡的水不要倒，沉淀后取上层清液做汤底。\n\n小笨鸡斩块焯水，锅中热油炒糖色，鸡块翻炒上色。加榛蘑和泡蘑菇的水，大火烧开转小火炖40分钟。粉条提前泡软，最后10分钟放入。鸡肉鲜嫩，榛蘑香气浓郁，粉条吸满汤汁。东北人说：姑爷进门，小鸡没魂。","category":"东北菜"},
{"title":"锅包肉的糖醋汁要趁热浇","content":"锅包肉是东北名菜。里脊切薄片挂淀粉糊，两次油炸至酥脆。糖醋汁：白醋、白糖、少许盐熬至起泡。\n\n关键：糖醋汁要趁热浇在刚出锅的肉片上，滋啦一声才能入味。凉了浇上去不吸收。翻匀后撒香菜胡萝卜丝。外酥里嫩，酸甜可口。正宗东北锅包肉用白醋，不用番茄酱。颜色金黄透亮，不是红色的。","category":"东北菜"},
{"title":"地三鲜为什么叫地三鲜","content":"地三鲜是东北家常菜。三种食材来自大地：茄子、土豆、青椒。茄子切滚刀块，土豆切块，青椒切块。\n\n三样分别油炸至金黄。锅中留底油，加蒜末爆香，倒入三鲜翻炒。调味用酱油、蚝油、白糖、少许水。大火收汁，每块食材都裹满酱汁。地三鲜不放肉也香，茄子软糯，土豆绵密，青椒脆嫩。东北人的下饭神器。","category":"东北菜"},
{"title":"东北大拉皮的麻酱调法","content":"东北大拉皮是凉菜。拉皮用绿豆淀粉制作，加水搅成糊，倒入平底盘蒸熟，放凉揭下切条。\n\n麻酱是灵魂：芝麻酱用温水慢慢澥开，加酱油、醋、蒜水、辣椒油、白糖。拉皮铺盘，放黄瓜丝、胡萝卜丝、紫甘蓝丝、干豆腐丝。浇上麻酱拌匀。酸辣爽滑，麻酱浓郁。夏天来一盘大拉皮，开胃解暑。","category":"东北菜"},
{"title":"猪肉炖粉条的粉条选用","content":"猪肉炖粉条必须用土豆粉条。土豆粉条比红薯粉条更筋道，炖久了也不烂。粉条提前泡软。\n\n五花肉切块焯水，锅中热油炒糖色，肉块翻炒上色。加八角桂皮姜片，倒入酱油料酒。加水炖30分钟，放入粉条再炖15分钟。粉条吸满肉汁，滑溜筋道，五花肉软烂入味。东北人冬天就靠这道菜续命。","category":"东北菜"},
{"title":"杀猪菜的血肠怎么做","content":"杀猪菜是东北过年的大菜。血肠是关键：新鲜猪血加盐、葱花、姜末、五香粉搅匀。灌入洗净的猪肠中，扎紧两端。\n\n水烧开后放入血肠，小火煮20分钟，不能大火否则会爆。煮熟切片，和酸菜五花肉一起炖。血肠嫩滑，酸菜酸爽，五花肉肥而不腻。东北农村过年杀猪，全村人一起吃杀猪菜，热闹非凡。","category":"东北菜"},
{"title":"东北烤冷面的铁板温度","content":"烤冷面是东北街头小吃。冷面皮放在铁板上，铁板温度要200度左右。温度太低面皮不焦，太高会糊。\n\n面皮煎至微焦翻面，打鸡蛋摊匀。再翻面刷酱料（蒜蓉辣酱、甜面酱、番茄酱调匀）。加火腿肠、香菜、洋葱丁。卷起来切段。酸甜微辣，Q弹有嚼劲。五块钱一份，学生最爱。东北的冬天，路边摊的烤冷面热气腾腾。","category":"东北菜"},
{"title":"东北粘豆包的黄米面处理","content":"粘豆包是东北冬季主食。黄米面加温水揉成团，要揉到不粘手。红豆提前泡8小时，煮烂压成泥加糖。\n\n黄米面分成小剂子，压扁包入红豆馅，收口搓成椭圆形。蒸笼铺上苏子叶防粘，大火蒸20分钟。粘豆包金黄软糯，红豆馅甜而不腻。蒸好后放外面冻硬，想吃时蒸热即可。东北人一冬天能吃几百个粘豆包。","category":"东北菜"},
]

def main():
    print("=== 批次10：东北菜10篇 ===")
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
        r=api_post("/api/coupon",{"articleId":aid,"title":"东北菜专享优惠","description":"凭此券可享受东北菜立减优惠","type":1,"threshold":20.0,"discount":5.0,"originalPrice":0,"totalCount":80,"limitPerUser":1,"startTime":"2026-05-14T00:00:00","endTime":"2026-06-14T23:59:59"},token=tk)
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
