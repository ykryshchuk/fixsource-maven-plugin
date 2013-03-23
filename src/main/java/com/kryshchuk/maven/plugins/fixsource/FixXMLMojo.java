/**
 * 
 */
package com.kryshchuk.maven.plugins.fixsource;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.kryshchuk.maven.plugins.filevisitor.FileSet;

/**
 * @author yura
 */
@Mojo(name = "fix-xml", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true, requiresProject = true)
public class FixXMLMojo extends AbstractFixMojo {

  @Parameter(required = true)
  private FileSet fileset;

  @Override
  protected AbstractHeaderSource createHeaderSource() {
    return new XMLHeaderSource();
  }

  @Override
  protected FileSet getSourcesSet() {
    return fileset;
  }

  private class XMLSource extends AbstractSource {

    private int lastDirectiveLine = -1;

    private int rootElementLine = -1;

    @Override
    protected String handleLine(final String line, final int index) {
      if (line.startsWith("<?")) {
        if (rootElementLine == -1) {
          lastDirectiveLine = index;
        }
      } else {
        if (rootElementLine == -1 && !line.startsWith("<!--")) {
          final String trimmed = line.trim();
          if (trimmed.startsWith("<")) {
            if (Character.isLetter(trimmed.charAt(1))) {
              rootElementLine = index;
            }
          }
        }
      }
      return line;
    }

    /**
     * 
     */
    @Override
    boolean fix() {
      boolean headerFixed = false;
      if (!headerSource.matches(this)) {
        while (rootElementLine > (lastDirectiveLine + 1)) {
          lines.remove(--rootElementLine);
        }
        lines.addAll(rootElementLine, headerSource.lines);
        rootElementLine += headerSource.lines.size();
        headerFixed = true;
      }
      return headerFixed;
    }

  }

  protected class XMLHeaderSource extends AbstractHeaderSource {

    @Override
    protected void read(final Reader reader) throws IOException {
      super.read(reader);
      lines.add("-->");
    }

    @Override
    protected String handleLine(final String line, final int index) {
      if (index == 0) {
        lines.add("<!--");
      }
      return "  " + line.trim();
    }

    @Override
    protected boolean matches(final AbstractSource source) {
      if (source instanceof XMLSource) {
        final XMLSource xmlSource = (XMLSource) source;
        final List<String> xmlHeader = xmlSource.lines.subList(xmlSource.lastDirectiveLine + 1,
            xmlSource.rootElementLine);
        return lines.equals(xmlHeader);
      } else {
        return false;
      }
    }

  }

  @Override
  protected AbstractSource createSource() {
    return new XMLSource();
  }

}
