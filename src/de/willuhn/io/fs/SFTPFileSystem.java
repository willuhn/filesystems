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
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import de.willuhn.logging.Logger;

/**
 * SFTP-Implementierung eines Filesystems.
 * known_hosts-Datei, SSH-Private-Key und Passwort des Private-Key koennen mit den
 * folgenden System-Properties uebergeben werden:
 * <ul>
 *   <li>filesystems.sftp.known_hosts - Pfad zur Datei known_hosts</li>
 *   <li>filesystems.sftp.private_key - Pfad zur Private-Key Datei id_rsa</li>
 *   <li>filesystems.sftp.passphrase - Password der Private-Key-Datei.</li>
 *   <li>filesystems.sftp.password - Passwort des Benutzers</li>
 */
public class SFTPFileSystem extends AbstractFileSystem
{
  private URI uri             = null;
  private Session session     = null;
  private ChannelSftp channel = null;
  private String basedir      = null;

  /**
   * @see de.willuhn.io.fs.FileSystem#init(java.net.URI)
   */
  public void init(URI uri) throws FSException
  {
    this.uri = uri;
    connect();
  }
  
  /**
   * Stellt die Verbindung zum Server her.
   * @throws FSException
   */
  private void connect() throws FSException
  {
    if (channel != null && channel.isConnected() &&
        session != null && session.isConnected())
      return;

    String home       = System.getProperty("user.home");
    String sep        = System.getProperty("file.separator");
    String password   = System.getProperty("filesystems.sftp.password");
    java.io.File knownhosts = new java.io.File(System.getProperty("filesystems.sftp.known_hosts",home + sep + ".ssh" + sep + "known_hosts"));
    java.io.File privatekey = new java.io.File(System.getProperty("filesystems.sftp.private_key",home + sep + ".ssh" + sep + "id_rsa"));

    String host = uri.getHost();

    // TCP-Port ermitteln
    int port = uri.getPort();
    if (port == -1)
      port = 22;
    
    String username = uri.getUserInfo();
    if (username == null || username.length() == 0)
    {
      username = "anonymous";
      Logger.info("no user given, fallback to anonymous login via " + username);
    }

    // Ist ein Passwort direkt in der URL angegeben?
    int dp = username.indexOf(':');
    if (dp > 0)
    {
      username = username.substring(0,dp);
      password = username.substring(dp+1);
    }
    
    try
    {
      JSch jsch = new JSch();
      
      if (knownhosts.exists() && knownhosts.isFile() && knownhosts.canRead())
      {
        Logger.info("using known_hosts file " + knownhosts);
        jsch.setKnownHosts(knownhosts.getAbsolutePath());
      }
      if (privatekey.exists() && privatekey.isFile() && privatekey.canRead())
      {
        Logger.info("using identity file " + privatekey);
        jsch.addIdentity(privatekey.getAbsolutePath());
      }
      
      session = jsch.getSession(username, host, port);
      session.setUserInfo(new SSHUserinfo(password));

      Hashtable config=new java.util.Hashtable();
      config.put("StrictHostKeyChecking","no"); // siehe http://www.jcraft.com/jsch/README
      session.setConfig(config);
      
      session.connect();

      this.channel = (ChannelSftp) session.openChannel("sftp");
      this.channel.connect();
      
      String dir = uri.getPath();
      if (dir != null && dir.length() > 0)
      {
        dir = clean("/" + dir);
        this.basedir = dir;
      }
      else
      {
        this.basedir = "/";
      }
    }
    catch (Exception e2)
    {
      throw new FSException(e2);
    }
  }
  
