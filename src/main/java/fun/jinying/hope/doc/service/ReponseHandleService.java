package fun.jinying.hope.doc.service;

import fun.jinying.hope.doc.config.Infconfig;
import fun.jinying.hope.doc.model.ApiDoc;
import fun.jinying.hope.doc.model.ApiParam;
import fun.jinying.hope.doc.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 返回结果处理
 *
 * @author jy
 * @date 2019-07-08 下午5:48
 **/
@Service
public class ReponseHandleService {
    @Autowired
    private Infconfig infconfig;

    public ApiDoc getReponse(ApiDoc apiDoc) {
        List<String> path = apiDoc.getPath();
        if (path.isEmpty()) {
            return apiDoc;
        }

        String apiPath = apiDoc.getPath().get(0);
        List<ApiParam> apiParams = apiDoc.getParams();
        Map<String, Object> headers = new HashMap<>(8);
        Map<String, Object> params = new HashMap<>(8);
        for (ApiParam param : apiParams) {
            if (param.getFromAnnotation().equals(RequestHeader.class.getSimpleName())) {
                headers.put(param.getName(), param.getDemoValue());
            } else {
                params.put(param.getName(), param.getDemoValue());
            }
        }
        String s = HttpUtils.doRequest(infconfig + "/" + apiPath, params, headers, null);
        apiDoc.setReturnValue(s);
        return apiDoc;
    }
}
