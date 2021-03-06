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
 * Wird geworfen, wenn es zu Fehlern beim Zugriff auf ein FS kam.
 */
public class FSException extends Exception
{

  /**
   * ct
   */
  public FSException()
  {
    super();
  }

  /**
   * ct
   * @param s der Fehlertext.
   */
  public FSException(String s)
  {
    super(s);
  }
  
  /**
   * ct
   * @param s der Fehlertext.
   * @param cause die ursaechliche Exception.
   */
  public FSException(String s, Throwable cause)
  {
    super(s,cause);
  }

  /**
   * ct
   * @param cause die ursaechliche Exception.
   */
  public FSException(Throwable cause)
  {
    super(cause);
  }
}

