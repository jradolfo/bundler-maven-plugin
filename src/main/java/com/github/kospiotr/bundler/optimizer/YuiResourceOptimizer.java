package com.github.kospiotr.bundler.optimizer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.mozilla.javascript.tools.ToolErrorReporter;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class YuiResourceOptimizer implements ResourceOptimizer {

    @Override
    public String optimizeJs(String content, JsOptimizerParams params) {
        if (content.isEmpty()) {
            return content;
        }
        try {
            StringWriter out = new StringWriter();
            ToolErrorReporter toolErrorReporter = new ToolErrorReporter(true);
            JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(content), toolErrorReporter);
            compressor.compress(out, -1, params.isMunge(), params.isVerbose(), params.isPreserveAllSemiColons(),
                    params.isDisableOptimizations());
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String optimizeCss(String content) {
        if (content.isEmpty()) {
            return content;
        }
        try {
            StringWriter out = new StringWriter();
            CssCompressor compressor = new CssCompressor(new StringReader(content));
            compressor.compress(out, -1);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
