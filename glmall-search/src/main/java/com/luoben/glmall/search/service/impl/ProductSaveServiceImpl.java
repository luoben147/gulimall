package com.luoben.glmall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.luoben.common.to.es.SkuEsModel;
import com.luoben.glmall.search.config.MallElasticSearchConfig;
import com.luoben.glmall.search.constant.EsConstant;
import com.luoben.glmall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    /**
     * 商品上架
     * @param skuEsModels
     */
    @Override
    public Boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //保存到es
        //1.给es中建立索引（product），在kibana中建立好映射关系(product-mapping.txt)
        //2.给es中保存这些数据
        BulkRequest bulkRequest=new BulkRequest();
        for (SkuEsModel model : skuEsModels) {
            //构造保存请求
            IndexRequest indexRequest = new IndexRequest();
            indexRequest.index(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String jsonString = JSON.toJSONString(model);
            indexRequest.source(jsonString,XContentType.JSON);

            bulkRequest.add(indexRequest);

        }
        //批量操作
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, MallElasticSearchConfig.COMMON_OPTIONS);

        //TODO 1.如果批量错误
        boolean b = bulk.hasFailures();
        if(b){
            log.error("商品上架错误：{}",bulk.buildFailureMessage());
        }
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架完成:{},返回数据：{}",collect,bulk.toString());

        return b;
    }
}
