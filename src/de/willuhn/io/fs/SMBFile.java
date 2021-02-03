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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;

import de.willuhn.logging.Logger;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

/**
 * Implementierung des SMB-Protokolls.
 */
public class SMBFile implements File
{

  private SmbFile file = null;
  private String dir   = null;

  /**
   * @param uri
   * @param name
   * @throws MalformedURLException
   * @throws SmbException
   */
  SMBFile(URI uri, String name) throws MalformedURLException, SmbException
  {
    this.dir = uri.toString();
    
    SmbFile parent = new SmbFile(this.dir);
    Logger.debug("checking, if dir " + this.dir + " exists");
    if (!parent.exists())
    {
      Logger.debug("creating dir " + this.dir);
      parent.mkdirs();
    }
    this.file = new SmbFile(this.dir,name);
  }

  /**
   * @see de.willuhn.io.fs.File#exists()
   */
  public boolean exists() throws FSException
  {
    try
    {
      return file.exists();
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#getOutputStream()
   */
  public OutputStream getOutputStream() throws FSException
  {
    try
    {
      OutputStream os = new SmbFileOutputStream(this.file);
      return new BufferedOutputStream(os);
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#getInputStream()
   */
  public InputStream getInputStream() throws FSException
  {
    try
    {
      InputStream is = new SmbFileInputStream(this.file);
      return new BufferedInputStream(is);
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

    try
    {
      this.file.delete();
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#length()
   */
  public long length() throws FSException
  {
    try
    {
      return this.file.length();
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#lastModified()
   */
  public long lastModified() throws FSException
  {
    try
    {
      return this.file.lastModified();
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

  /**
   * @see de.willuhn.io.fs.File#rename(java.lang.String)
   */
  public void rename(String name) throws FSException
  {
    if (name == null || name.length() == 0)
      throw new FSException("no filename given");

    try
    {
      this.file.renameTo(new SmbFile(this.dir,name));
    }
    catch (IOException e)
    {
      throw new FSException(e);
    }
  }

}
