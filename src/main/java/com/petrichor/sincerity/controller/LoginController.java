package com.petrichor.sincerity.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.petrichor.sincerity.api.CommonResult;
import com.petrichor.sincerity.dto.LoginBody;
import com.petrichor.sincerity.dto.LoginResult;
import com.petrichor.sincerity.entity.SysUser;
import com.petrichor.sincerity.service.LoginService;
import com.petrichor.sincerity.util.CaptchaUtil;
import com.petrichor.sincerity.annotation.NotNeedLogin;
import com.petrichor.sincerity.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/login")
public class LoginController {
    @Autowired
    CaptchaUtil captchaUtil;
    @Autowired
    DefaultKaptcha getCaptcha;
    @Autowired
    RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    LoginService userService;
    @Autowired
    TokenUtil tokenUtil;
    @Autowired
    ModelMapper modelMapper;

    //验证码
    @GetMapping("/getCaptcha")
    @NotNeedLogin
    public CommonResult<String> getCaptcha(@RequestParam String captchaKey) throws IOException {
        if (captchaKey == null || captchaKey.isEmpty()) return CommonResult.failed("未提供CaptchaKey");
        String base64;
        try {
            String text = getCaptcha.createText();
            base64 = captchaUtil.getCaptchaBase64(text);
            redisTemplate.opsForValue().set(captchaKey, text, 60, TimeUnit.SECONDS);
        } catch (IllegalArgumentException | IOException e) {
            return CommonResult.failed("验证码生成失败，请重试");
        }
        return CommonResult.success(base64);
    }

    //登录
    @NotNeedLogin
    @PostMapping
    public CommonResult<LoginResult> login(@RequestBody LoginBody loginBody, HttpServletRequest request) {
        String captcha = loginBody.getCaptcha();
        String captchaKey = loginBody.getCaptchaKey();
        String userName = loginBody.getUserName();
        String password = loginBody.getPassword();
        if (!Objects.equals(redisTemplate.opsForValue().get(captchaKey), captcha)) {
            return CommonResult.failed("验证码错误");
        }
        SysUser sysUser = userService.login(userName, password);
        if (sysUser == null) {
            return CommonResult.failed("账号或者密码错误");
        }
        String existToken = request.getHeader("X-Access-Token");
        if (!existToken.isEmpty()) {
            redisTemplate.delete(existToken);
        }
        LoginResult.UserInfo userInfo = modelMapper.map(sysUser, LoginResult.UserInfo.class);
        String token = tokenUtil.getToken(sysUser);
        LoginResult loginResult = new LoginResult();
        loginResult.setUserInfo(userInfo);
        loginResult.setToken(token);
        return CommonResult.success(loginResult);
    }
}
