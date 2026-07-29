package io.github.rohits1402.gimmecomments.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("api/v1/initialization")
public class InitializationController {
    record BuildFilesEnvelope(List<String> jsFiles, List<String> cssFiles) {
    }

    @GetMapping
    public BuildFilesEnvelope loadBuild() throws IOException {
        return new BuildFilesEnvelope(
                listFiles("static/build/static/js"),
                listFiles("static/build/static/css"));
    }

    private List<String> listFiles(String classpathDir) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:" + classpathDir + "/*");
        return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .filter(name -> name.endsWith(".js") || name.endsWith(".css"))
                .toList();

    }
}
