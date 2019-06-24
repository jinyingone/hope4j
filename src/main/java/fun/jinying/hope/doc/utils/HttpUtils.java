package fun.jinying.hope.doc.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * http工具类
 *
 * @author jy
 * @date 2019-06-13 下午5:22
 **/
public class HttpUtils {
    private static RestTemplate restTemplate = new RestTemplate();

    static {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        restTemplate.setRequestFactory(factory);
    }

    public static String doRequest(String url, Map<String, Object> params, Map<String, Object> headers, String uriVariables) {
        MultiValueMap mheaders = new LinkedMultiValueMap();
        headers.forEach((String k, Object v) -> {
            mheaders.add(k, v);
        });
        MultiValueMap mparams = new LinkedMultiValueMap();
        params.forEach((String k, Object v) -> {
            mparams.add(k, v);
        });
        HttpEntity httpEntity = new HttpEntity(mparams, mheaders);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class, uriVariables);
        return exchange.getBody();
    }
}
