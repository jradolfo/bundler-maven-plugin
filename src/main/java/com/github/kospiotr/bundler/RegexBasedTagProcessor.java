package com.github.kospiotr.bundler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public abstract class RegexBasedTagProcessor extends TagProcessor {

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String HASH_PLACEHOLDER = "#hash#";
    private static final String MINIFIED_KEYWORD = ".min.";
    private ResourceAccess resourceAccess = new ResourceAccess();

    /**
     * Construct tag which will be outputted as a result of bundle
     *
     * @param fileName output fileName
     * @return output tag
     */
    protected abstract String createBundledTag(String fileName);

    /**
     * Regex that represents inner tags that will be processed and bundled.
     * It MUST return first capturing group which represents partial file path to read
     *
     * @return inner tag regex
     */
    protected abstract String tagRegex();

    @Override
    public String process(Tag tag) {
        log.info("----------------------------------------");
        log.info("Processing bundling tag: " + tag.getContent());

        String fileName = extractFileName(tag);
        Path parentSrcPath = getMojo().getInputFilePah().getAbsoluteFile().toPath().getParent();
        String tagContent = tag.getContent();

        log.debug("FileName=" + fileName);
        log.debug("ParentSrcPath=" + parentSrcPath);
        log.debug("TagContent=\n" + tagContent.trim());

        try {
            List<TagSource> tagSources = processTags(fileName, parentSrcPath, tagContent);
            log.info("Optimizing...");

            StringBuilder outputBuilder = new StringBuilder();
            int lengthBeforeCompress = 0, lengthAfterCompress = 0;
            for (TagSource tagSource : tagSources) {
                String srcContent = tagSource.getSrcContent();
                lengthBeforeCompress += srcContent.getBytes(CHARSET).length;
                try {
                    // If the filename indicates that the content has been minified, we don't need to optimize it again.
                    String processedContent;
                    if (tagSource.getSrcPath().getFileName().toString().contains(MINIFIED_KEYWORD)) {
                        if (getMojo().isVerbose()) {
                            log.info("Skip optimizing " + tagSource.getSrcPath()
                                    + " because it's already been minified.");
                        }
                        processedContent = srcContent;
                    } else {
                        if (getMojo().isVerbose()) {
                            log.info("Optimizing " + tagSource.getSrcPath() + "...");
                        }
                        processedContent = postProcessOutputFileContent(srcContent);
                    }
                    outputBuilder.append(processedContent).append("\n");
                    lengthAfterCompress += processedContent != null ? processedContent.getBytes(CHARSET).length : 0;
                } catch (Exception ex) {
                    log.error("Failed to optimize data. Use it directly. File=" + tagSource.getSrcPath(), ex);
                }
            }
            double compressionRatio =
                    lengthAfterCompress != 0 ? (double) lengthAfterCompress / lengthBeforeCompress : 0;
            log.info(String.format("%d->%d CompressionRatio: %d%%", lengthBeforeCompress, lengthAfterCompress,
                    (int) (compressionRatio * 100)));

            String content = outputBuilder.toString();
            Path parentDestPath = getMojo().getOutputFilePath().getAbsoluteFile().toPath().getParent();
            if (fileName.contains(HASH_PLACEHOLDER)) {
                String hashValue = computeHash(content);
                fileName = fileName.replace(HASH_PLACEHOLDER, hashValue);
            }
            Path tagDestPath = parentDestPath.resolve(fileName);
            log.info("Writing to file: " + tagDestPath);
            resourceAccess.write(tagDestPath, content);
            String bundledTag = createBundledTag(fileName);
            log.info("Done");
            return bundledTag;
        } catch (Exception ex) {
            log.error(ex);
            throw ex;
        } finally {
            log.info("----------------------------------------");
        }
    }

    /**
     * Template method allowing enhance output file content
     *
     * @param content output file content
     * @return enhanced output file content
     */
    protected String postProcessOutputFileContent(String content) {
        return content;
    }

    private String extractFileName(Tag tag) {
        String[] attributes = tag.getAttributes();
        String fileName = attributes == null || attributes.length == 0 ? null : attributes[0];

        if (fileName == null) {
            throw new IllegalArgumentException("File Name attribute is required");
        }

        return fileName;
    }

    private List<TagSource> processTags(String fileName, Path parentSrcPath, String tagContent) {
        Pattern tagPattern = Pattern.compile(tagRegex(), Pattern.DOTALL);
        Matcher m = tagPattern.matcher(tagContent);
        List<TagSource> tagSources = new ArrayList<>();
        while (m.find()) {
            String src = m.group(1);
            Path tagSrcPath = parentSrcPath.resolve(src);
            String srcContent = resourceAccess.read(tagSrcPath);
            srcContent = preprocessTagContent(fileName, srcContent, src);
            if (getMojo().isVerbose()) {
                log.info(String.format("Loading %s. Length=%d", tagSrcPath, srcContent.getBytes(CHARSET).length));
            }
            tagSources.add(new TagSource(tagSrcPath, srcContent));
        }
        return tagSources;
    }

    protected String preprocessTagContent(String fileName, String srcContent, String src) {
        return srcContent;
    }

    private String computeHash(String content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance(getMojo().getHashingAlgorithm());
            byte[] digest = md5.digest(content.getBytes());
            return new HexBinaryAdapter().marshal(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TagSource {

        private Path srcPath;
        private String srcContent;

        public TagSource(Path srcPath, String srcContent) {
            this.srcPath = srcPath;
            this.srcContent = srcContent;
        }

        public Path getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(Path srcPath) {
            this.srcPath = srcPath;
        }

        public String getSrcContent() {
            return srcContent;
        }

        public void setSrcContent(String srcContent) {
            this.srcContent = srcContent;
        }
    }
}
