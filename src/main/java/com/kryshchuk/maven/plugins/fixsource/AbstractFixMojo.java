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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.impl.StaticLoggerBinder;

import com.kryshchuk.maven.plugins.filevisitor.AbstractFileVisitor;
import com.kryshchuk.maven.plugins.filevisitor.FileSet;
import com.kryshchuk.maven.plugins.filevisitor.FileSetIterator;
import com.kryshchuk.maven.plugins.filevisitor.IdentityFileMapper;
import com.kryshchuk.maven.plugins.filevisitor.VisitorException;

/**
 * @author yura
 */
public abstract class AbstractFixMojo extends AbstractMojo {

  @Parameter(defaultValue = "true", property = "fixHeader")
  private boolean fixHeader;

  @Parameter(property = "header")
  private String header;

  @Parameter(property = "headerFile")
  private File headerFile;

  @Parameter(defaultValue = "false", property = "dryRun")
  private boolean dryRun;

  protected final AbstractHeaderSource headerSource = createHeaderSource();

  protected abstract class AbstractHeaderSource extends AbstractSource {

    protected abstract boolean matches(AbstractSource source);

    @Override
    final boolean fix() {
      throw new UnsupportedOperationException("Fix not supported for header sources");
    }

  }

  protected abstract AbstractHeaderSource createHeaderSource();

  /**
   * @param header
   *          the header to set
   * @throws IOException
   *           if could not read from file
   */
  public void setHeader(final String header) throws IOException {
    this.header = header;
    headerSource.read(new StringReader(header));
  }

  /**
   * @param headerFile
   *          the headerFile to set
   * @throws IOException
   *           if could not read file
   */
  public void setHeaderFile(final File headerFile) throws IOException {
    this.headerFile = headerFile;
    headerSource.read(headerFile);
  }

  /**
   * @return the dryRun
   */
  public boolean isDryRun() {
    return dryRun;
  }

  /**
   * @since 1.1.5
   * @return true if should fix header
   */
  protected boolean isFixHeader() {
    return fixHeader;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    StaticLoggerBinder.getSingleton().setLog(getLog());
    final FileSet sourcesSet = getSourcesSet();
    if (sourcesSet.getDirectory().isDirectory()) {
      try {
        beforeExecute();
        final FileSetIterator i = new FileSetIterator(sourcesSet, new IdentityFileMapper());
        i.iterate(new SourceFixer());
      } catch (final VisitorException e) {
        throw new MojoExecutionException("Could not easy fix sources", e);
      }
    }
  }

  protected abstract AbstractSource createSource();

  /**
   * @return set of source locations
   */
  protected abstract FileSet getSourcesSet();

  protected void beforeExecute() {

  }

  protected class SourceFixer extends AbstractFileVisitor {

    @Override
    protected void handleFile(final File inputFile, final File outputFile) throws VisitorException {
      try {
        final AbstractSource source = createSource();
        try (final Reader reader = new FileReader(inputFile)) {
          source.read(reader);
        }
        if (source.fix()) {
          if (isDryRun()) {
            getLog().info("Source would be fixed " + inputFile);
          } else {
            source.store(outputFile);
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

  };

}
