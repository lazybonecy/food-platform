package com.food.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.ai.entity.Article;
import com.food.ai.entity.Comment;
import com.food.ai.mapper.ArticleMapper;
import com.food.ai.mapper.CommentMapper;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ArticleIndexService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private MilvusServiceClient milvusClient;

    @Value("${spring.ai.vectorstore.milvus.collection-name:food_articles}")
    private String collectionName;

    /**
     * 将所有已发布文章嵌入并存储到 Milvus（HNSW 索引）
     */
    public int reindexAll() {
        // 先清空旧数据，避免重复
        clearCollection();

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1);
        List<Article> articles = articleMapper.selectList(wrapper);

        if (articles.isEmpty()) {
            return 0;
        }

        // 批量查询所有评论，按 articleId 分组
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.in(Comment::getArticleId, articleIds);
        List<Comment> allComments = commentMapper.selectList(commentWrapper);
        Map<Long, List<String>> commentsMap = new HashMap<>();
        for (Comment c : allComments) {
            if (c.getContent() != null && !c.getContent().isBlank()) {
                commentsMap.computeIfAbsent(c.getArticleId(), k -> new ArrayList<>()).add(c.getContent());
            }
        }

        List<Document> docs = new ArrayList<>();
        for (Article a : articles) {
            List<String> comments = commentsMap.getOrDefault(a.getId(), Collections.emptyList());
            String text = buildEmbeddingText(a, comments);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("articleId", a.getId());
            metadata.put("title", a.getTitle() != null ? a.getTitle() : "");
            metadata.put("category", a.getCategory() != null ? a.getCategory() : "");
            metadata.put("likeCount", a.getLikeCount() != null ? a.getLikeCount() : 0);
            metadata.put("collectCount", a.getCollectCount() != null ? a.getCollectCount() : 0);
            metadata.put("viewCount", a.getViewCount() != null ? a.getViewCount() : 0);
            docs.add(new Document(String.valueOf(a.getId()), text, metadata));
        }

        // 分批写入，避免单次请求过大
        int batchSize = 50;
        for (int i = 0; i < docs.size(); i += batchSize) {
            List<Document> batch = docs.subList(i, Math.min(i + batchSize, docs.size()));
            vectorStore.add(batch);
            log.info("Indexed articles {} - {}", i, Math.min(i + batchSize, docs.size()));
        }

        log.info("Reindex complete: {} articles", docs.size());
        return docs.size();
    }

    /**
     * 增量索引单篇文章
     */
    public void indexArticle(Article article) {
        // 查询该文章的评论
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getArticleId, article.getId());
        List<String> comments = commentMapper.selectList(commentWrapper).stream()
                .map(Comment::getContent)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        String text = buildEmbeddingText(article, comments);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("articleId", article.getId());
        metadata.put("title", article.getTitle() != null ? article.getTitle() : "");
        metadata.put("category", article.getCategory() != null ? article.getCategory() : "");
        metadata.put("likeCount", article.getLikeCount() != null ? article.getLikeCount() : 0);
        metadata.put("collectCount", article.getCollectCount() != null ? article.getCollectCount() : 0);
        metadata.put("viewCount", article.getViewCount() != null ? article.getViewCount() : 0);
        Document doc = new Document(String.valueOf(article.getId()), text, metadata);
        vectorStore.add(List.of(doc));
    }

    private String buildEmbeddingText(Article a, List<String> comments) {
        StringBuilder sb = new StringBuilder();
        if (a.getTitle() != null) sb.append(a.getTitle());
        if (a.getCategory() != null) sb.append(" ").append(a.getCategory());
        if (a.getContent() != null) {
            String content = a.getContent();
            sb.append(" ").append(content.length() > 500 ? content.substring(0, 500) : content);
        }
        // 拼接评论内容（最多取前 10 条，每条截取 100 字符）
        if (comments != null && !comments.isEmpty()) {
            sb.append(" 评论：");
            int count = 0;
            for (String c : comments) {
                if (count >= 10) break;
                sb.append(c.length() > 100 ? c.substring(0, 100) : c).append("。");
                count++;
            }
        }
        return sb.toString().trim();
    }

    private void clearCollection() {
        // 删除旧 collection
        try {
            milvusClient.dropCollection(DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            log.info("[Reindex] Dropped collection '{}'", collectionName);
        } catch (Exception e) {
            log.warn("[Reindex] Drop failed: {}", e.getMessage());
        }
        // 直接用 Milvus client 创建 collection + HNSW index
        try {
            FieldType idField = FieldType.newBuilder()
                    .withName("doc_id").withDataType(DataType.VarChar)
                    .withMaxLength(36).withPrimaryKey(true).withAutoID(false).build();
            FieldType contentField = FieldType.newBuilder()
                    .withName("content").withDataType(DataType.VarChar)
                    .withMaxLength(65535).build();
            FieldType metadataField = FieldType.newBuilder()
                    .withName("metadata").withDataType(DataType.JSON).build();
            FieldType embeddingField = FieldType.newBuilder()
                    .withName("embedding").withDataType(DataType.FloatVector)
                    .withDimension(1024).build();

            milvusClient.createCollection(CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("Spring AI Vector Store")
                    .withShardsNum(2)
                    .addFieldType(idField)
                    .addFieldType(contentField)
                    .addFieldType(metadataField)
                    .addFieldType(embeddingField)
                    .build());

            milvusClient.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("embedding")
                    .withIndexType(IndexType.HNSW)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"M\":16,\"efConstruction\":200}")
                    .withSyncMode(false)
                    .build());

            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());

            log.info("[Reindex] Recreated collection '{}' with HNSW index", collectionName);
        } catch (Exception e) {
            log.error("[Reindex] Failed to recreate collection: {}", e.getMessage(), e);
        }
    }
}
