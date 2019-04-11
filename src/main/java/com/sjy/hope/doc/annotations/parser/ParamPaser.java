package com.sjy.hope.doc.annotations.parser;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.sjy.hope.doc.model.ApiParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 参数处理器
 *
 * @author jy
 * @create 2019-04-11 下午3:43
 **/
public class ParamPaser {
    private static List<AnnotationParser> parserList = new ArrayList<>(8);

    static {
        parserList.add(new RequestParamParser());
        parserList.add(new RequestParmsParser());
    }

    public static List<ApiParam> parse(AnnotationExpr annotationExpr, Parameter parameter) {
        for (AnnotationParser annotationParser : parserList) {
            List<ApiParam> apiParams = annotationParser.parse(annotationExpr);
            if (apiParams == null || apiParams.isEmpty()) {
                continue;
            }
            if (apiParams.size() == 1) {
                ApiParam apiParam = apiParams.get(0);
                if (parameter != null && apiParam.getName() == null) {
                    apiParam.setName(parameter.getNameAsString());
                }
                if (parameter != null && apiParam.getType() == null) {
                    apiParam.setType(parameter.getType().toString());
                }
                return apiParams;
            } else if (apiParams.size() > 1) {
                return apiParams;
            }

        }
        return Collections.singletonList(defaultParam(parameter));
    }

    private static ApiParam defaultParam(Parameter parameter) {
        if (parameter == null) {
            return null;
        }
        ApiParam apiParam = new ApiParam();
        apiParam.setName(parameter.getNameAsString());
        apiParam.setType(parameter.getType().toString());
        return apiParam;
    }
}
