package fun.jinying.hope.doc.annotations.parser;

import com.github.javaparser.ast.expr.AnnotationExpr;
import fun.jinying.hope.doc.model.ApiParam;

import java.util.List;

/**
 * 注解解析
 */
public interface AnnotationParser {
    /**
     * 注解解析
     *
     * @param annotationExpr
     * @return
     */
    List<ApiParam> parse(AnnotationExpr annotationExpr);
}
