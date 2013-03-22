/**
 * 
 */
package com.kryshchuk.maven.plugins.fixsource;

import java.io.File;

import com.kryshchuk.maven.plugins.filevisitor.FileMapper;

// TODO move to file-visitor
class IdentityFileMapper implements FileMapper {

  public File getMappedFile(final File file) {
    return file;
  }

}