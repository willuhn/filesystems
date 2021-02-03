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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.willuhn.logging.Logger;

/**
 * Implementierung des File-Interface fuer lokale Dateien.
 */
public class LocalFile implements File
{
  private java.io.File file = null;

  /**
   * ct.
   * @param path Verzeichnis, in dem sich die Datei befindet/befinden soll.
   * @param filename Dateiname.
   * @throws FSException
   */
  LocalFile(String path, String filename) throws FSException
  {
    if (path != null)
    {
      Logger.debug("checking, if dir " + path + " exists");
      java.io.File dir = new java.io.File(path);
      if (!dir.exists())
      {
        Logger.debug("creating dir " + dir.getAbsolutePath());
        dir.mkdirs();
      }
    }
    Logger.debug("creating file handle for dir " + path + ", file: " + filename);
    this.file = new java.io.File(path,filename);
  }

  /**
   * @see de.willuhn.io.fs.File#getOutputStream()
   */
  public OutputStream getOutputStream() throws FSException
  {
    try
    {
      return new FileOutputStream(this.file);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#exists()
   */
  public boolean exists() throws FSException
  {
    return this.file.exists();
  }

  /**
   * @see de.willuhn.io.fs.File#getInputStream()
   */
  public InputStream getInputStream() throws FSException
  {
    try
    {
      return new FileInputStream(this.file);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#delete()
   */
  public void delete() throws FSException
  {
    if (!exists())
      return;
    this.file.delete();
  }

  /**
   * @see de.willuhn.io.fs.File#length()
   */
  public long length() throws FSException
  {
    return this.file.length();
  }

  /**
   * @see de.willuhn.io.fs.File#lastModified()
   */
  public long lastModified() throws FSException
  {
    return this.file.lastModified();
  }

  /**
   * @see de.willuhn.io.fs.File#rename(java.lang.String)
   */
  public void rename(String name) throws FSException
  {
    if (name == null || name.length() == 0)
      throw new FSException("no filename given");
    
    this.file.renameTo(new java.io.File(this.file.getParent(),name));
  }

}
