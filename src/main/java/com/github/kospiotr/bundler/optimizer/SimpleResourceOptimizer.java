package com.github.kospiotr.bundler.optimizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.github.kospiotr.bundler.optimizer.support.CSSMin;
import com.github.kospiotr.bundler.optimizer.support.JSMin;

public class SimpleResourceOptimizer implements ResourceOptimizer {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    public String optimizeJs(String content, JsOptimizerParams params) {
        if (content.isEmpty()) {
            return content;
        }
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(CHARSET));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(output, true, StandardCharsets.UTF_8.name());
            new JSMin(input, printStream).jsmin();
            printStream.flush();
            return new String(output.toByteArray(), CHARSET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String optimizeCss(String content) {
        if (content.isEmpty()) {
            return content;
        }
        try {
            StringReader input = new StringReader(content);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(output, true, StandardCharsets.UTF_8.name());
            CSSMin.formatFile(input, printStream);
            printStream.flush();
            return new String(output.toByteArray(), CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
