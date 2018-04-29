package com.github.kospiotr.bundler.optimizer;

public interface ResourceOptimizer {

    String optimizeJs(String content, JsOptimizerParams params);

    String optimizeCss(String content);
}
