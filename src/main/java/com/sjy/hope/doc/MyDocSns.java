package com.sjy.hope.doc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.sjy.hope.doc.annotations.Api;
import com.sjy.hope.doc.annotations.RequestParms;
import com.sjy.hope.doc.annotations.parser.ParamPaser;
import com.sjy.hope.doc.model.ApiDoc;
import com.sjy.hope.doc.model.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 我的文档
 *
 * @author jy
 * @create 2019-01-24 下午5:26
 **/
@Slf4j
public class MyDocSns {
    public static void main(String[] args) throws IOException {
        String[] mySelfPaths = new String[]{"/home/jy/IdeaProjects/sns_api/src/main/java/com/sohu/sns/api/controller/"};
        List<ApiDoc> all = new ArrayList<>(512);
        all.addAll(parse(mySelfPaths, Api.class, RequestParms.class));


        String[] springmvcPaths = new String[]{"/home/jy/IdeaProjects/sns-user-api/user-api/src/main/java/com/sohu/sns/userapi/api/controller",
                "/home/jy/IdeaProjects/sns-ant-moving/src/main/java/com/sohu/sns/antmv/controller",
                "/home/jy/IdeaProjects/sns-rcmd-data-api/src/main/java/com/sohu/sns/rcmd/data/api/controller",
                "/home/jy/IdeaProjects/sns_service_upload/src/main/java/com/sohu/sns/upload/controller"};
        all.addAll(parse(springmvcPaths, RequestMapping.class, RequestMapping.class));


        List<ApiDoc> deprecateds = all.stream().filter(apiDoc -> apiDoc.isDeprecated()).collect(Collectors.toList());
        deprecateds.sort(Comparator.comparing(ApiDoc::getPath));
        System.out.println("过期的接口数量:" + deprecateds.size());
        deprecateds.forEach(System.out::println);

        List<ApiDoc> unDeprecateds = all.stream().filter(apiDoc -> !apiDoc.isDeprecated()).collect(Collectors.toList());
        unDeprecateds.sort(Comparator.comparing(ApiDoc::getPath));
        System.out.println("推荐使用接口数量:" + unDeprecateds.size());
        unDeprecateds.forEach(System.out::println);


    }

