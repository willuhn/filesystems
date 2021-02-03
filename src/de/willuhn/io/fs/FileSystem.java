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

/**
 * Basis-Interface eines File-Systems mit generischem Zugriff.
 */
public interface FileSystem
{
  /**
   * Initialisiert das File-System mit der genannten URI. 
   * @param uri die URI zum Zielsystem.
   * @throws FSException
   */
  public void init(URI uri) throws FSException;
  
  /**
   * Erzeugt eine neue Datei mit dem angegeben Dateinnamen.
   * @param filename der Dateiname.
   * @return die erzeugte Datei.
   * @throws FSException
   */
  public File create(String filename) throws FSException;
  
  /**
   * Erzeugt eine neue Datei mit dem angegeben Dateinnamen im angegebenen Verzeichnis.
   * Falls das Verzeichnis nicht existiert, sollte die Implementierung selbst
   * versuchen, es anzulegen. Falls die Implementierung keine Verzeichnisse
   * unterstuetzt (z.Bsp. JMS), sollte sie diese entweder ignorieren
   * oder anderweitig emulieren.
   * @param dir das Verzeichnis.
   * @param filename der Dateiname.
   * @return die erzeugte Datei.
   * @throws FSException
   */
  public File create(String dir, String filename) throws FSException;

  /**
   * Liefert eine Liste von Dateien.
   * @param filter optionaler Filter.
   * @return Liste der Dateien.
   * @throws FSException
   */
  public String[] list(FilenameFilter filter) throws FSException;
  
  /**
   * Liefert eine Liste von Dateien in diesem Verzeichnis.
   * @param dir Verzeichnis.
   * @param filter optionaler Filter.
   * @return Liste der Dateien.
   * @throws FSException
   */
  public String[] list(String dir, FilenameFilter filter) throws FSException;

  /**
   * Liefert eine Liste von Verzeichnissen.
   * @param filter optionaler Filter.
   * @return Liste der Verzeichnisse.
   * @throws FSException
   */
  public String[] listDirs(FilenameFilter filter) throws FSException;
  
  /**
   * Liefert eine Liste von Verzeichnissen in diesem Verzeichnis.
   * @param dir Verzeichnis.
   * @param filter optionaler Filter.
   * @return Liste der Verzeichnisse.
   * @throws FSException
   */
  public String[] listDirs(String dir, FilenameFilter filter) throws FSException;

  /**
   * Schliesst das File-System
   * @throws FSException
   */
  public void close() throws FSException;
}
