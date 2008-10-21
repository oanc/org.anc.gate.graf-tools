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
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.xces.standoff.Annotation;
import org.xces.standoff.AnnotationParser;

//import org.xces.gate.CesAnaParser;
//import ANC.creole.*;

/**
 * <p> </p>
 * <p> </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: American National Corpus</p>
 * @author Keith Suderman
 * @version 1.0
 */

public class LoadStandoff extends ANCLanguageAnalyzer
{
  public static final String STANDOFFASNAME_PARAMETER = "standoffASName";
  public static final String SOURCE_URL_PARAMETER = "sourceUrl";

  protected String standoffASName = null;
  protected URL sourceUrl = null;
//  private CesAnaParser parser = null;
  private AnnotationParser<List<Annotation>> parser = null;
  
  public LoadStandoff()
  {
//	 parser = new CesAnaParser();
     parser = new AnnotationParser<List<Annotation>>();
  }

  public void execute() throws ExecutionException
  {
	 AnnotationSet as = getAnnotations(standoffASName);
//	 parser.setAnnotationSet(as);
     List<Annotation> list = new LinkedList<Annotation>();
	 parser.parse(list, sourceUrl.getPath());
	 for (Annotation a : list)
	 {
	    FeatureMap fm = Factory.newFeatureMap();
	    for (Annotation.Feature f : a.getFeatures())
	    {
	       fm.put(f.getKey(), f.getValue());
	    }
	    try
      {
         as.add(new Long(a.getStart()), new Long(a.getEnd()), 
               a.getType().getLocalName(), fm);
      }
      catch (InvalidOffsetException e)
      {
         throw new ExecutionException(e);
      }
	 }
  }

  public Resource init() throws ResourceInstantiationException
  {
	 return super.init();
  }

  public void reInit() throws ResourceInstantiationException
  {
	 this.init();
  }

  public String getStandoffASName() { return standoffASName; }
  public void setStandoffASName(String name) { standoffASName = name; }

  public URL getSourceUrl() { return sourceUrl; }
  public void setSourceUrl(URL url) { sourceUrl = url; }
}
