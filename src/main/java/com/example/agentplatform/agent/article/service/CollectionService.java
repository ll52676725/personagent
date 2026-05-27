package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.dto.CollectionCreateDTO;
import com.example.agentplatform.agent.article.dto.CollectionOutlineDTO;
import com.example.agentplatform.agent.article.dto.CollectionUpdateDTO;
import com.example.agentplatform.agent.article.dto.GenerateArticleFromOutlineDTO;
import com.example.agentplatform.agent.article.dto.GenerateCollectionOutlineDTO;
import com.example.agentplatform.agent.article.entity.Article;
import com.example.agentplatform.agent.article.entity.Collection;
import com.example.agentplatform.agent.article.dto.GenerateResult;

import java.util.List;

public interface CollectionService {
    Collection createCollection(Long userId, CollectionCreateDTO dto);

    Collection updateCollection(Long userId, Long collectionId, CollectionUpdateDTO dto);

    Collection getCollection(Long userId, Long collectionId);

    List<Collection> listCollections(Long userId);

    void deleteCollection(Long userId, Long collectionId);

    CollectionOutlineDTO generateCollectionOutlines(Long userId, GenerateCollectionOutlineDTO dto);

    Collection saveCollectionOutlines(Long userId, Long collectionId, CollectionOutlineDTO outlines);

    Article generateArticleFromOutline(Long userId, GenerateArticleFromOutlineDTO dto);

    List<Article> generateAllArticlesFromCollection(Long userId, Long collectionId);
}