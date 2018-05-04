# bundler-maven-plugin

[![Build Status](https://travis-ci.org/CH3CHO/bundler-maven-plugin.svg?branch=develop)](https://travis-ci.org/CH3CHO/bundler-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.ch3cho/bundler-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.ch3cho/bundler-maven-plugin)

Maven plugin for creating bundle package of js and css files in Maven project.

This is a fork of https://github.com/kospiotr/bundler-maven-plugin.

Inspired by: https://github.com/dciccale/grunt-processhtml

# Goals

- ```process``` - analyse input html file for special comment block, create bundle resource packages and outputs html file with bundled blocks. Bundled resources are concatenated, minimized, optimized and if requested checksum is computed and used with bundled filename. (see example below)

# Configuration properties

| Property              | Description                                                  | Sample Value                                     |
| --------------------- | ------------------------------------------------------------ | ------------------------------------------------ |
| inputFilePah          | The path of a file to be optimized                           | ${project.basedir}/src/main/resources/index.html |
| outputFilePath        | The output path of the optimized file                        | ${project.build.outputDirectory}/index.html      |
| hashingAlgorithm      | The algorithm used to generated hash of the file content to be used in the output file name<br />Possible values: `MD5`(default), `SHA-1`, `SHA-256`, `SHA-384`, `SHA-512` | MD5 |
| verbose               | Whether to enable detailed output of the bundling process<br />Default: `false` | true |
| cssOptimizer          | The name of optimizer used to process CSS files.<br />Possible values: `simple` (default), `yui`, `none`<br />When choosing `none`, no optimization shall be performed. Contents from input files will just be concatenated and saved into the output file. | simple |
| jsOptimizer           | The name of optimizer used to process CSS files.<br />Possible values: `simple` (default), `yui`, `none`<br />When choosing `none`, no optimization shall be performed. Contents from input files will just be concatenated and saved into the output file. | simple |
| munge                 | Should be `true` if the compressor should shorten local variable names when possible.<br />Only works if `jsOptimize` is set to`yui`.<br />Default: `true` | true |
| preserveAllSemiColons | Should be `true` if the compressor should preserve all semicolons in the code.<br />Only works if `jsOptimize` is set to`yui`.<br />Default: `true` | true |
| disableOptimizations  | Should be `true` if the compressor should disable all micro optimizations. <br />Only works if `jsOptimize` is set to`yui`.<br />Default: `true` | true |

# Usage

Configure plugin:

```xml
      <plugin>
        <groupId>com.github.ch3cho</groupId>
        <artifactId>bundler-maven-plugin</artifactId>
        <version>1.12.1</version>
        <executions>
          <execution>
            <id>bundle</id>
            <goals>
              <goal>process</goal>
            </goals>
            <configuration>
              <inputFilePah>${project.basedir}/src/main/resources/index-dev.html</inputFilePah>
              <outputFilePath>${project.build.outputDirectory}/index.html</outputFilePath>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

Create source file ```${project.basedir}/src/main/resources/index-dev.html```

```html
<!DOCTYPE html>
<html lang="en">
<body>

<!-- bundle:js app-#hash#.min.js-->
<script src="js/lib.js"></script>
<script src="js/app.js"></script>
<!-- /bundle -->

<!-- bundle:css app-#hash#.min.css-->
<link href="css/lib.css"/>
<link href="css/app.css"/>
<!-- /bundle -->

</body>
</html>
```

After running plugin the result will be outputted to ```${project.build.outputDirectory}/index.html```


```html
<!DOCTYPE html>
<html lang="en">
<body>

<script src="app-0874ac8910c7b3d2e73da106ebca7329.min.js"></script>
<link rel="stylesheet" href="app-4971211a240c63874c6ae8c82bd0c88c.min.css" />

</body>
</html>
```

# Optimizers

- Simple

  For CSS, it uses Barry van Oudtshoorn's CSSMin (https://github.com/barryvan/CSSMin/), which is licensed under [the BSD license](https://github.com/barryvan/CSSMin/blob/master/LICENSE).

  For Javascript, it uses JSMin in wro4j (https://github.com/wro4j/wro4j), which is licensed under [the Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

- YUI

  Bundled files are automatically concatenated and minimized with http://yui.github.io/yuicompressor/

  YUI Compressor has some bugs when dealing with "data:svg+xml" values in CSS and doesn't support ES 6. You can have a try with it and see if it can work with your project.