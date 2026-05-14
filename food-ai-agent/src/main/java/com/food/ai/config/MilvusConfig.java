package com.food.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MilvusConfig {

    @Value("${spring.ai.vectorstore.milvus.uri:http://localhost:19530}")
    private String milvusUri;

    @Value("${spring.ai.vectorstore.milvus.collection-name:food_articles}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.embedding-dimension:1024}")
    private int embeddingDimension;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        String host = milvusUri.replace("http://", "").replace("https://", "").split(":")[0];
        int port = Integer.parseInt(milvusUri.replace("http://", "").replace("https://", "").split(":")[1]);
        return new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );
    }

    @Bean
    @Primary
    public VectorStore vectorStore(MilvusServiceClient milvusClient, @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        try {
            return MilvusVectorStore.builder(milvusClient, embeddingModel)
                    .collectionName(collectionName)
                    .embeddingDimension(embeddingDimension)
                    .indexType(IndexType.HNSW)
                    .metricType(MetricType.COSINE)
                    .indexParameters("{\"M\":16,\"efConstruction\":200}")
                    .build();
        } catch (Exception e) {
            // Milvus 不可用时降级为内存向量存储
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }
}
