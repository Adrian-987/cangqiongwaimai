package com.sky.controller.notify;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;

/**
 * 支付回调相关接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 支付成功回调
     *
     * @param request
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //读取数据
        String body = readData(request);
        log.info("支付成功回调：{}", body);

        //先验签，确认请求确实来自微信支付，防止有人伪造回调把订单改成已支付
        if (!verifySignature(request, body)) {
            log.error("微信支付回调验签失败，已拒绝处理");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        //数据解密
        String plainText = decryptData(body);
        log.info("解密后的文本：{}", plainText);

        JSONObject jsonObject = JSON.parseObject(plainText);
        if (jsonObject == null) {
            log.error("微信支付回调内容解析失败，无法获取订单号");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
        String transactionId = jsonObject.getString("transaction_id");//微信支付交易号

        log.info("商户平台订单号：{}", outTradeNo);
        log.info("微信支付交易号：{}", transactionId);

        //业务处理，修改订单状态、来单提醒
        orderService.paySuccess(outTradeNo);

        //给微信响应
        responseToWeixin(response);
    }

    /**
     * 校验微信支付回调签名（微信支付 APIv3 规范）。
     * <p>
     * 待验签串 = timestamp + "\n" + nonce + "\n" + body + "\n"，
     * 使用平台证书公钥做 SHA256withRSA 验签。验签通过才能确认报文来自微信，
     * 否则任何人都可以构造回调把订单刷成「已支付」。
     *
     * @param request 回调请求（需包含 Wechatpay-Timestamp / Nonce / Signature 头）
     * @param body    回调原始报文
     * @return true=验签通过
     */
    private boolean verifySignature(HttpServletRequest request, String body) {
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");
        if (timestamp == null || nonce == null || signature == null) {
            log.error("微信支付回调缺少验签头：timestamp={}, nonce={}, signature={}", timestamp, nonce, signature);
            return false;
        }
        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            X509Certificate certificate = PemUtil.loadCertificate(
                    new FileInputStream(new File(weChatProperties.getWeChatPayCertFilePath())));
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(certificate.getPublicKey());
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return sign.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.error("微信支付回调验签异常", e);
            return false;
        }
    }

    /**
     * 读取数据
     *
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(line);
        }
        return result.toString();
    }

    /**
     * 数据解密
     *
     * @param body
     * @return
     * @throws Exception
     */
    private String decryptData(String body) throws Exception {
        JSONObject resultObject = JSON.parseObject(body);
        if (resultObject == null) {
            throw new IllegalArgumentException("回调报文格式错误");
        }
        JSONObject resource = resultObject.getJSONObject("resource");
        if (resource == null || resource.getString("ciphertext") == null) {
            throw new IllegalArgumentException("回调报文缺少 resource.ciphertext");
        }
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        //密文解密
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        return plainText;
    }

    /**
     * 给微信响应
     * @param response
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception{
        response.setStatus(200);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("code", "SUCCESS");
        map.put("message", "SUCCESS");
        response.setHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        response.getOutputStream().write(JSONUtils.toJSONString(map).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
