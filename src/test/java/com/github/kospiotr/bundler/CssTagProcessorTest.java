package com.github.kospiotr.bundler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.nio.file.Path;

import com.github.kospiotr.bundler.optimizer.OptimizerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.kospiotr.bundler.optimizer.ResourceOptimizer;

@RunWith(MockitoJUnitRunner.class)
public class CssTagProcessorTest {

    @Spy
    ProcessMojo processMojo = new ProcessMojo(new File("index-dev.html"), new File("index.html"), new File("/input"), new File("/output"));

    @Mock
    ResourceAccess resourceAccess;

    @Mock
    OptimizerFactory optimizerFactory;

    @Mock
    ResourceOptimizer resourceOptimizer;

    @InjectMocks
    CssTagProcessor cssTagProcessor;

    @Before
    public void before() {
        Mockito.when(optimizerFactory.getOptimizer(anyString())).thenReturn(resourceOptimizer);
    }

    @Test
    public void shouldRejectWhenNoFileNameAttributeGiven() throws Exception {
        Tag jsTag = createCssTag("");

        try {
            cssTagProcessor.process(jsTag);
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertThat(e).hasMessage("File Name attribute is required");
            verify(resourceAccess, never()).read(any(Path.class));
            verify(resourceAccess, never()).write(any(Path.class), any(String.class));
            verify(resourceAccess, never()).write(any(Path.class), any(String.class));
            verify(resourceOptimizer, never()).optimizeCss(any(String.class));
        }
    }

    @Test
    public void shouldProcessEmptyTag() throws Exception {
        Tag jsTag = createCssTag("", "app.css");
        String result = cssTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"app.css\" />");
        verify(resourceAccess, never()).read(any(Path.class));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), any(String.class));
        verify(resourceOptimizer, never()).optimizeCss(any(String.class));
    }

    @Test
    public void shouldProcessSingleTag() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");
        Tag jsTag = createCssTag("<link rel=\"stylesheet\" href=\"my/lib/path/lib.css\" />", "app.css");
        String result = cssTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"app.css\" />");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib.css")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), any(String.class));
        verify(resourceOptimizer, times(1)).optimizeCss(any(String.class));
    }

    @Test
    public void shouldProcessMultipleInlineTags() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");
        Tag jsTag = createCssTag("<link href=\"my/lib/path/lib1.css\" /><link href=\"my/lib/path/lib2.css\" />", "app.css");
        String result = cssTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"app.css\" />");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.css")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.css")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), any(String.class));
        verify(resourceOptimizer, times(2)).optimizeCss(any(String.class));
    }

    @Test
    public void shouldProcessMultipleMultiLineTags() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");
        Tag jsTag = createCssTag("<link href=\"my/lib/path/lib1.css\" />\n<!-- sample comment -->\n<link href=\"my/lib/path/lib2.css\" />", "app.css");
        String result = cssTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"app.css\" />");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.css")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.css")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), any(String.class));
        verify(resourceOptimizer, times(2)).optimizeCss(any(String.class));
    }

