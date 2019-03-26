package com.sjy.hope.doc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 我的文档
 *
 * @author jy
 * @create 2019-01-24 下午5:26
 **/
public class MyDoc {
    public static void main(String[] args) throws IOException {
        createDoc();
    }

    public static void createDoc() throws IOException {
        String controllerFilePath = "/home/jy/IdeaProjects/sns-ant-moving/src/main/java/com/sohu/sns/antmv/controller/AntMvController.java";
        FileInputStream in = new FileInputStream(controllerFilePath);
        CompilationUnit cu = JavaParser.parse(in);
        List<Node> childNodes = cu.getChildNodes();
        List<Node> clazzList = childNodes.stream().filter(node -> node instanceof ClassOrInterfaceDeclaration).collect(Collectors.toList());
        assert (clazzList.size() == 1);
        ClassOrInterfaceDeclaration clazzNode = (ClassOrInterfaceDeclaration) clazzList.get(0);
        Optional<AnnotationExpr> clazzRequestMapping = clazzNode.getAnnotationByName("RequestMapping");

        List<Node> methodNodes = clazzNode.getChildNodes().stream().filter(node -> node instanceof MethodDeclaration).collect(Collectors.toList());
        for (Node methodNode : methodNodes) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) methodNode;
            Javadoc javadoc = JavaParser.parseJavadoc(methodDeclaration.getComment().orElse(new JavadocComment("")).getContent());
            List<JavadocBlockTag> blockTags = javadoc.getBlockTags();

            ApiDoc apiDoc = new ApiDoc();
            List<ApiParam> params = new ArrayList<>();
            for (JavadocBlockTag blockTag : blockTags) {
                if (blockTag.getType().equals(JavadocBlockTag.Type.PARAM)) {
                    methodDeclaration.getParameterByName(blockTag.getName().get()).ifPresent(parameter -> {
                        getParam(parameter, "RequestParam", "RequestHeader").ifPresent(apiParam -> {
                            params.add(apiParam);
                        });
                    });
                }
            }

            Optional<AnnotationExpr> MethodRequestMapping = methodDeclaration.getAnnotationByName("RequestMapping");
            String api = getApi(clazzRequestMapping, MethodRequestMapping);

            String httpMethod = getHttpMethod(clazzRequestMapping, MethodRequestMapping, "");


            apiDoc.setHttpMethod(httpMethod);
            apiDoc.setApi(api);
            apiDoc.setTitle(javadoc.getDescription().toText());
            apiDoc.setParams(params);

