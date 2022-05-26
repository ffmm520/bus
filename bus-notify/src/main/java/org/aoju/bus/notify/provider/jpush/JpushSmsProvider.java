package org.aoju.bus.notify.provider.jpush;

import org.aoju.bus.core.lang.Header;
import org.aoju.bus.core.lang.MediaType;
import org.aoju.bus.extra.json.JsonKit;
import org.aoju.bus.http.Httpx;
import org.aoju.bus.notify.Builder;
import org.aoju.bus.notify.Context;
import org.aoju.bus.notify.magic.Message;
import org.aoju.bus.notify.provider.AbstractProvider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 极光短信
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class JpushSmsProvider extends AbstractProvider<JpushProperty, Context> {

    public JpushSmsProvider(Context context) {
        super(context);
    }

    @Override
    public Message send(JpushProperty entity) {
        Map<String, Object> bodys = new HashMap<>();
        bodys.put("sign_id", entity.getSignature());
        bodys.put("mobile", entity.getReceive());
        bodys.put("temp_id", entity.getTemplate());
        bodys.put("temp_para", entity.getParams());

        Map<String, String> headers = new HashMap<>();
        headers.put(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(Header.AUTHORIZATION, "Basic " + getSign());

        String response = Httpx.post(entity.getUrl(), bodys, headers);
        boolean succeed = Objects.equals(JsonKit.getValue(response, "success_count"), 0);
        String errcode = succeed ? Builder.ErrorCode.SUCCESS.getCode() : Builder.ErrorCode.FAILURE.getCode();
        String errmsg = succeed ? Builder.ErrorCode.SUCCESS.getMsg() : Builder.ErrorCode.FAILURE.getMsg();

        return Message.builder()
                .errcode(errcode)
                .errmsg(errmsg)
                .build();
    }

    private String getSign() {
        String origin = context.getAppKey() + ":" + context.getAppSecret();
        return Base64.getEncoder().encodeToString(origin.getBytes(StandardCharsets.UTF_8));
    }

}