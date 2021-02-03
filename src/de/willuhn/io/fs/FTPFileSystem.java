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

import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;

import de.willuhn.logging.Logger;

/**
 * Implementierung des FileSystem-Interface mit FTP-Backend.
 */
public class FTPFileSystem extends AbstractFileSystem
{

  // Legt fest, ob passives FTP verwendet werden soll.
  private final static boolean USE_PASSIVE_MODE = Boolean.getBoolean("filesystems.ftp.usepassive");
  
  private FTPClient client = null;
  private String basedir   = null;
  private URI uri          = null;
  
  /**
   * @see de.willuhn.io.fs.FileSystem#init(java.net.URI)
   */
  public void init(URI uri) throws FSException
  {
    Logger.debug("creating ftp filesystem for " + uri.toString());
    this.uri = uri;
    connect();
  }
  
  /**
   * @throws FSException
   */
  private void connect() throws FSException
  {
    if (this.client != null && this.client.isConnected())
    {
      // "isConnected()" liefert auch dann true, wenn die Connection
      // vom Server aufgrund eines Timeouts getrennt wurde. Also
      // machen wir einfach einen Test
      
      try
      {
        this.client.changeWorkingDirectory(this.basedir);
        return;
      }
      catch (FTPConnectionClosedException e)
      {
        Logger.info("connection closed by foreign host, perfoming auto reconnect");
      }
      catch (IOException ioe)
      {
        throw new FSException(ioe);
      }
    }
    
    try
    {
      Logger.debug("open ftp connection to " + uri.toString());

      this.client = new FTPClient();

      // TCP-Port ermitteln
      int port = uri.getPort();

      String host = uri.getHost();
      // Verbinden
      if (port != -1)
        this.client.connect(host,port);
      else
        this.client.connect(host);


      // Checken ob Verbindung erfolgreich
      int reply = this.client.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply))
        throw new FSException("connect to ftp server failed. code: " + reply);

      Logger.debug("connected");

      String userInfo = uri.getUserInfo();
      if (userInfo == null || userInfo.indexOf(':') == -1)
      {
        userInfo = "anonymous:foo@bar.com";
        Logger.debug("no user given, fallback to anonymous login via " + userInfo);
      }

      // Login
      int dp = userInfo.indexOf(':');
      if (!this.client.login(userInfo.substring(0,dp),userInfo.substring(dp+1)))
        throw new FSException("login failed");
      Logger.debug("logged in");

      String dir = uri.getPath();
      if (dir != null && dir.length() > 0)
      {
        this.basedir = clean(dir);
        if (this.basedir != null && !this.client.changeWorkingDirectory(this.basedir))
          throw new FSException("error while switching into base dir " + this.basedir);
      }

      if (USE_PASSIVE_MODE)
      {
        Logger.debug("using passive mode");
        this.client.enterLocalPassiveMode();
      }
      else
        Logger.debug("using active mode");
        
      Logger.debug("activating binary transfer type");
      this.client.setFileType(FTP.BINARY_FILE_TYPE);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#create(java.lang.String)
   */
  public File create(String filename) throws FSException
  {
    return create(null, filename);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#create(java.lang.String, java.lang.String)
   */
  public File create(String dir, String filename) throws FSException
  {
    // Ueberfluessige Verzeichnis-Trenner entfernen und vereinheitlichen
    if (dir != null && dir.length() > 0)
    {
      dir = clean(dir);
      if (dir.startsWith("/"))           // Slash am Anfang entfernen
        dir = dir.substring(1);
    }
    return new FTPFile(this,dir,filename);
  }

  /**
   * @see de.willuhn.io.fs.FileSystem#close()
   */
  public void close() throws FSException
  {
    if (client == null || !client.isConnected())
      return;

    try
    {
      if (!client.logout())
        Logger.warn("ftp logout failed");
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
    finally
    {
      Logger.debug("disconnect");
      try
      {
        client.disconnect();
      }
      catch (IOException e) {/*ignore */}
      Logger.debug("filesystem closed");
    }
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
      public boolean accept(java.io.File dir, org.apache.commons.net.ftp.FTPFile file)
      {
        return file.isFile() && (filter == null || filter.accept(dir,file.getName()));
      }
    });
  }
  
