package com.github.kospiotr.bundler;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class CssTagProcessor extends RegexBasedTagProcessor {

    private static final String TAG_REGEX = "\\Q<link\\E.*?href\\=\"(.*?)\".*?\\>";

    private final PathNormalizator pathNormalizator = new PathNormalizator();
    // We don't add a final modifier here because we need to mock this field in unit test.
    private OptimizerFactory optimizerFactory = OptimizerFactory.getInsatnce();

    @Override
    public String getType() {
        return "css";
    }

    @Override
    public String createBundledTag(String fileName) {
        return "<link rel=\"stylesheet\" href=\"" + fileName + "\" />";
    }

    @Override
    protected String postProcessOutputFileContent(String content) {
        return getResourceOptimizer().optimizeCss(content);
    }

    @Override
    protected String tagRegex() {
        return TAG_REGEX;
    }

    @Override
    protected String preprocessTagContent(String targetCssPath, String content, String sourceCssPath) {
        StringBuilder sb = new StringBuilder();

        Pattern urlPattern = Pattern.compile("url\\(\\s*(['\"]?)\\s*(.*?)\\s*(\\1)\\s*\\)", Pattern.DOTALL);
        Matcher m = urlPattern.matcher(content);
        int previousIndex = 0;
        while (m.find(previousIndex)) {
            String quote = m.group(1);
            String resourcePath = m.group(2);
            sb.append(content.substring(previousIndex, m.start()));
            String relativizedResourcePathUrl = relativizeResourcePath(targetCssPath, sourceCssPath, resourcePath);
            sb.append("url(").append(quote).append(relativizedResourcePathUrl).append(quote).append(")");
            previousIndex = m.end();
        }
        sb.append(content.substring(previousIndex, content.length()));
        return sb.toString();
    }

    private String relativizeResourcePath(String targetCssPath, String sourceCssPath, String resourcePath) {
        if (isUrlAbsolute(resourcePath) || resourcePath.startsWith("data:")) {
            return resourcePath;
        }

        String queryString = "";
        int queryStartIndex = resourcePath.indexOf('?');
        if (queryStartIndex != -1) {
            queryString = resourcePath.substring(queryStartIndex);
            resourcePath = resourcePath.substring(0, queryStartIndex);
        }
        Path absoluteResourcePath = pathNormalizator
                .getAbsoluteResourcePath(getMojo().getInputFilePah().getAbsolutePath(), sourceCssPath, resourcePath);
        Path absoluteTargetCssPath = pathNormalizator
                .getAbsoluteTargetCssPath(getMojo().getOutputFilePath().getAbsolutePath(), targetCssPath);
        return pathNormalizator.relativize(absoluteResourcePath, absoluteTargetCssPath) + queryString;
    }

    private boolean isUrlAbsolute(String url) {
        return url.startsWith("/");
    }

    private ResourceOptimizer getResourceOptimizer() {
        return optimizerFactory.getOptimizer(getMojo().getCssOptimizer());
    }
}
