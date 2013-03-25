/*
 * FixSource Maven Plugin.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kryshchuk.maven.plugins.fixsource;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.kryshchuk.maven.plugins.filevisitor.FileSet;

/**
 * @author yura
 */
@Mojo(name = "fix-test-java", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, threadSafe = true, requiresProject = true)
public class FixTestJavaMojo extends FixJavaMojo {

  @Override
  protected FileSet getSourcesSet() {
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
