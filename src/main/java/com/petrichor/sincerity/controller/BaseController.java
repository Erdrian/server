package com.petrichor.sincerity.controller;


import com.github.pagehelper.PageHelper;
import com.petrichor.sincerity.entity.User;
import com.petrichor.sincerity.util.Convert;
import com.petrichor.sincerity.util.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class BaseController {
    @Autowired
    RedisTemplate<Object, Object> redisTemplate;

    protected void startPage() {
        HttpServletRequest request = ServletUtils.getRequest();
        int page = Convert.toInt(request.getParameter("page"));
        int pageSize = Convert.toInt(request.getParameter("pageSize"));
        PageHelper.startPage(page, pageSize);
    }

    public String getUserName() {
        return getUser().getUserName();
    }

    public User getUser() {
        String token = ServletUtils.getRequest().getHeader("X-Access-Token");
        return (User) redisTemplate.opsForValue().get(token);
    }

    public Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}