/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/CandidateInfo.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
***********************************************************************************
*
* Copyright (c) 2003, 2004 The Regents of the University of Michigan, Trustees of Indiana University,
*                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
* 
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
* 
*      http://cvs.sakaiproject.org/licenses/license_1_0.html
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/

//package
package org.sakaiproject.api.app.dissertation;

//imports
import java.util.Vector;

import org.sakaiproject.service.legacy.entity.Entity;
import org.sakaiproject.service.legacy.time.Time;


/**
* <p>CandidateInfo is the interface for BaseCandidateInfo, defined in BaseDissertationService.</p>
* <p>It is the bridge between user/department info from Rackham's database with user/site info in CHEF. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface CandidateInfo
	extends Entity
{
	/**
	* Access the database id for the CandidateInfo object.
	* @return String - the database id.
	*/
	public String getId();

	/**
	* Access the University Emplid for the candidate for this CandidateInfo.
	* @return String - the University Emplid.
	*/
	public String getEmplid();
	
	/**
	* Access CHEF user id for the candidate for this CandidateInfo.
	* @return String - the CHEF user id.
	*/
	public String getChefId();

	/**
	* Access the Rackham Department Id (Group Roll-Up) for the candidate for this CandidateInfo.
	* @return String - Rackham Department Id.
	*/
	public String getProgram();

	/**
	* Access the Site Id for the parent department corresponding with the Rackham Department Id.
	* @return String - the site id.
	*/
	public String getParentSite();

	/**
	* Access the Site Id for this object - There is no active site - the objects are created from the DissertationDataListenerService.
	* @return String - site id - hardcoded as datalistener.
	*/
	public String getSite();
	
	/**
	* Access the place of the oral exam.
	* @return String - oral exam place.
	*/
	public String getOralExamPlace();

	
	/**
	* Access the value of presence of MP record in extract.
	* @return boolean - true if MP record in extract, false otherwise.
	*/
	public boolean getMPRecInExtract();
	
	/**
	* Access the value of presence of OARD record in extract.
	* @return boolean - true if OARD record in extract, false otherwise.
	*/
	public boolean getOARDRecInExtract();
	
	/**
	* Access the date of the oral exam.
	* @return Time - oral exam date.
	*/
	public Time getOralExam();
	
	/**
	* Access the time the committee evaluations were completed.
	* Latest of the times in the Vector containing committee completion times - returns null if any time contained is null
	* @return Time - time of completion.
	*/
	public Time getTimeCommitteeEvalCompleted();
	
	/**
	* Access the time of the oral exam.
	* @return String - time of oral exam.
	*/
	public String getOralExamTime();
	
	/**
	* Access the Advanced to Candidacy Term for this CandidateInfo.
	* @return String - the term (e.g., "FN-2003").
	*/
	public String getAdvCandDesc();

		
	/**
	* Access whether the committee evaluations were completed.
	* @return boolean - false if any times in the Vector are null, true otherwise.
	*/
	public boolean getCommitteeEvalCompleted();
	
	/**
	* Access the text of committee evaluations completed.
	* @return Vector of String - the individual member/evaluation completed Strings.
	*/
	public Vector getCommitteeEvalsCompleted();
	
	/**
	* Access the students degree term as TT-CCYY (e.g. FA-2003)
	* @return The degree term string.
	*/
	public String getDegreeTermTrans();

	/**
	* Access the Time that a Rackham auto-validated step was completed.
	* @return Time - the Time of completion, or null if the step is not completed.
	*/
	public Time getExternalValidation(int externalStepNumber);
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/CandidateInfo.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
