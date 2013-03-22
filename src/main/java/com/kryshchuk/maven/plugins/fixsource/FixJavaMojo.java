/**
 * 
 */
package com.kryshchuk.maven.plugins.fixsource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.kryshchuk.maven.plugins.filevisitor.AbstractFileVisitor;
import com.kryshchuk.maven.plugins.filevisitor.FileMapper;
import com.kryshchuk.maven.plugins.filevisitor.FileSet;
import com.kryshchuk.maven.plugins.filevisitor.FileSetIterator;
import com.kryshchuk.maven.plugins.filevisitor.VisitorException;

/**
 * @author yura
 */
@Mojo(name = "fix-java", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresProject = true)
public class FixJavaMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject mavenProject;

  @Parameter(defaultValue = "true", property = "fixjava.fixDevelopmentVersion")
  private boolean fixDevelopmentVersion;

  @Parameter(defaultValue = "true", property = "fixjava.fixHeader")
  private boolean fixHeader;

  @Parameter(defaultValue = "${basedir}/src/template/java-header.txt", property = "fixjava.headerTemplate")
  private File headerTemplateFile;

  @Parameter(defaultValue = "false", property = "dryRun")
  private boolean dryRun;

  private HeaderSource headerSource;

  private String sinceVersion;

  public void execute() throws MojoExecutionException, MojoFailureException {
    final JavaSourcesSet fileset = new JavaSourcesSet();
    final FileSetIterator i = new FileSetIterator(fileset, new IdentityFileMapper());
    try {
      try {
        headerSource = new HeaderSource();
        headerSource.read(headerTemplateFile);
      } catch (final IOException e) {
        throw new MojoExecutionException("Could not read java header template", e);
      }
      final String sinceComplex = "@since " + mavenProject.getVersion();
      sinceVersion = sinceComplex.endsWith("-SNAPSHOT") ? sinceComplex.substring(0, sinceComplex.length() - 9)
          : sinceComplex;
      i.iterate(new JavaSourceFixer());
    } catch (final VisitorException e) {
      throw new MojoExecutionException("Could not easy fix sources", e);
    }
  }

  private class JavaSourceFixer extends AbstractFileVisitor {

    @Override
    protected void handleFile(final File inputFile, final File outputFile) throws VisitorException {
      try {
        final JavaSource javaSource = new JavaSource();
        javaSource.read(inputFile);
        if (javaSource.fix()) {
          if (dryRun) {
            getLog().info("Source would be fixed " + inputFile);
          } else {
            javaSource.store(outputFile);
            getLog().info("Fixed " + inputFile);
          }
        }
      } catch (final IOException e) {
        throw new VisitorException("Failed to read java source " + inputFile, e);
      }
    }

    @Override
    protected boolean shouldOverwrite(final File inputFile, final File outputFile) {
      return false;
    }

  }

  private abstract class AbstractSource {

    protected final List<String> lines = new LinkedList<String>();

    protected void read(final File file) throws IOException {
      final long startTime = System.nanoTime();
      final BufferedReader reader = new BufferedReader(new FileReader(file));
      try {
        String line;
        int index = 0;
        while ((line = reader.readLine()) != null) {
          final String handledLine = handleLine(line, index);
          if (handledLine != null) {
            lines.add(handledLine);
            index++;
          }
        }
      } finally {
        reader.close();
      }
      final long readTime = System.nanoTime() - startTime;
      getLog().debug("File " + file + " read in " + readTime + " ns");
    }

    protected abstract String handleLine(String line, int index);

  }

  private class HeaderSource extends AbstractSource {

    @Override
    protected void read(final File file) throws IOException {
      super.read(file);
      lines.add(" */");
    }

    @Override
    protected String handleLine(final String line, final int index) {
      if (index == 0) {
        lines.add("/*");
      }
      return " * " + line;
    }

    private boolean matches(final JavaSource javaSource) {
      final Iterator<String> javaLines = javaSource.lines.iterator();
      for (final String s : lines) {
        if (!s.equals(javaLines.next())) {
          return false;
        }
      }
      return true;
    }

  }

  private class JavaSource extends AbstractSource {

    private int packageLine = -1;

    private final List<Integer> developmentVersionLines = new LinkedList<Integer>();

    @Override
    protected String handleLine(final String line, final int index) {
      if (line.startsWith("package ") && packageLine != -1) {
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
    private boolean fix() {
      boolean developmentVersionFixed = false;
      for (final Integer idx : developmentVersionLines) {
        lines.set(idx, lines.get(idx).replace("@sinceDevelopmentVersion", sinceVersion));
        developmentVersionFixed = true;
      }
      boolean headerFixed = false;
      if (!headerSource.matches(this)) {
        if (packageLine != -1) {
          lines.subList(0, packageLine).clear();
        }
        lines.addAll(0, headerSource.lines);
        headerFixed = true;
      }
      return developmentVersionFixed || headerFixed;
    }

    private void store(final File file) throws IOException {
      final long startTime = System.nanoTime();
      final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      try {
        for (final String line : lines) {
          writer.write(line);
          writer.write("\n");
        }
      } finally {
        writer.close();
      }
      final long storeTime = System.nanoTime() - startTime;
      getLog().debug("File " + file + " stored in " + storeTime + " ns");
    }

  }

  private class IdentityFileMapper implements FileMapper {

    public File getMappedFile(final File file) {
      return file;
    }

  }

  private class JavaSourcesSet extends FileSet {

    private JavaSourcesSet() {
      final File sourceDir = new File(mavenProject.getBuild().getSourceDirectory());
      setDirectory(sourceDir);
      setIncludes(Arrays.asList("**/*.java"));
    }

  }

}
