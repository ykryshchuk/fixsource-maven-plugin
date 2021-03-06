/*
 * FixSource Maven Plugin
 * Copyright (C) 2013  Yuriy Kryshchuk
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses />.
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
 * Fixes the java sources by adding the header comments and replacing <code><i>@</i>sinceDevelopmentVersion</code> with
 * a next suggested release version value.
 * 
 * @author yura
 * @since 1.0
 */
@Mojo(name = "fix-java", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresProject = true)
public class FixJavaMojo extends AbstractFixMojo {

  /**
   * 
   */
  private static final String PACKAGE_KW = "package ";

  /**
   * Note, concatenation needed to avoid the 'fix' in this source.
   */
  private static final String JAVADOC_TAG_SINCEDEVVER = "@since" + "DevelopmentVersion";

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

    private int packageAnnLine = -1;

    private final List<Integer> developmentVersionLines = new LinkedList<Integer>();

    @Override
    protected String handleLine(final String line, final int index) {
      if (line.startsWith("@") && packageLine == -1 && packageAnnLine == -1) {
        packageAnnLine = index;
      } else if (line.startsWith(PACKAGE_KW) && packageLine == -1) {
        packageLine = index;
      } else if (line.indexOf(JAVADOC_TAG_SINCEDEVVER) != -1) {
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
      if (fixDevelopmentVersion) {
        for (final Integer idx : developmentVersionLines) {
          lines.set(idx, lines.get(idx).replace(JAVADOC_TAG_SINCEDEVVER, sinceVersion));
          developmentVersionFixed = true;
        }
      }
      boolean headerFixed = false;
      if (isFixHeader()) {
        if (!headerSource.matches(this)) {
          int linesToRemove;
          if (packageLine == -1) {
            linesToRemove = 0;
          } else {
            if (packageAnnLine == -1) {
              linesToRemove = packageLine;
            } else {
              linesToRemove = packageAnnLine;
            }
          }
          for (int i = 0; i < linesToRemove; i++) {
            lines.remove(0);
          }
          lines.addAll(0, headerSource.lines);
          if (packageAnnLine != -1) {
            packageAnnLine = packageAnnLine - linesToRemove + headerSource.lines.size();
          }
          if (packageLine != -1) {
            packageLine = packageLine - linesToRemove + headerSource.lines.size();
          }
          headerFixed = true;
        }
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
        return packageLine.startsWith(PACKAGE_KW) || packageLine.startsWith("@");
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
