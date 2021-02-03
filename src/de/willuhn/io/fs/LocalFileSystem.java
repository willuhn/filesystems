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
import java.net.URI;

import de.willuhn.logging.Logger;

/**
 * Implementierung des FileSystem-Interface, welches in das lokale Filesystem schreibt.
 */
public class LocalFileSystem extends AbstractFileSystem
{

  // Das Arbeitsverzeichnis
  private String path = null;

  /**
   * @see de.willuhn.io.fs.FileSystem#init(java.net.URI)
   */
  public void init(URI uri) throws FSException
  {
    Logger.debug("creating filesystem for uri: " + uri.toString());
    this.path = clean(uri.toString());
    if (this.path.startsWith("file://"))
    {
      Logger.debug("removing file:// prefix");
      this.path = this.path.substring(7);
    }
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#create(java.lang.String)
   */
  public File create(String filename) throws FSException
  {
    return create(null,filename);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#create(java.lang.String, java.lang.String)
   */
  public File create(String dir, String filename) throws FSException
  {
    if (dir == null || dir.length() == 0)
      return new LocalFile(path, filename);

    // Ueberfluessige Verzeichnis-Trenner entfernen und vereinheitlichen
    dir = clean(dir);
    if (dir.startsWith("/"))           // Slash am Anfang entfernen
      dir = dir.substring(1);

    return new LocalFile(path + "/" + dir, filename);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#close()
   */
  public void close() throws FSException
  {
    // nicht noetig
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#list(java.io.FilenameFilter)
   */
  public String[] list(FilenameFilter filter) throws FSException
  {
    return list(null,filter);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#list(java.lang.String, java.io.FilenameFilter)
   */
  public String[] list(String dir, final FilenameFilter filter) throws FSException
  {
    return _list(dir,new FilenameFilter()
    {
      /**
       * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
       */
      public boolean accept(java.io.File dir, String name)
      {
        java.io.File test = new java.io.File(dir,name);
        return test.isFile() && (filter == null || filter.accept(dir,name));
      }
    });
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#listDirs(java.io.FilenameFilter)
   */
  public String[] listDirs(FilenameFilter filter) throws FSException
  {
    return listDirs(null,filter);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#listDirs(java.lang.String, java.io.FilenameFilter)
   */
  public String[] listDirs(String dir, final FilenameFilter filter) throws FSException
  {
    return _list(dir,new FilenameFilter()
    {
      /**
       * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
       */
      public boolean accept(java.io.File dir, String name)
      {
        java.io.File test = new java.io.File(dir,name);
        return test.isDirectory() && (filter == null || filter.accept(dir,name));
      }
    });
  }

  /**
   * Liefert eine Liste von Dateien oder Verzeichnissen.
   * @param dir Pfad.
   * @param filter Filter.
   * @return Liste von Dateien oder Verzeichnissen.
   * @throws FSException
   */
  private String[] _list(String dir, final FilenameFilter filter) throws FSException
  {
    java.io.File file = null;
    
    if (dir != null && dir.length() > 0)
    {
      dir = clean(dir);
      if (dir.startsWith("/"))           // Slash am Anfang entfernen
        dir = dir.substring(1);
      file = new java.io.File(this.path,dir);
    }
    else
    {
      file = new java.io.File(this.path);
    }

    return file.list(filter);
  }

}
