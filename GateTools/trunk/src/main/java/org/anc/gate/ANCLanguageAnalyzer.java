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

import gate.*;
import gate.util.*;
import gate.creole.*;
import gate.creole.AbstractLanguageAnalyser;

public class ANCLanguageAnalyzer extends AbstractLanguageAnalyser
{
   protected boolean failed = false;
   
   public boolean failed() { return failed; }
   public boolean defined(String s)
   {
     return !((s == null) || ("".equals(s)));
   }

   public AnnotationSet getAnnotations(String name) throws ExecutionException
   {
     if (document == null)
       throw new ExecutionException("Document has not been set.");

     if (name == null ||
         "".equals(name) ||
         "Default".equals(name))
     {
       return document.getAnnotations();
     }
     return document.getAnnotations(name);
   }

   public long getAnnotationStart(Object a)
   {
     assert(a instanceof Annotation);
     return ((Annotation)a).getStartNode().getOffset().intValue();
   }

   public long getAnnotationEnd(Object a)
   {
     assert(a instanceof Annotation);
     return ((Annotation)a).getEndNode().getOffset().intValue();
   }
}
