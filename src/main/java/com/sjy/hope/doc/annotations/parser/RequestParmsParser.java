package com.sjy.hope.doc.annotations.parser;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.sjy.hope.doc.annotations.RequestParms;
import com.sjy.hope.doc.model.ApiParam;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数解析
 *
 * @author jy
 * @create 2019-04-11 下午4:20
 **/
@Slf4j
public class RequestParmsParser implements AnnotationParser {
    @Override
    public List<ApiParam> parse(AnnotationExpr annotationExpr) {
        if (annotationExpr == null || !RequestParms.class.getSimpleName().equals(annotationExpr.getNameAsString())) {
            log.info(annotationExpr.getNameAsString() + "不被处理");
            return null;
        }

        if (annotationExpr.isNormalAnnotationExpr()) {
            NodeList<MemberValuePair> pairs = annotationExpr.asNormalAnnotationExpr().getPairs();
            for (MemberValuePair pair : pairs) {
                if (pair.getName().getIdentifier().equals("required")) {
                    if (pair.getValue().isArrayInitializerExpr()) {
                        NodeList<Expression> values = pair.getValue().asArrayInitializerExpr().getValues();
                        List<ApiParam> params = new ArrayList<>(8);
                        for (Expression value : values) {
                            if (value.isStringLiteralExpr()) {
                                ApiParam apiParam = new ApiParam();
                                apiParam.setType("String");
                                apiParam.setRequired(true);
                                apiParam.setName(value.asStringLiteralExpr().getValue());
                                params.add(apiParam);
                            }
                        }
                        return params;
                    }
                }
            }
        }
        return null;
    }
}
