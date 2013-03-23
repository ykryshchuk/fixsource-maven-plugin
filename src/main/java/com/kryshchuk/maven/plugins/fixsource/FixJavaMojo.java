/**
 * 
 */
package com.kryshchuk.maven.plugins.fixsource;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.kryshchuk.maven.plugins.filevisitor.FileSet;

/**
 * @author yura
 */
@Mojo(name = "fix-java", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresProject = true)
public class FixJavaMojo extends AbstractFixMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject mavenProject;

  @Parameter(defaultValue = "true", property = "fixDevelopmentVersion")
  private boolean fixDevelopmentVersion;

  private String sinceVersion;

  @Override
  protected void beforeExecute() {
    final String sinceComplex = "@since " + mavenProject.getVersion();
    sinceVersion = sinceComplex.endsWith("-SNAPSHOT") ? sinceComplex.substring(0, sinceComplex.length() - 9)
        : sinceComplex;
  }

  @Override
  protected AbstractHeaderSource createHeaderSource() {
    return new JavaHeaderSource();
  }

  @Override
  protected FileSet getSourcesSet() {
    return new JavaSourcesSet();
  }

  private class JavaSource extends AbstractSource {

    private int packageLine = -1;

    private final List<Integer> developmentVersionLines = new LinkedList<Integer>();

    @Override
    protected String handleLine(final String line, final int index) {
      if (line.startsWith("package ") && packageLine == -1) {
        packageLine = index;
      }
      if (line.indexOf("@sinceDevelopmentVersion") != -1) {
        developmentVersionLines.add(index);
      }
      return line;
    }

    /**
     * 
     */
    @Override
    boolean fix() {
      boolean developmentVersionFixed = false;
      for (final Integer idx : developmentVersionLines) {
        lines.set(idx, lines.get(idx).replace("@sinceDevelopmentVersion", sinceVersion));
        developmentVersionFixed = true;
      }
      boolean headerFixed = false;
      if (!headerSource.matches(this)) {
        while (packageLine-- > 0) {
          lines.remove(0);
        }
        lines.addAll(0, headerSource.lines);
        packageLine = headerSource.lines.size();
        headerFixed = true;
      }
      return developmentVersionFixed || headerFixed;
    }

  }

  class JavaSourcesSet extends FileSet {

    JavaSourcesSet() {
      setDirectory(getJavaSourceDirectory());
      setIncludes(Arrays.asList("**/*.java"));
    }

    protected File getJavaSourceDirectory() {
      return new File(mavenProject.getBuild().getSourceDirectory());
    }

  }

  protected class JavaHeaderSource extends AbstractHeaderSource {

    @Override
    protected void read(final Reader reader) throws IOException {
      super.read(reader);
      lines.add(" */");
    }

    @Override
    protected String handleLine(final String line, final int index) {
      if (index == 0) {
        lines.add("/*");
      }
      return " * " + line.trim();
    }

    @Override
    protected boolean matches(final AbstractSource source) {
      if (source instanceof JavaSource) {
        final JavaSource javaSource = (JavaSource) source;
        final Iterator<String> javaLines = javaSource.lines.iterator();
        for (final String s : lines) {
          if (!s.equals(javaLines.next())) {
            return false;
          }
        }
        final String packageLine = javaLines.next();
        return packageLine.startsWith("package ");
      } else {
        return false;
      }
    }

  }

  @Override
  protected AbstractSource createSource() {
    return new JavaSource();
  }

}
