package fun.jinying.hope.doc.views;

import fun.jinying.hope.doc.model.ApiDoc;
import fun.jinying.hope.doc.model.ApiParam;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * markdown 显示
 *
 * @author jy
 * @create 2019-05-10 下午5:12
 **/
public class MdView {
    Pattern normalPattern = Pattern.compile("\\$\\{(\\w+)+\\}");
    private Pattern tableLineStart = Pattern.compile("^table_tr:(\\w+)\\s+");

    public void convert(ApiDoc apiDoc) {
        try {
            Map<String, Object> map = getMap(apiDoc);

            URL resource = getClass().getClassLoader().getResource("api_doc_template.md");
            Path path = Paths.get(resource.toURI());

            List<String> collect = Files.readAllLines(path).stream().map(line -> {
                Matcher normalMatcher = normalPattern.matcher(line);
                Matcher tableLineMacher = tableLineStart.matcher(line);
                if (tableLineMacher.find() && normalMatcher.find()) {
                    //表格
                    List<Map<String, Object>> tabDatas = (List<Map<String, Object>>) map.get(tableLineMacher.group(1));
                    if (tabDatas == null) {
                        return "";
                    }
                    StringJoiner stringJoiner = new StringJoiner("|");
                    List<String> params = new ArrayList<>();
                    for (; ; ) {
                        params.add(normalMatcher.group(1));
                        if (!normalMatcher.find()) {
                            break;
                        }
                    }
                    for (Map<String, Object> tabData : tabDatas) {
                        for (String param : params) {
                            Object o = tabData.get(param);
                            if (o != null) {
                                stringJoiner.add(o.toString());
                            } else {
                                stringJoiner.add("");
                            }
                        }
                        stringJoiner.add("\n");
                    }
                    return stringJoiner.toString();
                } else if (normalMatcher.find()) {
                    String group = normalMatcher.group(1);
                    Object data = map.get(group);
                    if (group != null && data != null) {
                        return normalMatcher.replaceFirst(data.toString());
                    }

                }
                return line;
            })
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            String infMd = apiDoc.getPath().get(0).replaceAll("/", "_") + ".md";
            Path infMdPath = Paths.get(infMd);
            if (!Files.exists(infMdPath)) {
                Files.createFile(infMdPath);
            } else {
                Files.delete(infMdPath);
                Files.createFile(infMdPath);
            }

            for (String line : collect) {
                line = line + "  \n";
                Files.write(infMdPath
                        , line.getBytes()
                        , StandardOpenOption.APPEND);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Map<String, Object> getMap(ApiDoc apiDoc) {
        Map<String, Object> map = new HashMap<>();
        String apiPath = apiDoc.getPath().stream().collect(Collectors.joining(";"));
        String httpMethod = apiDoc.getHttpMethod() == "" ? HttpMethod.GET.name() + "/" + HttpMethod.POST.name() : apiDoc.getHttpMethod();
        map.put("inf_name", apiPath);
        map.put("inf_desc", apiDoc.getTitle().replaceAll("\n", "<br>"));
        map.put("inf_hosts", "127.0.0.1:8080");
        map.put("inf_path", apiPath);
        map.put("inf_http_method", httpMethod);

        List<ApiParam> params = apiDoc.getParams();
        List<Map<String, Object>> headers = new ArrayList<>(8);
        List<Map<String, Object>> urlParams = new ArrayList<>(8);
        for (ApiParam param : params) {
            if (param == null) {
                continue;
            }
            String fromAnnotation = param.getFromAnnotation();
            Map<String, Object> pmap = new HashMap<>();
            if (RequestHeader.class.getSimpleName().equals(fromAnnotation)
                    || RequestAttribute.class.getSimpleName().equals(fromAnnotation)) {
                pmap.put("header_name", param.getName());
                pmap.put("header_value_type", param.getType());
                pmap.put("header_required", param.isRequired());
                pmap.put("header_desc", param.getDesc());
                headers.add(pmap);
            } else if (RequestParam.class.getSimpleName().equals(fromAnnotation)) {
                pmap.put("param_name", param.getName());
                pmap.put("param_value_type", param.getType());
                pmap.put("param_required", param.isRequired());
                pmap.put("param_desc", param.getDesc());
                urlParams.add(pmap);
            }
        }
        map.put("header", headers);
        return map;
    }


}
