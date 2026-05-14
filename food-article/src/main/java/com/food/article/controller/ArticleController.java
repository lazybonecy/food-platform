package com.food.article.controller;

import com.food.article.dto.ArticleDTO;
import com.food.article.dto.ArticleListDTO;
import com.food.article.service.ArticleService;
import com.food.common.core.constant.CommonConstant;
import com.food.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public R<Long> publishArticle(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                  @RequestBody ArticleDTO dto) {
        return R.ok(articleService.publishArticle(merchantId, dto));
    }

    @PutMapping("/{id}")
    public R<Void> updateArticle(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                 @PathVariable Long id,
                                 @RequestBody ArticleDTO dto) {
        articleService.updateArticle(merchantId, id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteArticle(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                 @PathVariable Long id) {
        articleService.deleteArticle(merchantId, id);
        return R.ok();
    }

    @GetMapping("/list")
    public R<ArticleListDTO> listArticles(
            @RequestParam(name = "current", defaultValue = "1") int current,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(articleService.listArticles(current, size, category, keyword));
    }

    @GetMapping("/hot")
    public R<ArticleListDTO> listHotArticles(
            @RequestParam(name = "current", defaultValue = "1") int current,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(articleService.listHotArticles(current, size, category, keyword));
    }

    @GetMapping("/{id}")
    public R<ArticleDTO> getArticleDetail(
            @PathVariable Long id,
            @RequestHeader(value = CommonConstant.USER_ID_HEADER, required = false) Long userId) {
        return R.ok(articleService.getArticleDetail(id, userId));
    }

    @PostMapping("/{id}/like")
    public R<Void> like(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                        @PathVariable Long id) {
        articleService.like(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/unlike")
    public R<Void> unlike(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                          @PathVariable Long id) {
        articleService.unlike(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/collect")
    public R<Void> collect(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                           @PathVariable Long id) {
        articleService.collect(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/uncollect")
    public R<Void> uncollect(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                             @PathVariable Long id) {
        articleService.uncollect(userId, id);
        return R.ok();
    }
}
