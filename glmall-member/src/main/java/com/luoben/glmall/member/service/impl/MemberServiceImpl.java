package com.luoben.glmall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.utils.HttpUtils;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.glmall.member.dao.MemberDao;
import com.luoben.glmall.member.entity.MemberEntity;
import com.luoben.glmall.member.entity.MemberLevelEntity;
import com.luoben.glmall.member.exception.PhoneExistExcetpion;
import com.luoben.glmall.member.exception.UserNameExistExcetpion;
import com.luoben.glmall.member.service.MemberLevelService;
import com.luoben.glmall.member.service.MemberService;
import com.luoben.glmall.member.vo.MemberLoginVo;
import com.luoben.glmall.member.vo.MemberRegistVo;
import com.luoben.glmall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }


    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        //默认等级
        MemberLevelEntity memberLevelEntity= memberLevelService.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        //检查用户名、手机号是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());
        //密码
        BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
        String encode = encoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        //其他默认信息

        baseMapper.insert(memberEntity);
    }


    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistExcetpion{

        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile",phone);

        Integer cout = baseMapper.selectCount(wrapper);
        if(cout>0){
            throw new PhoneExistExcetpion();
        }

    }

    @Override
    public void checkUserNameUnique(String userName) throws UserNameExistExcetpion{
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username",userName);

        Integer cout = baseMapper.selectCount(wrapper);
        if(cout>0){
            throw new UserNameExistExcetpion();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username",loginacct).or().eq("mobile",loginacct);

        MemberEntity memberEntity = baseMapper.selectOne(wrapper);
        if(memberEntity==null){
            return null;
        }else {
            //验证密码
            String dbPassWord = memberEntity.getPassword();
            BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
            boolean matches = encoder.matches(password, dbPassWord);
            if(matches){
                return memberEntity;
            }else {
                return null;
            }
        }
    }

    /**
     * 社交登陆
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        String uid = socialUser.getUid();
        //判断当前社交用户是否登陆过
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("social_uid",uid);
        MemberEntity memberEntity = baseMapper.selectOne(wrapper);
        if(memberEntity!=null){
            //更新token
            MemberEntity update=new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());

            baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());

            return  memberEntity;
        }else {
            MemberEntity regist=new MemberEntity();
            try {
                //查询当前社交用户的社交账号信息（昵称，性别等）
                Map<String,String> query=new HashMap<>();
                query.put("access_token",socialUser.getAccess_token());
                query.put("uid",socialUser.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if(response.getStatusLine().getStatusCode()==200){
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String nikeName = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");
                    //....
                    regist.setNickname(nikeName);
                    regist.setGender("m".equals(gender)?1:0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());

            baseMapper.insert(regist);

            return regist;
        }
    }

}