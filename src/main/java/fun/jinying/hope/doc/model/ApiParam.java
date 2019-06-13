package fun.jinying.hope.doc.model;

import lombok.Data;

/**
 * 接口参数
 *
 * @author jy
 * @create 2019-04-11 下午3:24
 **/
@Data
public class ApiParam {
    /**
     * 参数名称
     */
    private String name;
    /**
     * 参数类型
     */
    private String type;
    /**
     * 默认值
     */
    private boolean required;
    /**
     * 参数描述
     */
    private String desc;
    /**
     * 取值示例
     */
    private String demoValue;
    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 取值来自于哪个注解
     */
    private String fromAnnotation;
}