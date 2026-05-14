package com.food.article.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArticleListDTO {

    private List<ArticleDTO> records;
    private Long total;
    private Long current;
    private Long size;
}
