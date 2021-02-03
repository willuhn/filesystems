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

package de.willuhn.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import de.willuhn.io.fs.FSException;
import de.willuhn.io.fs.File;
import de.willuhn.io.fs.FileSystem;
import de.willuhn.io.fs.FileSystemFactory;

/**
 * Ein Kommandozeilen-Client fuer die FS-API, welcher eine FTP-aehnliche
 * Syntax verwendet.
 * @author willuhn
 */
public class Client
{
  
  
  private static Client client      = null;
    private FileSystem fs           = null;
    private BufferedReader keyboard = null;
    private boolean quit            = false;
    private String dir              = null;


  
  /**
   * Main-Methode.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception
  {
    try
    {
      client = new Client((args == null || args.length == 0) ? null : args[0]);
    }
    finally
    {
      if (client != null)
        client.close();
    }
  }

  
  /**
   * ct.
   * @param url optionale Angabe der URL.
   * @throws Exception
   */
  private Client(String url) throws Exception
  {
    this.keyboard = new BufferedReader(new InputStreamReader(System.in));

    println("press <Ctrl><C> to quit, \"help\" to show available commands.");
    if (url != null && url.length() > 0)
      connect(url);
    
    while (!quit)
    {
      try
      {
        handleCommand(prompt());
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * Behandelt das eingegebene Kommando.
   * @param cmd
   * @throws Exception
   */
  private void handleCommand(String cmd) throws Exception
  {
    if (cmd == null || cmd.length() == 0)
      return;
    
    cmd = cmd.trim();

    String params = null;
    int i = cmd.indexOf(" ");
    if (i != -1)
    {
      // hui, an dem Kommando haengen noch Parameter dran.
      params = cmd.substring(i+1);
      cmd = cmd.substring(0,i);
    }

    cmd = cmd.toLowerCase();
    
    if ("connect".equals(cmd))
    {
      connect(params);
      return;
    }
    
    if ("put".equals(cmd))
    {
      put(params);
      return;
    }

    if ("get".equals(cmd))
    {
      get(params);
      return;
    }

    if ("cd".equals(cmd))
    {
      cd(params);
      return;
    }

    if ("quit".equals(cmd))
    {
      println("quit");
      quit = true;
      return;
    }

    if ("help".equals(cmd) || "h".equals(cmd))
    {
      help();
      return;
    }
    if ("dir".equals(cmd))
    {
      dir();
      return;
    }

    println("invalid command: " + cmd);
    
    
  }
  
  /**
   * Zeigt eine Hilfe an.
   */
  private void help()
  {
    println("available commands:\n");
    println("connect <url>  connect to <url>");
    println("cd <dir>       change into server directory <dir>");
    println("put <file>     upload <file> to server");
    println("get <file>     download <file> from server");
    println("dir            list server directory");
    println("quit           close connection and quit client");
    println("help/h         print this help text");
  }
  
  /**
   * Oeffnet den Prompt und wartet auf Kommandos.
   * @return der eingegebene Befehl.
   * @throws IOException
   */
  private String prompt() throws IOException
  {
    if (dir != null)
      System.out.print(dir + "> ");
    else
      System.out.print("> ");
    return keyboard.readLine();
  }

  /**
   * Gibt den Text auf der Console aus und fuegt einen Zeilenumbruch an.
   * @param msg
   */
  private void println(String msg)
  {
    System.out.println(msg);
  }
  
  /**
   * Gibt den Text auf der Console aus.
   * @param msg
   */
  private void print(String msg)
  {
    System.out.print(msg);
  }

  /**
   * Verbindet den Client zur angegebenen URL.
   * @param url URL.
   * @throws Exception
   */
  private void connect(String url) throws Exception
  {
    if (url == null || url.length() == 0)
    {
      println("no url given");
      return;
    }

    // ggf. die vorherige Connection schliessen.
    close();
    this.fs = FileSystemFactory.createFileSystem(url);
    println("ok");
  }

  /**
   * Zeigt den Verzeichnisinhalt auf dem Server an.
   * @throws Exception
   */
  private void dir() throws Exception
  {
    if (fs == null)
    {
      println("not connected");
      return;
    }
    String[] files = fs.list(dir,null);
    if (files == null)
    {
      println("directory " + (dir == null ? "/" : dir) + " does not exist");
      return;
    }
    println("dir: " + (dir == null ? "/" : dir) + "\n");

    int count = 0;
    String[] dirs = fs.listDirs(dir,null);
    if (dirs != null && dirs.length > 0)
    {
      Arrays.sort(dirs);
      for (int i=0;i<dirs.length;++i)
      {
        System.out.println("[DIR] " + dirs[i]);
        count++;
      }
    }

    Arrays.sort(files);
    for (int i=0;i<files.length;++i)
    {
      println(files[i]);
      count++;
    }
    println("\nentries: " + count + "\n");
  }
  
  /**
   * Wechselt auf dem Server in das angegebene Verzeichnis.
   * @param s das Zielverzeichnis.
   * @throws Exception
   */
  private void cd(String s) throws Exception
  {
    if (fs == null)
    {
      println("not connected");
      return;
    }

    if (s == null || s.length() == 0)
    {
      println("no directory given");
      return;
    }
    
    if (".".equals(s))
      return;

    if ("/".equals(s))
    {
      dir = null;
      return;
    }
    
    if (dir != null && s.startsWith(".."))
    {
      int i = dir.lastIndexOf("/");
      if (i == -1)
        dir = null;
      else
        dir = dir.substring(0,i);
      return;
    }
    
    String d = null;
    if (s.startsWith("/"))
      d = s;
    else
      d = (dir == null ? "" : dir) + "/" + s;

    String[] test = fs.list(d,null);
    if (test == null)
    {
      println("directory " + d + " does not exist");
      return;
    }
    dir = d;
  }

  
  /**
   * Kopiert die Datei auf den Server.
   * @param file zu kopierende Datei.
   * @throws Exception
   */
  private void put(String file) throws Exception
  {
    if (fs == null)
    {
      println("not connected");
      return;
    }
    
    if (file == null || file.length() == 0)
    {
      println("no file given");
      return;
    }
    
    java.io.File f = new java.io.File(file);
    
    if (!f.exists())
    {
      println("file does not exist: " + file);
      return;
    }
    if (!f.isFile())
    {
      println("no regular file: " + file);
      return;
    }
    if (!f.canRead())
    {
      println("file not readable: " + file);
      return;
    }
    
    InputStream is = null;
    OutputStream os = null;
    try
    {
      is = new FileInputStream(f);
      File target = fs.create(dir,f.getName());
      os = target.getOutputStream();
      
      print("uploading file " + f.getName() + " ...");
      
      byte[] buf = new byte[4096];
      int read;
      do
      {
        read = is.read(buf);
        if (read > 0)
        {
          os.write(buf,0,read);
        }
      }
      while (read > 0);
      println("ok");
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
          e.printStackTrace();
        }
      }
      if (os != null)
      {
        try
        {
          os.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }

  }
  

  /**
   * Kopiert die Datei vom Server.
   * @param file zu kopierende Datei.
   * @throws Exception
   */
  private void get(String file) throws Exception
  {
    if (fs == null)
    {
      println("not connected");
      return;
    }
    
    if (file == null || file.length() == 0)
    {
      println("no file given");
      return;
    }
    
    File f = fs.create(dir,file);
    
    if (!f.exists())
    {
      println("file does not exist: " + file);
      return;
    }
    
    InputStream is = null;
    OutputStream os = null;
    try
    {
      is = f.getInputStream();
      os = new FileOutputStream(file);
      
      print("downloading file " + file + " ...");
      
      byte[] buf = new byte[4096];
      int read;
      do
      {
        read = is.read(buf);
        if (read > 0)
        {
          os.write(buf,0,read);
        }
      }
      while (read > 0);
      println("ok");
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
          e.printStackTrace();
        }
      }
      if (os != null)
      {
        try
        {
          os.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }

  }
  
  /**
   * Schliesst den Client.
   * @throws FSException
   */
  private void close() throws FSException
  {
    if (fs == null)
      return;

    try
    {
      print("closing connection...");
      fs.close();
      println("ok");
    }
    finally
    {
      fs = null;
    }
  }
}
