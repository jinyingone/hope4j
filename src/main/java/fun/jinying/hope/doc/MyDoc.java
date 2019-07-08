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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
@SpringBootApplication
public class MyDoc implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        String[] springmvcPaths = new String[]{
                "/home/jy/IdeaProjects/hope4j/src/test/java/fun/jinying/hope/doc/ApiControllerTest.java"
        };
        
    }
}
