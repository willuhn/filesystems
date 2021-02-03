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
import java.io.OutputStream;

/**
 * Interface, welches den Zugriff auf eine Datei protokoll-unabhaengig
 * kapselt.
 */
public interface File
{

  /**
   * Prueft, ob die Datei bereits existiert.
   * @return <code>true</code> wenn die Datei existiert, sonst <code>false</code>.
   * @throws FSException Wenn das Pruefen der Existenz fehlschlug.
   */
  public boolean exists() throws FSException;

  /**
   * Loescht die Datei.
   * Existiert die Datei nicht, kehrt die Funktion fehlerfrei zurueck.
   * @throws FSException Falls es beim Loeschen zu einem Fehler kommt.
   */
  public void delete() throws FSException;
  
  /**
   * Liefert die Groesse der Datei in Bytes.
   * @return Groesse der Datei in Bytes.
   * @throws FSException Wenn das Ermitteln der Dateigroesse fehlschlug.
   */
  public long length() throws FSException;
  
  /**
   * Liefert das Datum der letzten Aenderung der Datei als Millisekunden seit 1.1.1970.
   * @return UNIX-Timestamp (in Milli-Sekunden!) der letzten Aenderung.
   * @see java.io.File#lastModified()
   * @throws FSException Wenn das Ermitteln des Aenderungsdatum fehlschlug.
   */
  public long lastModified() throws FSException;
  
  /**
   * Liefert einen Outputstream, welcher zum Schreiben in die Datei
   * verwendet werden kann.
   * Existiert die Datei bereits, wird sie ueberschrieben.
   * @return ein Outputstream, dessen Ziel in die Datei muendet.
   * @throws FSException Wenn das Erzeugen des Streams fehlschlug.
   */
  public OutputStream getOutputStream() throws FSException;

  /**
   * Liefert einen InputStream zum Lesen aus der Datei.
   * @return InpuStream, mit dem aus der Datei gelesen werden kann.
   * @throws FSException Wenn das Erzeugen des Streams fehlschlug.
   */
  public InputStream getInputStream() throws FSException;
  
  /**
   * Benennt die Datei um.
   * @param name der neue Dateiname (ohne Pfadangabe).
   * @throws FSException Wenn das Umbenennen fehlschlug.
   */
  public void rename(String name) throws FSException;
}
