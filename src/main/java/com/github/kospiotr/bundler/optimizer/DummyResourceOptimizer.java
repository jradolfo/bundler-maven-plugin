package com.github.kospiotr.bundler.optimizer;

public class DummyResourceOptimizer implements ResourceOptimizer {

    @Override
    public String optimizeJs(String content, JsOptimizerParams params) {
        return content;
    }

    @Override
    public String optimizeCss(String content) {
        return content;
    }
}
