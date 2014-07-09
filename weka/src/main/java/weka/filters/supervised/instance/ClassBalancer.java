/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    ClassBalancer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.supervised.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.SpecialFunctions;
import weka.core.Statistics;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.filters.SimpleBatchFilter;
import weka.filters.SupervisedFilter;

/**
 * <!-- globalinfo-start --> Merges values of all nominal attributes among the
 * specified attributes, excluding the class attribute, using the CHAID method,
 * but without considering to re-split merged subsets. It implements Steps 1 and
 * 2 described by Kass (1980), see<br/>
 * <br/>
 * Gordon V. Kass (1980). An Exploratory Technique for Investigating Large
 * Quantities of Categorical Data. Applied Statistics. 29(2):119-127.<br/>
 * <br/>
 * Once attribute values have been merged, a chi-squared test using the
 * Bonferroni correction is applied to check if the resulting attribute is a
 * valid predictor, based on the Bonferroni multiplier in Equation 3.2 in Kass
 * (1980). If an attribute does not pass this test, all remaining values (if
 * any) are merged. Nevertheless, useless predictors can slip through without
 * being fully merged, e.g. identifier attributes.<br/>
 * <br/>
 * The code applies the Yates correction when the chi-squared statistic is
 * computed.<br/>
 * <br/>
 * Note that the algorithm is quadratic in the number of attribute values for an
 * attribute.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -D
 *  Turns on output of debugging information.
 * </pre>
 * 
 * <pre>
 * -L &lt;double&gt;
 *  The significance level (default: 0.05).
 * </pre>
 * 
 * <pre>
 * -R &lt;range&gt;
 *  Sets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'
 *  E.g.: first-5,7,9,20-last
 *  (default: first-last)
 * </pre>
 * 
 * <pre>
 * -V
 *  Invert matching sense (i.e. act on all attributes not specified in list)
 * </pre>
 * 
 * <pre>
 * -O
 *  Use short identifiers for merged subsets.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank
 * @version $Revision: 10215 $
 */
public class ClassBalancer extends SimpleBatchFilter implements SupervisedFilter, WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = 6237337831221353842L;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Reweights the instances in the data so that each class has the same total "
      + "weight. The total sum of weights accross all instances will be maintained. Only "
      + "the weights in the first batch of data received by this filter are changed, so "
      + "it can be used with the FilteredClassifier.";
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) {
    return new Instances(inputFormat, 0);
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Processes the given data.
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    // Only change first batch of data
    if (isFirstBatchDone()) {
      return new Instances(instances);
    }

    // Generate the output and return it
    Instances result = new Instances(instances, instances.numInstances());
    double[] sumOfWeightsPerClass = new double[instances.numClasses()];
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);
      sumOfWeightsPerClass[(int)inst.classValue()] += inst.weight();
    }
    double sumOfWeights = Utils.sum(sumOfWeightsPerClass);

    // Rescale weights
    double factor = sumOfWeights / (double)instances.numClasses();
    for (int i = 0; i < instances.numInstances(); i++) {
      result.add(instances.instance(i)); // This will make a copy
      Instance newInst = result.instance(i);
      copyValues(newInst, false, instances, outputFormatPeek());
      newInst.setWeight(factor * newInst.weight() / 
                        sumOfWeightsPerClass[(int)newInst.classValue()]);
    }
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10215 $");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new ClassBalancer(), args);
  }
}
