package org.anc.gate;

import gate.*;
import gate.creole.*;
import gate.corpora.*;

public class GrafDocumentFormat extends XmlDocumentFormat
{

   public GrafDocumentFormat()
   {
   }

   /** Initialise this resource, and return it. */
   public Resource init() throws ResourceInstantiationException {
     // Register XML mime type
     MimeType mime = new MimeType("text", "xml");
     suffixes2mimeTypeMap.put("hdr", mime);
     setMimeType(mime);
     return this;
   }// init()

}