  /**
   * Wechselt in das angegebene Verzeichnis.
   * @param dir das Verzeichnis.
   * @throws FSException
   */
  private void cd(String dir) throws FSException
  {
    if (dir == null)
      return;

    connect();
    
    try
    {
      if (this.basedir != null)
        dir = clean(this.basedir + (dir.startsWith("/") ? "" : "/") + dir);

      if (!this.client.changeWorkingDirectory(dir))
        throw new FSException("error while switching into dir " + dir);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }
  
  /**
   * Liefert einen OutputStream fuer die Datei.
   * @param file die Datei.
   * @return der OutputStream.
   * @throws FSException
   */
  OutputStream getOutputStream(final FTPFile file) throws FSException
  {
    cd(file.getDir());
    Logger.debug("creating output stream for file " + file.getName());
    try
    {
      return this.client.storeFileStream(file.getName());
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }
  
  /**
   * Liefert einen InputStream fuer die Datei.
   * @param file die Datei.
   * @return der InputStream.
   * @throws FSException
   */
  InputStream getInputStream(final FTPFile file) throws FSException
  {
    cd(file.getDir());
    Logger.debug("creating input stream for file " + file.getName());
    
    try
    {
      final InputStream is = this.client.retrieveFileStream(file.getName());
      if (is == null)
        throw new FileNotFoundException("Datei " + file.getName() + " wurde nicht gefunden");
      return is;
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }
  
  /**
   * Loescht die angegebene Datei.
   * @param file die Datei.
   * @throws FSException
   */
  void delete(FTPFile file) throws FSException
  {
    if (!file.exists())
      return;

    cd(file.getDir());

    Logger.debug("deleting " + file.getName());
    
    try
    {
      this.client.deleteFile(file.getName());
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }
  
  /**
   * Benennt die Datei um.
   * @param file die umzubenennende Datei.
   * @param name der neue Dateiname.
   * @throws FSException
   */
  void rename(FTPFile file,String name) throws FSException
  {
    if (name == null || name.length() == 0)
      throw new FSException("no filename given");

    if (!file.exists())
      throw new FSException("file " + file.getName() + " does not exist in dir " + file.getDir());
    
    cd(file.getDir());

    Logger.debug("renaming " + file.getName() + " to " + name);
    
    try
    {
      this.client.rename(file.getName(),name);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * Liefert das FTP-Handle zu der Datei.
   * @param file Datei.
   * @return FTP-Handle.
   * @throws FSException
   */
  org.apache.commons.net.ftp.FTPFile getFile(FTPFile file) throws FSException
  {
    String[] names = list(file.getDir(),null);
    if (names == null || names.length == 0)
      throw new FSException("file does not exist: " + file.getName());

    try
    {
      org.apache.commons.net.ftp.FTPFile[] files = client.listFiles();

      if (files != null)
      {
        for (int i=0;i<files.length;++i)
        {
          if (files[i].getName().equals(file.getName()))
            return files[i];
        }
      }
      throw new FSException("file does not exist: " + file.getName());
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
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
    return _list(dir,new Filter()
    {
      public boolean accept(java.io.File dir, org.apache.commons.net.ftp.FTPFile file)
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
  private String[] _list(String dir, Filter filter) throws FSException
  {
    cd(dir);
    try
    {
      ArrayList matches = new ArrayList();
      org.apache.commons.net.ftp.FTPFile[] files = client.listFiles();
      java.io.File fd = new java.io.File(dir == null ? "." : dir);

      if (files != null)
      {
        for (int i=0;i<files.length;++i)
        {
          if (filter.accept(fd,files[i]))
            matches.add(files[i].getName());
        }
      }
      return (String[])matches.toArray(new String[matches.size()]);
    }
    catch (IOException e)
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
     * @param file FTPFile-Objekt.
     * @return true, wenn es uebernommen werden soll.
     */
    public boolean accept(java.io.File dir, org.apache.commons.net.ftp.FTPFile file);
  }

}