    private static List<ApiDoc> parse(String[] paths, Class clazzMapping, Class methodMapping) throws IOException {
        List<ApiDoc> all = new ArrayList<>(64);
        for (String path : paths) {
            File file = new File(path);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    if (f.isDirectory()) {
                        continue;
                    }
                    FileInputStream in = new FileInputStream(f);
                    List<ApiDoc> doc = createDoc(in, clazzMapping, methodMapping);
                    all.addAll(doc);
                    in.close();
                }
            } else if (file.isFile()) {
                FileInputStream in = new FileInputStream(path);
                List<ApiDoc> doc = createDoc(in, RequestMapping.class, RequestMapping.class);
                all.addAll(doc);
                in.close();
            }
        }
        return all;
    }

    public static List<ApiDoc> createDoc(InputStream controllerInputStream, Class clazzMapping, Class methodMapping) throws
            IOException {
        CompilationUnit cu = JavaParser.parse(controllerInputStream);
        List<Node> childNodes = cu.getChildNodes();
        List<Node> clazzList = childNodes.stream().filter(node -> node instanceof ClassOrInterfaceDeclaration).collect(Collectors.toList());
        assert (clazzList.size() == 1);
        ClassOrInterfaceDeclaration clazzNode = (ClassOrInterfaceDeclaration) clazzList.get(0);
        Optional<AnnotationExpr> clazzRequestMapping = clazzNode.getAnnotationByName(clazzMapping.getSimpleName());
        List<Node> methodNodes = clazzNode.getChildNodes().stream().filter(node -> node instanceof MethodDeclaration).collect(Collectors.toList());
        List<ApiDoc> docs = new ArrayList<>();
        for (Node methodNode : methodNodes) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) methodNode;
            if (!methodDeclaration.getAccessSpecifier().equals(Modifier.Keyword.PUBLIC)) {
                log.warn(methodDeclaration.getNameAsString() + " :不是public方法，跳过");
                continue;
            }
            Javadoc javadoc = JavaParser.parseJavadoc(methodDeclaration.getComment().orElse(new JavadocComment("")).getContent());
            List<JavadocBlockTag> blockTags = javadoc.getBlockTags();

            ApiDoc apiDoc = new ApiDoc();

            Map<String, JavadocBlockTag> paramDocs = new HashMap<>(8);
            for (JavadocBlockTag blockTag : blockTags) {
                if (blockTag.getType().equals(JavadocBlockTag.Type.PARAM)) {
                    paramDocs.put(blockTag.getTagName(), blockTag);
                }
            }

            List<ApiParam> params = getMethodParams(methodDeclaration);
            Optional<AnnotationExpr> methodRequestMapping = methodDeclaration.getAnnotationByName(methodMapping.getSimpleName());
            if (!methodRequestMapping.isPresent()) {
                log.warn(methodDeclaration.getNameAsString() + ":不存在注解" + methodMapping.getSimpleName());
                continue;
            }
            String api = getApi(clazzRequestMapping, methodRequestMapping);

            String httpMethod = getHttpMethod(clazzRequestMapping, methodRequestMapping, "");
            boolean isDeprecated = clazzNode.getAnnotationByName(Deprecated.class.getSimpleName()).isPresent()
                    || methodDeclaration.getAnnotationByName(Deprecated.class.getSimpleName()).isPresent();

            apiDoc.setHttpMethod(httpMethod);
            apiDoc.setPath(api);
            apiDoc.setTitle(javadoc.getDescription().toText());
            apiDoc.setParams(params);
            apiDoc.setDeprecated(isDeprecated);
            docs.add(apiDoc);
        }
        return docs;
    }

    private static List<ApiParam> getMethodParams(MethodDeclaration methodDeclaration) {
        NodeList<Parameter> parameters = methodDeclaration.getParameters();
        List<ApiParam> apiParams = new ArrayList<>(8);
        parameters.forEach(parameter -> {
            NodeList<AnnotationExpr> annotations = parameter.getAnnotations();
            annotations.forEach(annotationExpr -> {
                List<ApiParam> apiParam = ParamPaser.parse(annotationExpr, parameter);
                if (apiParam != null) {
                    apiParams.addAll(apiParam);
                }
            });

        });

        methodDeclaration.getAnnotations().forEach(annotationExpr -> {
            List<ApiParam> apiParam = ParamPaser.parse(annotationExpr, null);
            if (apiParam != null) {
                apiParams.addAll(apiParam);
            }
        });
        return apiParams;
    }

    private static String getHttpMethod
            (Optional<AnnotationExpr> clazzRequestMapping, Optional<AnnotationExpr> methodRequestMapping, String
                    defalut) {
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
        StringJoiner apiJoiner = new StringJoiner(";");
        List<String> clazzPaths = null;
        if (clazzRequestMapping.isPresent()) {
            clazzPaths = getPath(clazzRequestMapping);
        }
        List<String> methodPaths = null;
        if (methodRequestMapping.isPresent()) {
            methodPaths = getPath(methodRequestMapping);
        }

        if (clazzPaths != null && !clazzPaths.isEmpty()) {
            for (String clazzPath : clazzPaths) {
                if (methodPaths != null && !methodPaths.isEmpty()) {
                    for (String methodPath : methodPaths) {
                        apiJoiner.add(clazzPath + methodPath);
                    }
                }
            }
        } else {
            for (String methodPath : methodPaths) {
                apiJoiner.add(methodPath);
            }
        }
        return apiJoiner.toString();
    }

    private static List<String> getPath(Optional<AnnotationExpr> requestMapping) {
        AnnotationExpr annotationExpr = requestMapping.get();
        List<String> paths = new ArrayList<>(8);
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
            paths.add(value);
        } else if (annotationExpr.isNormalAnnotationExpr()) {
            NodeList<MemberValuePair> pairs = annotationExpr.asNormalAnnotationExpr().getPairs();
            for (MemberValuePair pair : pairs) {
                if (pair.getName().getIdentifier().equals("value") || pair.getName().getIdentifier().equals("path")) {
                    if (pair.getValue().isStringLiteralExpr()) {
                        paths.add(pair.getValue().asStringLiteralExpr().getValue());

                    } else if (pair.getValue().isArrayInitializerExpr()) {
                        NodeList<Expression> values = pair.getValue().asArrayInitializerExpr().getValues();
                        String pt = "";
                        for (Expression value : values) {
                            paths.add(value.asStringLiteralExpr().getValue());
                        }
                    }
                } else if (pair.getName().getIdentifier().equals("versionPrefix")) {
                    paths.add(pair.getValue().asStringLiteralExpr().getValue());
                }
            }
        }
        return paths;
    }

    private static Optional<List<String>> getHttpMethod(Optional<AnnotationExpr> requestMapping) {
        if (requestMapping.isPresent()) {
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
        }
        return Optional.empty();
    }
}
