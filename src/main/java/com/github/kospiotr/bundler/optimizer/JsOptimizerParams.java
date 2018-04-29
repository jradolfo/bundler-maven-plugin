package com.github.kospiotr.bundler.optimizer;

public class JsOptimizerParams {

    private boolean munge;
    private boolean verbose;
    private boolean preserveAllSemiColons;
    private boolean disableOptimizations;

    public boolean isMunge() {
        return munge;
    }

    public void setMunge(boolean munge) {
        this.munge = munge;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isPreserveAllSemiColons() {
        return preserveAllSemiColons;
    }

    public void setPreserveAllSemiColons(boolean preserveAllSemiColons) {
        this.preserveAllSemiColons = preserveAllSemiColons;
    }

    public boolean isDisableOptimizations() {
        return disableOptimizations;
    }

    public void setDisableOptimizations(boolean disableOptimizations) {
        this.disableOptimizations = disableOptimizations;
    }
}
