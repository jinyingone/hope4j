package fun.jinying.hope.doc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 接口配置
 *
 * @author jy
 * @date 2019-07-08 下午5:43
 **/
@Data
@Configuration
public class Infconfig {
    @Value("${fun.jinying.inf.host}")
    private String host;
}
