package com.luoben.glmall.search.service;

import com.luoben.glmall.search.vo.SearchParam;
import com.luoben.glmall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     * 检索查询
     * @param param 检索参数
     * @return 检索结果
     */
    SearchResult search(SearchParam param);
}
