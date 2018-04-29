package com.github.kospiotr.bundler;

import com.github.kospiotr.bundler.optimizer.JsOptimizerParams;
import com.github.kospiotr.bundler.optimizer.OptimizerFactory;
import com.github.kospiotr.bundler.optimizer.ResourceOptimizer;

/**
 * Usage:
 * 
 * <pre>
 * {@code
 *     <!-- build:js inline app.min.js -->
 *     <script src="my/lib/path/lib.js"></script>
 *     <script src="my/deep/development/path/script.js"></script>
 *     <!-- /build -->
 *
 *     <!-- changed to -->
 *     <script>
 *     // app.min.js code here
 *     </script>
 * }
 * </pre>
 */
public class JsTagProcessor extends RegexBasedTagProcessor {

    private static final String TAG_REGEX = "\\Q<script\\E.*?src\\=\"(.*?)\".*?\\>.*?\\Q</script>\\E";

    // We don't add a final modifier here because we need to mock this field in unit test.
    private OptimizerFactory optimizerFactory = OptimizerFactory.getInsatnce();

    @Override
    public String getType() {
        return "js";
    }

    @Override
    public String createBundledTag(String fileName) {
        return "<script src=\"" + fileName + "\"></script>";
    }

    @Override
    protected String postProcessOutputFileContent(String content) {
        JsOptimizerParams params = new JsOptimizerParams();
        params.setMunge(getMojo().isMunge());
        params.setVerbose(getMojo().isVerbose());
        params.setPreserveAllSemiColons(getMojo().isPreserveAllSemiColons());
        params.setDisableOptimizations(getMojo().isDisableOptimizations());
        return getResourceOptimizer().optimizeJs(content, params);
    }

    @Override
    protected String tagRegex() {
        return TAG_REGEX;
    }

    private ResourceOptimizer getResourceOptimizer() {
        return optimizerFactory.getOptimizer(getMojo().getJsOptimizer());
    }
}
