/**
 * 
 */
package com.kryshchuk.maven.plugins.fixsource;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * @author yura
 */
@Mojo(name = "fix-test-java", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, threadSafe = true, requiresProject = true)
public class FixTestJavaMojo extends FixJavaMojo {

  @Override
  protected JavaSourcesSet getJavaSourcesSet() {
    return new JavaTestSourcesSet();
  }

  /**
   * @author yura
   */
  class JavaTestSourcesSet extends JavaSourcesSet {

    @Override
    protected File getJavaSourceDirectory() {
      return new File(mavenProject.getBuild().getTestSourceDirectory());
    }

  }
}
