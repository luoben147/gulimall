package com.luoben.glmall.search;

import com.alibaba.fastjson.JSON;
import com.luoben.glmall.search.config.MallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GlmallSearchApplicationTests {

    @Resource
    private RestHighLevelClient client;

    /**
     * 复杂检索
     * @throws IOException
     */
    @Test
    public void searchData()throws IOException {
        //1.创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");
        //指定DSL 检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //构造检索条件
//        sourceBuilder.query();
//        sourceBuilder.from();
//        sourceBuilder.size();
//        sourceBuilder.aggregations();
        sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));

        //聚合条件
        //1.1 按年龄的值分布聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);
        //1.2计算平均薪资
        AvgAggregationBuilder blanceAgg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(blanceAgg);

        searchRequest.source(sourceBuilder);

        //2.执行检索
        SearchResponse searchResponse = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);

        //3.分析结果
        //Map map = JSON.parseObject(searchResponse.toString(), Map.class);
        //3.1获取所有查到的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit:searchHits){
            hit.getIndex();
            hit.getId();
            String sourceAsString = hit.getSourceAsString();
        }

        //3.2 获取检索的分析信息
        Aggregations aggregations = searchResponse.getAggregations();
//        for (Aggregation aggregation : aggregations.asList()) {
//            System.out.println("当前聚合:"+ aggregation.getName());
//        }

        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            System.out.println("年龄:"+keyAsString+"; 数量="+docCount);
        }

        Avg balanceAvg = aggregations.get("balanceAvg");

        System.out.println("凭借薪资:"+balanceAvg.getValue());
    }

    /**
     * 测试存储数据到ex
     * 更新也可以
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");//数据id
        //indexRequest.source("userName","zhangsan","age",18,"gender","男");
        User user = new User();
        user.setUserName("张三");
        user.setAge(20);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        System.out.println("jsonString="+jsonString);
        //要保存的内容
        indexRequest.source(jsonString, XContentType.JSON);

        //执行创建索引和保存数据
        IndexResponse index = client.index(indexRequest, MallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println(index);
    }



    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

}
