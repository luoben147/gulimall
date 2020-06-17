package com.luoben.glmall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.glmall.product.dao.CategoryDao;
import com.luoben.glmall.product.entity.CategoryEntity;
import com.luoben.glmall.product.service.CategoryBrandRelationService;
import com.luoben.glmall.product.service.CategoryService;
import com.luoben.glmall.product.vo.Catelog2VO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }


    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装树形结构
        //2.1 一级分类
        List<CategoryEntity> levelOneMenus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map(menu -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted(
                //Sort升序排序
                //Comparator.comparing(CategoryEntity::getSort)
                (menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }
        ).collect(Collectors.toList());

        return levelOneMenus;
    }

    @Override
    public void removeMenuByIds(List<Long> ids) {

        //TODO  检查当前删除的菜单是否被别的地方引用
        //逻辑删除
        baseMapper.deleteBatchIds(ids);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return paths.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联数据
     *  缓存失效模式的使用：@CacheEvict 删除缓存
     *  1.同时进行多种缓存操作 @Caching
     *  2. 指定删除某个分区下的所有数据 @CacheEvict(value = "category",allEntries = true)
     *  3. 存储同一类型的数据，都可以指定同一分区
     */
//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getLevelOneCategorys'"),
//            @CacheEvict(value = "category",key = "'getCatelogJson'"),
//
//    })
    @CacheEvict(value = "category",allEntries = true) //失效模式
    //@CachePut  //双写模式
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 查询所有一级分类
     *
     *  1.每一个需要缓存的数据，都要指定放入缓存的名字    【缓存的分区（按业务类型）】
     *  2. @Cacheable({"category"})
     *        代表当前方法结果需要缓存，如果缓存中有，方法不用调用，
     *        如果缓存中没有，就会调用方法，将方法返回结果放入缓存
     *  3.默认行为：
     *      1)、缓存中有，方法不用调用
     *      2)、key 默认自动生成： 缓存名字::category::SimpleKey[]
     *      3）、缓存的value的值： 默认使用jdk序列化机制，将序列化后的数据存入redis
     *      4）、默认ttl时间 -1
     *    自定义：
     *      1）、指定生成的缓存使用的key   : key属性指定，接受一个SpEL表达式
     *      2）、指定缓存的存活时间    :   spring.cache.redis.time-to-live 配置
     *      3）、将数据保存为json
     * @return
     */
    //
    @Cacheable(value ={"category"},key = "#root.method.name",sync = true)
    @Override
    public List<CategoryEntity> getLevelOneCategorys() {
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_cid", 0);
        List<CategoryEntity> entities = baseMapper.selectList(queryWrapper);
        return entities;
    }

    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2VO>> getCatelogJson() {

        System.out.println("查询了数据库.....");
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查询所有一级分类
        List<CategoryEntity> levelOneCategorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2VO>> parent_cid = levelOneCategorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1.每个一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //2.封装结果为指定格式
            List<Catelog2VO> catelog2VOS = null;
            if (!CollectionUtils.isEmpty(categoryEntities)) {
                catelog2VOS = categoryEntities.stream().map(l2 -> {
                    Catelog2VO catelog2VO = new Catelog2VO(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> category3List = getParent_cid(selectList, l2.getCatId());
                    if (!CollectionUtils.isEmpty(category3List)) {
                        //封装三级分类数据
                        List<Catelog2VO.Catelog3VO> collect = category3List.stream().map(l3 -> {
                            Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3VO;
                        }).collect(Collectors.toList());
                        catelog2VO.setCatalog3List(collect);
                    }

                    return catelog2VO;
                }).collect(Collectors.toList());
            }

            return catelog2VOS;
        }));
        return parent_cid;
    }

    //@Override
    public Map<String, List<Catelog2VO>> getCatelogJson2() {
        /**
         * 1.空结果缓存：解决缓存穿透问题
         * 2.设置过期时间（加随机值）：解决缓存雪崩
         * 3.加锁：解决缓存击穿
         */

        //缓存   json字符串
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (StringUtils.isEmpty(catelogJson)) {
            //缓存中没有，查询数据库
            //保证数据库查询完成后，将数据放入redis中 是一个原子操作
            Map<String, List<Catelog2VO>> catelogJsonFromDB = getCatelogJsonFromDBWithRedssonLock();

            return catelogJsonFromDB;
        }

        Map<String, List<Catelog2VO>> result = JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2VO>>>() {
        });

        return result;
    }


    /**
     * 缓存里的数据一致性
     * 1.双写模式   改数据库 改缓存
     * 2.失效模式   改完数据库  删除缓存
     * 解决方案：
     *  1.缓存的所有数据都有过期时间，数据过期，下一次查询触发主动更新
     *  2.读写数据的时候，加上分布式读写锁。
     * @return
     */
    public Map<String, List<Catelog2VO>> getCatelogJsonFromDBWithRedssonLock() {

        //1.占分布式锁 ： redis 占位 setNX
        RLock lock = redisson.getLock("catelogJson-lock");
        lock.lock();

        Map<String, List<Catelog2VO>> dataFromDB = null;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }

        return dataFromDB;
    }


    /**
     * 添加redis分布式锁从数据库获取分类Json数据
     * @return
     */
    public Map<String, List<Catelog2VO>> getCatelogJsonFromDBWithReidsLock() {

        //1.占分布式锁 ： redis 占位 setNX
        //2.设置过期时间，避免异常导致无法删除锁造成死锁，必须和加锁同步，原子操作
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功...");
            //加锁成功  执行业务
            Map<String, List<Catelog2VO>> dataFromDB = null;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                //获取值对比+对比成功删除=原子操作，lua脚本解锁
                /**
                 if redis.call("get",KEYS[1]) == ARGV[1]
                 then
                 return redis.call("del",KEYS[1])
                 else
                 return 0
                 end
                 */
                //lua脚本删除锁 保证原子性
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then    return redis.call('del',KEYS[1]) else return 0 end";
                //进行原子删锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                        , Arrays.asList("lock"), uuid);
            }

            return dataFromDB;
        } else {
            //加锁失败  重试
            //休眠100ms重试
            System.out.println("获取分布式锁失败，等待重试...");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatelogJsonFromDBWithReidsLock();  //自旋的方式
        }
    }

    //从数据库查询并封装分类数据
    private Map<String, List<Catelog2VO>> getDataFromDB() {
        //得到锁之后，再去缓存中确认一次，如果没有才需要查数据库
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (!StringUtils.isEmpty(catelogJson)) {
            //缓存不为null 直接返回
            Map<String, List<Catelog2VO>> result = JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2VO>>>() {
            });
            return result;
        }
        System.out.println("查询了数据库.....");
        //1.将数据库多次查询变为1次
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查询所有一级分类
        List<CategoryEntity> levelOneCategorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2VO>> parent_cid = levelOneCategorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1.每个一级分类，查到这个一级分类的二级分类

            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //2.封装结果为指定格式
            List<Catelog2VO> catelog2VOS = null;
            if (!CollectionUtils.isEmpty(categoryEntities)) {
                catelog2VOS = categoryEntities.stream().map(l2 -> {
                    Catelog2VO catelog2VO = new Catelog2VO(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> category3List = getParent_cid(selectList, l2.getCatId());
                    if (!CollectionUtils.isEmpty(category3List)) {
                        //封装三级分类数据
                        List<Catelog2VO.Catelog3VO> collect = category3List.stream().map(l3 -> {
                            Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3VO;
                        }).collect(Collectors.toList());
                        catelog2VO.setCatalog3List(collect);
                    }

                    return catelog2VO;
                }).collect(Collectors.toList());
            }

            return catelog2VOS;
        }));

        //放入缓存
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catelogJson", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    //本地锁
    public Map<String, List<Catelog2VO>> getCatelogJsonFromDBWithLocalLock() {

        //只要是一把锁，就能锁住需要这个锁的所有线程
        //1.  synchronized (this),springboot所有的组件在容器中都是单例的
        //TODO 本地锁:synchronized ，在分布式情况下就必须使用分布式锁

        synchronized (this) {

            //得到锁以后再去缓存中确认一次，如果没有才需要去继续查询
            return getDataFromDB();
        }

    }


    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid().longValue() == parent_cid.longValue();
        }).collect(Collectors.toList());

        return collect;
    }


    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity entity = this.getById(catelogId);
        if (entity.getParentCid() != 0) {
            findParentPath(entity.getParentCid(), paths);
        }
        return paths;
    }

    /**
     * 获取当前菜单的子菜单
     *
     * @param root 当前菜单
     * @param all  所有菜单
     * @return
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            //找到子菜单
            return categoryEntity.getParentCid().longValue() == root.getCatId().longValue();
        }).map(categoryEntity -> {
            //设置子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted(
                //排序
                //Comparator.comparing(CategoryEntity::getSort)
                (menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }
        ).collect(Collectors.toList());
        return children;
    }

}