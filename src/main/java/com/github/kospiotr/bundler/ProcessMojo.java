package com.github.kospiotr.bundler;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Generate package bundles.
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ProcessMojo extends AbstractMojo {

    /**
     * Input file.
     */
    @Parameter(property = "inputFile", required = true)
    File inputFilePah;

    /**
     * Location of the output file.
     */
    @Parameter(property = "outputFile", required = true)
    File outputFilePath;
    
    /**
     * Location of the base of the web application. This will be used to enable the processing of JS / CSS resources that 
     * contains EL Expressions like # {request.contextPath} or # {facescontext.externalContext.request.contextPath}. Those 
     * expressions will be replaced by the webappBaseDir for processing purpose.
     * If those EL Expressions are found but no webappdir is defined an exception will be thrown.
     */
    @Parameter(property = "inputBaseDir", required = false, defaultValue="${project.basedir}/src/main/webapp/")
    File inputBaseDir;
    
    @Parameter(property = "outputBaseDir", required = false)
    File outputBaseDir;


	/**
     * Hashing Algrithm. Possible values for shipped providers:
     * MD5,
     * SHA-1,
     * SHA-256
     */
    @Parameter(defaultValue = "MD5", property = "hashingAlgorithm", required = true)
    String hashingAlgorithm;

    @Parameter(defaultValue = "true", property = "munge", required = true)
    boolean munge;

    @Parameter(defaultValue = "false", property = "verbose", required = true)
    boolean verbose;

    @Parameter(defaultValue = "true", property = "preserveAllSemiColons", required = true)
    boolean preserveAllSemiColons;

    @Parameter(defaultValue = "true", property = "disableOptimizations", required = true)
    boolean disableOptimizations;

    @Parameter(defaultValue = "simple", property = "cssOptimizer", required = true)
    String cssOptimizer;

    @Parameter(defaultValue = "simple", property = "jsOptimizer", required = true)
    String jsOptimizer;

    public ProcessMojo() {
    }

    ProcessMojo(File inputFilePah, File outputFilePath) {
        this.inputFilePah = inputFilePah;
        this.outputFilePath = outputFilePath;
        this.hashingAlgorithm = "MD5";
    }
    
    ProcessMojo(File inputFilePah, File outputFilePath, File inputBaseDir, File outputBaseDir) {
        this.inputFilePah = inputFilePah;
        this.outputFilePath = outputFilePath;
        this.inputBaseDir = inputBaseDir;
        this.outputBaseDir = outputBaseDir;
        this.hashingAlgorithm = "MD5";
    }
    

    public void execute() {
        Tokenizer tokenizer = new Tokenizer(this);
        tokenizer.registerProcessor(new RemoveTagProcessor());
        tokenizer.registerProcessor(new JsTagProcessor());
        tokenizer.registerProcessor(new CssTagProcessor());

        FileProcessor fileProcessor = new FileProcessor(tokenizer);
        fileProcessor.process(inputFilePah.toPath(), outputFilePath.toPath());
    }

    public File getInputFilePah() {
        return inputFilePah;
    }

    public File getOutputFilePath() {
        return outputFilePath;
    }
        
    public File getInputBaseDir() {
		return inputBaseDir;
	}

	public File getOutputBaseDir() {
		return outputBaseDir;
	}

	public String getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    public boolean isMunge() {
        return munge;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isPreserveAllSemiColons() {
        return preserveAllSemiColons;
    }

    public boolean isDisableOptimizations() {
        return disableOptimizations;
    }

    public String getCssOptimizer() {
        return cssOptimizer;
    }

    public String getJsOptimizer() {
        return jsOptimizer;
    }
}