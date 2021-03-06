/**
 Copyright 2009-2013 The MITRE Corporation.
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 **/
package org.opensextant.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The Class PostProcessorBase.<br>
 * This abstract class provides a base class for PostProcessor implementations which perform de-duping or disambiguation
 * type postprocessing. It provides the base logic for this type of processing which consists of finding all sets of
 * "interacting" annotations (those that overlap, contain/are contained by each other), sorting each interacting set
 * using a Comparator (specified by the extending subclass) and then selecting the first (top ranked) annotation in each
 * interacting set. This base class is extended by providing an implementation of the getComparator() method. The
 * comparator returned by this method is used to sort each interacting set and thus determines which annotation is
 * selected from each interacting set.
 */
public abstract class PostProcessorBase implements PostProcessor {

  /**
   * Find all sets of interacting (overlapping, containing/contained by) RegexAnnotations.
   * @param annos
   *          the annotations to analyze for interactions
   * @return the list of lists of interacting annotations
   */
  public List<List<RegexAnnotation>> findInteractions(List<RegexAnnotation> annos) {

    // the list of lists of interacting annos, to be returned
    List<List<RegexAnnotation>> inters = new ArrayList<List<RegexAnnotation>>();

    // null or empty list as input, no interactions, return list with one empty list
    if (annos == null || annos.size() == 0) {
      inters.add(new ArrayList<RegexAnnotation>());
      return inters;
    }

    // sort the annotations into the order in which they appear in the document
    Collections.sort(annos, new PositionComparator());

    // the current group of interacting annos
    List<RegexAnnotation> currentGroup = new ArrayList<RegexAnnotation>();

    // add the first anno to the current group to start
    RegexAnnotation a = annos.get(0);
    currentGroup.add(a);
    // add the current group to the list
    // inters.add(currentGroup);

    // loop over all the rest of the annos
    for (int i = 1; i < annos.size(); i++) {

      // get the next anno to compare
      RegexAnnotation b = annos.get(i);

      // TODO confirm this logic for all possible combos of interaction
      // compare the next anno to the current group
      if (interactsWithGroup(currentGroup, b)) {
        currentGroup.add(b);
      } else {// end of current group, b goes in next group
        // add the current group to the list
        inters.add(currentGroup);
        // get a new group and add to list
        currentGroup = new ArrayList<RegexAnnotation>();
        inters.add(currentGroup);
        // add b to new group
        currentGroup.add(b);
      }

      // slide forward to next anno
      a = b;
    }

    // catch the last group if not already added
    if (!inters.contains(currentGroup)) {
      inters.add(currentGroup);
    }
    return inters;
  }

  // check for interactions between an annotation and an existing group
  private boolean interactsWithGroup(List<RegexAnnotation> group, RegexAnnotation b) {

    for (RegexAnnotation g : group) {
      if (g.interactsWith(b)) {
        return true;
      }
    }

    return false;
  }

  /*
   * (non-Javadoc)
   * @see org.opensextant.regex.PostProcessor#postProcess(java.util.List, java.util.Set)
   */
  public void postProcess(List<RegexAnnotation> annos, Set<String> types) {

    // null or empty input list, do nothing
    if (annos == null || annos.size() == 0) {
      return;
    }

    // the subset of all annos to keeep
    List<RegexAnnotation> keeperAnnos = new ArrayList<RegexAnnotation>();

    // the list of groups of interacting (overlapping,contained/containing) annos
    List<List<RegexAnnotation>> interactors = findInteractions(annos);

    // for each group of interacting annos decide which if any to keep
    for (List<RegexAnnotation> inters : interactors) {
      // pass to decision logic
      List<RegexAnnotation> keepers = decide(inters);
      // add returned list to annotations to kepp
      keeperAnnos.addAll(keepers);
    }

    // keep only those annotations selected by decision logic
    annos.retainAll(keeperAnnos);
    return;

  }

  /**
   * The decision logic. Sort each set of interacting annotations and pick the top ranked annotation.
   * @param inters
   *          a list of interacting annotation
   * @return the list of annotations to keep
   */
  public List<RegexAnnotation> decide(List<RegexAnnotation> inters) {
    List<RegexAnnotation> keepers = new ArrayList<RegexAnnotation>();

    if (inters != null && inters.size() > 0) {
      // sort the annotations by temporal resolution
      Collections.sort(inters, this.getComparator());
      // select annotation with highest resolution
      keepers.add(inters.get(0));
    }
    return keepers;
  }

  /**
   * Gets the comparator. Subclasses provide an implementation of this to supply the Comparator to be used in the
   * decide() method.
   * @return the comparator to be used to sort
   */
  public abstract Comparator<? super RegexAnnotation> getComparator();

}
