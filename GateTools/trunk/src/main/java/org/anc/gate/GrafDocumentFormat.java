package org.anc.gate;

import gate.*;
import gate.creole.*;
import gate.corpora.*;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleResource;


@CreoleResource(
        isPrivate = true,
        name = "GrAF Document Format",
        comment = "Used to tell GATE the GrAF header files are text/xml.",
        autoinstances = { @AutoInstance(hidden=true) }
)
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
