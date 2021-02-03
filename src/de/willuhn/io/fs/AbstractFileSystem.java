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


/**
 * Abstrakte Basis-Implementierung eines Filesystems.
 * @author willuhn
 */
public abstract class AbstractFileSystem implements FileSystem
{

  /**
   * ct.
   */
  public AbstractFileSystem()
  {
    super();
  }

  /**
   * Bereinigt ein Verzeichnis.
   * Dabei werden Backslashes gegen Slashes ersetzt, alle doppelten
   * Slashes entfernt sowie der Slash am Ende.
   * @param dir zu bereinigender Verzeichnisname.
   * @return bereinigter Verzeichnisname.
   */
  protected String clean(String dir)
  {
    if (dir == null || dir.length() == 0)
      return dir;

    dir = dir.replaceAll("\\\\","/");   // Backslashes gegen Slashes ersetzen
    // dir = dir.replaceAll("/{2,}","/");  // redundante Slashes entfernen

    if (dir.endsWith("/"))              // Slash am Ende entfernen
      dir = dir.substring(0,dir.length()-1);

    return dir;
  }

}
