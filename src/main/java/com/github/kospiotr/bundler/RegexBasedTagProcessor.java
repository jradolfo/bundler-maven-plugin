package com.github.kospiotr.bundler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.kospiotr.bundler.util.HashGenerator;

public abstract class RegexBasedTagProcessor extends TagProcessor {

	/**
	 * Placeholder used in the filename to indicate that the hash of the content should be calculated
	 * and placed in the filename.
	 */
	public static final String HASH_PLACEHOLDER = "#hash#";
	
	private static final String REQUEST_CONTEXTPATH_EL_EXPRESSION_REGEX = "#\\{request.contextPath\\}/";
	private static final String FACES_REQUEST_CONTEXTPATH_EL_EXPRESSION_REGEX = "#\\{facesContext.externalContext.request.contextPath\\}/";
    private static final Charset CHARSET = StandardCharsets.UTF_8;    
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
        Path parentSrcPath = getMojo().getInputFilePath().getAbsoluteFile().toPath().getParent();
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
                    	log("Skip optimizing %s because it's already been minified.", tagSource.getSrcPath());
                        processedContent = srcContent;
                    } else {
                    	log("Optimizing %s ...", tagSource.getSrcPath());
                        processedContent = postProcessOutputFileContent(srcContent);
                    }
                    
                    outputBuilder.append(processedContent).append("\n");
                    lengthAfterCompress += processedContent != null ? processedContent.getBytes(CHARSET).length : 0;
                    
                } catch (Exception ex) {
                    log.error("Failed to optimize data. Use it directly. File=" + tagSource.getSrcPath(), ex);
                }
            }
            
            double compressionRatio = lengthAfterCompress != 0 ? (double) lengthAfterCompress / lengthBeforeCompress : 0;
            log.info(String.format("%d->%d CompressionRatio: %d%%", lengthBeforeCompress, lengthAfterCompress, (int) (compressionRatio * 100)));

            String content = outputBuilder.toString();
            Path parentDestPath = getMojo().getOutputFilePath().getAbsoluteFile().toPath().getParent();            
            
            fileName = verifyAndReplaceHashPlaceholder(fileName, content);
            
            Path tagDestPath = getAbsolutResourcePath(fileName, parentDestPath, getMojo().getOutputBaseDir().getAbsoluteFile().toPath());
            
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
     * Verifies if the filename contains the #{@link RegexBasedTagProcessor#HASH_PLACEHOLDER} and if so, calculates the
     * hash and replaces it the filename's placeholder.
     * @param fileName
     * @param content
     * @return The filename with the placeholder replaced or the untouched filename if no placeholder found
     */
	private String verifyAndReplaceHashPlaceholder(String fileName, String content) {
		
		if (fileName.contains(HASH_PLACEHOLDER)) {
		    String hashValue = HashGenerator.computeHash(content, getMojo().getHashingAlgorithm());
		    fileName = fileName.replace(HASH_PLACEHOLDER, hashValue);
		}
		
		return fileName;
	}

    protected Path getAbsolutResourcePath(String srcPath, Path parentSrcPath, Path alternativeParentPath) {
    	String src = srcPath.replaceAll(REQUEST_CONTEXTPATH_EL_EXPRESSION_REGEX, "").replaceAll(FACES_REQUEST_CONTEXTPATH_EL_EXPRESSION_REGEX, "");    	
    	return (src.equals(srcPath)) ? parentSrcPath.resolve(srcPath) : alternativeParentPath.resolve(src);
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
        	Path tagSrcPath = getAbsolutResourcePath(src, parentSrcPath, getMojo().getInputBaseDir().getAbsoluteFile().toPath());            
            String srcContent = resourceAccess.read(tagSrcPath);
            srcContent = preprocessTagContent(fileName, srcContent, src);
            
           log("Loading %s. Length=%d", tagSrcPath, srcContent.getBytes(CHARSET).length);            
        
            tagSources.add(new TagSource(tagSrcPath, srcContent));
        }
        
        return tagSources;
    }

    protected String preprocessTagContent(String fileName, String srcContent, String src) {
        return srcContent;
    }
        
    protected void log(String text, Object...args) {
    	 if (getMojo().isVerbose()) {
             log.info(String.format(text, args));
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
       
        public String getSrcContent() {
            return srcContent;
        }

    }
}