  /**
   * @see de.willuhn.io.fs.FileSystem#close()
   */
  public void close() throws FSException
  {
    try
    {
      if (channel != null)
        channel.disconnect();
    }
    finally
    {
      this.channel = null;
    }
    try
    {
      if (session != null)
        session.disconnect();
    }
    finally
    {
      this.session = null;
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
    return new SFTPFile(this,dir,filename);
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
      public boolean accept(java.io.File dir, LsEntry file)
      {
        SftpATTRS at = file.getAttrs();
        return !at.isDir() && !at.isLink() && (filter == null || filter.accept(dir,file.getFilename()));
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
    return _list(dir,new Filter()
    {
      public boolean accept(java.io.File dir, LsEntry file)
      {
        String name = file.getFilename();
        return file.getAttrs().isDir() && 
               !".".equals(name) && 
               !"..".equals(name) && 
               (filter == null || filter.accept(dir,file.getFilename()));
      }
    });
  }

  /**
   * Liefert die Attribute einer Datei oder null, wenn sie nicht gefunden wurden.
   * @param file die Datei.
   * @return Attribute.
   * @throws FSException
   */
  private SftpATTRS getAttributes(SFTPFile file) throws FSException
  {
    connect();

    String dir = file.getDir() == null ? "" : file.getDir();
    dir = clean(this.basedir + "/" + dir);
    
    try
    {
      Vector v = this.channel.ls(dir);
      if (v == null || v.size() == 0)
      {
        Logger.warn("file " + file.getName() + " not found");
        return null;
      }

      for (int i=0;i<v.size();++i)
      {
        Object o = v.get(i);
        if (o == null || !(o instanceof LsEntry))
          continue; // interessiert uns nicht
        LsEntry entry = (LsEntry) o;
        if (!file.getName().equals(entry.getFilename()))
          continue;
        return entry.getAttrs();
      }
      Logger.warn("file " + file.getName() + " not found");
      return null;
    }
    catch (SftpException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * Liefert die Laenge der Datei in Bytes.
   * @param f Datei.
   * @return Laenge in Bytes.
   * @throws FSException
   */
  long length(SFTPFile f) throws FSException
  {
    SftpATTRS attrs = getAttributes(f);
    return attrs == null ? 0L : attrs.getSize();
  }
  
  /**
   * Liefert einen UNIX-Timestamp mit dem Datum der letzten Aenderung der Datei.
   * @param f Datei.
   * @return Timestamp oder 0, wenn sie nicht existiert.
   * @see java.io.File#lastModified()
   * @throws FSException
   */
  long lastModified(SFTPFile f) throws FSException
  {
    SftpATTRS attrs = getAttributes(f);
    return attrs == null ? 0L : (attrs.getMTime() * 1000L); // getMTime() liefert Sekunden
  }

  /**
   * Liefert einen OutputStream fuer die Datei.
   * @param file die Datei.
   * @return der OutputStream.
   * @throws FSException
   */
  OutputStream getOutputStream(SFTPFile file) throws FSException
  {
    connect();

    String dir = file.getDir() == null ? "" : file.getDir();
    dir = clean(this.basedir + "/" + dir);

    try
    {
      return channel.put(dir + "/" + file.getName());
    }
    catch (SftpException e)
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
  InputStream getInputStream(SFTPFile file) throws FSException
  {
    connect();

    String dir = file.getDir() == null ? "" : file.getDir();
    dir = clean(this.basedir + "/" + dir);

    try
    {
      return channel.get(dir + "/" + file.getName());
    }
    catch (SftpException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * Loescht die angegebene Datei.
   * @param file die zu loeschende Datei.
   * @throws FSException
   */
  void delete(SFTPFile file) throws FSException
  {
    connect();

    String dir = file.getDir() == null ? "" : file.getDir();
    dir = clean(this.basedir + "/" + dir);

    try
    {
      channel.rm(dir + "/" + file.getName());
    }
    catch (SftpException e)
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
  void rename(SFTPFile file,String name) throws FSException
  {
    if (name == null || name.length() == 0)
      throw new FSException("no filename given");

    connect();

    String dir = file.getDir() == null ? "" : file.getDir();
    dir = clean(this.basedir + "/" + dir);

    if (!file.exists())
      throw new FSException("file " + file.getName() + " does not exist in dir " + dir);

    try
    {
      channel.rename(dir + "/" + file.getName(),dir + "/" + name);
    }
    catch (SftpException e)
    {
      throw new FSException(e);
    }
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
    connect();
    try
    {
      dir = dir == null ? "" : dir;
      dir = clean(this.basedir + "/" + dir);
      Vector v = this.channel.ls(dir);
      if (v == null || v.size() == 0)
        return new String[0];
      
      ArrayList list = new ArrayList();
      java.io.File fd = new java.io.File(dir);
      for (int i=0;i<v.size();++i)
      {
        Object o = v.get(i);
        if (o == null || !(o instanceof LsEntry))
          continue; // interessiert uns nicht
        LsEntry entry = (LsEntry) o;
        String name = entry.getFilename();
        
        if (filter.accept(fd,entry))
          list.add(name);
      }
      return (String[])list.toArray(new String[list.size()]);
    }
    catch (SftpException e)
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
     */
    public boolean accept(java.io.File dir, LsEntry file);
  }

  
  /**
   * Implementierung des Userinfo-Callback.
   * Wir machen jedoch keine Interaktion per STDIN.
   */
  private class SSHUserinfo implements UserInfo
  {
    private String password = null;
    
    /**
     * ct.
     * @param password das Passwort.
     */
    private SSHUserinfo(String password)
    {
      this.password = password;
    }

    /**
     * @see com.jcraft.jsch.UserInfo#getPassphrase()
     */
    public String getPassphrase()
    {
      return System.getProperty("filesystems.sftp.passphrase");
    }

    /**
     * @see com.jcraft.jsch.UserInfo#getPassword()
     */
    public String getPassword()
    {
      return this.password;
    }

    /**
     * @see com.jcraft.jsch.UserInfo#promptPassphrase(java.lang.String)
     */
    public boolean promptPassphrase(String message)
    {
      // Wenn wir eine Passphrase haben, melden wir, dass wir eines bereitstellen koennen.
      String p = getPassphrase();
      return p != null && p.length() > 0;
    }

    /**
     * @see com.jcraft.jsch.UserInfo#promptPassword(java.lang.String)
     */
    public boolean promptPassword(String message)
    {
      return this.password != null && this.password.length() > 0;
    }

    /**
     * @see com.jcraft.jsch.UserInfo#promptYesNo(java.lang.String)
     */
    public boolean promptYesNo(String message)
    {
      Logger.info("sftp question: \"" + message + "\": answering with: yes");
      return true;
    }

    /**
     * @see com.jcraft.jsch.UserInfo#showMessage(java.lang.String)
     */
    public void showMessage(String message)
    {
      Logger.info("sftp message: \"" + message + "\"");
    }
    
  }
}