//    @Test
//    public void shouldNormalizePathsWhenProcessingFilesFromDifferentPathLevels() throws Exception {
//        when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib1.css"))))
//                .thenReturn("h1 {background-image: url(\"paper1.gif\");}\n" +
//                            "h2 {background-image: url(../paper2.gif);}\n" +
//                            "h3 {background-image: url('app/paper3.gif');}");
//        
//        when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib2.css"))))
//                .thenReturn("h4 {background-image: url( \"paper4.gif\" );}\n" +
//                            "h5 {background-image: url( ../paper5.gif);}\n" +
//                            "h6 {background-image: url('app/paper6.gif' );}");
//        
//        when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib3.css"))))
//                .thenReturn("h7 {background-image: url('/paper7.gif');}");
//        
//        when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib4.css"))))
//                .thenReturn("h7 {background-image: url('paper8.gif?#iefix');}");
//        
//        when(resourceOptimizer.optimizeCss(anyString())).then(returnsFirstArg());
//
//        Tag jsTag = createCssTag("<link href=\"../lib1.css\" /><link href=\"lib2.css\" /><link href=\"lib/lib3.css\" /><link href=\"lib/lib4.css\" />", "app.css");
//        String result = cssTagProcessor.process(jsTag);
//
//        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"app.css\" />");
//        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.css")));
//        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.css")));
//        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib3.css")));
//        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib4.css")));
//        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), eq(
//                "h1 {background-image: url(\"../paper1.gif\");}\n" +
//                        "h2 {background-image: url(../../paper2.gif);}\n" +
//                        "h3 {background-image: url('../app/paper3.gif');}\n" +
//                        "h4 {background-image: url(\"paper4.gif\");}\n" +
//                        "h5 {background-image: url(../paper5.gif);}\n" +
//                        "h6 {background-image: url('app/paper6.gif');}\n" +
//                        "h7 {background-image: url('/paper7.gif');}\n" +
//                        "h7 {background-image: url('lib/paper8.gif?#iefix');}\n"));
//        verify(resourceOptimizer, times(4)).optimizeCss(any(String.class));
//    }

    @Test
    public void shouldProcessWithMinifiedFiles() throws Exception {
        when(resourceAccess.read(any(Path.class))).thenReturn("");
        Tag jsTag = createCssTag("<link href=\"my/lib/path/lib1.min.css\" /><link href=\"my/lib/path/lib2.css\" />", "app.css");
        String result = cssTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"app.css\" />");
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.min.css")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.css")));
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), any(String.class));
        verify(resourceOptimizer, times(1)).optimizeCss(any(String.class));
    }
    
    @Test
    public void shouldNormalizePathsWhenProcessingFilesFromDifferentPathLevels1() throws Exception {
        
    	when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib1.css"))))
                .thenReturn("h1 {background-image: url(\"../../images/paper1.gif\");}\n" +
                            "h2 {background-image: url(../../images/paper2.gif);}");
    	
    	when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib2.css"))))
        .thenReturn("h3 {background-image: url('../images/paper3.gif');}");
    	
    	when(resourceAccess.read(argThat(new PathHamcrestMatcher("glob:**/lib3.css"))))
        .thenReturn("h4 {background-image: url(\"../images/paper4.gif?#iefix\");}");
      
        when(resourceOptimizer.optimizeCss(anyString())).then(returnsFirstArg());

        Tag jsTag = createCssTag("<link href=\"#{request.contextPath}/resources/css/lib/lib1.css\" />"
        					   + "<link href=\"#{request.contextPath}/resources/css/lib2.css\" />"
        					   + "<link href=\"#{request.contextPath}/resources/css/lib3.css\" />",
        					   "#{request.contextPath}/resources/css/app.css");
        
        String result = cssTagProcessor.process(jsTag);

        assertThat(result).isEqualTo("<link rel=\"stylesheet\" href=\"#{request.contextPath}/resources/css/app.css\" />");
        
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib1.css")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib2.css")));
        verify(resourceAccess).read(argThat(new PathHamcrestMatcher("glob:**/lib3.css")));
        
        verify(resourceAccess).write(argThat(new PathHamcrestMatcher("glob:**/app.css")), 
        							eq("h1 {background-image: url(\"../images/paper1.gif\");}\n" +
        							   "h2 {background-image: url(../images/paper2.gif);}\n" +
        							   "h3 {background-image: url('../images/paper3.gif');}\n" +                        
        							   "h4 {background-image: url(\"../images/paper4.gif?#iefix\");}\n"));
        
        verify(resourceOptimizer, times(3)).optimizeCss(any(String.class));
    }


    private Tag createCssTag(String content, String... attributes) {
        return new Tag(content, "css", attributes);
    }

}