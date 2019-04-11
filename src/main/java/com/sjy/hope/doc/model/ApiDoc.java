package com.sjy.hope.doc.model;

import com.sjy.hope.doc.MyDocSns;
import lombok.Data;

import java.util.List;

/**
 * api接口文档
 *
 * @author jy
 * @create 2019-04-11 下午3:24
 **/
@Data
public class ApiDoc {
    private String title;
    private String path;
    private String httpMethod;
    private List<ApiParam> params;
    private String demoRequest;
    private String returnValue;
    private boolean deprecated;
}