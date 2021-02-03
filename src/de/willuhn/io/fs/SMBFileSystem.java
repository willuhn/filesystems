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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import de.willuhn.logging.Logger;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFilenameFilter;

/**
 * Implementierung des FileSystem-Interfaces mit CIFS/SMB-Backend.
 * Username und Passwort koennen auch mit den folgenden System-Properties
 * uebergeben werden, wenn sie nicht in der URL stehen:
 * 
 * jcifs.smb.client.username
 * jcifs.smb.client.password
 */
public class SMBFileSystem extends AbstractFileSystem
{

  private URI uri = null;

  /**
   * @see de.willuhn.io.fs.FileSystem#init(java.net.URI)
   */
  public void init(URI uri) throws FSException
  {
    Logger.debug("open smb connection to " + uri.toString());

    // Das Verzeichnis muss bei der JCifs-Implementierung mit einem
    // Slash enden. Also checken wir, ob einer dranhaengt und pappen
    // bei Bedarf selbst einen dran.
    String s = uri.toString();
    if (!s.endsWith("/"))
    {
      try
      {
        uri = new URI(s + "/");
      }
      catch (URISyntaxException e)
      {
        throw new FSException("invalid uri syntax: " + uri.toString());
      }
    }
    this.uri = uri;
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
    try
    {
      if (dir == null || dir.length() == 0)
        return new SMBFile(this.uri, filename);

      // Ueberfluessige Verzeichnis-Trenner entfernen und vereinheitlichen
      dir = clean(dir);
      if (dir.startsWith("/"))           // Slash am Anfang entfernen
        dir = dir.substring(1);

      URI newUri = new URI(this.uri.toString() + "/" + dir);
      return new SMBFile(newUri, filename);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
    catch (URISyntaxException e)
    {
      throw new FSException(e);
    }
    
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#close()
   */
  public void close() throws FSException
  {
    // nichts zu tun
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
    return _list(dir,new Filter()
    {
      public boolean accept(java.io.File dir, SmbFile file) throws SmbException
      {
        return file.isFile() && (filter == null || filter.accept(dir,file.getName()));
      }
    });
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#listDirs(java.io.FilenameFilter)
   */
  public String[] listDirs(final FilenameFilter filter) throws FSException
  {
    return listDirs(null,filter);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#listDirs(java.lang.String, java.io.FilenameFilter)
   */
  public String[] listDirs(String dir, final FilenameFilter filter) throws FSException
  {
    return _list(dir,new Filter()
    {
      public boolean accept(java.io.File dir, SmbFile file) throws SmbException
      {
        return file.isDirectory() && (filter == null || filter.accept(dir,file.getName()));
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
  public String[] _list(String dir, final Filter filter) throws FSException
  {
    try
    {
      SmbFile file = null;
      if (dir != null && dir.length() > 0)
      {
        dir = clean(dir);
        if (dir.startsWith("/"))
          dir = dir.substring(1);
        if (!dir.endsWith("/"))
          dir = dir + "/"; // muss bei jcifs mit einem Slash enden
        file = new SmbFile(uri.toString(),dir);
      }
      else
        file = new SmbFile(uri.toString());
      
      return file.list(new SmbFilenameFilter()
      {
        public boolean accept(SmbFile dir, String name) throws SmbException
        {
          try
          {
            return filter.accept(new java.io.File(dir.getPath()),new SmbFile(dir,name));
          }
          catch (SmbException e)
          {
            throw e;
          }
          catch (Exception e2)
          {
            throw new RuntimeException(e2);
          }
        }
      });
    }
    catch (Exception e)
    {
      throw new FSException(e);
    }
  }
  
  /**
   * Hilfsklasse zum Trennen von Verzeichnissen und Dateien.
   */
  private interface Filter
  {
    /**
     * Prueft, ob die Datei oder das Verzeichnis uebernommen werden kann.
     * @param dir Dir.
     * @param file SmbFile-Objekt.
     * @return true, wenn es uebernommen werden soll.
     * @throws SmbException
     */
    public boolean accept(java.io.File dir, SmbFile file) throws SmbException;
  }

}
