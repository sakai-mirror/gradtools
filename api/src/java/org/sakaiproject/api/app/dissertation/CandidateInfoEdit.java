/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/CandidateInfoEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
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
import java.util.Collection;

import org.sakaiproject.service.legacy.entity.Edit;
import org.sakaiproject.service.legacy.time.Time;

/**
* <p>CandidateInfoEdit is the interface for BaseCandidateInfoEdit, defined in BaseDissertationService.</p>
* <p>It is the bridge between user/department info from Rackham's database with user/site info in CHEF. </p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public interface CandidateInfoEdit
	extends Edit, CandidateInfo
{
	/**
	* Access the University Emplid for the candidate for this CandidateInfo.
	* @return String - the University Emplid.
	*/
	public void setEmplid(String emplid);

	/**
	* Access CHEF user id for the candidate for this CandidateInfo.
	* @return String - the CHEF user id.
	*/
	public void setChefId(String chefid);
	
	/**
	* Set the individual member/eval completed text to display with time committee eval completed.
	*/
	public void addCommitteeEvalsCompleted(Collection evals);
	

	/**
	* Access the Rackham Department Id (Group Roll-Up) for the candidate for this CandidateInfo.
	* @return String - Rackham Department Id.
	*/
	public void setProgram(String program);

	/**
	* Access the Site Id for the parent department corresponding with the Rackham Department Id.
	* @return String - the site id.
	*/
	public void setParentSite(String groupId);

	/**
	* Access the Site Id for this object - There is no active site - the objects are created from the DissertationDataListenerService.
	* @return String - site id - hardcoded as datalistener.
	*/
	public void setSite(String site);	

	/**
	* Set the Rackham Milestone.
	* @param String - the Advance to Candidacy Description.
	*/
	public void setAdvCandDesc(String desc);

	/**
	* Set the Rackham Milestone.
	* @param String - the Milestone.
	*/	
	public void setMilestone(String milestone);
	
	/**
	* Set the source of values from MP record.
	* @param boolean - true if MP record in extract, false otherwise.
	*/	
	public void setMPRecInExtract(boolean present);
	
	/**
	* Set the source of values from OARD record.
	* @param boolean - true if OARD record in extract, false otherwise.
	*/	
	public void setOARDRecInExtract(boolean present);
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - milestone completed
	*/
	public void setTimeMilestoneCompleted(Time completed);

	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - committee approval
	*/	
	public void setTimeCommitteeApproval(Time approval);
	
	/**
	* Set the Rackham Prelim Milestone.
	* @param String - the Milestone.
	*/	
	public void setPrelimMilestone(String milestone);
	
	/**
	* Set the time of completion of the Rackham Prelim Milestone.
	* @param Time - milestone completed
	*/
	public void setPrelimTimeMilestoneCompleted(Time completed);
	/**
	* Set the Rackham Avdcand Milestone.
	* @param String - the Milestone.
	*/	
	public void setAdvcandMilestone(String milestone);
	
	/**
	* Set the time of completion of the Rackham Advcand Milestone.
	* @param Time - milestone completed
	*/
	public void setAdvcandTimeMilestoneCompleted(Time completed);
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - oral exam
	*/
	public void setTimeOralExam(Time exam);
	
	/**
	* Set the Rackham Milestone.
	* @param String - oral exam place
	*/
	public void setOralExamPlace(String place);
	
	/**
	* Set the Rackham Milestone.
	* @param String - oral exam date
	*/
	public void setOralExamTime(String date);
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - first format
	*/
	public void setTimeFirstFormat(Time format);

	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - committee evaluation
	*/
	public void addTimeCommitteeEval(Time timeCompleted);
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - oral report returned
	*/
	public void setTimeOralReportReturned(Time timeReturned);
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - committee certification
	*/
	public void setTimeCommitteeCert(Time timeOfCert);
	
	/**
	* Set the Rackham Milestone.
	* @param String - final format recorder
	*
	public void setFinalFormatRecorder(String recorder);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - binder receipt
	*
	public void setTimeBinderReceipt(Time receipt);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - fee requirement met
	*
	public void setFeeRequirementMet(boolean met);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - receipt seen
	*
	public void setTimeReceiptSeen(Time seen);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - publication fee
	*
	public void setTimePubFee(Time fee);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - unbound
	*
	public void setTimeUnbound(Time unbound);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - abstract
	*
	public void setTimeAbstract(Time abs);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - bound
	*
	public void setTimeBound(Time bound);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - diploma application
	*
	public void setTimeDiplomaApp(Time app);
	*/
	
	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - contract
	*
	public void setTimeContract(Time contract);
	*/

	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - survey
	*
	public void setTimeSurvey(Time survey);
	*/

	/**
	* Set the time of completion of the Rackham Milestone.
	* @param Time - degree conferred
	*/
	public void setTimeDegreeConferred(Time conferred);
	
	/**
	* Set the Rackham Milestone.
	* @param String - the Degree Term Translation.
	*/
	public void setDegreeTermTrans(String trans);
}

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/api/src/java/org/sakaiproject/api/app/dissertation/CandidateInfoEdit.java,v 1.1 2005/05/04 20:27:12 ggolden.umich.edu Exp $
*
**********************************************************************************/
