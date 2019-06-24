package fun.jinying.hope.doc;

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
import fun.jinying.hope.doc.annotations.parser.ParamPaser;
import fun.jinying.hope.doc.model.ApiDoc;
import fun.jinying.hope.doc.model.ApiParam;
import fun.jinying.hope.doc.utils.HttpUtils;
import fun.jinying.hope.doc.views.MdView;
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
public class MyDoc {
    public static void main(String[] args) throws IOException {

        String[] springmvcPaths = new String[]{
        };
        List<ApiDoc> apiDocs = parse(springmvcPaths, RequestMapping.class, RequestMapping.class);
        MdView mdView = new MdView();
        apiDocs.forEach(apiDoc -> {
            mdView.convert(apiDoc);
        });


    }

    private static String getApiPath(ApiDoc apiDoc) {
        StringJoiner stringJoiner = new StringJoiner(";");
        for (String path : apiDoc.getPath()) {
            if (path.startsWith("/*")) {
                stringJoiner.add(path.replace("/*", "/${appid}"));
            } else if (path.startsWith("/{appid}")) {
                stringJoiner.add(path.replace("/{appid}", "/${appid}"));
            } else if (path.startsWith("/660001") || path.startsWith("/660000") || path.startsWith("/330002")) {
                stringJoiner.add(path);
            } else if (path.startsWith("/health")
                    || path.equals("/")
                    || path.equals("")
                    || path.startsWith("/webHealth")
                    || path.startsWith("config")) {
                return "";
            } else {
                stringJoiner.add("/${appid}" + path);
            }
        }
        return stringJoiner.toString();
    }

    private static String getTitle(ApiDoc apiDoc, int length) {
        if (apiDoc.getTitle() == null) {
            return "";
        }
        String subTitle = apiDoc.getTitle().substring(0, apiDoc.getTitle().length() > length ? length : apiDoc.getTitle().length());
        return subTitle.replaceAll("\r|\n", " ");
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
            String httpMethod = getHttpMethod(clazzRequestMapping, methodRequestMapping, "");
            boolean isDeprecated = clazzNode.getAnnotationByName(Deprecated.class.getSimpleName()).isPresent()
                    || methodDeclaration.getAnnotationByName(Deprecated.class.getSimpleName()).isPresent();

            apiDoc.setHttpMethod(httpMethod);
            apiDoc.setPath(getApi(clazzRequestMapping, methodRequestMapping));
            apiDoc.setTitle(javadoc.getDescription().toText());
            apiDoc.setParams(params);
            apiDoc.setDeprecated(isDeprecated);
            List<String> path = apiDoc.getPath();
            String s = HttpUtils.doRequest("" + path.get(0), Collections.singletonMap("appid", "580000"), Collections.emptyMap(), "580000");
            System.out.println(s);

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

    private static List<String> getApi(Optional<AnnotationExpr> clazzRequestMapping, Optional<AnnotationExpr> methodRequestMapping) {
        List<String> paths = new ArrayList<>(8);
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
                        paths.add(clazzPath + methodPath);
                    }
                }
            }
        } else {
            paths.addAll(methodPaths);
        }
        return paths;
    }

    private static List<String> getPath(Optional<AnnotationExpr> requestMapping) {
        AnnotationExpr annotationExpr = requestMapping.get();
        List<String> paths = new ArrayList<>(8);
        if (annotationExpr.isSingleMemberAnnotationExpr()) {
            Expression memberValue = annotationExpr.asSingleMemberAnnotationExpr().getMemberValue();
            if (memberValue.isStringLiteralExpr()) {
                paths.add(memberValue.asStringLiteralExpr().getValue());
            } else {
                List<Node> childNodes = memberValue.getChildNodes();
                for (Node childNode : childNodes) {
                    paths.add(childNode.toString().replaceAll("\"", ""));
                }
            }
        } else if (annotationExpr.isNormalAnnotationExpr()) {
            NodeList<MemberValuePair> pairs = annotationExpr.asNormalAnnotationExpr().getPairs();
            for (MemberValuePair pair : pairs) {
                if (pair.getName().getIdentifier().equals("value") || pair.getName().getIdentifier().equals("path")) {
                    if (pair.getValue().isStringLiteralExpr()) {
                        paths.add(pair.getValue().asStringLiteralExpr().getValue());
                    } else if (pair.getValue().isArrayInitializerExpr()) {
                        NodeList<Expression> values = pair.getValue().asArrayInitializerExpr().getValues();
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
