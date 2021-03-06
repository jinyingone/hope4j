package fun.jinying.hope.doc.annotations.parser;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import fun.jinying.hope.doc.model.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 请求参数解析
 *
 * @author jy
 * @create 2019-04-11 下午3:27
 **/
@Slf4j
public class RequestParamParser implements AnnotationParser {
    private static final Set<String> supportAnnotations = new HashSet<>();

    static {
        supportAnnotations.add(RequestParam.class.getSimpleName());
        supportAnnotations.add(RequestAttribute.class.getSimpleName());
        supportAnnotations.add(RequestHeader.class.getSimpleName());
    }

    @Override
    public List<ApiParam> parse(AnnotationExpr annotationExpr) {
        if (annotationExpr == null &&
                !supportAnnotations.contains(annotationExpr.getNameAsString())) {
            log.debug(annotationExpr.getNameAsString() + "不被处理");
            return null;
        }
        ApiParam apiParam = new ApiParam();
        apiParam.setRequired(true);
        if (annotationExpr.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normalAnnotationExpr = annotationExpr.asNormalAnnotationExpr();
            NodeList<MemberValuePair> pairs = normalAnnotationExpr.getPairs();
            pairs.forEach(memberValuePair -> {
                Expression value = memberValuePair.getValue();
                String name = memberValuePair.getName().getIdentifier();
                if ("value".equals(name) || "name".equals(name)) {
                    if (value.isStringLiteralExpr()) {
                        apiParam.setName(value.asStringLiteralExpr().getValue());
                        apiParam.setFromAnnotation(annotationExpr.getName().getIdentifier());
                    } else {
                        throw new RuntimeException(value.toString());
                    }
                } else if ("required".equals(name)) {
                    BooleanLiteralExpr booleanLiteralExpr = value.asBooleanLiteralExpr();
                    apiParam.setRequired(booleanLiteralExpr.getValue());
                    apiParam.setFromAnnotation(annotationExpr.getName().getIdentifier());
                } else if ("defaultValue".equals(name)) {
                    apiParam.setDefaultValue(value.asStringLiteralExpr().getValue());
                } else {
                    log.warn("不支持的注解参数" + annotationExpr.getName() + ":" + value.toString());
                }
            });
        } else if (annotationExpr.isSingleMemberAnnotationExpr()) {
            Expression value = annotationExpr.asSingleMemberAnnotationExpr().getMemberValue();
            apiParam.setName(value.asStringLiteralExpr().getValue());
            apiParam.setFromAnnotation(annotationExpr.getName().getIdentifier());
        } else if (annotationExpr.isMarkerAnnotationExpr()) {
            apiParam.setFromAnnotation(annotationExpr.getName().getIdentifier());
        } else {
            log.warn("不支持的注解" + annotationExpr.getName());
        }
        return Collections.singletonList(apiParam);
    }
}
