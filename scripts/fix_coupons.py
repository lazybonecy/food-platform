"""为批次1的10篇文章创建优惠券"""
import json, time, urllib.request, urllib.error

BASE_URL = "http://localhost:8080"

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

def login(username, password):
    res = api_post("/api/auth/login", {"username": username, "password": password})
    if res.get("code") == 200:
        return res["data"]["accessToken"]
    return None

def main():
    # 登录商家
    merchant_map = {
        "川菜": login("chef_wang", "123456"),
        "粤菜": login("chef_li", "123456"),
    }

    # 批次1文章ID和分类
    articles = [
        (53, "川菜"), (54, "川菜"), (55, "川菜"), (56, "川菜"),
        (57, "粤菜"), (58, "粤菜"), (59, "粤菜"), (60, "粤菜"), (61, "粤菜"), (62, "粤菜"),
    ]

    ok = 0
    for article_id, cat in articles:
        token = merchant_map.get(cat)
        if not token:
            continue
        res = api_post("/api/coupon", {
            "articleId": article_id,
            "title": f"{cat}美食专享优惠",
            "description": f"凭此券可享受{cat}美食立减优惠",
            "type": 1,
            "threshold": 20.0,
            "discount": 5.0,
            "originalPrice": 0,
            "totalCount": 100,
            "limitPerUser": 1,
            "startTime": "2026-05-14T00:00:00",
            "endTime": "2026-06-14T23:59:59",
        }, token=token)
        if res.get("code") == 200:
            ok += 1
            print(f"  + 优惠券: article#{article_id} ({cat})")
        else:
            print(f"  x article#{article_id}: {res.get('message')}")
        time.sleep(0.05)

    print(f"\n完成: {ok}/{len(articles)} 张优惠券")

if __name__ == "__main__":
    main()
