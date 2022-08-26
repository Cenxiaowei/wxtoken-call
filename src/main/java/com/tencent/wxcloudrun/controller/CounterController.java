package com.tencent.wxcloudrun.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;

/**
 * counter控制器
 */
@RestController

public class CounterController {

  final CounterService counterService;
  final Logger logger;

  /**
   * 验证签名
   *
   * @param signature
   * @param timestamp
   * @param nonce
   * @return
   */
  public static boolean checkSignature(String signature, String timestamp, String nonce) {

    // 将token、timestamp、nonce三个参数进行字典排序
    String[] arr = new String[] {"yanjiaoshou", timestamp, nonce};
    Arrays.sort(arr);

    // 将三个参数字符串拼接成一个字符串
    StringBuilder content = new StringBuilder();
    for (int i = 0; i < arr.length; i++) {
      content.append(arr[i]);
    }
    try {
      //获取加密工具
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      // 对拼接好的字符串进行sha1加密
      byte[] digest = md.digest(content.toString().getBytes());
      String tmpStr = byteToStr(digest);
      //获得加密后的字符串与signature对比
      return tmpStr != null ? tmpStr.equals(signature.toUpperCase()): false;
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return false;
  }

  private static String byteToStr(byte[] byteArray) {
    String strDigest = "";
    for (int i = 0; i < byteArray.length; i++) {
      strDigest += byteToHexStr(byteArray[i]);
    }
    return strDigest;
  }

  private static String byteToHexStr(byte mByte) {
    char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
            'B', 'C', 'D', 'E', 'F' };
    char[] tempArr = new char[2];
    tempArr[0] = Digit[(mByte >>> 4) & 0X0F];
    tempArr[1] = Digit[mByte & 0X0F];
    String s = new String(tempArr);
    return s;
  }




  @GetMapping(value = "/wxCallback")
  @ResponseBody
  public void openPushMsg(HttpServletRequest request, HttpServletResponse response) {

    // 微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
    String signature = request.getParameter("signature");
    // 时间戳
    String timestamp = request.getParameter("timestamp");
    // 随机数
    String nonce = request.getParameter("nonce");
    // 随机字符串
    String echostr = request.getParameter("echostr");

    PrintWriter out = null;
    try {
      out = response.getWriter();
      if (checkSignature(signature, timestamp, nonce)) {
        out.print(echostr);
        out.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      out.close();
    }

  }












  public CounterController(@Autowired CounterService counterService) {
    this.counterService = counterService;
    this.logger = LoggerFactory.getLogger(CounterController.class);
  }


  /**
   * 获取当前计数
   * @return API response json
   */
  @GetMapping(value = "/api/count")
  ApiResponse get() {
    logger.info("/api/count get request");
    Optional<Counter> counter = counterService.getCounter(1);
    Integer count = 0;
    if (counter.isPresent()) {
      count = counter.get().getCount();
    }

    return ApiResponse.ok(count);
  }


  /**
   * 更新计数，自增或者清零
   * @param request {@link CounterRequest}
   * @return API response json
   */
  @PostMapping(value = "/api/count")
  ApiResponse create(@RequestBody CounterRequest request) {
    logger.info("/api/count post request, action: {}", request.getAction());

    Optional<Counter> curCounter = counterService.getCounter(1);
    if (request.getAction().equals("inc")) {
      Integer count = 1;
      if (curCounter.isPresent()) {
        count += curCounter.get().getCount();
      }
      Counter counter = new Counter();
      counter.setId(1);
      counter.setCount(count);
      counterService.upsertCount(counter);
      return ApiResponse.ok(count);
    } else if (request.getAction().equals("clear")) {
      if (!curCounter.isPresent()) {
        return ApiResponse.ok(0);
      }
      counterService.clearCount(1);
      return ApiResponse.ok(0);
    } else {
      return ApiResponse.error("参数action错误");
    }
  }












}