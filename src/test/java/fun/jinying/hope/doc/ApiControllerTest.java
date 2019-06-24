package fun.jinying.hope.doc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试类
 *
 * @author jy
 * @date 2019-06-24 下午6:27
 **/
@Controller
public class ApiControllerTest {
    @RequestMapping("/v1/api/test")
    @ResponseBody
    public Map<String, Object> a1(@RequestParam(required = false, name = "p_1") String t1,
                                  @RequestHeader(name = "h_1") String h1) {
        Map<String, Object> result = new HashMap<>();
        result.put("t1", t1);
        result.put("h1", h1);
        return result;
    }
}
