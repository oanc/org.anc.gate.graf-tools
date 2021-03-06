/*-
 * Copyright (c) 2008 American National Corpus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.anc.gate;

import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import org.anc.gate.core.ANCLanguageAnalyzer;

@CreoleResource(
        name = "GrAF Save Content",
        comment = "Saves the text content from a GATE document."
)
public class SaveContent extends ANCLanguageAnalyzer
{
   // Properties to hold parameter values.
   private java.net.URL destination = null;
   private java.lang.String encoding = null;

   public SaveContent()
   {
      
   }

   @Override
   public Resource init() throws ResourceInstantiationException
   {
      super.init();
      return this;
   }

   @Override
   public void reInit() throws ResourceInstantiationException
   {
      this.init();
   }

   @Override
   public void execute() throws ExecutionException
   {
      failed = true;
      if (null == destination)
      {
         throw new ExecutionException("Parameter destination has not been set.");
      }

      try
      {
         File outputFile = new File(destination.getPath());
         if (outputFile.isDirectory())
         {
            String name = document.getName();
            System.out.println("Using document name as file name: " + name);
            int index = name.lastIndexOf(".txt");
            if (index > 0)
            {
               name = name.substring(0, index) + ".txt";
            }
            outputFile = new File(outputFile, name);
         }
         FileOutputStream ofstream = new FileOutputStream(outputFile);
         OutputStreamWriter writer = new OutputStreamWriter(ofstream, encoding);
//        System.out.println("Set encoding to " + writer.getEncoding());
//        FileWriter writer = new FileWriter(destination.getPath());

         String content = document.getContent().toString();
         writer.write(content);
         writer.close();

//	       System.out.println("Content for " + document.getName());
//	        System.out.println(content);
         failed = false;
      }
      catch (IOException ex)
      {
         throw new ExecutionException("SaveContent I/O Exception: ", ex);
      }
   }

   @RunTime
   @Optional(false)
   @CreoleParameter(comment = "Where the standoff annotations will be saved.")
   public void setDestination(java.net.URL destination)
   {
      this.destination = destination;
   }
   public java.net.URL getDestination()
   {
      return destination;
   }

   @RunTime
   @Optional
   @CreoleParameter(
           comment = "Character encoding of the standoff file.",
           defaultValue = "UTF-8"
   )
   public void setEncoding(java.lang.String encoding)
   {
      this.encoding = encoding;
   }
   public java.lang.String getEncoding()
   {
      return encoding;
   }

}
