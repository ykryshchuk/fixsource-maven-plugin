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
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractSource {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final List<String> lines = new LinkedList<String>();

  void read(final Reader reader) throws IOException {
    final long startTime = System.nanoTime();
    lines.clear();
    final BufferedReader breader = new BufferedReader(reader);
    String line;
    int index = 0;
    while ((line = breader.readLine()) != null) {
      final String handledLine = handleLine(line, index);
      if (handledLine != null) {
        lines.add(handledLine);
        index++;
      }
    }
    final long readTime = System.nanoTime() - startTime;
    logger.debug("Source read in {} ns", readTime);
  }

  void read(final File file) throws IOException {
    try (final FileReader reader = new FileReader(file)) {
      read(reader);
    }
  }

  void store(final File file) throws IOException {
    final long startTime = System.nanoTime();
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      for (final String line : lines) {
        writer.write(line);
        writer.write("\n");
      }
    }
    final long storeTime = System.nanoTime() - startTime;
    logger.debug("File {} stored in {} ns", file, storeTime);
  }

  protected abstract String handleLine(String line, int index);

  abstract boolean fix();

}