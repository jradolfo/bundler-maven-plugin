package com.github.kospiotr.bundler.optimizer;

public class OptimizerFactory {

    private static final OptimizerFactory INSATNCE = new OptimizerFactory();

    public static OptimizerFactory getInsatnce() {
        return INSATNCE;
    }

    public ResourceOptimizer getOptimizer(String name) {
        if (Optimizers.YUI.equalsIgnoreCase(name)) {
            return new YuiResourceOptimizer();
        }
        return new SimpleResourceOptimizer();
    }
}
