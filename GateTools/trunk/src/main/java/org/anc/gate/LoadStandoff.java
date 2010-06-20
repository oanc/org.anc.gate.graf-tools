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

import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.xces.standoff.Annotation;
import org.xces.standoff.AnnotationParser;
import org.xml.sax.SAXException;

/**
 * 
 * @author Keith Suderman
 */

public class LoadStandoff extends ANCLanguageAnalyzer
{
   public static final long serialVersionUID = 1L;
   public static final String STANDOFFASNAME_PARAMETER = "standoffASName";
   public static final String SOURCE_URL_PARAMETER = "sourceUrl";

   protected transient String standoffASName = null;
   protected transient URL sourceUrl = null;
//   private transient AnnotationParser<List<Annotation>> parser = null;
   private transient AnnotationParser parser;

   public LoadStandoff()
   {
//      parser = new AnnotationParser<List<Annotation>>();
      parser = new AnnotationParser();
   }

   @Override
   public void execute() throws ExecutionException
   {
      AnnotationSet as = getAnnotations(standoffASName);
//	 parser.setAnnotationSet(as);
      List<Annotation> list = null; // = new LinkedList<Annotation>();
      try
      {
         list = parser.parse(sourceUrl.toURI());
//         parser.parse(list, sourceUrl.getPath());
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new ExecutionException(e);
      }
      for (Annotation a : list)
      {
         long start = a.getStart();
         long end = a.getEnd();

         if (start < 0 || end < start)
         {
            System.out.println("Invalid offsets for "
                  + a.getType().getLocalName() + " from " + a.getStart()
                  + " to " + a.getEnd());
         }
         else
         {
            FeatureMap fm = Factory.newFeatureMap();
            for (Annotation.Feature f : a.getFeatures())
            {
//   	       System.out.println("   feature " + f.getKey() + "=" + f.getValue());
               fm.put(f.getKey().toString(), f.getValue());
            }
            try
            {
               as.add(Long.valueOf(a.getStart()), Long.valueOf(a.getEnd()), 
                     a.getType().getLocalName(), fm);
            }
            catch (InvalidOffsetException e)
            {
               System.out.println("Invalid offsets for "
                     + a.getType().getLocalName() + " from " + a.getStart()
                     + " to " + a.getEnd());
               //throw new ExecutionException(e);
            }
         }
      }
   }

   @Override
   public Resource init() throws ResourceInstantiationException
   {
      return super.init();
   }

   @Override
   public void reInit() throws ResourceInstantiationException
   {
      this.init();
   }

   public String getStandoffASName()
   {
      return standoffASName;
   }

   public void setStandoffASName(String name)
   {
      standoffASName = name;
   }

   public URL getSourceUrl()
   {
      return sourceUrl;
   }

   public void setSourceUrl(URL url)
   {
      sourceUrl = url;
   }
}
