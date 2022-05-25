package org.aoju.bus.pay.provider.jdpay.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 商户二维码支付接口
 */
@Data
@Builder
@AllArgsConstructor
public class CustomerPayModel extends JdPayEntity {

    private String version;
    private String sign;
    private String merchant;
    private String payMerchant;
    private String device;
    private String tradeNum;
    private String tradeName;
    private String tradeDesc;
    private String tradeTime;
    private String amount;
    private String orderType;
    private String industryCategoryCode;
    private String currency;
    private String note;
    private String callbackUrl;
    private String notifyUrl;
    private String ip;
    private String expireTime;
    private String riskInfo;
    private String goodsInfo;
    private String bizTp;

}