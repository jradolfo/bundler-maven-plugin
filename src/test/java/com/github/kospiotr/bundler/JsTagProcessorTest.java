package com.github.kospiotr.bundler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.kospiotr.bundler.optimizer.JsOptimizerParams;
import com.github.kospiotr.bundler.optimizer.OptimizerFactory;
import com.github.kospiotr.bundler.optimizer.ResourceOptimizer;

@RunWith(MockitoJUnitRunner.class)
public class JsTagProcessorTest {

    @Spy
    ProcessMojo processMojo = new ProcessMojo(new File("index-dev.html"), new File("index.html"), new File("/input"), new File("/output"));

    @Mock
    ResourceAccess resourceAccess;

    @Mock
    OptimizerFactory optimizerFactory;

    @Mock
    ResourceOptimizer resourceOptimizer;

    @InjectMocks
    JsTagProcessor jsTagProcessor;

    @Before
    public void before() {
        Mockito.when(optimizerFactory.getOptimizer(anyString())).thenReturn(resourceOptimizer);
    }

    @Test
    public void shouldRejectWhenNoFileNameAttributeGiven() throws Exception {
        Tag jsTag = createJsTag("");

        try {
            jsTagProcessor.process(jsTag);
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertThat(e).hasMessage("File Name attribute is required");
            verify(resourceAccess, never()).read(any(Path.class));
            verify(resourceAccess, never()).write(any(Path.class), any(String.class));
            verify(resourceAccess, never()).write(any(Path.class), any(String.class));
            verify(resourceOptimizer, never()).optimizeJs(any(String.class), any(JsOptimizerParams.class));
        }
    }

    @Test
    public void shouldProcessEmptyTag() throws Exception {
        Tag jsTag = createJsTag("", "app.js");
        String result = jsTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<script type=\"text/javascript\" src=\"app.js\"></script>");
        verify(resourceAccess, never()).read(any(Path.class));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.js")), any(String.class));
        verify(resourceOptimizer, never()).optimizeJs(any(String.class), any(JsOptimizerParams.class));
    }

    @Test
    public void shouldProcessSingleTag() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");

        Tag jsTag = createJsTag("<script src=\"my/lib/path/lib.js\"></script>", "app.js");
        String result = jsTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<script type=\"text/javascript\" src=\"app.js\"></script>");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib.js")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.js")), any(String.class));
        verify(resourceOptimizer, times(1)).optimizeJs(any(String.class), any(JsOptimizerParams.class));
    }

    @Test
    public void shouldProcessMultipleInlineTags() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");

        Tag jsTag = createJsTag("<script src=\"my/lib/path/lib1.js\"></script><script src=\"my/lib/path/lib2.js\"></script>", "app.js");
        String result = jsTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<script type=\"text/javascript\" src=\"app.js\"></script>");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.js")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.js")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.js")), any(String.class));
        verify(resourceOptimizer, times(2)).optimizeJs(any(String.class), any(JsOptimizerParams.class));
    }

    @Test
    public void shouldProcessMultipleMultiLineTags() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");

        Tag jsTag = createJsTag("<script src=\"my/lib/path/lib1.js\"></script>\n<!-- sample comment -->\n<script src=\"my/lib/path/lib2.js\"></script>", "app.js");
        String result = jsTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<script type=\"text/javascript\" src=\"app.js\"></script>");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.js")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.js")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.js")), any(String.class));
        verify(resourceOptimizer, times(2)).optimizeJs(any(String.class), any(JsOptimizerParams.class));
    }

    @Test
    public void shouldProcessWithMinifiedFiles() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");

        Tag jsTag = createJsTag("<script src=\"my/lib/path/lib1.min.js\"></script>\n<!-- sample comment -->\n<script src=\"my/lib/path/lib2.js\"></script>", "app.js");
        String result = jsTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<script type=\"text/javascript\" src=\"app.js\"></script>");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.min.js")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.js")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.js")), any(String.class));
        verify(resourceOptimizer, times(1)).optimizeJs(any(String.class), any(JsOptimizerParams.class));
    }
    
    
    @Test
    public void shouldProcessMultipleInlineTagsWithELExpression() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");

        Tag jsTag = createJsTag("<script src=\"#{request.contextPath}/my/lib/path/lib1.js\"></script><script src=\"#{facesContext.externalContext.request.contextPath}/my/lib/path/lib2.js\"></script>", "#{request.contextPath}/js/app.js");
        String result = jsTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<script type=\"text/javascript\" src=\"#{request.contextPath}/js/app.js\"></script>");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.js")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.js")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.js")), any(String.class));
        verify(resourceOptimizer, times(2)).optimizeJs(any(String.class), any(JsOptimizerParams.class));
    }


    private Tag createJsTag(String content, String... attributes) {
        return new Tag(content, "js", attributes);
    }
}