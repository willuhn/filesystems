/**********************************************************************
 *
 * Copyright (c) 2021 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * GNU LESSER GENERAL PUBLIC LICENSE 2.1.
 * Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.io.fs;

import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * File-Implementierung fuer SFTP.
 */
public class SFTPFile implements File
{
  private SFTPFileSystem fs = null;
  private String dir        = null;
  private String name       = null;

  /**
   * ct.
   * @param fs Das Filesystem.
   * @param dir das Verzeichnis.
   * @param name der Dateiname.
   */
  SFTPFile(SFTPFileSystem fs, String dir, String name)
  {
    this.fs = fs;
    this.dir = dir;
    this.name = name;
  }

  /**
   * @see de.willuhn.io.fs.File#delete()
   */
  public void delete() throws FSException
  {
    this.fs.delete(this);
  }

  /**
   * @see de.willuhn.io.fs.File#exists()
   */
  public boolean exists() throws FSException
  {
    String[] names = this.fs.list(this.getDir(), new FilenameFilter() {
      public boolean accept(java.io.File dir, String otherName)
      {
        return name.equals(otherName);
      }
    });
    return names != null && names.length == 1;
  }

  /**
   * @see de.willuhn.io.fs.File#getInputStream()
   */
  public InputStream getInputStream() throws FSException
  {
    return this.fs.getInputStream(this);
  }

  /**
   * @see de.willuhn.io.fs.File#getOutputStream()
   */
  public OutputStream getOutputStream() throws FSException
  {
    return this.fs.getOutputStream(this);
  }

  /**
   * @see de.willuhn.io.fs.File#length()
   */
  public long length() throws FSException
  {
    return this.fs.length(this);
  }
  
  /**
   * @see de.willuhn.io.fs.File#lastModified()
   */
  public long lastModified() throws FSException
  {
    return this.fs.lastModified(this);
  }

  
  /**
   * @see de.willuhn.io.fs.File#rename(java.lang.String)
   */
  public void rename(String name) throws FSException
  {
    this.fs.rename(this,name);
  }

  /**
   * Liefert das Verzeichnis der Datei.
   * @return Verzeichnis.
   */
  String getDir()
  {
    return this.dir;
  }
  
  /**
   * Liefert den Dateinamen.
   * @return Dateiname.
   */
  String getName()
  {
    return this.name;
  }
}
