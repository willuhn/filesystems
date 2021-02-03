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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Implementierung des File-Interfaces mit FTP-Backend.
 */
public class FTPFile implements File
{

  private FTPFileSystem fs = null;
  private String filename  = null;
  private String dir       = null;
  
  /**
   * ct.
   * @param fs
   * @param dir
   * @param filename
   * @throws FSException
   */
  FTPFile(FTPFileSystem fs, String dir, String filename) throws FSException
  {
    this.fs       = fs;
    this.filename = filename;
    this.dir      = dir;
  }
  
  String getDir()
  {
    return this.dir;
  }
  
  String getName()
  {
    return this.filename;
  }

  /**
   * @see de.willuhn.io.fs.File#getOutputStream()
   */
  public OutputStream getOutputStream() throws FSException
  {
    return this.fs.getOutputStream(this);
  }

  /**
   * @see de.willuhn.io.fs.File#exists()
   */
  public boolean exists() throws FSException
  {
    return this.fs.getFile(this) != null;
  }

  /**
   * @see de.willuhn.io.fs.File#delete()
   */
  public void delete() throws FSException
  {
    this.fs.delete(this);
  }

  /**
   * @see de.willuhn.io.fs.File#length()
   */
  public long length() throws FSException
  {
    org.apache.commons.net.ftp.FTPFile f = this.fs.getFile(this);
    return f == null ? 0 : f.getSize();
  }

  /**
   * @see de.willuhn.io.fs.File#lastModified()
   */
  public long lastModified() throws FSException
  {
    org.apache.commons.net.ftp.FTPFile f = this.fs.getFile(this);
    if (f == null) return 0;
    
    Calendar timestamp = f.getTimestamp();
    return timestamp == null ? 0 : timestamp.getTimeInMillis();
  }

  /**
   * @see de.willuhn.io.fs.File#getInputStream()
   */
  public InputStream getInputStream() throws FSException
  {
    return this.fs.getInputStream(this);
  }

  /**
   * @see de.willuhn.io.fs.File#rename(java.lang.String)
   */
  public void rename(String name) throws FSException
  {
    this.fs.rename(this,name);
  }
  
  
}
