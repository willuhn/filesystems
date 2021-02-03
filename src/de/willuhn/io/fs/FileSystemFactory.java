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
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.willuhn.logging.Logger;

/**
 * Implementierung der FS-Factory.
 */
public class FileSystemFactory
{

  // Hier halten wir die registrierten File-Systeme.
  private static Map<String,Class<? extends FileSystem>> filesystems = new HashMap<String,Class<? extends FileSystem>>();

  static
  {
    InputStream is = null;
    
    try
    {
      ClassLoader loader = FileSystemFactory.class.getClassLoader();

      Logger.debug("loading filesystems.properties");
      is = FileSystemFactory.class.getResourceAsStream("/filesystems.properties");
      Properties props = new Properties();
      props.load(is);

      Enumeration e = props.keys();
      while (e.hasMoreElements())
      {
        // Name der implementierenden Klasse
        String clazz = (String) e.nextElement();
        
        // Protokoll
        String prot = props.getProperty(clazz);
        if (prot.indexOf(",") != -1)
        {
          // Uuh, eine komma-separierte Liste von Protokollen
          String[] list = prot.split(",");
          for (int i=0;i<list.length;++i)
          {
            String s = list[i].trim().toLowerCase();
            Class c = loader.loadClass(clazz);
            register(s,c);
          }
        }
        else
        {
          String s = prot.trim().toLowerCase();
          Logger.debug("  " + s + " - implemented by " + clazz);
          try
          {
            filesystems.put(s,(Class<FileSystem>)loader.loadClass(clazz));
          }
          catch (Throwable t)
          {
            Logger.error("unable to load filesystem class " + clazz,t);
          }
        }
      }
    }
    catch (Throwable t)
    {
      Logger.error("unable to load filesystems",t);
    }
    finally
    {
      if (is != null)
      {
        try
        {
          is.close();
        }
        catch (Exception e)
        {
          Logger.error("error while closing stream",e);
        }
      }
    }
  }

  /**
   * Liefert eine File-System-Implementierung passend fuer die uebergebene URI.
   * @param uri URI im Format &lt;protocol&gt;://[&lt;username&gt;:&lt;password&gt;@][&lt;host&gt;]/&lt;dir&gt;.
   * Beispiele:
   * <ul>
   *   <li>ftp://user:password@servername/zielverzeichnis</li>
   *   <li>file:///zielverzeichnis</li>
   *   <li>smb://user:password@servername/zielverzeichnis</li>
   *   <li>jdbc:....... (Bsp.: jdbc:oracle:thin:scott/tiger@host:1521:DATABASE)</li>
   *   <li>jms://&lt;provider-url&gt;?connectionfactory=&lt;wert&gt;&amp;queue=&lt;wert&gt;&amp;&lt;jndi-param&gt;=&lt;wert&gt;[&amp;user=&lt;user&gt;&amp;password=&lt;password&gt;]</li>
   * </ul>
   * @return das entsprechende Filesystem.
   * @throws Exception Falls beim Erstellen des File-Systems ein Fehler auftrat oder
   * fuer das Protokoll kein File-System existiert.
   */
  public static FileSystem createFileSystem(String uri) throws Exception
  {
    Logger.debug("loading filesystem for uri " + uri);

    if (uri == null || uri.length() == 0)
      throw new Exception("uri cannot be null");

    int i = uri.indexOf(':');
    
    String prot = null;
    if (i != -1)
    {
      if (i == 1)
      {
        // Das ist eine Windows-Pfadangabe.
        // Z.Bsp.: "E:/daten/foo/bar"
        prot = "file";
      }
      else
      {
        prot = uri.substring(0,i);
      }
      Logger.debug("protocoll: " + prot);
    }

    Class<? extends FileSystem> c = filesystems.get(prot);
    
    if (c == null)
    {
      Logger.warn("no filesystem found for url, fallback to local filesystem: " + LocalFileSystem.class);

      c = LocalFileSystem.class;
    }
      
    FileSystem f = c.newInstance();
    f.init(new URI(uri));
    return f;
  }
  
  /**
   * Registriert ein Custom-Filesystem.
   * @param protocol Name des zu registrierenden Protokolls.
   * Zum Beispiel "file" oder "ftp".
   * @param filesystem Klasse der Implementierung.
   * Diese Klasse muss das Interface <code>FileSystem</code> implementieren.
   */
  public static void register(String protocol, Class filesystem)
  {
    if (filesystem != null)
      Logger.debug("register filesystem " + filesystem.getName() + " for protocol " + protocol);
    else
      Logger.debug("unsregister filesystem for protocol " + protocol);
      
    filesystems.put(protocol,filesystem);
  }
}