            System.out.println(apiDoc);

        }
    }

    private static String getHttpMethod(Optional<AnnotationExpr> clazzRequestMapping, Optional<AnnotationExpr> methodRequestMapping, String defalut) {
        Optional<List<String>> clazzHttpMethods = getHttpMethod(clazzRequestMapping);
        Optional<List<String>> methodHttpMethods = getHttpMethod(methodRequestMapping);
        Set<String> allHttpMethod = new HashSet<>();
        if (clazzHttpMethods.isPresent()) {
            allHttpMethod.addAll(clazzHttpMethods.get());
        }
        if (methodHttpMethods.isPresent()) {
            allHttpMethod.addAll(methodHttpMethods.get());
        }

        allHttpMethod.remove(null);
        if (!allHttpMethod.isEmpty()) {
            String join = String.join(",", allHttpMethod);
            return join;
        }
        return defalut;
    }

    private static String getApi(Optional<AnnotationExpr> clazzRequestMapping, Optional<AnnotationExpr> methodRequestMapping) {
        Optional<String> clazzApi = getApi(clazzRequestMapping);
        Optional<String> methodApi = getApi(methodRequestMapping);
        String api = "";
        if (clazzApi.isPresent()) {
            api += clazzApi.get();
        }
        if (methodApi.isPresent()) {
            api += methodApi.get();
        }
        return api;
    }

    private static Optional<String> getApi(Optional<AnnotationExpr> clazzRequestMapping) {
        AnnotationExpr annotationExpr = clazzRequestMapping.get();
        if (annotationExpr.isSingleMemberAnnotationExpr()) {
            Expression memberValue = annotationExpr.asSingleMemberAnnotationExpr().getMemberValue();
            String value = "";
            if (memberValue.isStringLiteralExpr()) {
                value = memberValue.asStringLiteralExpr().getValue();
            } else {
                List<Node> childNodes = memberValue.getChildNodes();

                for (Node childNode : childNodes) {
                    value += childNode.toString();
                }
            }
            return Optional.ofNullable(value);
        } else if (annotationExpr.isNormalAnnotationExpr()) {
            NodeList<MemberValuePair> pairs = annotationExpr.asNormalAnnotationExpr().getPairs();
            for (MemberValuePair pair : pairs) {
                if (pair.getName().getIdentifier().equals("value") || pair.getName().getIdentifier().equals("path")) {
                    return Optional.of(pair.getValue().asStringLiteralExpr().getValue());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<List<String>> getHttpMethod(Optional<AnnotationExpr> requestMapping) {
        AnnotationExpr annotationExpr = requestMapping.get();
        if (annotationExpr.isNormalAnnotationExpr()) {
            NodeList<MemberValuePair> pairs = annotationExpr.asNormalAnnotationExpr().getPairs();
            for (MemberValuePair pair : pairs) {
                if (pair.getName().getIdentifier().equals("method")) {
                    Expression value = pair.getValue();
                    if (value.isArrayInitializerExpr()) {
                        NodeList<Expression> values = value.asArrayInitializerExpr().getValues();
                        List<String> collect = values.stream().map(expression -> {
                            if (expression.isFieldAccessExpr()) {
                                return expression.asFieldAccessExpr().getName().getIdentifier();
                            }
                            return null;
                        }).collect(Collectors.toList());
                        return Optional.of(collect);
                    } else if (value.isFieldAccessExpr()) {
                        return Optional.of(Arrays.asList(value.asFieldAccessExpr().getName().getIdentifier()));
                    }

                }
            }
        }
        return Optional.empty();
    }

    private static Optional<ApiParam> getParam(Parameter parameter, String... annotations) {
        for (String annotation : annotations) {
            Optional<AnnotationExpr> annotationByNameOp = parameter.getAnnotationByName(annotation);
            if (annotationByNameOp.isPresent()) {
                AnnotationExpr annotationExpr = annotationByNameOp.get();
                ApiParam apiParam = new ApiParam();
                if (annotationExpr instanceof SingleMemberAnnotationExpr) {
                    Expression memberValue = ((SingleMemberAnnotationExpr) annotationExpr).getMemberValue();
                    if (memberValue.isStringLiteralExpr()) {
                        apiParam.setName(memberValue.asStringLiteralExpr().getValue());
                    }
                } else if (annotationExpr instanceof NormalAnnotationExpr) {
                    NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) annotationExpr).getPairs();
                    for (MemberValuePair pair : pairs) {
                        if (pair.getName().getIdentifier().equals("required")) {
                            apiParam.setRequired(pair.getValue().asBooleanLiteralExpr().getValue());
                        } else if (pair.getName().getIdentifier().equals("value")) {
                            apiParam.setName(pair.getValue().asStringLiteralExpr().getValue());
                        }
                    }
                } else {
                    apiParam.setName(parameter.getNameAsString());
                    apiParam.setRequired(true);
                    apiParam.setFromAnnotation(annotation);
                }
                apiParam.setType(parameter.getTypeAsString());
                apiParam.setFromAnnotation(annotation);
                return Optional.of(apiParam);
            }
        }


        //处理注解的参数
        ApiParam apiParam = new ApiParam();
        apiParam.setType(parameter.getTypeAsString());
        apiParam.setName(parameter.getNameAsString());
        apiParam.setRequired(true);


        return Optional.of(apiParam);


    }


    @Setter
    @Getter
    @ToString
    public static class ApiDoc {
        private String title;
        private String api;
        private String httpMethod;
        private List<ApiParam> params;
        private String demoRequest;
        private String returnValue;
    }

    @Setter
    @Getter
    @ToString
    public static class ApiParam {
        private String name;
        private String type;
        private boolean required;
        private String demoValue;
        private String fromAnnotation;
    }

}
