/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/component/src/java/org/sakaiproject/component/app/dissertation/BaseDissertationService.java,v 1.2 2005/05/12 02:08:36 ggolden.umich.edu Exp $
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

// package
package org.sakaiproject.component.app.dissertation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.sakaiproject.api.app.dissertation.BlockGrantGroup;
import org.sakaiproject.api.app.dissertation.BlockGrantGroupEdit;
import org.sakaiproject.api.app.dissertation.CandidateInfo;
import org.sakaiproject.api.app.dissertation.CandidateInfoEdit;
import org.sakaiproject.api.app.dissertation.CandidatePath;
import org.sakaiproject.api.app.dissertation.CandidatePathEdit;
import org.sakaiproject.api.app.dissertation.Dissertation;
import org.sakaiproject.api.app.dissertation.DissertationEdit;
import org.sakaiproject.api.app.dissertation.DissertationService;
import org.sakaiproject.api.app.dissertation.DissertationStep;
import org.sakaiproject.api.app.dissertation.DissertationStepEdit;
import org.sakaiproject.api.app.dissertation.StepStatus;
import org.sakaiproject.api.app.dissertation.StepStatusEdit;
import org.sakaiproject.api.kernel.function.cover.FunctionManager;
import org.sakaiproject.api.kernel.session.cover.SessionManager;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.service.framework.config.ServerConfigurationService;
import org.sakaiproject.service.framework.log.Logger;
import org.sakaiproject.service.framework.memory.Cache;
import org.sakaiproject.service.framework.memory.CacheRefresher;
import org.sakaiproject.service.framework.memory.MemoryService;
import org.sakaiproject.service.framework.session.SessionStateBindingListener;
import org.sakaiproject.service.legacy.entity.Edit;
import org.sakaiproject.service.legacy.entity.Entity;
import org.sakaiproject.service.legacy.entity.EntityManager;
import org.sakaiproject.service.legacy.entity.EntityProducer;
import org.sakaiproject.service.legacy.entity.HttpAccess;
import org.sakaiproject.service.legacy.entity.Reference;
import org.sakaiproject.service.legacy.entity.ResourceProperties;
import org.sakaiproject.service.legacy.entity.ResourcePropertiesEdit;
import org.sakaiproject.service.legacy.event.Event;
import org.sakaiproject.service.legacy.event.cover.EventTrackingService;
import org.sakaiproject.service.legacy.id.cover.IdService;
import org.sakaiproject.service.legacy.security.cover.SecurityService;
import org.sakaiproject.service.legacy.site.Site;
import org.sakaiproject.service.legacy.time.Time;
import org.sakaiproject.service.legacy.time.cover.TimeService;
import org.sakaiproject.service.legacy.user.User;
import org.sakaiproject.service.legacy.user.cover.UserDirectoryService;
import org.sakaiproject.util.Validator;
import org.sakaiproject.util.java.StringUtil;
import org.sakaiproject.util.resource.BaseResourcePropertiesEdit;
import org.sakaiproject.util.storage.StorageUser;
import org.sakaiproject.util.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
* <p>BaseDissertationService is the abstract service class for Dissertations.</p>
* <p>The Concrete Service classes extending this are the XmlFile and DbCached storage classes.</p>
*
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
* @see org.chefproject.core.Dissertation
*/
public abstract class BaseDissertationService
	implements DissertationService
{	
	/** A Storage object for persistent storage of Dissertations. */
	protected DissertationStorage m_dissertationStorage = null;

	/** A Storage object for persistent storage of DissertationSteps. */
	protected DissertationStepStorage m_stepStorage = null;
	
	/** A Storage object for persistent storage of CandidatePaths. */
	protected CandidatePathStorage m_pathStorage = null;

	/** A Storage object for persistent storage of StepStatus. */
	protected StepStatusStorage m_statusStorage = null;
	
	/** A Storage object for persistent storage of BlockGrantGroup. */
	protected BlockGrantGroupStorage m_groupStorage = null;

	/** A Storage object for persistent storage of CandidateInfos. */
	protected CandidateInfoStorage m_infoStorage = null;

	/** A Cache for this service -  Dissertations keyed by reference. */
	protected Cache m_dissertationCache = null;

	/** A Cache for this service -  DissertationSteps keyed by reference. */
	protected Cache m_stepCache = null;
	
	/** A Cache for this service -  CandidatePaths keyed by reference. */
	protected Cache m_pathCache = null;

	/** A Cache for this service -  StepStatus keyed by reference. */
	protected Cache m_statusCache = null;
	
	/** A Cache for this service -  BlockGrantGroup keyed by reference. */
	protected Cache m_groupCache = null;

	/** A Cache for this service -  CandidateInfos keyed by reference. */
	protected Cache m_infoCache = null;
	
	/** The access point URL. */
	protected String m_relativeAccessPoint = null;

	/** Is the system ready for student use ? */
	protected boolean m_initialized = false;
	
	/** Is a Rackham OARD/MP data load in progress? */
	protected boolean m_loading = false;
	
	/** Is an admin step change in progress? */
	protected boolean m_changing_step = false;
	
	/** The group id for the parent graduate school. */
	protected String m_schoolSite = null;
	
	/** The group id for the parent music performance. */
	protected String m_musicPerformanceSite = null;
	
	/** Holds the time as a long that the db was last queried. */
	protected long m_lastQuery = -1;

	/** Holds the School administrative group numbers. */
	protected Hashtable m_schoolGroups = null;
	
	/*******************************************************************************
	* Abstractions, etc.
	*******************************************************************************/
	
	/**
	* Construct a Storage object for Dissertations.
	* @return The new storage object.
	*/
	protected abstract DissertationStorage newDissertationStorage();
	
	/**
	* Construct a Storage object for DissertationSteps.
	* @return The new storage object.
	*/
	protected abstract DissertationStepStorage newDissertationStepStorage();
	
	/**
	* Construct a Storage object for CandidatePaths.
	* @return The new storage object.
	*/
	protected abstract CandidatePathStorage newCandidatePathStorage();
	
	/**
	* Construct a Storage object for StepStatus.
	* @return The new storage object.
	*/
	protected abstract StepStatusStorage newStepStatusStorage();

	/**
	* Construct a Storage object for CandidateInfos.
	* @return The new storage object.
	*/
	protected abstract CandidateInfoStorage newCandidateInfoStorage();
	
	/**
	* Construct a Storage object for BlockGrantGroups.
	* @return The new storage object.
	*/
	protected abstract BlockGrantGroupStorage newBlockGrantGroupStorage();


	/**
	* Access the partial URL that forms the root of resource URLs.
	* @param relative - if true, form within the access path only (i.e. starting with /msg)
	* @return the partial URL that forms the root of resource URLs.
	*/
	protected String getAccessPoint(boolean relative)
	{
		return (relative ? "" : m_serverConfigurationService.getAccessUrl()) + m_relativeAccessPoint;

	}

	/**
	* Access the internal reference which can be used to access the resource from within the system.
	* @param id The dissertation id string.
	* @return The the internal reference which can be used to access the resource from within the system.
	*/
	protected String dissertationReference(String site, String id)
	{
		String retVal = null;
		if(site == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "d" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "d" + Entity.SEPARATOR + getSchoolSite() + Entity.SEPARATOR + site + Entity.SEPARATOR + id;
		return retVal;

	}
	
	/**
	* Access the internal reference which can be used to access the resource from within the system.
	* @param id The step id string.
	* @return The the internal reference which can be used to access the resource from within the system.
	*/
	protected String stepReference(String site, String id)
	{
		String retVal = null;
		if(site == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + getSchoolSite() + Entity.SEPARATOR + site + Entity.SEPARATOR + id;
		return retVal;

	}

	/**
	* Access the internal reference which can be used to access the resource from within the system.
	* @param id The status id string.
	* @return The the internal reference which can be used to access the resource from within the system.
	*/
	protected String statusReference(String site, String id)
	{
		String retVal = null;
		if(site == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "ss" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "ss" + Entity.SEPARATOR + getSchoolSite() + Entity.SEPARATOR + site + Entity.SEPARATOR + id;
		return retVal;

	}

	/**
	* Access the internal reference which can be used to access the resource from within the system.
	* @param id The path id string.
	* @return The the internal reference which can be used to access the resource from within the system.
	*/
	protected String pathReference(String site, String id)
	{
		String retVal = null;
		if(site == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "p" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "p" + Entity.SEPARATOR + getSchoolSite() + Entity.SEPARATOR + site + Entity.SEPARATOR + id;
		return retVal;

	}

	/**
	* Access the internal reference which can be used to access the resource from within the system.
	* @param id The info id string.
	* @return The the internal reference which can be used to access the resource from within the system.
	*/
	protected String infoReference(String site, String id)
	{
		String retVal = null;
		if(site == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "i" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "i" + Entity.SEPARATOR + getSchoolSite() + Entity.SEPARATOR + site + Entity.SEPARATOR + id;
		return retVal;

	}
	
	/**
	* Access the internal reference which can be used to access the resource from within the system.
	* @param id The Block Grant Group id string.
	* @return The internal reference which can be used to access the resource from within the system.
	*/
	protected String blockGrantGroupReference(String site, String id)
	{
		String retVal = null;
		if(site == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "g" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "g" + Entity.SEPARATOR + getSchoolSite() + Entity.SEPARATOR + site + Entity.SEPARATOR + id;
		
		return retVal;

	}
	
	/**
	* Access the dissertation id extracted from an dissertation reference.
	* @param ref The dissertation reference string.
	* @return The the dissertation id extracted from an dissertation reference.
	*/
	protected String dissertationId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	}
	
	/**
	* Access the BlockGrantGroup id extracted from a BlockGrantGroup reference.
	* @param ref The BlockGrantGroup reference string.
	* @return The BlockGrantGroup id extracted from a BlockGrantGroup reference.
	*/
	protected String blockGrantGroupId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	}

	/**
	* Access the step id extracted from an step reference.
	* @param ref The step reference string.
	* @return The the step id extracted from an step reference.
	*/
	protected String stepId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	}

	/**
	* Access the path id extracted from an path reference.
	* @param ref The path reference string.
	* @return The the path id extracted from an path reference.
	*/
	protected String pathId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	}

	/**
	* Access the status id extracted from an status reference.
	* @param ref The status reference string.
	* @return The the status id extracted from an status reference.
	*/
	protected String statusId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	}

	/**
	* Access the info id extracted from an info reference.
	* @param ref The info reference string.
	* @return The the info id extracted from an info reference.
	*/
	protected String infoId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	}


	/**
	* Check security permission.
	* @param lock - The lock id string.
	* @param resource - The resource reference string, or null if no resource is involved.
	* @return true if allowed, false if not
	*/
	protected boolean unlockCheck(String lock, String resource)
	{
		if (!SecurityService.unlock(lock, resource))
		{
			return false;
		}

		return true;

	}

	/**
	* Check security permission.
	* @param lock - The lock id string.
	* @param resource - The resource reference string, or null if no resource is involved.
	* @exception PermissionException Thrown if the user does not have access
	*/
	protected void unlock(String lock, String resource)
		throws PermissionException
	{
		if (!unlockCheck(lock, resource))
		{
			throw new PermissionException(lock, resource);
		}

	}



	/*******************************************************************************
	* Dependencies and their setter methods
	*******************************************************************************/

	/** Dependency: logging service */
	protected Logger m_logger = null;

	/**
	 * Dependency: logging service.
	 * @param service The logging service.
	 */
	public void setLogger(Logger service)
	{
		m_logger = service;
	}

	/** Dependency: MemoryService. */
	protected MemoryService m_memoryService = null;

	/**
	 * Dependency: MemoryService.
	 * @param service The MemoryService.
	 */
	public void setMemoryService(MemoryService service)
	{
		m_memoryService = service;
	}

	/**
	 * Configuration: Set the school group id.
	 * @param id the schoold group id.
	 */
	public void setSchoolSite(String id)
	{
		m_schoolSite = id;
	}
	
	/**
	 * Configuration: Set the music performance site id.
	 * @param id the music performanced site id.
	 */
	public void setMusicPerformanceSite(String id)
	{
		m_musicPerformanceSite = id;
	}

	/** Configuration: cache, or not. */
	protected boolean m_caching = false;

	/**
	 * Configuration: set the locks-in-db
	 * @param path The storage path.
	 */
	public void setCaching(String value)
	{
		m_caching = new Boolean(value).booleanValue();
	}

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		m_serverConfigurationService = service;
	}
	
	/** Dependency: Quartz Job. */
	protected Job m_uploadExtractsJob = null;
	
	/**
	 * Dependency: Quartz Job.
	 * 
	 * @param job
	 *        The Job.
	 */
	public void setUploadExtractsJob(Job job)
	{
		m_uploadExtractsJob = job;
	}
	
	/** Dependency: Quartz Job. */
	protected Job m_stepChangeJob = null;
	
	/**
	 * Dependency: Quartz Job.
	 * 
	 * @param job
	 *        The Job.
	 */
	public void setStepChangeJob(Job job)
	{
		m_stepChangeJob = job;
	}
	
	/** Dependency: EntityManager. */
	protected EntityManager m_entityManager = null;

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		m_entityManager = service;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			
			//TODO is this set?
			m_relativeAccessPoint = REFERENCE_ROOT;
			
			setSchoolSite("rackham");
			setMusicPerformanceSite("diss089");

			// construct storage helpers and read
			m_groupStorage = newBlockGrantGroupStorage();
			m_groupStorage.open();
			m_dissertationStorage = newDissertationStorage();
			m_dissertationStorage.open();
			m_stepStorage = newDissertationStepStorage();
			m_stepStorage.open();
			m_pathStorage = newCandidatePathStorage();
			m_pathStorage.open();
			m_statusStorage = newStepStatusStorage();
			m_statusStorage.open();
			m_infoStorage = newCandidateInfoStorage();
			m_infoStorage.open();

			if (m_caching)
			{
				m_groupCache = m_memoryService.newCache(new BlockGrantGroupCacheRefresher(), blockGrantGroupReference(null, ""));
				m_dissertationCache = m_memoryService.newCache(new DissertationCacheRefresher(), dissertationReference(null, ""));
				m_stepCache = m_memoryService.newCache(new DissertationStepCacheRefresher(), stepReference(null, ""));
				m_pathCache = m_memoryService.newCache(new CandidatePathCacheRefresher(), pathReference(null, ""));
				m_statusCache = m_memoryService.newCache(new StepStatusCacheRefresher(), statusReference(null, ""));
				m_infoCache = m_memoryService.newCache(new CandidateInfoCacheRefresher(), infoReference(null, ""));
				
				//preload caches
				List infos = getCandidateInfos();
				List paths = getCandidatePaths();
				List dissertations = getDissertations();
				List steps = getDissertationSteps();
				List groups = getBlockGrantGroups();	
			}
		
			// lookup table of field/program to block grant group used in loading student data
			m_schoolGroups = new Hashtable();
			
			// test identities
			
			//TODO check that application has data in key tables
			m_initialized = true;
			
			//check that group code storage has been initialized
			List storedCodes = m_groupStorage.getAll();
			boolean initialized = storedCodes.isEmpty() ? false : true;
			if(initialized)
			{
				List schoolGroups = (List)getBlockGrantGroups();
				for(int i=0; i<schoolGroups.size(); i++)
				{
					BlockGrantGroup group = (BlockGrantGroup)schoolGroups.get(i);
					Hashtable fields = (Hashtable)group.getFieldsOfStudy();
					Enumeration codes = fields.keys();
					while(codes.hasMoreElements())
					{
						String field = (String)codes.nextElement();
						m_schoolGroups.put(field, group.getCode());
					}
				}
			}
			else
			{
				//initialize the group code storage
				//TODO permission exception during init
				//m_schoolGroups = initCodes();
			}
			
			// register as an entity producer
			m_entityManager.registerEntityProducer(this, REFERENCE_ROOT);

			// register functions
			FunctionManager.registerFunction(SECURE_ADD_DISSERTATION);
			FunctionManager.registerFunction(SECURE_ACCESS_DISSERTATION);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION);
			FunctionManager.registerFunction(SECURE_REMOVE_DISSERTATION);
			FunctionManager.registerFunction(SECURE_ADD_DISSERTATION_CANDIDATEPATH);
			FunctionManager.registerFunction(SECURE_ACCESS_DISSERTATION_CANDIDATEPATH);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION_CANDIDATEPATH);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION_CANDIDATEPATH_COMM);
			FunctionManager.registerFunction(SECURE_REMOVE_DISSERTATION_CANDIDATEPATH);
			FunctionManager.registerFunction(SECURE_ADD_DISSERTATION_STEP);
			FunctionManager.registerFunction(SECURE_ACCESS_DISSERTATION_STEP);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION_STEP);
			FunctionManager.registerFunction(SECURE_REMOVE_DISSERTATION_STEP);
			FunctionManager.registerFunction(SECURE_ADD_DISSERTATION_STEPSTATUS);
			FunctionManager.registerFunction(SECURE_ACCESS_DISSERTATION_STEPSTATUS);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION_STEPSTATUS);
			FunctionManager.registerFunction(SECURE_REMOVE_DISSERTATION_STEPSTATUS);
			FunctionManager.registerFunction(SECURE_ADD_DISSERTATION_CANDIDATEINFO);
			FunctionManager.registerFunction(SECURE_ACCESS_DISSERTATION_CANDIDATEINFO);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION_CANDIDATEINFO);
			FunctionManager.registerFunction(SECURE_REMOVE_DISSERTATION_CANDIDATEINFO);
			FunctionManager.registerFunction(SECURE_ACCESS_DISSERTATION_GROUP);
			FunctionManager.registerFunction(SECURE_ADD_DISSERTATION_GROUP);
			FunctionManager.registerFunction(SECURE_REMOVE_DISSERTATION_GROUP);
			FunctionManager.registerFunction(SECURE_UPDATE_DISSERTATION_GROUP);
		}
		catch (Throwable t)
		{
			m_logger.warn(this +".init(): ", t);
		}

	} // init
	
	/**
	* Returns to uninitialized state.
	*/
	public void destroy()
	{
		if (m_caching)
		{
			m_groupCache.clear();
			m_dissertationCache.clear();
			m_dissertationCache = null;
			m_stepCache.destroy();
			m_stepCache = null;
			m_pathCache.destroy();
			m_pathCache = null;
			m_statusCache.destroy();
			m_statusCache = null;
			m_infoCache.destroy();
			m_infoCache = null;
		}

		m_groupStorage.close();
		m_groupStorage = null;
		m_dissertationStorage.close();
		m_dissertationStorage = null;
		m_stepStorage.close();
		m_stepStorage = null;
		m_pathStorage.close();
		m_pathStorage = null;
		m_statusStorage.close();
		m_statusStorage = null;
		m_infoStorage.close();
		m_infoStorage = null;
		
	}// destroy

	/*******************************************************************************
	* DissertationServiceimplementation
	*******************************************************************************/
	
	/**
	* Get properties of a Rackham data extract file.
	* @param content - The content for which properties are requested.
	* @return Hastable of property name(s) and value(s).
	*/
	public Hashtable getDataFileProperties(byte[] content)
	{
		Hashtable retVal = new Hashtable();
		try
		{
			//get number of lines of content property
			int lns = 0;
			char[] chars = ((String)new String(content)).toCharArray();
			for(int i = 0; i < chars.length; i++)
			{
				if(chars[i]=='\n')
				{
					lns = lns + 1;
				}
			}
			Integer lines = new Integer(lns);
			retVal.put(DissertationService.DATAFILE_LINES, lines);
		}
		catch (Exception e)
		{
			m_logger.warn(this + " .getDataFileProperties: " + e.getMessage());
		}
		return retVal;
		
	} //getDataFileProperties
	

	/**
	* See if Rackham extract data loading is in progress.
	* @return boolean - true if in progress, false otherwise.
	*/
	public boolean isLoading()
	{
		CandidateInfoEdit lock = null;
		String lock_ref = DissertationService.IS_LOADING_LOCK_REFERENCE;
		
		//initialize
		try
		{
			m_loading = true;
			
			//take out a lock on a well-known object
			lock = editCandidateInfo(lock_ref);
			
			//CandidateInfo indicates operation in progress
			if(lock != null && lock.isActiveEdit())
				commitEdit(lock);
		}
		catch(IdUnusedException e)
		{
			//no lock, not loading
			m_loading = false;
		}
		catch(Exception e)
		{
			if(lock != null && lock.isActiveEdit())
			{
				try
				{
					removeCandidateInfo(lock);
				}
				catch(Exception ee)
				{
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".isLoading removeCandidateInfo(lock) " + ee);
				}
			}
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".isLoading Exception " + e);
		}
		return m_loading;
		
	}//isLoading
	
	/**
	* See if admin step change is in progress.
	* @return boolean - true if in progress, false otherwise.
	*/
	public boolean isChangingStep()
	{
		CandidateInfoEdit lock = null;
		String lock_ref = DissertationService.IS_CHANGING_STEP_LOCK_REFERENCE;
		
		//initialize
		try
		{
			m_changing_step = true;
			
			//take out a lock on a well-known object
			lock = editCandidateInfo(lock_ref);
			
			//CandidateInfo indicates operation in progress
			if(lock != null && lock.isActiveEdit())
				commitEdit(lock);
		}
		catch(IdUnusedException e)
		{
			//no lock, so not changing step
			m_changing_step = false;

		}
		catch(Exception e)
		{
			if(lock != null && lock.isActiveEdit())
			{
				try
				{
					removeCandidateInfo(lock);
				}
				catch(Exception ee)
				{
					if(m_logger.isWarnEnabled())
						m_logger.warn(this + ".isChangingStep removeCandidateInfo(lock) " + ee);
				}
			}
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".isChangingStep Exception " + e);
		}
		return m_changing_step;
		
	}//isChangingStep
	
	/** 
	* Access the Rackham program id for Rackham field of study id.
	* @return The program id.
	*/
	public String getProgram(String fos)
	{
		String retVal = "";
		if(fos != null)
			retVal = (String)m_schoolGroups.get(fos);
		return retVal;
	}
	
	/** 
	* Access whether the system is initialized - i.e. whether any CandidateInfo objects exist.
	* @return True if any CandidateInfo objects exist, false if not.
	*/
	public boolean hasCandidateInfos()
	{	
		boolean retVal = false;
		if (m_caching)
		{
			List keys = m_infoCache.getKeys();
			if(keys.size() > 0)
				retVal = true;
			else
			{
				// CHECK THE DB
				retVal = !m_infoStorage.isEmpty();
			}
		}
		else
		{
			// CHECK THE DB
			retVal = !m_infoStorage.isEmpty();
		}
		return retVal;
	}

	/** 
	* Access whether the db-integration system is initialized.
	* @return True if it's initialized, false if not.
	*/
	public boolean isInitialized()
	{
		return m_initialized;
	}

	/** 
	* Access the site for a Rackham program.
	* @param program The Rackham program id.
	* @return The CHEF site id for that program.
	*/
	protected String getSiteForProgram(String program)
	{
		String retVal = "diss" + program;
		return retVal;
		
	}
	
	/** 
	* Access the list of candidates with the parent department site, optionally filtered by dissertation step type.
	* @param site The site id.
	* @param type The dissertation step type.
	* @return The list of users with the parent site.
	*/
	public User[] getAllUsersForSite(String site, String type)
	{
		User[] retVal = null;
		
		CandidatePath aPath = null;
		List userIds = new Vector();
		List paths = getCandidatePaths();
		for(int x = 0; x < paths.size(); x++)
		{
			aPath = (CandidatePath)paths.get(x);
			if(site.equals(getSchoolSite()))
			{
				if(type == null)
					userIds.add(aPath.getCandidate());
				else if(aPath.getType().equals(type))
					userIds.add(aPath.getCandidate());
			}
			else
			{
				//GET ALL USERS FOR SITE : PATH PARENT SITE
				if(site.equals(getParentSiteForUser(aPath.getCandidate())))
				{
					//GET ALL USERS FOR SITE : SITE MATCH
					userIds.add(aPath.getCandidate());
					//GET ALL USERS FOR SITE : A USER ID
				}
			}
		}
			
		retVal = new User[userIds.size()];
		for(int x = 0; x < userIds.size(); x++)
		{
			try
			{
				retVal[x] = UserDirectoryService.getUser((String) userIds.get(x));
			}
			catch(IdUnusedException e)
			{
				m_logger.warn(this + ".getAllUsersForSite " + site + " User not found " + e);
			}
		}
		return retVal;
		
	} //getAllUsersForSite
	
	/** 
	* Access whether this user is a candidate.
	* @param userId The user's CHEF id.
	* @return True if a CandidateInfo exists for this user, false if not.
	*/
	public boolean isCandidate(String userId)
	{
		boolean retVal = false;
		if(userId != null)
		{
			try
			{
				retVal = m_infoStorage.checkCandidate(userId);
			}
			catch(Exception e){}
		}
		return retVal;
		
	}
	
	/** 
	* Access whether this user is a Music Performance candidate.
	* @param userId The user's CHEF id.
	* @return True if CandidateInfo getProgram() is Music Performance for this user, false if not.
	*/
	public boolean isMusicPerformanceCandidate(String userId)
	{
		boolean retVal = false;
		if(userId != null)
		{
			try
			{
				retVal = m_infoStorage.checkMusic(userId);
			}
			catch(Exception e){}
		}
		return retVal;
		
	}
	
	/**
	* Access the site for the user's Rackham department.
	* @param userId The user's CHEF id.
	* @return The site id of the parent site.
	*/
	public String getParentSiteForUser(String userId)
	{
		String retVal = "";
		if(userId != null)
		{
			try
			{
				retVal = m_infoStorage.getParent(userId);
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".getParentSiteForUser " + userId + " Exception " + e);
			}
		}
		return retVal;
		
	}

	/**
	* Access the University id for the user.
	* @param userId The user's CHEF id.
	* @return The university id for the user, or "" if none found.
	*/
	public String getEmplidForUser(String userId)
	{
		String retVal = "";
		if(userId != null)
		{
			try
			{
				retVal = m_infoStorage.getEmplid(userId);
			}
			catch(Exception e){}
		}
		return retVal;

	}
	
	/** 
	* Creates and adds a new BlockGrantGroup to the service.
	* @param id - The BlockGrantGroup id.
	* @return BlockGrantGroupEdit The new BlockGrantGroup object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public BlockGrantGroupEdit addBlockGrantGroup(String site)
		throws PermissionException
	{
		String blockGrantGroupId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			blockGrantGroupId = IdService.getUniqueId();
			try
			{
				Validator.checkResourceId(blockGrantGroupId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_stepStorage.check(blockGrantGroupId))
				badId = true;
			
		}while(badId);
		
		String key = blockGrantGroupReference(site, blockGrantGroupId);
			
		unlock(SECURE_ADD_DISSERTATION_GROUP, key);
		
		BlockGrantGroupEdit blockGrantGroupEdit = m_groupStorage.put(blockGrantGroupId, site);
		
		((BaseBlockGrantGroupEdit) blockGrantGroupEdit).setEvent(SECURE_ADD_DISSERTATION_GROUP);
		
		return blockGrantGroupEdit;
	}
	
	/**
	* Get a locked BlockGrantGroup object for editing.  Must commitEdit() to make official, or cancelEdit() when done!
	* @param blockGrantGroupReference The Block Grant Group reference string.
	* @return A BlockGrantGroupEdit object for editing.
	* @exception IdUnusedException if not found, or if not a BlockGrantGroupEdit object
	* @exception PermissionException if the current user does not have permission to edit this BlockGrantGroup.
	* @exception InUseException if the BlockGrantGroup is being edited by another user.
	*/
	public BlockGrantGroupEdit editBlockGrantGroup(String blockGrantGroupReference)
		throws IdUnusedException, PermissionException, InUseException
	{
		unlock(SECURE_UPDATE_DISSERTATION_GROUP, blockGrantGroupReference);
		
		String blockGrantGroupId = blockGrantGroupId(blockGrantGroupReference);

		// check for existance
		if (m_caching)
		{
			if ((m_groupCache.get(blockGrantGroupReference) == null) && (!m_groupStorage.check(blockGrantGroupId)))
			{
				throw new IdUnusedException(blockGrantGroupId);
			}
		}
		else
		{
			if (!m_groupStorage.check(blockGrantGroupId))
			{
				throw new IdUnusedException(blockGrantGroupId);
			}
		}

		// ignore the cache - get the Block Grant Group with a lock from the info store
		BlockGrantGroupEdit blockGrantGroupEdit = m_groupStorage.edit(blockGrantGroupId);
		
		if (blockGrantGroupEdit == null) throw new InUseException(blockGrantGroupId);

		((BaseBlockGrantGroupEdit) blockGrantGroupEdit).setEvent(SECURE_UPDATE_DISSERTATION_GROUP);

		return blockGrantGroupEdit;

	}   // editBlockGrantGroup
	
	/**
	* Cancel the changes made to a BlockGrantGroupEdit object, and release the lock.
	* @param blockGrantGroup The BlockGrantGroupEdit object to commit.
	*/
	public void cancelEdit(BlockGrantGroupEdit blockGrantGroup)
	{
		// check for closed edit
		if (!blockGrantGroup.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".cancelEdit(): closed BlockGrantGroupEdit", e); }
			return;
		}

		// release the edit lock
		m_groupStorage.cancel(blockGrantGroup);

		// close the edit object
		((BaseBlockGrantGroupEdit) blockGrantGroup).closeEdit();

	}	// cancelEdit(BlockGrantGroupEdit)
	
	/**
	* Commit the changes made to a BlockGrantGroupEdit object, and release the lock.
	* @param blockGrantGroup The BlockGrantGroupEdit object to commit.
	*/
	public void commitEdit(BlockGrantGroupEdit blockGrantGroup)
	{
		// check for closed edit
		if (!blockGrantGroup.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".commitEdit(): closed BlockGrantGroupEdit", e); }
			return;
		}

		// update the properties
		addLiveUpdateProperties(blockGrantGroup.getPropertiesEdit());

		String key;
		String bgg;
		Enumeration keys;
		Vector removes = new Vector();
		
		//update the m_schoolGroups mapping
		keys = m_schoolGroups.keys();
		while(keys.hasMoreElements())
		{
			key = (String)keys.nextElement();
			bgg = (String)m_schoolGroups.get(key);
			if(bgg.equals(blockGrantGroup.getCode()))
				removes.add(key);
		}
		for(int i = 0; i < removes.size(); i++)
		{
			key = (String)removes.get(i);
			m_schoolGroups.remove(key);
		}
		Hashtable fields = blockGrantGroup.getFieldsOfStudy();
		keys = fields.keys();
		while (keys.hasMoreElements())
		{
			key = (String)keys.nextElement();
			m_schoolGroups.put(key, blockGrantGroup.getCode());
		}
		
		// complete the edit
		m_groupStorage.commit(blockGrantGroup);
		
		// track it
		EventTrackingService.post(
			EventTrackingService.newEvent(((BaseBlockGrantGroupEdit) blockGrantGroup).getEvent(), blockGrantGroup.getReference(), true));

		// close the edit object
		((BaseBlockGrantGroupEdit) blockGrantGroup).closeEdit();

	}	// commitEdit(BlockGrantGroupEdit)
	
	
	/**
	* Access all BlockGrantGroup objects.
	* @return A list of BlockGrantGroup objects.
	*/
	public List getBlockGrantGroups()
	{
		List blockGrantGroups = new Vector();

		// if we have disabled the cache, don't use if
		if ((!m_caching) || (m_groupCache.disabled()))
		{
			blockGrantGroups = m_groupStorage.getAll();
		}

		else
		{
			
			// if the cache is complete, use it
			if (m_groupCache.isComplete())
			{
				blockGrantGroups = m_groupCache.getAll();
			}
	
			// otherwise get all the Block Grant Groups from storage
			else
			{
				// Note: while we are getting from storage, storage might change.  These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_groupCache)
				{
					// if we were waiting and it's now complete...
					if (m_groupCache.isComplete())
					{
						blockGrantGroups = m_groupCache.getAll();
						return blockGrantGroups;
					}
	
					// save up any events to the cache until we get past this load
					m_groupCache.holdEvents();
					blockGrantGroups = m_groupStorage.getAll();
	
					// update the cache, and mark it complete
					for (int i = 0; i < blockGrantGroups.size(); i++)
					{
						BlockGrantGroup blockGrantGroup = (BlockGrantGroup) blockGrantGroups.get(i);
						m_groupCache.put(blockGrantGroup.getReference(), blockGrantGroup);
					}
	
					m_groupCache.setComplete();
	
					// now we are complete, process any cached events
					m_groupCache.processEvents();
				}
			}
		}
		return blockGrantGroups;

	}   // getBlockGrantGroups
	
	/** 
	* Removes this BlockGrantGroup and all references to it.
	* @param blockGrantGroup - The BlockGrantGroup to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeBlockGrantGroup(BlockGrantGroupEdit blockGrantGroupEdit)
		throws PermissionException
	{
		if(blockGrantGroupEdit != null)
		{
			if(!blockGrantGroupEdit.isActiveEdit())
			{
				try { throw new Exception(); }
				catch (Exception e) { m_logger.warn(this + ".removeBlockGrantGroup(): closed BlockGrantGroupEdit", e); }
				return;
			}
			
			unlock(SECURE_REMOVE_DISSERTATION_GROUP, blockGrantGroupEdit.getReference());
			
			//update the local FOS/BCCG map
			Hashtable fields = blockGrantGroupEdit.getFieldsOfStudy();
			Enumeration keys = fields.keys();
			while (keys.hasMoreElements())
			{
				String key = (String)keys.nextElement();
				m_schoolGroups.remove(key);
			}
			
			m_groupStorage.remove(blockGrantGroupEdit);
			
			EventTrackingService.post(EventTrackingService.newEvent(SECURE_REMOVE_DISSERTATION_GROUP, blockGrantGroupEdit.getReference(), true));

			((BaseBlockGrantGroupEdit)blockGrantGroupEdit).closeEdit();
		}
		
	}//removeBlockGrantGroup
	
	/**
	* Access the BlockGrantGroup for the specified Field of Study.
	* @param field - The field for which BlockGrantGroup is to be returned.
	* @return The BlockGrantGroup corresponding to the field, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public BlockGrantGroup getBlockGrantGroupForFieldOfStudy(String field)
		throws PermissionException
	{
		BlockGrantGroup retVal = null;
		if(field != null)
		{
			BlockGrantGroup tempGroup = null;
			List allGroups = getBlockGrantGroups();
			Hashtable groupFields = null;
			for(int x = 0; x < allGroups.size(); x++)
			{
				try
				{
					tempGroup = (BlockGrantGroup)allGroups.get(x);					
					groupFields = tempGroup.getFieldsOfStudy();
					if(groupFields.containsKey(field))
					{
						retVal = (BlockGrantGroup) m_groupStorage.get(tempGroup.getId());
						return retVal;
					}
				}
				catch(Exception e)
				{
					if(m_logger.isWarnEnabled())
					{
						m_logger.warn("DISSERTATION : BASE SERVICE : GET BLOCK GRANT GROUP EDIT FOR FIELD OF STUDY : EXCEPTION " + e);
					}
				}
			}
		}
		return retVal;

	}	

	
	/** 
	* Creates and adds a new Dissertation to the service.
	* @param site - The site for which permissions are being checked.
	* @return The new Dissertation object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public DissertationEdit addDissertation(String site)
		throws PermissionException
	{
		String dissertationId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			dissertationId = IdService.getUniqueId();
			try
			{
				Validator.checkResourceId(dissertationId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_dissertationStorage.check(dissertationId))
				badId = true;
			
		}while(badId);
		
		String key = dissertationReference(site, dissertationId);
		
		unlock(SECURE_ADD_DISSERTATION, key);
		
		//storage
		DissertationEdit dissertation = m_dissertationStorage.put(dissertationId, site);
		
		dissertation.setSite(site);
		
		((BaseDissertationEdit) dissertation).setEvent(SECURE_ADD_DISSERTATION);
		
		return dissertation;
	}

	/**
	* Add a new dissertation to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the dissertation.
	* @return A locked DissertationEdit object (reserving the id).
	* @exception IdInvalidException if the dissertation id is invalid.
	* @exception IdUsedException if the dissertation id is already used.
	* @exception PermissionException if the current user does not have permission to add a dissertation.
	*/
	public DissertationEdit mergeDissertation(Element el)
		throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		Dissertation dissertationFromXml = new BaseDissertation(el);

		// check for a valid dissertation name
		Validator.checkResourceId(dissertationFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_DISSERTATION, dissertationFromXml.getReference());

		// reserve a dissertation with this id from the info store - if it's in use, this will return null
		DissertationEdit dissertation = m_dissertationStorage.put(dissertationFromXml.getId(), dissertationFromXml.getSite());
		if (dissertation == null)
		{
			throw new IdUsedException(dissertationFromXml.getId());
		}

		// transfer from the XML read dissertation object to the DissertationEdit
		((BaseDissertationEdit) dissertation).set(dissertationFromXml);

		((BaseDissertationEdit) dissertation).setEvent(SECURE_ADD_DISSERTATION);

		return dissertation;

	}
	
	/**
	* Access the Dissertation with the specified reference.
	* @param dissertationReference - The reference of the Dissertation.
	* @return The Dissertation corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this id.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public Dissertation getDissertation(String dissertationReference)
		throws IdUnusedException, PermissionException
	{
		Dissertation dissertation = null;
		
		String dissertationId = dissertationId(dissertationReference);

		if (m_caching)
		{
			if(m_dissertationCache.containsKey(dissertationReference))
				dissertation = (Dissertation)m_dissertationCache.get(dissertationReference);
			else
			{
				dissertation = m_dissertationStorage.get(dissertationId);
				m_dissertationCache.put(dissertationReference, dissertation);
			}
		}
		else
		{
			dissertation = m_dissertationStorage.get(dissertationId);
		}
		
		if(dissertation == null) throw new IdUnusedException(dissertationId);
		
		unlock(SECURE_ACCESS_DISSERTATION, dissertationReference);
		
		// EventTrackingService.post(EventTrackingService.newEvent(SECURE_ACCESS_DISSERTATION, dissertation.getReference(), false));
		
		return dissertation;
	}
	
	
	/**
	* Access all dissertation objects - known to us (not from external providers).
	* @return A list of dissertation objects.
	*/
	public List getDissertations()
	{
		List dissertations = new Vector();

		// if we have disabled the cache, don't use if
		if ((!m_caching) || (m_dissertationCache.disabled()))
		{
			dissertations = m_dissertationStorage.getAll();
		}

		else
		{
			// if the cache is complete, use it
			if (m_dissertationCache.isComplete())
			{
				dissertations = m_dissertationCache.getAll();
			}
	
			// otherwise get all the dissertations from storage
			else
			{
				// Note: while we are getting from storage, storage might change.  These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_dissertationCache)
				{
					// if we were waiting and it's now complete...
					if (m_dissertationCache.isComplete())
					{
						dissertations = m_dissertationCache.getAll();
						return dissertations;
					}
	
					// save up any events to the cache until we get past this load
					m_dissertationCache.holdEvents();
	
					dissertations = m_dissertationStorage.getAll();
	
					// update the cache, and mark it complete
					for (int i = 0; i < dissertations.size(); i++)
					{
						Dissertation dissertation = (Dissertation) dissertations.get(i);
						m_dissertationCache.put(dissertation.getReference(), dissertation);
					}
	
					m_dissertationCache.setComplete();
	
					// now we are complete, process any cached events
					m_dissertationCache.processEvents();
				}
			}
		}

		return dissertations;

	}   // getDissertations
	
	/**
	* Access the Dissertations of this type (e.g, “Dissertation Steps”, “Dissertation Steps: Music Performance”)
	* @param type - The Dissertation type.
	* @return The Dissertations of this type, or null if none exist.
	*/
	public List getDissertationsOfType(String type)
	{
		List dissertations = new Vector();
		if(type != null)
		{
			dissertations = m_dissertationStorage.getAllOfType(type);
		}
		return dissertations;
		
	}
	
	
	/**
	* Get a locked dissertation object for editing.  Must commitEdit() to make official, or cancelEdit() when done!
	* @param dissertationReference The dissertation reference string.
	* @return An DissertationEdit object for editing.
	* @exception IdUnusedException if not found, or if not an DissertationEdit object
	* @exception PermissionException if the current user does not have permission to edit this dissertation.
	* @exception InUseException if the dissertation is being edited by another user.
	*/
	public DissertationEdit editDissertation(String dissertationReference)
		throws IdUnusedException, PermissionException, InUseException
	{
		unlock(SECURE_UPDATE_DISSERTATION, dissertationReference);
		
		String dissertationId = dissertationId(dissertationReference);

		// check for existance
		if (m_caching)
		{
			if ((m_dissertationCache.get(dissertationReference) == null) && (!m_dissertationStorage.check(dissertationId)))
			{
				throw new IdUnusedException(dissertationId);
			}
		}
		else
		{
			if (!m_dissertationStorage.check(dissertationId))
			{
				throw new IdUnusedException(dissertationId);
			}
		}

		// ignore the cache - get the dissertation with a lock from the info store
		DissertationEdit dissertation = m_dissertationStorage.edit(dissertationId);
		
		if (dissertation == null) throw new InUseException(dissertationId);

		((BaseDissertationEdit) dissertation).setEvent(SECURE_UPDATE_DISSERTATION);

		return dissertation;

	}
	
	
	/**
	* Commit the changes made to an DissertationEdit object, and release the lock.
	* @param dissertation The DissertationEdit object to commit.
	*/
	public void commitEdit(DissertationEdit dissertation)
	{
		// check for closed edit
		if (!dissertation.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".commitEdit(): closed DissertationEdit", e); }
			return;
		}

		// update the properties
		addLiveUpdateProperties(dissertation.getPropertiesEdit());

		// complete the edit
		m_dissertationStorage.commit(dissertation);

		// track it
		EventTrackingService.post(
			EventTrackingService.newEvent(((BaseDissertationEdit) dissertation).getEvent(), dissertation.getReference(), true));

		// close the edit object
		((BaseDissertationEdit) dissertation).closeEdit();

	}

	
	/**
	* Cancel the changes made to a DissertationEdit object, and release the lock.
	* @param dissertation The DissertationEdit object to commit.
	*/
	public void cancelEdit(DissertationEdit dissertation)
	{
		// check for closed edit
		if (!dissertation.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".cancelEdit(): closed DissertationEdit", e); }
			return;
		}

		// release the edit lock
		m_dissertationStorage.cancel(dissertation);

		// close the edit object
		((BaseDissertationEdit) dissertation).closeEdit();

	}
	
	/** 
	* Removes this Dissertation and all references to it.
	* @param dissertation - The Dissertation to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeDissertation(DissertationEdit dissertation)
		throws PermissionException
	{
		if(dissertation != null)
		{
			if(!dissertation.isActiveEdit())
			{
				try { throw new Exception(); }
				catch (Exception e) { m_logger.warn(this + ".removeDissertation(): closed DissertationEdit", e); }
				return;
			}
			
			unlock(SECURE_REMOVE_DISSERTATION, dissertation.getReference());
			
			m_dissertationStorage.remove(dissertation);
			
			EventTrackingService.post(EventTrackingService.newEvent(SECURE_REMOVE_DISSERTATION, dissertation.getReference(), true));
			
			((BaseDissertationEdit)dissertation).closeEdit();
		}
	}

	/** 
	* Creates and adds a new DissertationStep to the service.
	* @param site - The site for which permissions are being checked.
	* @return DissertationStepEdit The new DissertationStep object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public DissertationStepEdit addDissertationStep(String site)
		throws PermissionException
	{
		String stepId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			stepId = IdService.getUniqueId();
			try
			{
				Validator.checkResourceId(stepId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_stepStorage.check(stepId))
				badId = true;
			
		}while(badId);
		
		String key = stepReference(site, stepId);
		
		unlock(SECURE_ADD_DISSERTATION_STEP, key);
		
		DissertationStepEdit step = m_stepStorage.put(stepId, site);

		step.setSite(site);
		
		((BaseDissertationStepEdit) step).setEvent(SECURE_ADD_DISSERTATION_STEP);
		
		return step;
	}
	
	/** 
	* Creates and adds a new DissertationStep to the service.
	* @param site - The site for which permissions are being checked.
	* @param type - The pedagogical type inhent in the steps.
	* @return DissertationStepEdit The new DissertationStep object.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public DissertationStepEdit addDissertationStep(String site, String type)
		throws PermissionException
	{
		String stepId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			stepId = IdService.getUniqueId();
			try
			{
				Validator.checkResourceId(stepId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_stepStorage.check(stepId))
				badId = true;
			
		}while(badId);
		
		String key = stepReference(site, stepId);
		
		unlock(SECURE_ADD_DISSERTATION_STEP, key);
		
		DissertationStepEdit step = m_stepStorage.put(stepId, site);

		step.setSite(site);
		
		((BaseDissertationStepEdit) step).setEvent(SECURE_ADD_DISSERTATION_STEP);
		
		return step;
	}
	

	/**
	* Add a new step to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the step.
	* @return A locked DissertationStepEdit object (reserving the id).
	* @exception IdInvalidException if the step id is invalid.
	* @exception IdUsedException if the step id is already used.
	* @exception PermissionException if the current user does not have permission to add a DissertationStep.
	*/
	public DissertationStepEdit mergeDissertationStep(Element el)
		throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		DissertationStep stepFromXml = new BaseDissertationStep(el);

		// check for a valid step name
		Validator.checkResourceId(stepFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_DISSERTATION_STEP, stepFromXml.getReference());

		// reserve a step with this id from the info store - if it's in use, this will return null
		DissertationStepEdit step = m_stepStorage.put(stepFromXml.getId(), stepFromXml.getSite());
		if (step == null)
		{
			throw new IdUsedException(stepFromXml.getId());
		}

		// transfer from the XML read step object to the StepEdit
		((BaseDissertationStepEdit) step).set(stepFromXml);

		((BaseDissertationStepEdit) step).setEvent(SECURE_ADD_DISSERTATION_STEP);

		return step;
		
	}
	
	/**
	* Access the DissertationStep with the specified id.
	* @param stepReference - The reference of the DissertationStep.
	* @return The DissertationStep corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public DissertationStep getDissertationStep(String stepReference)
		throws IdUnusedException, PermissionException
	{
		DissertationStep step = null;

		String stepId = stepId(stepReference);

		if (m_caching)
		{
			if(m_stepCache.containsKey(stepReference))
				step = (DissertationStep)m_stepCache.get(stepReference);
			else
			{
				step = m_stepStorage.get(stepId);
				m_stepCache.put(stepReference, step);
			}
		}
		else
		{
			step = m_stepStorage.get(stepId);
		}
		
		if(step == null) throw new IdUnusedException(stepId);

		unlock(SECURE_ACCESS_DISSERTATION_STEP, stepReference);
		
		return step;

	}


	/**
	* Access all DissertationStep objects - known to us (not from external providers).
	* @return A list of DissertationStep objects.
	*/
	public List getDissertationSteps()
	{
		List steps = new Vector();

		// if we have disabled the cache, don't use if
		if ((!m_caching) || m_stepCache.disabled())
		{
			steps = m_stepStorage.getAll();
		}

		else
		{
			// if the cache is complete, use it
			if (m_stepCache.isComplete())
			{
				steps = m_stepCache.getAll();
			}
	
			// otherwise get all the steps from storage
			else
			{
				// Note: while we are getting from storage, storage might change.  These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_stepCache)
				{
					// if we were waiting and it's now complete...
					if (m_stepCache.isComplete())
					{
						steps = m_stepCache.getAll();
						return steps;
					}
	
					// save up any events to the cache until we get past this load
					m_stepCache.holdEvents();
					steps = m_stepStorage.getAll();
	
					// update the cache, and mark it complete
					for (int i = 0; i < steps.size(); i++)
					{
						DissertationStep step = (DissertationStep) steps.get(i);
						m_stepCache.put(step.getReference(), step);
					}
					m_stepCache.setComplete();
					
					// now we are complete, process any cached events
					m_stepCache.processEvents();
				}
			}
		}

		return steps;

	}// getDissertationSteps
	
	
	/**
	* Get a locked step object for editing.  Must commitEdit() to make official, or cancelEdit() when done!
	* @param stepReference The step reference string.
	* @return A DissertationStepEdit object for editing.
	* @exception IdUnusedException if not found, or if not a DissertationStepEdit object
	* @exception PermissionException if the current user does not have permission to edit this step.
	* @exception InUseException if the step is being edited by another user.
	*/
	public DissertationStepEdit editDissertationStep(String stepReference)
		throws IdUnusedException, PermissionException, InUseException
	{
		// check security (throws if not permitted)
		unlock(SECURE_UPDATE_DISSERTATION_STEP, stepReference);

		String stepId = stepId(stepReference);
		
		// check for existance
		if (m_caching)
		{
			if ((m_stepCache.get(stepReference) == null) && (!m_stepStorage.check(stepId)))
			{
				throw new IdUnusedException(stepId);
			}
		}
		else
		{
			if (!m_stepStorage.check(stepId))
			{
				throw new IdUnusedException(stepId);
			}
		}

		// ignore the cache - get the step with a lock from the info store
		DissertationStepEdit step = m_stepStorage.edit(stepId);
		
		if (step == null) throw new InUseException(stepId);

		((BaseDissertationStepEdit) step).setEvent(SECURE_UPDATE_DISSERTATION_STEP);

		return step;

	}

	
	/**
	* Commit the changes made to a DissertationStepEdit object, and release the lock.
	* @param step The DissertationStepEdit object to commit.
	*/
	public void commitEdit(DissertationStepEdit step)
	{
		// check for closed edit
		if (!step.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".commitEdit(): closed DissertationStepEdit", e); }
			return;
		}

		// update the properties
		addLiveUpdateProperties(step.getPropertiesEdit());
		
		// complete the edit
		m_stepStorage.commit(step);
		
		// track it
		EventTrackingService.post(
			EventTrackingService.newEvent(((BaseDissertationStepEdit) step).getEvent(), step.getReference(), true));
		
		// close the edit object
		((BaseDissertationStepEdit) step).closeEdit();

	}

	
	/**
	* Cancel the changes made to a DissertationStepEdit object, and release the lock.
	* @param step The DissertationStepEdit object to commit.
	*/
	public void cancelEdit(DissertationStepEdit step)
	{
		// check for closed edit
		if (!step.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".cancelEdit(): closed DissertationStepEdit", e); }
			return;
		}

		// release the edit lock
		m_stepStorage.cancel(step);

		// close the edit object
		((BaseDissertationStepEdit) step).closeEdit();

	}
	
	/** 
	* Removes a DissertationStep
	* @param step - the DissertationStep to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeDissertationStep(DissertationStepEdit step)
		throws PermissionException
	{
		if(step != null)
		{
			if(!step.isActiveEdit())
			{
				try { throw new Exception(); }
				catch (Exception e) { m_logger.warn(this + ".removeDissertationStep(): closed DissertationStepEdit", e); }
				return;
			}

			unlock(SECURE_REMOVE_DISSERTATION_STEP, step.getReference());

			m_stepStorage.remove(step);

			EventTrackingService.post(EventTrackingService.newEvent(SECURE_REMOVE_DISSERTATION_STEP, step.getReference(), true));
			
			((BaseDissertationStepEdit)step).closeEdit();
		}
	}

	/** 
	* Adds an CandidatePath to the service.
	* @param dissertation - The parent Dissertation.
	* @param site - The site for which permissions are being checked.
	* @return The new CandidatePath.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public CandidatePathEdit addCandidatePath(Dissertation dissertation, String site)
		throws PermissionException
	{
		String pathId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			pathId = IdService.getUniqueId();
			try
			{
				Validator.checkResourceId(pathId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_pathStorage.check(pathId))
				badId = true;

		}while(badId);
		
		String key = pathReference(site, pathId);
		
		unlock(SECURE_ADD_DISSERTATION_CANDIDATEPATH, key);
		
		CandidatePathEdit path = m_pathStorage.put(pathId, site);
		
		dissertation.initializeCandidatePath(path, site);
		
		((BaseCandidatePathEdit) path).setEvent(SECURE_ADD_DISSERTATION_CANDIDATEPATH);
		
		return path;
	}
	
	/**
	* Add a new CandidatePath to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the CandidatePath.
	* @return A locked CandidatePathEdit object (reserving the id).
	* @exception IdInvalidException if the CandidatePath id is invalid.
	* @exception IdUsedException if the CandidatePath id is already used.
	* @exception PermissionException if the current user does not have permission to add a CandidatePath.
	*/
	public CandidatePathEdit mergeCandidatePath(Element el)
		throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		CandidatePath pathFromXml = new BaseCandidatePath(el);

		// check for a valid path name
		Validator.checkResourceId(pathFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_DISSERTATION_CANDIDATEPATH, pathFromXml.getReference());

		// reserve a path with this id from the info store - if it's in use, this will return null
		CandidatePathEdit path = m_pathStorage.put(pathFromXml.getId(), pathFromXml.getSite());
		if (path == null)
		{
			throw new IdUsedException(pathFromXml.getId());
		}

		// transfer from the XML read path object to the CandidatePathEdit
		((BaseCandidatePathEdit) path).set(pathFromXml);

		((BaseCandidatePathEdit) path).setEvent(SECURE_ADD_DISSERTATION_CANDIDATEPATH);

		return path;

	}
	
	/**
	* Check whether there exists a CandidatePath with this parent site
	* for which corresponding sort name’s first letter of the last name matches the letter.
	* @param parent - The parent site from which this candidate path steps come.
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return True if there exists such a CandidatePath, false otherwise.
	*/
	public boolean isUserOfParentForLetter(String parent, String letter)
	{
		boolean retVal = false;
		if(!letter.matches("[A-Za-z]"))
		{
			return retVal;
		}
		CandidatePath path = null;
		String sortName = null;
		if(parent != null)
		{
			List paths = m_pathStorage.getAllOfParentForLetter(parent, letter);
			for (int i = 0; i < paths.size(); i++)
			{
				path = (CandidatePath)paths.get(i);
				try
				{
					User student = UserDirectoryService.getUser(path.getCandidate());
					sortName = student.getSortName();
					if(sortName.startsWith(letter.toUpperCase()) || sortName.startsWith(letter.toLowerCase()))
					{
						return true;
					}
				}
				catch (IdUnusedException e) {};
			}
		}
		return retVal;
		
	}

	/**
	* Check whether there exists a CandidatePath having candidate attribute (uniqname)
	* for which corresponding sort name’s first letter of the last name matches the letter.
	* @param type - The type of CandidatePath (e.g, “Dissertation Steps”, “Dissertation
	* Steps: Music Performance”)
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return True if there exists such a CandidatePath, false otherwise.
	*/
	public boolean isUserOfTypeForLetter(String type, String letter)
	{
		boolean retVal = false;
		if(!letter.matches("[A-Za-z]"))
		{
			return retVal;
		}
		CandidatePath path = null;
		String sortName = null;
		if(type != null)
		{
			List paths = m_pathStorage.getAllOfTypeForLetter(type, letter);
			for (int i = 0; i < paths.size(); i++)
			{
				path = (CandidatePath)paths.get(i);
				try
				{
					User student = UserDirectoryService.getUser(path.getCandidate());
					sortName = student.getSortName();
					if(sortName.startsWith(letter.toUpperCase()) || sortName.startsWith(letter.toLowerCase()))
					{
						return true;
					}
				}
				catch (IdUnusedException e) {};
			}
		}
		return retVal;
		
	}
	
	/**
	* Access the alphabetical candidate chooser letter for this student. 
	* @param chefid – The user's id.
	* @return The alphabetical candidate chooser letter, A-Z, or "".
	*/
	public String getSortLetter(String chefId)
	{
		String retVal = "";
		if(chefId != null)
		{
			try
			{
				String sortName = UserDirectoryService.getUser(chefId).getSortName();
				if(sortName != null)
				{
					retVal = sortName.substring(0,1).toUpperCase();
				}
			}
			catch(IdUnusedException e) 
			{
				m_logger.warn(this + ".getSortLetter for " + chefId + " IdUnusedException " + e.toString());
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".getSortLetter for " + chefId + " Exception " + e.toString());
			}
		}
		return retVal;
		
	}

	/**
	* Access in Sort Name order the student User objects for which CandidatePath candidate 
	* attribute (uniqname) has a Sort Name starting with this letter.
	* @param type - The CandidatePath type (e.g., “Dissertation Steps”, “Dissertation Steps: Music Performance”.
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return The List of Users, or null if no such Users exist.
	*/
	public List getSortedUsersOfTypeForLetter(String type, String letter)
	{
		List retVal = new ArrayList();
		if(!letter.matches("[A-Za-z]"))
		{
			return retVal;
		}
		if(type != null)
		{
			CandidatePath path = null;
			String sortName = null;
			List paths = m_pathStorage.getAllOfTypeForLetter(type, letter);
			for (int i = 0; i < paths.size(); i++)
			{
				path = (CandidatePath)paths.get(i);
				try
				{
					User student = UserDirectoryService.getUser(path.getCandidate());
					sortName = student.getSortName();
					if(sortName.startsWith(letter.toUpperCase()) || sortName.startsWith(letter.toLowerCase()))
					{
						retVal.add(student);
					}
				}
				catch (IdUnusedException e) {}
			}
			if(retVal.size() > 1)
				Collections.sort(retVal, new UserComparator());
		}
		return retVal;
		
	}
	
	/**
	* Access in Sort Name order the student User objects for which CandidatePath candidate 
	* attribute (uniqname) has a Sort Name starting with this letter, for this parent site and letter.
	* @param type - The CandidatePath type (e.g., “Dissertation Steps”, “Dissertation Steps: Music Performance”.
	* @param letter – A letter of the alphabet, A-Z,a-z.
	* @return The List of Users, or null if no such Users exist.
	*/
	public List getSortedUsersOfParentForLetter(String parentSite, String letter)
	{
		List retVal = new ArrayList();
		if(!letter.matches("[A-Za-z]"))
		{
			return retVal;
		}
		if(parentSite != null)
		{
			CandidatePath path = null;
			String sortName = null;
			List paths = m_pathStorage.getAllOfParentForLetter(parentSite, letter);
			for (int i = 0; i < paths.size(); i++)
			{
				path = (CandidatePath)paths.get(i);
				try
				{
					User student = UserDirectoryService.getUser(path.getCandidate());
					sortName = student.getSortName();
					if(sortName.startsWith(letter.toUpperCase()) || sortName.startsWith(letter.toLowerCase()))
					{
						retVal.add(student);
					}
				}
				catch (IdUnusedException e) {}
			}
			if(retVal.size() > 1)
				Collections.sort(retVal, new UserComparator());
		}
		return retVal;
		
	}

	/**
	* Access the CandidatePaths of this type (e.g, “Dissertation Steps”, “Dissertation Steps: Music Performance”)
	* @param type - The CandidatePath type.
	* @return The CandidatePaths of this type, or null if none exist.
	*/
	public List getCandidatePathsOfType(String type)
	{
		List paths = new Vector();
		if(type != null)
		{
			paths = m_pathStorage.getAllOfType(type);
		}
		return paths;
		
	}

	/**
	* Check whether a CandidatePath of this type exists.
	* @param type - The CandidatePath type (e.g, “Dissertation Steps”, “Dissertation Steps: Music Performance”)
	* @return True if such a CandidatePath exists, false if not.
	*/
	public boolean isCandidatePathOfType(String type)
	{
		boolean exists = false;
		exists = m_pathStorage.existsPathOfType(type);
		return exists;
		
	}

	/**
	* Access the CandidatePath with the specified reference.
	* @param pathReference - The reference of the CandidatePath.
	* @return The CandidatePath corresponding to the id, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this id.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidatePath getCandidatePath(String pathReference)
		throws IdUnusedException, PermissionException
	{
		CandidatePath path = null;
		
		String pathId = pathId(pathReference);
		
		if (m_caching)
		{
			if(m_pathCache.containsKey(pathReference))
				path = (CandidatePath)m_pathCache.get(pathReference);
			else
			{
				path = m_pathStorage.get(pathId);
				m_pathCache.put(pathReference, path);
			}
		}
		else
		{
			path = m_pathStorage.get(pathId);
		}
		
		if(path == null) throw new IdUnusedException(pathId);

		unlock(SECURE_ACCESS_DISSERTATION_CANDIDATEPATH, pathReference);
		
		// EventTrackingService.post(EventTrackingService.newEvent(SECURE_ACCESS_DISSERTATION_CANDIDATEPATH, path.getReference(), false));
		
		return path;
	}
	
	/**
	* Access all CandidatePath objects - known to us (not from external providers).
	* @return A list of CandidatePath objects.
	*/
	public List getCandidatePaths()
	{
		List paths = new Vector();

		// if we have disabled the cache, don't use if
		if ((!m_caching) || m_pathCache.disabled())
		{
			paths = m_pathStorage.getAll();
		}

		else
		{
			// if the cache is complete, use it
			if (m_pathCache.isComplete())
			{
				paths = m_pathCache.getAll();
			}
	
			// otherwise get all the paths from storage
			else
			{
				// Note: while we are getting from storage, storage might change.  These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_pathCache)
				{
					// if we were waiting and it's now complete...
					if (m_pathCache.isComplete())
					{
						paths = m_pathCache.getAll();
						return paths;
					}
	
					// save up any events to the cache until we get past this load
					m_pathCache.holdEvents();
	
					paths = m_pathStorage.getAll();
	
					// update the cache, and mark it complete
					for (int i = 0; i < paths.size(); i++)
					{
						CandidatePath path = (CandidatePath) paths.get(i);
						m_pathCache.put(path.getReference(), path);
					}
	
					m_pathCache.setComplete();
	
					// now we are complete, process any cached events
					m_pathCache.processEvents();
				}
			}
		}

		return paths;

	}//getCandidatePaths
	
	
	/**
	* Get a locked CandidatePath object for editing.  Must commitEdit() to make official, or cancelEdit() when done!
	* @param pathReference The path reference string.
	* @return A CandidatePathEdit object for editing.
	* @exception IdUnusedException if not found, or if not an CandidatePathEdit object
	* @exception PermissionException if the current user does not have permission to edit this path.
	* @exception InUseException if the CandidatePath is being edited by another user.
	*/
	public CandidatePathEdit editCandidatePath(String pathReference)
		throws IdUnusedException, PermissionException, InUseException
	{
		unlock(SECURE_UPDATE_DISSERTATION_CANDIDATEPATH, pathReference);
		
		String pathId = pathId(pathReference);

		// check for existance
		if (m_caching)
		{
			if ((m_pathCache.get(pathReference) == null) && (!m_pathStorage.check(pathId)))
			{
				throw new IdUnusedException(pathId);
			}
		}
		else
		{
			if (!m_pathStorage.check(pathId))
			{
				throw new IdUnusedException(pathId);
			}
		}

		// ignore the cache - get the CandidatePath with a lock from the info store
		CandidatePathEdit path = m_pathStorage.edit(pathId);
		
		if (path == null) throw new InUseException(pathId);

		((BaseCandidatePathEdit) path).setEvent(SECURE_UPDATE_DISSERTATION_CANDIDATEPATH);

		return path;

	}

	/**
	* Commit the changes made to a CandidatePathEdit object, and release the lock.
	* @param path The CandidatePathEdit object to commit.
	*/
	public void commitEdit(CandidatePathEdit path)
	{
		// check for closed edit
		if (!path.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".commitEdit(): closed CandidatePathEdit", e); }
			return;
		}

		// update the properties
		addLiveUpdateProperties(path.getPropertiesEdit());

		// complete the edit
		m_pathStorage.commit(path);

		// track it
		EventTrackingService.post(
			EventTrackingService.newEvent(((BaseCandidatePathEdit) path).getEvent(), path.getReference(), true));

		// close the edit object
		((BaseCandidatePathEdit) path).closeEdit();

	}
	
	/**
	* Cancel the changes made to a CandidatePathEdit object, and release the lock.
	* @param path The CandidatePathEdit object to commit.
	*/
	public void cancelEdit(CandidatePathEdit path)
	{
		// check for closed edit
		if (!path.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".cancelEdit(): closed CandidatePathEdit", e); }
			return;
		}

		// release the edit lock
		m_pathStorage.cancel(path);

		// close the edit object
		((BaseCandidatePathEdit) path).closeEdit();

	}
	
	/** 
	* Removes an CandidatePath and all references to it
	* @param path - the CandidatePath to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeCandidatePath(CandidatePathEdit path)
		throws PermissionException
	{
		if(path != null)
		{
			if(!path.isActiveEdit())
			{
				try { throw new Exception(); }
				catch (Exception e) { m_logger.warn(this + ".removeCandidatePath(): closed CandidatePathEdit", e); }
				return;
			}

			unlock(SECURE_REMOVE_DISSERTATION_CANDIDATEPATH, path.getReference());

			m_pathStorage.remove(path);
			
			EventTrackingService.post(EventTrackingService.newEvent(SECURE_REMOVE_DISSERTATION_CANDIDATEPATH, path.getReference(), true));
			
			((BaseCandidatePathEdit)path).closeEdit();
		}
	}

	/** 
	* Adds an StepStatus to the service.
	* @param site - The site for which permissions are being checked.
	* @param step - The parent DissertationStep.
	* @param oardStep - Is this step auto-validated from Rackham's database?
	* @return The new StepStatus.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public StepStatusEdit addStepStatus(String site, DissertationStep step, boolean oardStep)
		throws PermissionException
	{
		String statusId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			statusId = IdService.getUniqueId();
		
			try
			{
				Validator.checkResourceId(statusId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_statusStorage.check(statusId))
				badId = true;
			
		}while(badId);
		
		String key = statusReference(site, statusId);
		
		unlock(SECURE_ADD_DISSERTATION_STEPSTATUS, key);
		
		StepStatusEdit status = m_statusStorage.put(statusId, site);

		status.initialize(site, step, oardStep);

		((BaseStepStatusEdit) status).setEvent(SECURE_ADD_DISSERTATION_STEPSTATUS);
		
		return status;
	}

	/** 
	* Adds an StepStatus to the service.
	* @param site - There is no current site - request comes from DissertationDataListenerService.
	* @param step - The parent DissertationStep.
	* @param oardStep - Is this step auto-validated from Rackham's database?
	* @return The new StepStatus.
	*/
	protected StepStatusEdit addStepStatusFromListener(String site, DissertationStep step, boolean oardStep)
	{
		String statusId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			statusId = IdService.getUniqueId();
		
			try
			{
				Validator.checkResourceId(statusId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_statusStorage.check(statusId))
				badId = true;
			
		}while(badId);
		
		StepStatusEdit status = m_statusStorage.put(statusId, site);
		status.initialize(site, step, oardStep);

		((BaseStepStatusEdit) status).setEvent(SECURE_ADD_DISSERTATION_STEPSTATUS);
		
		return status;
	}
	
	/** 
	* Adds an StepStatus to the service.
	* @param site - The site for which permissions are being checked.
	* @return The new StepStatus.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public StepStatusEdit addStepStatus(String site)
		throws PermissionException
	{
		String statusId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			statusId = IdService.getUniqueId();
		
			try
			{
				Validator.checkResourceId(statusId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_statusStorage.check(statusId))
				badId = true;
			
		}while(badId);
		
		String key = statusReference(site, statusId);
		
		unlock(SECURE_ADD_DISSERTATION_STEPSTATUS, key);

		StepStatusEdit status = m_statusStorage.put(statusId, site);
		
		status.setSite(site);
		
		((BaseStepStatusEdit) status).setEvent(SECURE_ADD_DISSERTATION_STEPSTATUS);
		
		return status;
	}

	/**
	* Add a new StepStatus to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the StepStatus.
	* @return A locked StepStatusEdit object (reserving the id).
	* @exception IdInvalidException if the StepStatus id is invalid.
	* @exception IdUsedException if the StepStatus id is already used.
	* @exception PermissionException if the current user does not have permission to add a CandidatePath.
	*/
	public StepStatusEdit mergeStepStatus(Element el)
		throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		StepStatus statusFromXml = new BaseStepStatus(el);

		// check for a valid StepStatus name
		Validator.checkResourceId(statusFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_DISSERTATION_STEPSTATUS, statusFromXml.getReference());

		// reserve a StepStatus with this id from the info store - if it's in use, this will return null
		StepStatusEdit status = m_statusStorage.put(statusFromXml.getId(), statusFromXml.getSite());
		if (status == null)
		{
			throw new IdUsedException(statusFromXml.getId());
		}

		// transfer from the XML read StepStatus object to the StepStatusEdit
		((BaseStepStatusEdit) status).set(statusFromXml);

		((BaseStepStatusEdit) status).setEvent(SECURE_ADD_DISSERTATION_STEPSTATUS);

		return status;

	}

	/**
	* Access the StepStatus with the specified reference.
	* @param pathReference - The reference of the StepStatus.
	* @return The StepStatus corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this id.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public StepStatus getStepStatus(String statusReference)
		throws IdUnusedException, PermissionException
	{	
		StepStatus status = null;
		String statusId = statusId(statusReference);

		if (m_caching)
		{
			if(m_statusCache.containsKey(statusReference))
				status = (StepStatus)m_statusCache.get(statusReference);
			else
			{
				status = m_statusStorage.get(statusId);
				m_statusCache.put(statusReference, status);
			}
		}
		else
		{
			status = m_statusStorage.get(statusId);
		}
		
		if(status == null) throw new IdUnusedException(statusId);

		unlock(SECURE_ACCESS_DISSERTATION_STEPSTATUS, statusReference);
		
		// EventTrackingService.post(EventTrackingService.newEvent(SECURE_ACCESS_DISSERTATION_STEPSTATUS, status.getReference(), false));
		
		return status;

	}
	
	/**
	* Access all StepStatus objects - known to us (not from external providers).
	* @return A list of StepStatus objects.
	*/
	public List getStepStatus()
	{
		List statusi = new Vector();

		// if we have disabled the cache, don't use if
		if ((!m_caching) || m_statusCache.disabled())
		{
			statusi = m_statusStorage.getAll();
		}
		else
		{
			// if the cache is complete, use it
			if (m_statusCache.isComplete())
			{
				statusi = m_statusCache.getAll();
			}
	
			// otherwise get all the status from storage
			else
			{
				// Note: while we are getting from storage, storage might change.  These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_statusCache)
				{
					// if we were waiting and it's now complete...
					if (m_statusCache.isComplete())
					{
						statusi = m_statusCache.getAll();
						return statusi;
					}
	
					// save up any events to the cache until we get past this load
					m_statusCache.holdEvents();
	
					statusi = m_statusStorage.getAll();
	
					// update the cache, and mark it complete
					for (int i = 0; i < statusi.size(); i++)
					{
						StepStatus status = (StepStatus) statusi.get(i);
						m_statusCache.put(status.getReference(), status);
					}
	
					m_statusCache.setComplete();
	
					// now we are complete, process any cached events
					m_statusCache.processEvents();
				}
			}
		}

		return statusi;

	}
	
	/**
	* Get a locked StepStatus object for editing.  Must commitEdit() to make official, or cancelEdit() when done!
	* @param statusReference The StepStatus reference string.
	* @return An StepStatusEdit object for editing.
	* @exception IdUnusedException if not found, or if not an StepStatusEdit object
	* @exception PermissionException if the current user does not have permission to edit this StepStatus.
	* @exception InUseException if the StepStatus is being edited by another user.
	*/
	public StepStatusEdit editStepStatus(String statusReference)
		throws IdUnusedException, PermissionException, InUseException
	{
		// check security (throws if not permitted)
		unlock(SECURE_UPDATE_DISSERTATION_STEPSTATUS, statusReference);
		
		String statusId = statusId(statusReference);

		// check for existance
		if (m_caching)
		{
			if ((m_statusCache.get(statusReference) == null) && (!m_statusStorage.check(statusId)))
			{
				throw new IdUnusedException(statusId);
			}
		}
		else
		{
			if (!m_statusStorage.check(statusId))
			{
				throw new IdUnusedException(statusId);
			}
		}

		// ignore the cache - get the StepStatus with a lock from the info store
		StepStatusEdit status = m_statusStorage.edit(statusId);
		
		if (status == null) throw new InUseException(statusId);

		((BaseStepStatusEdit) status).setEvent(SECURE_UPDATE_DISSERTATION_STEPSTATUS);

		return status;

	}
	
	/**
	* Commit the changes made to an StepStatusEdit object, and release the lock.
	* @param status The StepStatusEdit object to commit.
	*/
	public void commitEdit(StepStatusEdit status)
	{
		// check for closed edit
		if (!status.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".commitEdit(): closed StepStatusEdit", e); }
			return;
		}

		// update the properties
		addLiveUpdateProperties(status.getPropertiesEdit());

		// complete the edit
		m_statusStorage.commit(status);

		// track it
		EventTrackingService.post(
			EventTrackingService.newEvent(((BaseStepStatusEdit) status).getEvent(), status.getReference(), true));

		// close the edit object
		((BaseStepStatusEdit) status).closeEdit();

	}

	/**
	* Cancel the changes made to a StepStatusEdit object, and release the lock.
	* @param status The StepStatusEdit object to commit.
	*/
	public void cancelEdit(StepStatusEdit status)
	{
		// check for closed edit
		if (!status.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".cancelEdit(): closed StepStatusEdit", e); }
			return;
		}

		// release the edit lock
		m_statusStorage.cancel(status);

		// close the edit object
		((BaseStepStatusEdit) status).closeEdit();

	}

	/** 
	* Removes a StepStatus and all references to it
	* @param status - the StepStatus to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeStepStatus(StepStatusEdit status)
		throws PermissionException
	{
		if(status != null)
		{
			if(!status.isActiveEdit())
			{
				try { throw new Exception(); }
				catch (Exception e) { m_logger.warn(this + ".removeStepStatus(): closed StepStatusEdit", e); }
				return;
			}

			unlock(SECURE_REMOVE_DISSERTATION_STEPSTATUS, status.getReference());

			m_statusStorage.remove(status);
			
			EventTrackingService.post(EventTrackingService.newEvent(SECURE_REMOVE_DISSERTATION_STEPSTATUS, status.getReference(), true));

			((BaseStepStatusEdit)status).closeEdit();		
		}

	}
	
	/** 
	* Adds an CandidateInfo to the service.
	* @param site - The site for which permissions are being checked.
	* @return The new CandidateInfo.
	* @throws PermissionException if the current User does not have permission to do this.
	*/
	public CandidateInfoEdit addCandidateInfo(String site)
		throws PermissionException
	{
		String infoId = null;
		boolean badId = false;
		
		do
		{
			badId = false;
			infoId = IdService.getUniqueId();
			try
			{
				Validator.checkResourceId(infoId);
			}
			catch(IdInvalidException iie)
			{
				badId = true;
			}

			if(m_infoStorage.check(infoId))
				badId = true;

		}while(badId);
		
		String key = infoReference(site, infoId);

		unlock(SECURE_ADD_DISSERTATION_CANDIDATEINFO, key);
		
		CandidateInfoEdit info = m_infoStorage.put(infoId, site);
	
		info.setSite(site);
		
		try
		{
			((BaseCandidateInfoEdit) info).setEvent(SECURE_ADD_DISSERTATION_CANDIDATEINFO);
		}
		catch(Exception e)
		{
			m_logger.warn("DISSERTATION : BASE SERVICE : ADD CANDIDATE INFO : EXCEPTION TRACKING EVENT : " + e);
		}
		
		return info;
	}

	/**
	* Add a new CandidateInfo to the directory, from a definition in XML.
	* Must commitEdit() to make official, or cancelEdit() when done!
	* @param el The XML DOM Element defining the CandidateInfo.
	* @return A locked CandidateInfoEdit object (reserving the id).
	* @exception IdInvalidException if the CandidateInfo id is invalid.
	* @exception IdUsedException if the CandidateInfo id is already used.
	* @exception PermissionException if the current user does not have permission to add a CandidateInfo.
	*/
	public CandidateInfoEdit mergeCandidateInfo(Element el)
		throws IdInvalidException, IdUsedException, PermissionException
	{
		// construct from the XML
		CandidateInfo infoFromXml = new BaseCandidateInfo(el);

		// check for a valid CandidateInfo name
		Validator.checkResourceId(infoFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_DISSERTATION_CANDIDATEINFO, infoFromXml.getReference());

		// reserve a CandidateInfo with this id from the info store - if it's in use, this will return null
		CandidateInfoEdit info = m_infoStorage.put(infoFromXml.getId(), infoFromXml.getSite());
		if (info == null)
		{
			throw new IdUsedException(infoFromXml.getId());
		}

		// transfer from the XML read CandidateInfo object to the CandidateInfoEdit
		((BaseCandidateInfoEdit) info).set(infoFromXml);

		((BaseCandidateInfoEdit) info).setEvent(SECURE_ADD_DISSERTATION_CANDIDATEINFO);

		return info;

	}

	/**
	* Access the CandidateInfo with the specified reference.
	* @param infoReference - The reference of the CandidateInfo.
	* @return The CandidateInfo corresponding to the reference, or null if it does not exist.
	* @throws IdUnusedException if there is no object with this reference.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidateInfo getCandidateInfo(String infoReference)
		throws IdUnusedException, PermissionException
	{
		CandidateInfo info = null;
		
		String infoId = infoId(infoReference);
		
		if (m_caching)
		{
			if(m_infoCache.containsKey(infoReference))
				info = (CandidateInfo)m_infoCache.get(infoReference);
			else
			{
				info = m_infoStorage.get(infoId);
				m_infoCache.put(infoReference, info);
			}
		}
		else
		{
			info = m_infoStorage.get(infoId);
		}
		
		if(info == null) throw new IdUnusedException(infoId);

		unlock(SECURE_ACCESS_DISSERTATION_CANDIDATEINFO, infoReference);

		// EventTrackingService.post(EventTrackingService.newEvent(SECURE_ACCESS_DISSERTATION_CANDIDATEINFO, info.getReference(), false));
		
		return info;

	}

	/**
	* Access the CandidateInfo for the specified user.
	* @param emplid - The emplid of the user.
	* @return The CandidateInfo corresponding to the emplid, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidateInfoEdit getCandidateInfoEditForEmplid(String emplid)
		throws PermissionException
	{
		CandidateInfoEdit retVal = null;
		if(emplid != null)
		{
			CandidateInfo tempInfo = null;
			tempInfo = m_infoStorage.getForEmplid(emplid);
			if(tempInfo != null)
			{
				retVal = m_infoStorage.edit(tempInfo.getId());
			}
		}
		
		return retVal;

	}	
	
	/**
	* Access all CandidateInfo objects - known to us (not from external providers).
	* @return A list of CandidateInfo objects.
	*/
	protected List getCandidateInfos()
	{
		List infos = new Vector();

		// if we have disabled the cache, don't use if
		if ((!m_caching) || m_infoCache.disabled())
		{
			infos = m_infoStorage.getAll();
		}

		else
		{
			// if the cache is complete, use it
			if (m_infoCache.isComplete())
			{
				infos = m_infoCache.getAll();
			}
	
			// otherwise get all the infos from storage
			else
			{
				// Note: while we are getting from storage, storage might change.  These can be processed
				// after we get the storage entries, and put them in the cache, and mark the cache complete.
				// -ggolden
				synchronized (m_infoCache)
				{
					// if we were waiting and it's now complete...
					if (m_infoCache.isComplete())
					{
						infos = m_infoCache.getAll();
						return infos;
					}
	
					// save up any events to the cache until we get past this load
					m_infoCache.holdEvents();
	
					infos = m_infoStorage.getAll();
	
					// update the cache, and mark it complete
					for (int i = 0; i < infos.size(); i++)
					{
						CandidateInfo info = (CandidateInfo) infos.get(i);
						m_infoCache.put(info.getReference(), info);
					}
	
					m_infoCache.setComplete();
	
					// now we are complete, process any cached events
					m_infoCache.processEvents();
				}
			}
		}

		return infos;

	}//getCandidateInfos
	
	
	/**
	* Get a locked CandidateInfo object for editing.  Must commitEdit() to make official, or cancelEdit() when done!
	* @param infoReference The CandidateInfo reference string.
	* @return An CandidateInfoEdit object for editing.
	* @exception IdUnusedException if not found, or if not an CandidateInfoEdit object
	* @exception PermissionException if the current user does not have permission to edit this CandidateInfo.
	* @exception InUseException if the CandidateInfo is being edited by another user.
	*/
	public CandidateInfoEdit editCandidateInfo(String infoReference)
		throws IdUnusedException, PermissionException, InUseException
	{
		// check security (throws if not permitted)
		unlock(SECURE_UPDATE_DISSERTATION_CANDIDATEINFO, infoReference);
		
		String infoId = infoId(infoReference);

		// check for existance
		if (m_caching)
		{
			if ((m_infoCache.get(infoReference) == null) && (!m_infoStorage.check(infoId)))
			{
				throw new IdUnusedException(infoId);
			}
		}
		else
		{
			if (!m_infoStorage.check(infoId))
			{
				throw new IdUnusedException(infoId);
			}
		}

		// ignore the cache - get the CandidateInfo with a lock from the info store
		CandidateInfoEdit info = m_infoStorage.edit(infoId);
		
		if (info == null) throw new InUseException(infoId);

		((BaseCandidateInfoEdit) info).setEvent(SECURE_UPDATE_DISSERTATION_CANDIDATEINFO);

		return info;

	}

	/**
	* Commit the changes made to an CandidateInfoEdit object, and release the lock.
	* @param info The CandidateInfoEdit object to commit.
	*/
	public void commitEdit(CandidateInfoEdit info)
	{
		// check for closed edit
		if (!info.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".commitEdit(): closed CandidateInfoEdit", e); }
			return;
		}

		// update the properties
		addLiveUpdateProperties(info.getPropertiesEdit());

		// complete the edit
		m_infoStorage.commit(info);
		
		// track it
		EventTrackingService.post(
			EventTrackingService.newEvent(((BaseCandidateInfoEdit) info).getEvent(), info.getReference(), true));

		// close the edit object
		((BaseCandidateInfoEdit) info).closeEdit();

	}

	/**
	* Cancel the changes made to a CandidateInfoEdit object, and release the lock.
	* @param info The CandidateInfoEdit object to commit.
	*/
	public void cancelEdit(CandidateInfoEdit info)
	{
		// check for closed edit
		if (!info.isActiveEdit())
		{
			try { throw new Exception(); }
			catch (Exception e) { m_logger.warn(this + ".cancelEdit(): closed CandidateInfoEdit", e); }
			return;
		}

		// release the edit lock
		m_infoStorage.cancel(info);

		// close the edit object
		((BaseCandidateInfoEdit) info).closeEdit();

	}

	/** 
	* Removes an CandidateInfo and all references to it
	* @param info - the CandidateInfo to remove.
	* @throws PermissionException if current User does not have permission to do this.
	*/
	public void removeCandidateInfo(CandidateInfoEdit info)
		throws PermissionException
	{
		if(info != null)
		{
			if(!info.isActiveEdit())
			{
				try { throw new Exception(); }
				catch (Exception e) { m_logger.warn(this + ".removeCandidateInfo(): closed CandidateInfoEdit", e); }
				return;
			}

			unlock(SECURE_REMOVE_DISSERTATION_CANDIDATEINFO, info.getReference());

			m_infoStorage.remove(info);
			
			EventTrackingService.post(EventTrackingService.newEvent(SECURE_REMOVE_DISSERTATION_CANDIDATEINFO, info.getReference(), true));

			((BaseCandidateInfoEdit)info).closeEdit();		
		}

	}


	/** 
	* Get a List of DissertationSteps with site equal to this site
	* @param site - the site to match
	* @throws PermissionException if current User does not have permission to do this.
	* @throws IdUnusedException if step cannot be found
	*/
	public List getDissertationStepsForSite(String site)
		throws IdUnusedException, PermissionException
	{
		List retVal = new ArrayList();
		DissertationStep tempStep = null;
		if(site != null)
		{
			List allSteps = getDissertationSteps();
			for(int x = 0; x < allSteps.size(); x++)
			{
				tempStep = (DissertationStep)allSteps.get(x);
				if(tempStep.getSite().equals(site))
				{
					retVal.add(tempStep);
				}
			}
		}

		return retVal;
		
	}
	
	/** 
	* Get the List of CandidatePaths assiciated with a parent (department) site.
	* @param site - The site id.
	* @return The List of CandidatePath objects.
	*/
	public List getCandidatePathsForParentSite(String site)
	{
		List retVal = new ArrayList();
		if(site != null)
		{
			retVal = m_pathStorage.getAllForParent(site);
		}
		return retVal;
		
	}
	
	/**
	* Access the CandidatePath for the specified candidate.
	* @param candidateId The CHEF user id of the candidate.
	* @return The CandidatePath corresponding to the candidate, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidatePath getCandidatePathForCandidate(String candidateId)
		throws PermissionException
	{
		CandidatePath retVal = null;
		if(candidateId != null) 
		{
			retVal = m_pathStorage.getForCandidate(candidateId);
		}
		return retVal;
		
	}
	
	/**
	* Access the CandidatePath for the specified site.
	* @param siteId The CHEF site id.
	* @return The CandidatePath corresponding to the site, or null if it does not exist.
	* @throws PermissionException if the current user is not allowed to access this.
	*/
	public CandidatePath getCandidatePathForSite(String siteId)
		throws PermissionException
	{
		CandidatePath retVal = null;
		try
		{
			retVal = m_pathStorage.getForSite(siteId);
		}
		catch(Exception e) {}
		return retVal;
		
	}
	
	/**
	* Access the Dissertation associated with pedagogical approach of Music Performance or otherwise.
	* @param site - The site id.
	* @param type - The pedagogical approach inherent in the steps.
	* @return Dissertation associated with a site and type, or null if one does not exist.
	*/
	public Dissertation getDissertationForSite(String site, String type)
		throws IdUnusedException, PermissionException
	{
		Dissertation retVal = null;
		if((site != null) && (type != null))
		{
			try
			{
				retVal = m_dissertationStorage.getForSiteOfType(site, type);
			}
			catch(Exception e)
			{
				m_logger.warn("DISSERTATION : BASE SERVICE : GET DISSERTATION FOR SITE : EXCEPTION : " + e);
			}
		}
		return retVal;
		
	}
	
	/**
	* Access the Dissertation associated with a site.
	* @param site - The site id.
	* @return Dissertation associated with a site, or null if one does not exist.
	*/
	public Dissertation getDissertationForSite(String site)
		throws IdUnusedException, PermissionException
	{
		Dissertation retVal = null;
		if(site != null)
		{
			retVal = m_dissertationStorage.getForSite(site);
		}
		return retVal;
		
	}
	
	
	/** 
	* Access whether any active CandidatePaths exist.
	* @return True if a CandidateInfo exists, false if not.
	*/
	public boolean getActivePaths()
	{
		boolean retVal = false;
		if (m_caching)
		{
			List keys = m_pathCache.getKeys();
			if(keys.size() > 0)
				retVal = true;
			else
			{
				retVal = !m_pathStorage.isEmpty();
			}
		}
		else
		{
			retVal = !m_pathStorage.isEmpty();
		}
		return retVal;
	}
	
	/** 
	* Access CandidateInfo for a candidate.
	* @param candidate The candidate's Chef id.
	* @return CandidateInfo of the specified candidate or null if not found.
	*/
	public CandidateInfo getCandidateInfoForCandidate(String candidate)
	{
		CandidateInfo retVal = null;
		if(candidate != null)
		{
			m_infoStorage.getForCandidate(candidate);
		}
		return retVal;
		
	}
	
	/** 
	* Access whether a department site has any active CandidatePaths associated with it.
	* @param site The site id.
	* @return True if a CandidateInfo exists for this parent site, false if not.
	*/
	public boolean getActivePathsForSite(String site)
	{
		boolean retVal = false;
		if(site != null)
		{
			try
			{
				retVal = m_pathStorage.existsPathForParent(site);
			}
			catch(Exception e){}
		}
		return retVal;
		
	}
	
	/**
	* Access the CandidateInfo for the candidate.
	* @param id - The CHEF user id of the candidate.
	* @return The CandidateInfo for the candidate, or null if it does not exist.
	*/
	public CandidateInfo getInfoForCandidate(String id)
	{
		CandidateInfo retVal = null;
		if(id != null)
		{
			retVal = m_infoStorage.getForCandidate(id);
		}
		return retVal;
	}
	
	//%%% redundant getInfoForCandidate() and getCandiateInfoForCandidate(), get rid of one
	
	/** 
	* Access the site id of the Rackham site.
	* @return The school site id.
	*/
	public String getSchoolSite()
	{
		return m_schoolSite;
	}
	
	/** 
	* Access the site id of the Music Performance site.
	* @return The Music Performance site id.
	*/
	public String getMusicPerformanceSite()
	{
		return m_musicPerformanceSite;
	}

	/**
	* Return the reference root for use in resource references and urls.
	* @return The reference root for use in resource references and urls.
	*/
	protected String getReferenceRoot()
	{
		return REFERENCE_ROOT;
	}

	/**
	* Update the live properties for an object when modified.
	*/
	protected void addLiveUpdateProperties(ResourcePropertiesEdit props)
	{
		props.addProperty(ResourceProperties.PROP_MODIFIED_BY,
				SessionManager.getCurrentSessionUserId());

		props.addProperty(ResourceProperties.PROP_MODIFIED_DATE,
			TimeService.newTime().toString());

	}

	/**
	* Create the live properties for the object.
	*/
	protected void addLiveProperties(ResourcePropertiesEdit props)
	{
		String current = SessionManager.getCurrentSessionUserId();
		props.addProperty(ResourceProperties.PROP_CREATOR, current);
		props.addProperty(ResourceProperties.PROP_MODIFIED_BY, current);
		
		String now = TimeService.newTime().toString();
		props.addProperty(ResourceProperties.PROP_CREATION_DATE, now);
		props.addProperty(ResourceProperties.PROP_MODIFIED_DATE, now);

	}
	
	/**
	* Check permissions for adding a BlockGrantGroup
	* @param site - the site id for which permissions are to be checked.
	* @return True if the current User is allowed to add a BlockGrantGroup to site, false if not.
	*/
	public boolean allowAddBlockGrantGroup(String site)
	{
		return unlockCheck(SECURE_ADD_DISSERTATION_GROUP, site);
	}
	
	/**
	* Check permissions for accessing the BlockGrantGroup
	* @param bggReference - The BlockGrantGroup's reference.
	* @return True if the current User is allowed to remove the BlockGrantGroup, false if not.
	*/
	public boolean allowGetBlockGrantGroup(String bggReference)
	{
		return unlockCheck(SECURE_ACCESS_DISSERTATION_GROUP, bggReference);
	}
	
	/**
	* Check permissions for removing the BlockGrantGroup.
	* @param bggReference - The BlockGrantGroup's reference.
	* @return True if the current User is allowed to remove the BlockGrantGroup, false if not.
	*/
	public boolean allowRemoveBlockGrantGroup(String bggReference)
	{
		return unlockCheck(SECURE_REMOVE_DISSERTATION_GROUP, bggReference);
	}
	
	/**
	* Check permissions for removing the CandidatePath
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to remove the CandidatePath, false if not.
	*/
	public boolean allowUpdateBlockGrantGroup(String bggReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION_GROUP, bggReference);
	}
	
	/**
	* Check permissions for adding an Dissertation.
	* @param site - the site id.
	* @return True if the current User is allowed to add an Dissertation, false if not.
	*/
	public boolean allowAddDissertation(String site)
	{
		// check security (throws if not permitted)
		String resourceString = dissertationReference(site, "");
		return unlockCheck(SECURE_ADD_DISSERTATION, resourceString);
	}

	/**
	* Check permissions for accessing a Dissertation.
	* @param dissertationReference - The Dissertation's reference.
	* @return True if the current User is allowed to access the Dissertation, false if not.
	*/
	public boolean allowGetDissertation(String dissertationReference)
	{
		return unlockCheck(SECURE_ACCESS_DISSERTATION, dissertationReference);
	}

	/**
	* Check permissions for updating a Dissertation.
	* @param dissertationReference - The Dissertation's reference.
	* @return True if the current User is allowed to update the Dissertation, false if not.
	*/
	public boolean allowUpdateDissertation(String dissertationReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION, dissertationReference);
	}

	/**
	* Check permissions for removing a Dissertation.
	* @param dissertationReference - The Dissertation's reference.
	* @return True if the current User is allowed to remove the Dissertation, false if not.
	*/
	public boolean allowRemoveDissertation(String dissertationReference)
	{
		return unlockCheck(SECURE_REMOVE_DISSERTATION, dissertationReference);
	}

	/**
	* Check permissions for add CandidatePath
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add an CandidatePath, false if not.
	*/
	public boolean allowAddCandidatePath(String site)
	{
		String resourceString = pathReference(site, "");
		return unlockCheck(SECURE_ADD_DISSERTATION_CANDIDATEPATH, resourceString);
	}

	/**
	* Check permissions for accessing a CandidatePath.
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to get the CandidatePath, false if not.
	*/
	public boolean allowGetCandidatePath(String pathReference)
	{
		return unlockCheck(SECURE_ACCESS_DISSERTATION_CANDIDATEPATH, pathReference);
	}

	/**
	* Check permissions for updating CandidatePath.
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to update the CandidatePath, false if not.
	*/
	public boolean allowUpdateCandidatePath(String pathReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION_CANDIDATEPATH, pathReference);
	}

	/**
	* Check permissions for updating CandidatePath for Committee Members.
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to update the CandidatePath, false if not.
	*/
	public boolean allowUpdateCandidatePathComm(String pathReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION_CANDIDATEPATH_COMM, pathReference);
	}
	
	/**
	* Check permissions for removing the CandidatePath
	* @param pathReference - The CandidatePath's reference.
	* @return True if the current User is allowed to remove the CandidatePath, false if not.
	*/
	public boolean allowRemoveCandidatePath(String pathReference)
	{
		return unlockCheck(SECURE_REMOVE_DISSERTATION_CANDIDATEPATH, pathReference);
	}

	/**
	* Check permissions for get DissertationStep
	* @param stepReference - The DissertationStep reference.
	* @return True if the current User is allowed to get the DissertationStep, false if not.
	*/
	public boolean allowGetDissertationStep(String stepReference)
	{
		return unlockCheck(SECURE_ACCESS_DISSERTATION_STEP, stepReference);
	}

	/**
	* Check permissions for updating DissertationStep
	* @param stepReference - The DissertationStep reference.
	* @return True if the current User is allowed to update the DissertationStep, false if not.
	*/
	public boolean allowUpdateDissertationStep(String stepReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION_STEP, stepReference);
	}

	/**
	* Check permissions for adding a DissertationStep.
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add a DissertationStep, false if not.
	*/
	public boolean allowAddDissertationStep(String site)
	{
		String resourceString = stepReference(site, "");
		return unlockCheck(SECURE_ADD_DISSERTATION_STEP, resourceString);
	}
	
	/**
	* Check permissions for removing Dissertation step
	* @param stepReference - The DissertationStep reference.
	* @return True if the current User is allowed to remove the DissertationStep, false if not.
	*/
	public boolean allowRemoveDissertationStep(String stepReference)
	{
		return unlockCheck(SECURE_REMOVE_DISSERTATION_STEP, stepReference);
	}

	/**
	* Check permissions for accessing a StepStatus.
	* @param statusReference - The StepStatus's reference.
	* @return True if the current User is allowed to get the StepStatus, false if not.
	*/
	public boolean allowGetStepStatus(String statusReference)
	{
		return unlockCheck(SECURE_ACCESS_DISSERTATION_STEP, statusReference);
	}

	/**
	* Check permissions for updating StepStatus.
	* @param statusReference - The StepStatus's reference.
	* @return True if the current User is allowed to update the StepStatus, false if not.
	*/
	public boolean allowUpdateStepStatus(String statusReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION_STEP, statusReference);
	}

	/**
	* Check permissions for add StepStatus
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add an StepStatus, false if not.
	*/
	public boolean allowAddStepStatus(String site)
	{
		String resourceString = statusReference(site, "");
		return unlockCheck(SECURE_ADD_DISSERTATION_STEP, resourceString);
	}
	
	/**
	* Check permissions for removing the StepStatus
	* @param statusReference - The StepStatus's reference.
	* @return True if the current User is allowed to remove the StepStatus, false if not.
	*/
	public boolean allowRemoveStepStatus(String statusReference)
	{
		return unlockCheck(SECURE_REMOVE_DISSERTATION_STEP, statusReference);
	}

	/**
	* Check permissions for add CandidateInfo
	* @param site - The site for which permissions are being checked.
	* @return True if the current User is allowed to add an CandidateInfo, false if not.
	*/
	public boolean allowAddCandidateInfo(String site)
	{
		String resourceString = infoReference(site, "");
		return unlockCheck(SECURE_ADD_DISSERTATION_CANDIDATEINFO, resourceString);
	}

	/**
	* Check permissions for accessing a CandidateInfo.
	* @param infoReference - The CandidateInfo's reference.
	* @return True if the current User is allowed to get the CandidateInfo, false if not.
	*/
	public boolean allowGetCandidateInfo(String infoReference)
	{
		return unlockCheck(SECURE_ACCESS_DISSERTATION_CANDIDATEINFO, infoReference);
	}

	/**
	* Check permissions for updating CandidateInfo.
	* @param infoReference - The CandidateInfo's reference.
	* @return True if the current User is allowed to update the CandidateInfo, false if not.
	*/
	public boolean allowUpdateCandidateInfo(String infoReference)
	{
		return unlockCheck(SECURE_UPDATE_DISSERTATION_CANDIDATEINFO, infoReference);
	}

	/**
	* Check permissions for removing the CandidateInfo
	* @param infoReference - The CandidateInfo's reference.
	* @return True if the current User is allowed to remove the CandidateInfo, false if not.
	*/
	public boolean allowRemoveCandidateInfo(String infoReference)
	{
		return unlockCheck(SECURE_REMOVE_DISSERTATION_CANDIDATEINFO, infoReference);
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * EntityProducer implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
 	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		return "gradtools";
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willImport()
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public HttpAccess getHttpAccess()
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		// for dissertations
		if (reference.startsWith(REFERENCE_ROOT))
		{
			String subType = null;
			String container = null;
			String context = null;
			String id = null;

			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);
			// we will get null, dissertation, [d|s|p|ss|i], outer context, inner context, id

			if (parts.length > 2)
			{
				subType = parts[2];

				if (parts.length > 3)
				{
					// inner context is container
					container = parts[3];

					if (parts.length > 4)
					{
						// outer context is context
						context = parts[4];

						if (parts.length > 5)
						{
							id = parts[5];
						}
					}
				}
			}

			ref.set(SERVICE_NAME, subType, id, container, context);

			return true;
		}
		
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Entity getEntity(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getEntityAuthzGroups(Reference ref)
	{
		// double check that it's mine
		if (SERVICE_NAME != ref.getType()) return null;

		Collection rv = new Vector();
		try
		{
			// site
			ref.addSiteContextAuthzGroup(rv);

			// specific
			rv.add(ref.getReference());

			// school reference
			String schoolRef = REFERENCE_ROOT + Entity.SEPARATOR + ref.getSubType() + Entity.SEPARATOR
					+ ref.getContainer();
			rv.add(schoolRef);
		}
		catch (NullPointerException e)
		{
			m_logger.warn(this + ".getEntityRealms(): " + e);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityUrl(Reference ref)
	{
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void syncWithSiteChange(Site site, EntityProducer.ChangeType change)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames,
			Map userIdTrans, Set userListAllowImport)
	{
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	public void importEntities(String fromContext, String toContext, List ids)
	{
	}

	/*******************************************************************************
	* BlockGrantGroup implementation
	*******************************************************************************/

	public class BaseBlockGrantGroup
		implements BlockGrantGroup
	{
		/** The BlockGrantGroup id. */
		protected String m_id;
		
		/** The BlockGrantGroup code. */
		protected String m_code;
		
		/** The BlockGrantGroup description. */
		protected String m_description;
		
		/** The BlockGrantGroup site. */
		protected String m_site;
		
		/** The properties. */
		protected ResourcePropertiesEdit m_properties;
		
		/** The DissertationStep references, keyed by display order, starting with 1. */
		protected Hashtable m_fieldsOfStudy;
		
		/**
		* Constructor.
		* @param code The BlockGrantGroup code (BGGC).
		* @param description The BlockGrantGroup description (BGGD).
		*/
		public BaseBlockGrantGroup(String id, String site)
		{
			m_id = id;
			m_site = site;
			m_code = null;
			m_description = null;
			m_fieldsOfStudy = new Hashtable();
			m_properties = new BaseResourcePropertiesEdit();
			addLiveProperties(m_properties);
			
		}//BaseBlockGrantGroup
		
		/**
		* Copy constructor.
		* @param blockGrantGroup The BlockGrantGroup to copy.
		*/
		public BaseBlockGrantGroup(BlockGrantGroup blockGrantGroup)
		{
			setAll(blockGrantGroup);

		}	// BaseBlockGrantGroup
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The BlockGrantGroup in XML in a DOM element.
		*/
		//public BaseDissertation(Element el)
		public BaseBlockGrantGroup(Element el)
		{
			this("", "");
			
			String keyString = null;
			String valueString = null;
			
			m_id = el.getAttribute("id");
			m_code = el.getAttribute("code");
			m_description = el.getAttribute("description");
			m_site = el.getAttribute("site");
			
				// READ THE CHILD ELEMENTS
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element)child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					m_properties = new BaseResourcePropertiesEdit(element);
				}
				else if(element.getTagName().equals("fields"))
				{
					NodeList osChildren = element.getChildNodes();
					final int osLength = osChildren.getLength();
					
					Node osChild = null;
					Element osElement = null;
					for(int x = 0; x < osLength; x++)
					{
						osChild = osChildren.item(x);
						if (osChild.getNodeType() != Node.ELEMENT_NODE) continue;
						osElement = (Element)osChild;
						if(osElement.getTagName().equals("field"))
						{
							keyString = osElement.getAttribute("code");
							valueString = osElement.getAttribute("description");
							if((keyString != null) && (valueString != null))
							{
								m_fieldsOfStudy.put(keyString, valueString);
							}
						}
					}
				}
			}

		}// storage constructor

		/**
		* Deep copy of this object.
		* @param b - The BlockGrantGroup object to be copied.
		*/
		protected void setAll(BlockGrantGroup b)
		{
			m_id = b.getId();
			m_code = b.getCode();
			m_description = b.getDescription();
			m_site = b.getSite();
			m_fieldsOfStudy = b.getFieldsOfStudy();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(b.getProperties());

		}   // setAll
				
		/**
		* Serialize the resource into XML, adding an element to the doc under the top of the stack element.
		* @param doc The DOM doc to contain the XML (or null for a string return).
		* @param stack The DOM elements, the top of which is the containing element of the new "resource" element.
		* @return The newly added element.
		*/
		public Element toXml(Document doc, Stack stack)
		{
			Element group = doc.createElement("group");
			
			if(stack.isEmpty())
			{
				doc.appendChild(group);
			}
			else
			{
				((Element)stack.peek()).appendChild(group);
			}
			
			stack.push(group);
			
			Enumeration keys = null;
			String fieldCode = null;
			String fieldName = null;

			// SAVE THE BLOCK GRANT GROUP ATTRIBUTES
			group.setAttribute("id", m_id);
			group.setAttribute("code", m_code);
			group.setAttribute("description", m_description);
			group.setAttribute("site", m_site);
			
			// SAVE THE FIELDS OF STUDY
			Element field = null;
			Element fields = doc.createElement("fields");
			keys = m_fieldsOfStudy.keys();
			while(keys.hasMoreElements())
			{
				field = doc.createElement("field");
				fieldCode = (String)keys.nextElement();
				fieldName = (String)m_fieldsOfStudy.get((String)fieldCode);
				if(fieldCode!=null && fieldName!=null)
				{
					field.setAttribute("code", fieldCode);
					field.setAttribute("description", fieldName);
					fields.appendChild(field);
				}

				fields.appendChild(field);
			}
			group.appendChild(fields);

				// SAVE THE PROPERTIES
			m_properties.toXml(doc, stack);
			stack.pop();

			return group;

		}//toXML
		
		/*******************************************************************************
		* Reference Implementation
		*******************************************************************************/
		
		public String getId()
		{
			return m_id;
		}
		
		/**
		* Access the BlockGrantGroup code.
		* @return The BlockGrantGroup code string.
		*/
		public String getCode()
		{
			return m_code;
		}
			
		/**
		* Access the BlockGrantGroup description.
		* @return The BlockGrantGroup description string.
		*/
		public String getDescription()
		{
			return m_description;
		}
		
		/**
		* Access the BlockGrantGroup FieldsOfStudy.
		* @return The BlockGrantGroup FieldsOfStudy hash table.
		*/
		public Hashtable getFieldsOfStudy()
		{
			return m_fieldsOfStudy;
		}
		
		/**
		* Access the URL which can be used to access the resource.
		* @return The URL which can be used to access the resource.
		*/
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}
		
		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The internal reference which can be used to access the resource from within the system.
		*/
		public String getReference()
		{
			return blockGrantGroupReference(m_site, m_id);

		}   // getReference

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The internal reference which can be used to access the resource from within the system.
		*/
		public String getSite()
		{
			return m_site;
			
		}//getSite
		
		/**
		* Access the resources's properties.
		* @return The resources's properties.
		*/
		public ResourceProperties getProperties()
		{
			return m_properties;

		}   // getProperties

		
	}//BaseBlockGrantGroup


	/*******************************************************************************
	* BlockGrantGroupEdit implementation
	*******************************************************************************/

	public class BaseBlockGrantGroupEdit
		extends BaseBlockGrantGroup
		implements BlockGrantGroupEdit, SessionStateBindingListener
	{
		/** The event code for the edit. */
		protected String m_event = null;
		
		/** Active flag. */
		protected boolean m_active = false;
		
		/**
		* Constructor.
		* @param id The BlockGrantGroup id.
		* @param site The BlockGrantGroup site.
		*/
		public BaseBlockGrantGroupEdit(String id, String site)
		{
			super(id, site);

		}// BaseBlockGrantGroupEdit

		/**
		* Construct from an existing definition, in xml.
		* @param el The BlockGrantGroupEdit in XML in a DOM element.
		*/
		public BaseBlockGrantGroupEdit(Element el)
		{
			super(el);

		}// BaseBlockGrantGroupEdit
		
		/**
		* Copy constructor.
		* @param blockGrantGroup The BlockGrantGroup to be copied.
		*/
		public BaseBlockGrantGroupEdit(BlockGrantGroup blockGrantGroup)
		{
			super(blockGrantGroup);

		}// BaseBlockGrantGroupEdit

		

		/**
		* Clean up.
		*/
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}

		}	// finalize
		
		/**
		* Access the event code for this edit.
		* @return The event code for this edit.
		*/
		protected String getEvent() { return m_event; }
		
		/**
		* Set the event code for this edit.
		* @param event The event code for this edit.
		*/
		protected void setEvent(String event) { m_event = event; }

		/**
		* Access the resource's properties for modification
		* @return The resource's properties.
		*/
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;

		}	// getPropertiesEdit

		/**
		* Enable editing.
		*/
		protected void activate()
		{
			m_active = true;

		}	// activate
		
		/**
		* Check to see if the edit is still active, or has already been closed.
		* @return true if the edit is active, false if it's been closed.
		*/
		public boolean isActiveEdit()
		{
			return m_active;

		}	// isActiveEdit

		/**
		* Close the edit object - it cannot be used after this.
		*/
		protected void closeEdit()
		{
			m_active = false;

		}	// closeEdit

		/**
		* Set the code for this BlockGrantGroup.
		* @param code The BlockGrantGroup code.
		*/
		public void setCode(String code)
		{
			m_code = code;
		}
		
		/**
		* Set the description for this BlockGrantGroup.
		* @param description The BlockGrantGroup description.
		* */
		public void setDescription(String description)
		{
			m_description = description;
		}
		
		/**
		* Set the description for this BlockGrantGroup.
		* * @param code The BlockGrantGroup code.
		* @param description The BlockGrantGroup description.
		* */
		public void addFieldOfStudy(String code, String description)
		{
			m_fieldsOfStudy.put(code, description);
		}
		
		/**
		* Set the description for this BlockGrantGroup.
		* @param code The code of the BlockGrantGroup to remove.
		* */
		public void removeFieldOfStudy(String code)
		{
			m_schoolGroups.remove(code);
			m_fieldsOfStudy.remove(code);
		}
		
		/*******************************************************************************
		* SessionStateBindingListener implementation
		*******************************************************************************/
	
		/**
		* Accept notification that this object has been bound as a SessionState attribute.
		* @param sessionStateKey The id of the session state which holds the attribute.
		* @param attributeName The id of the attribute to which this object is now the value.
		*/
		public void valueBound(String sessionStateKey, String attributeName) {}
	
		/**
		* Accept notification that this object has been removed from a SessionState attribute.
		* @param sessionStateKey The id of the session state which held the attribute.
		* @param attributeName The id of the attribute to which this object was the value.
		*/
		public void valueUnbound(String sessionStateKey, String attributeName)
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
	
		}	// valueUnbound

		
	}//BaseBlockGrantGroupEdit
		
		
	/*******************************************************************************
	* Dissertation implementation
	*******************************************************************************/

	public class BaseDissertation
		implements Dissertation
	{
		/** The Dissertation id. */
		protected String m_id;
		
		/** The properties. */
		protected ResourcePropertiesEdit m_properties;
		
		/** The DissertationStep references, keyed by display order, starting with 1. */
		protected Hashtable m_orderedSteps;
		
		/** The List objects containing DissertationStep references, which are
			the prerequisites added to School steps by department administrators for
			their department only.  Keyed by DissertationStep reference. */
		protected Hashtable m_schoolStepPrereqs;
		
		/** The site id. */
		protected String m_site;
		
		/** The time this object was last updated. */
		protected Time m_timeLastModified;
		
		/** The type of dissertation. */
		protected String m_type;

		/**
		* Constructor.
		* @param dissertationId The Dissertation id.
		* @param site The site id.
		*/
		public BaseDissertation(String dissertationId, String site)
		{
			m_id = dissertationId;
			m_site = site;
			m_type = "";
			m_orderedSteps = new Hashtable();
			m_schoolStepPrereqs = new Hashtable();
			m_properties = new BaseResourcePropertiesEdit();
			addLiveProperties(m_properties);
			
		}//BaseDissertation
		
		/**
		* Copy constructor.
		* @param dissertation The Dissertation to copy.
		*/
		public BaseDissertation(Dissertation dissertation)
		{
			setAll(dissertation);
		}

		/**
		* Construct from an existing definition, in xml.
		* @param el The Dissertation in XML in a DOM element.
		*/
		public BaseDissertation(Element el)
		{
			this("", "");
			
			String keyString = null;
			String valueString = null;
			List schoolPrereqs = null;

			m_id  = el.getAttribute("id");
			m_site = el.getAttribute("site");
			m_type = el.getAttribute("type");
			m_timeLastModified = getTimeObject(el.getAttribute("lastmod"));

			
				// READ THE CHILD ELEMENTS
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element)child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					m_properties = new BaseResourcePropertiesEdit(element);
				}
				else if(element.getTagName().equals("orderedsteps"))
				{
					NodeList osChildren = element.getChildNodes();
					final int osLength = osChildren.getLength();
					Node osChild = null;
					Element osElement = null;
					for(int x = 0; x < osLength; x++)
					{
						osChild = osChildren.item(x);
						if (osChild.getNodeType() != Node.ELEMENT_NODE) continue;
						osElement = (Element)osChild;
						if(osElement.getTagName().equals("order"))
						{
							keyString = osElement.getAttribute("ordernum");
							valueString = osElement.getAttribute("stepreference");
							if((keyString != null) && (valueString != null))
							{
								m_orderedSteps.put(keyString, valueString);
							}
						}
					}
				}
				else if(element.getTagName().equals("schoolstepprereqs"))
				{
					keyString = element.getAttribute("stepreference");
					schoolPrereqs = new Vector();
					NodeList spChildren = element.getChildNodes();
					final int spLength = spChildren.getLength();
					Node spChild = null;
					Element spElement = null;
					for(int x = 0; x < spLength; x++)
					{
						spChild = spChildren.item(x);
						if (spChild.getNodeType() != Node.ELEMENT_NODE) continue;
						spElement = (Element)spChild;
						if(spElement.getTagName().equals("prereq"))
						{
							valueString = spElement.getAttribute("prereqstepreference");
							if(valueString != null)
							{
								schoolPrereqs.add(valueString);
							}
						}
					}
					if(keyString != null)
						m_schoolStepPrereqs.put(keyString, schoolPrereqs);
				}
			}

		}// storage constructor
		
		/**
		* Deep copy of this object.
		* @param d - The Disseration object to be copied.
		*/
		protected void setAll(Dissertation d)
		{
			m_id = d.getId();
			m_orderedSteps = d.getOrderedSteps();
			m_schoolStepPrereqs = d.getSchoolStepPrereqs();
			m_site = d.getSite();
			m_timeLastModified = d.getTimeLastModified();
			m_type = d.getType();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(d.getProperties());

		}   // setAll

		/**
		* Serialize the resource into XML, adding an element to the doc under the top of the stack element.
		* @param doc The DOM doc to contain the XML (or null for a string return).
		* @param stack The DOM elements, the top of which is the containing element of the new "resource" element.
		* @return The newly added element.
		*/
		public Element toXml(Document doc, Stack stack)
		{
			Element dissertation = doc.createElement("dissertation");
			
			if(stack.isEmpty())
			{
				doc.appendChild(dissertation);
			}
			else
			{
				((Element)stack.peek()).appendChild(dissertation);
			}
			
			stack.push(dissertation);
			
			
			List schoolPrereqs = null;
			String itemString = null;
			String keyString = null;
			Enumeration keys = null;
			
			dissertation.setAttribute("id", m_id);
			dissertation.setAttribute("site", m_site);
			dissertation.setAttribute("lastmod", getTimeString(m_timeLastModified));
			dissertation.setAttribute("type", m_type);

			// SAVE THE ORDERED STEPS
			Element order = null;
			Element orderedSteps = doc.createElement("orderedsteps");
			try
			{
				if(m_orderedSteps!=null && m_orderedSteps.size()>0)
				{
					for(int x = 1; x < (m_orderedSteps.size()+1); x++)
					{
						order = doc.createElement("order");
						keyString = "" + x;
						order.setAttribute("ordernum", keyString);
						itemString = (String)m_orderedSteps.get(keyString);
						if(itemString == null)
							itemString = "";
						order.setAttribute("stepreference", itemString);
						orderedSteps.appendChild(order);
					}
				dissertation.appendChild(orderedSteps);
				}
			}
			catch(Exception e)
			{
				m_logger.warn("BaseDissertationService.BaseDissertation.toXml m_orderedSteps " + e);
			}
			
			// SAVE THE SCHOOL STEP PREREQS
			try
			{
				Element schoolStepPrereqs = null;
				Element prereq = null;
				if(m_schoolStepPrereqs!=null && m_schoolStepPrereqs.size()>0)
				{
					keys = m_schoolStepPrereqs.keys();
					while(keys.hasMoreElements())
					{
						schoolStepPrereqs = doc.createElement("schoolstepprereqs");
						keyString = (String)keys.nextElement();
						schoolStepPrereqs.setAttribute("stepreference", keyString);
						schoolPrereqs = (List)m_schoolStepPrereqs.get(keyString);
						for(int x = 0; x < schoolPrereqs.size(); x++)
						{
							prereq = doc.createElement("prereq");
							prereq.setAttribute("prereqstepreference", (String) schoolPrereqs.get(x));
							schoolStepPrereqs.appendChild(prereq);
						}
						dissertation.appendChild(schoolStepPrereqs);
					}
				}
			}
			catch(Exception e)
			{
				m_logger.warn("BaseDissertationService.BaseDissertation.toXml m_schoolStepPrereqs " + e);
			}
			// SAVE THE PROPERTIES
			m_properties.toXml(doc, stack);
			stack.pop();

			return dissertation;

		}//toXML

		
		/*******************************************************************************
		* Reference Implementation
		*******************************************************************************/
		
		/**
		* Access the Dissertation id.
		* @return The Dissertation id string.
		*/
		public String getId()
		{
			return m_id;
		}
		
		/**
		* Access the URL which can be used to access the resource.
		* @return The URL which can be used to access the resource.
		*/
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}

		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The the internal reference which can be used to access the resource from within the system.
		*/
		public String getReference()
		{
			return dissertationReference(m_site, m_id);
		}

		/**
		* Access the resources's properties.
		* @return The resources's properties.
		*/
		public ResourceProperties getProperties()
		{
			return m_properties;

		}   // getProperties

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		* Access the reference of the DissertationStep by display order.
		* @param order The order number as a String.
		* @return The reference of the DissertationStep, or null if there is no match.
		*/
		public String getStepReferenceByOrder(String order)
		{
			if(order == null)
				return null;
			else
				return (String)m_orderedSteps.get(order);
		}

		/**
		* Access the Hashtable containing the DissertationSteps.
		* @return The DissertationSteps keyed by display order.
		*/
		public Hashtable getOrderedSteps()
		{
			return m_orderedSteps;
		}
		
		/**
		* Access the display order for a DissertationStep.
		* @param stepReference The reference of the DissertationStep.
		* @return The display order number as a String.
		*/
		public String getOrderForStep(String stepReference)
		{
			String retVal = null;
			if(stepReference != null)
			{
				String key = null;
				String value = null;
				Enumeration keys = m_orderedSteps.keys();
				while(keys.hasMoreElements())
				{
					key = (String)keys.nextElement();
					value = (String)m_orderedSteps.get(key);
					if(stepReference.equals(value))
						retVal = key;
				}
			}
			return retVal;
		}
		
		/**
		* Access the site id for the Dissertation's site.
		* @return the site id.
		*/
		public String getSite()
		{
			return m_site;
		}
		
		/**
		* Access the references of the prerequisite steps for a School step.
		* @param schoolStepReference The reference of the step.
		* @return List containing the prerequisite step references.
		*/
		public List getSchoolPrereqs(String schoolStepReference)
		{
			List retVal = null;
			retVal = (List) m_schoolStepPrereqs.get(schoolStepReference);
			if(retVal == null)
				retVal = new Vector();
			return retVal;
		}

		/**
		* Access the order numbers of the prerequisites in a comma-delimited string for display.
		* @param step The DissertationStep.
		* @return The prerequisite steps display string.
		*/
		public String getPrerequisiteStepsDisplayString(DissertationStep step)
		{
			StringBuffer retVal = new StringBuffer();
			
			if(step != null)
			{
				String keyString = "";
				String stepReference = "";
				boolean firstAdded = true;
				boolean addToString = false;
				List deptAddedPrereqs = (List) m_schoolStepPrereqs.get(step.getReference());
				for(int x = 1; x < (m_orderedSteps.size()+1); x++)
				{
					addToString = false;
					keyString = "" + x;
					stepReference = (String)m_orderedSteps.get(keyString);
					if(step.hasPrerequisiteStep(stepReference))
					{
						addToString = true;
					}
					else
					{
						if(deptAddedPrereqs != null)
						{
							if(deptAddedPrereqs.contains(stepReference))
							{
								addToString = true;
							}
						}
					}
					
					if(addToString)
					{
						if(firstAdded)
						{
							retVal.append(keyString);
							firstAdded = false;
						}
						else
							retVal.append(", " + keyString);
					}
				}
			}
			return retVal.toString();
		}

		/**
		* A snapshot of this Dissertation's structure is used to create the steps in the CandidatePath.
		* This method is used in the creation of new CandidatePath objects.  This Dissertation becomes
		* the CandidatePath's parent Dissertation.
		* @param candidatePath The CandidatePath to initialize.
		* @param siteString The site id of the CandidatePath.
		*/
		public void initializeCandidatePath(CandidatePathEdit candidatePath, String siteString)
		{
			Hashtable stepToStatus = new Hashtable();
			List stepPrereqs = null;
			List statusPrereqs = null;
			List schoolPrereqStatus = null;
			List schoolPrereqSteps = null;
			String keyString = null;
			String stepId = null;
			String stepReference = null;
			String statusId = null;
			String statusReference = null;
			String tempStatusReference = null;
			DissertationStep aStep = null;
			StepStatusEdit aStatus = null;
			Hashtable orderedStatus = new Hashtable();
			Hashtable schoolPrereqs = new Hashtable();
			StepStatusEdit stepStatus = null;
			Enumeration keys = null;
	
			// TRANSFER THE ORDERED STEPS DATA
			boolean oardStep = false;
			for(int x = 1; x < (m_orderedSteps.size()+1); x++)
			{
				oardStep = false;
				keyString = "" + x;
				try
				{
					stepReference = (String)m_orderedSteps.get(keyString);
					stepId = stepId(stepReference);
					aStep = m_stepStorage.get(stepId);
					stepStatus = addStepStatusFromListener(siteString, aStep, oardStep);
					orderedStatus.put(keyString, stepStatus.getReference());
					stepToStatus.put(stepReference, stepStatus.getReference());
					commitEdit(stepStatus);
				}
				catch(Exception e)
				{
					m_logger.warn("DISSERTATION : BASE SERVICE : BASE DISS : initializeCandidatePath : EXCEPTION : " + e);
				}
			}
			candidatePath.setOrderedStatus(orderedStatus);
			
			// TRANSFER THE PREREQUISITE STEPS DATA FOR INDIVIDUAL STEPS
			keys = m_orderedSteps.keys();
			while(keys.hasMoreElements())
			{
				statusPrereqs = new Vector();
				keyString = (String)keys.nextElement();
				try
				{
					schoolPrereqStatus = new Vector();
					stepReference = (String)m_orderedSteps.get(keyString);
					stepId = stepId(stepReference);
					aStep = m_stepStorage.get(stepId);
					statusReference = candidatePath.getStatusReferenceByOrder(keyString);
					schoolPrereqSteps = getSchoolPrereqs(aStep.getReference());
					for(int y = 0; y < schoolPrereqSteps.size(); y++)
					{
						tempStatusReference = (String)stepToStatus.get(schoolPrereqSteps.get(y));
						schoolPrereqStatus.add(tempStatusReference);
					}
					if(schoolPrereqStatus.size() > 0)
						schoolPrereqs.put(statusReference, schoolPrereqStatus);

					statusId = statusId(statusReference);
					aStatus = m_statusStorage.edit(statusId);
					
					stepPrereqs = aStep.getPrerequisiteStepReferences();
					for(int x = 0; x < stepPrereqs.size(); x++)
					{
						statusReference = (String)stepToStatus.get(stepPrereqs.get(x));
						statusPrereqs.add(statusReference);
					}
					aStatus.setPrereqs(statusPrereqs);
					commitEdit(aStatus);
				}
				catch(Exception e)
				{
					m_logger.warn("DISSERTATION : BASE SERVICE : BASE DISS : initializeCandidatePath : EXCEPTION : " + e);
				}
			}
			
			candidatePath.setSchoolPrereqs(schoolPrereqs);
			candidatePath.setSite(siteString);			
		}

		/**
		* Access the creator of this Dissertation.
		* @return The user id of the creator.
		*/
		public String getCreator()
		{
			return m_properties.getProperty(ResourceProperties.PROP_CREATOR);
		}
		
		/**
		* Access the user who last modified this Disseration.
		* @return the user id the the author who last modified the Dissertation.
		*/
		public String getAuthorLastModified()
		{
			return m_properties.getProperty(ResourceProperties.PROP_MODIFIED_BY);
		}
		
		/**
		* Access the Time that this Dissertation was last modified.
		* @return the Time last modified.
		*/
		public Time getTimeLastModified()
		{
			return m_timeLastModified;
		}
		
		/**
		* Access the type of this Dissertation.
		* @return the String type.
		*/
		public String getType()
		{
			return m_type;
		}
		
		/**
		* Access the prerequisites added by department administrators to School steps for their department.
		* @return the Hashtable of Lists containing the references of prerequisite steps, keyed by DissertationStep reference.
		*/
		public Hashtable getSchoolStepPrereqs()
		{
			return m_schoolStepPrereqs;
		}

		/**
		* Are these objects equal?  If they are both Dissertation objects, and they have
		* matching id's, they are.
		* @return true if they are equal, false if not.
		*/
		public boolean equals(Object obj)
		{
			if (!(obj instanceof Dissertation)) return false;
			return ((Dissertation)obj).getId().equals(getId());

		}   // equals

		/**
		* Make a hash code that reflects the equals() logic as well.
		* We want two objects, even if different instances, if they have the same id to hash the same.
		*/
		public int hashCode()
		{
			return getId().hashCode();

		}	// hashCode

		/**
		* Compare this object with the specified object for order.
		* @return A negative integer, zero, or a positive integer as this object is
		* less than, equal to, or greater than the specified object.
		*/
		public int compareTo(Object obj)
		{
			if (!(obj instanceof Dissertation)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their sort names
			int compare = getSite().compareTo(((Dissertation)obj).getSite());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((Dissertation)obj).getId());
			}

			return compare;

		}	// compareTo
		
	}//BaseDissertation



	public class BaseDissertationEdit
		extends BaseDissertation
		implements DissertationEdit, SessionStateBindingListener
	{
		/** The event code for the edit. */
		protected String m_event = null;
		
		/** Active flag. */
		protected boolean m_active;

		/**
		* Constructor.
		* @param dissertationId The Dissertation id.
		* @param site The site id.
		*/
		public BaseDissertationEdit(String dissertationId, String site)
		{
			super(dissertationId, site);
		}
		
		/**
		* Copy constructor.
		* @param d The Dissertation to be copied.
		*/
		public BaseDissertationEdit(Dissertation d)
		{
			super(d);
		}

		/**
		* Construct from an existing definition, in xml.
		* @param el The DissertationEdit in XML in a DOM element.
		*/
		public BaseDissertationEdit(Element el)
		{
			super(el);
		}// storage constructor

		/**
		* Clean up.
		*/
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}

		}	// finalize
		
		/**
		* Set the DissertationSteps for this Dissertation.
		* @param table The Hashtable of DissertationStep references, keyed by display order as a String.
		*/
		public void setOrderedSteps(Hashtable table)
		{
			if(table != null)
				m_orderedSteps = table;
		}

		/**
		* Set the site for this Dissertation.
		* @param site The site id.
		*/
		public void setSite(String site)
		{
			m_site = site;
		}
		
		/**
		* Set the pedagogical type for this Dissertation.
		* @param type The dissertation steps type.
		*/
		public void setType(String type)
		{
			m_type = type;
		}

		/**
		* Set the prerequisites for a School step.
		* @param schoolStepReference The reference of the school step.
		* @param prereqs The List of DissertationStep references.
		*/
		public void setSchoolPrereqs(String schoolStepReference, List prereqs)
		{
			if((schoolStepReference != null) && (prereqs != null))
				m_schoolStepPrereqs.put(schoolStepReference, prereqs);
		}

		/**
		* Add a prerequisite to a School step.
		* @param schoolStepReference The reference of the School step to which an prerequisite is to be added.
		* @param prereqStepReference The reference of the prerequisite step to be added.
		*/
		public void addSchoolPrereq(String schoolStepReference, String prereqStepReference)
		{
			List prereqs = null;
			if((schoolStepReference != null) && (prereqStepReference != null))
			{
				prereqs = (List) m_schoolStepPrereqs.get(schoolStepReference);
				if(prereqs == null)
					prereqs = new Vector();
				
				if(!prereqs.contains(prereqStepReference))
				{
					prereqs.add(prereqStepReference);
				}
				m_schoolStepPrereqs.put(schoolStepReference, prereqs);
			}
		}

		
		/**
		* Remove a prerequisite from a School step.
		* @param schoolStepReference The reference of the School step from which an prerequisite is to be removed.
		* @param prereqStepReference The reference of the prerequisite step to be removed.
		*/
		public void removeSchoolPrereq(String schoolStepReference, String prereqStepReference)
		{
			List prereqs = null;
			if((schoolStepReference != null) && (prereqStepReference != null))
			{
				prereqs = (List) m_schoolStepPrereqs.get(schoolStepReference);
				if(prereqs == null)
					prereqs = new Vector();

				prereqs.remove(prereqStepReference);
			}
		}

		/**
		* Remove a DissertationStep from this Dissertation.
		* @param stepToRemoveReference The reference of the DissertationStep to be removed.
		*/
		public void removeStep(String stepToRemoveReference)
		{
			if(stepToRemoveReference != null)
			{
				Hashtable newOrder = new Hashtable();
				List schoolStepPrereqs = null;
				String keyString = null;
				String stepReference = null;
				boolean foundIt = false;
				for(int y = 1; y < (m_orderedSteps.size()+1); y++)
				{
					keyString = "" + y;
					stepReference = (String)m_orderedSteps.get(keyString);
					
					// CHECK THE SCHOOL-STEP PREREQUISITES
					schoolStepPrereqs = (List)m_schoolStepPrereqs.get(stepReference);
					if(schoolStepPrereqs != null)
					{
						schoolStepPrereqs.remove(stepToRemoveReference);
					}
					
					// NOW REMOVE THE STEP FROM ORDERED STEPS
					if(stepReference.equals(stepToRemoveReference))
					{
						foundIt = true;
					}
					else
					{
						if(foundIt)
						{
							keyString = "" + (y-1);
							newOrder.put(keyString, stepReference);
						}
						else
						{
							newOrder.put(keyString, stepReference);
						}
					}
				}
				m_orderedSteps = newOrder;
			}
		}

		
		/**
		* Add a DissertationStep to this Dissertation.
		* @param step The DissertationStep to be added.
		* @param previousstepReference The reference of the DissertationStep after which the step will be added.
		*/
		public void addStep(DissertationStep step, String previousstepReference)
		{
			if(step != null)
			{
				int position = 0;

				// NOW LOOK FOR THE PREVIOUS STEP ID
				if(previousstepReference != null)
				{
					try
					{
							// FIRST CHECK TO SEE IF ALL OF THIS STEPS PREREQUSISTES EXIST IN THIS DISSERTATION
						List prereqs = step.getPrerequisiteStepReferences();
						for(int x = 0; x < prereqs.size(); x++)
						{
							if(getOrderForStep((String) prereqs.get(x)) == null)
							{
								DissertationStepEdit stepEdit = editDissertationStep(step.getReference());
								stepEdit.removePrerequisiteStep((String) prereqs.get(x));
								commitEdit(stepEdit);
							}
						}

						if(previousstepReference.equals("start"))
						{
							addToOrderedSteps(step, 1);
						}
						else if(m_orderedSteps.contains(previousstepReference))
						{
								// FIND THE KEY FOR THE PREVIOUS STEP
							String aKey = null;
							String aValue = null;
							String previousStepKey = null;
							Enumeration keys = m_orderedSteps.keys();
							while(keys.hasMoreElements())
							{
								aKey = (String)keys.nextElement();
								aValue = (String)m_orderedSteps.get(aKey);
								if(aValue.equals(previousstepReference))
								{
									previousStepKey = aKey;
								}
							}

							position = Integer.parseInt(previousStepKey);
							addToOrderedSteps(step, (position+1));
						}
						else
						{
								// IF THE PREVIOUS STEP DOES NOT EXIST - PUT THE STEP AT THE END
							int stepPos = (m_orderedSteps.size() + 1);
							addToOrderedSteps(step, stepPos);
						}
					}
					catch(Exception e)
					{
						m_logger.warn("DISSERTATION : BASE SERVICE : BASE DISSERTATION : ADD STEP : EXCEPTION : "+ e);
					}
				}
			}
		}
		
		
		/**
		* Move a DissertationStep within this Dissertation.
		* @param stepToMoveRef The reference of the DissertationStep to be moved.
		* @param location The reference of the step after which the DissertationStep is to be moved.
		*/
		public void moveStep(String stepToMoveRef, String location)
		{
			Hashtable newOrder = new Hashtable();
			String keyString = null;
			String stepReference = null;
			
				// FIND THE ORDER NUMBERS FOR THE STEP TO MOVE AND LOCATION
			int oldPosition = -1;
			int newPosition = -1;
			String aKey = null;
			String aValue = null;
			Enumeration keys = m_orderedSteps.keys();
			if(location.equals("start"))
				newPosition = 1;

			while(keys.hasMoreElements())
			{
				aKey = (String)keys.nextElement();
				aValue = (String)m_orderedSteps.get(aKey);
				if(aValue.equals(stepToMoveRef))
				{
					try
					{
						oldPosition = Integer.parseInt(aKey);
					}
					catch(Exception e){}
				}
				if(aValue.equals(location))
				{
					try
					{
						newPosition = Integer.parseInt(aKey);
						newPosition++;
					}
					catch(Exception e){}
				}
			}

			if((oldPosition != -1) && (newPosition != -1))
			{
				if(newPosition > oldPosition)     // MOVING UP
				{
					newPosition--;
					for(int x = 1; x < (m_orderedSteps.size()+1); x++)
					{
						if((x < oldPosition) || (x > newPosition))
						{
							keyString = "" + x;
							stepReference = (String)m_orderedSteps.get(keyString);
							newOrder.put(keyString, stepReference);
						}
						else if(x == newPosition)
						{
							keyString = "" + x;
							newOrder.put(keyString, stepToMoveRef);
						}
						else
						{
							keyString = "" + (x+1);
							stepReference = (String)m_orderedSteps.get(keyString);
							keyString = "" + x;
							newOrder.put(keyString, stepReference);
						}
					}
					m_orderedSteps = newOrder;
				}
				else if(oldPosition > newPosition)		// MOVING DOWN
				{
					for(int x = 1; x < (m_orderedSteps.size()+1); x++)
					{
						if((x < newPosition) || (x > oldPosition))
						{
							keyString = "" + x;
							stepReference = (String)m_orderedSteps.get(keyString);
							newOrder.put(keyString, stepReference);
						}
						else if(x == newPosition)
						{
							keyString = "" + x;
							newOrder.put(keyString, stepToMoveRef);
						}
						else
						{
							keyString = "" + (x-1);
							stepReference = (String)m_orderedSteps.get(keyString);
							keyString = "" + x;
							newOrder.put(keyString, stepReference);
						}
					}
					m_orderedSteps = newOrder;
				}
				for(int y = 1; y < (newOrder.size()+1); y++)
				{
					keyString = "" + y;
					stepReference = (String)newOrder.get(keyString);
				}
			}
		}
		
		
		/**
		* Add a DissertationStep to this Dissertation.
		* @param newStep The DissertationStep to be added.
		* @param locationInt The display order number of this step.
		*/
		protected void addToOrderedSteps(DissertationStep newStep, int locationInt)
		{
			Hashtable newOrder = new Hashtable();
			String keyString = null;
			String stepReference = null;
			for(int x = 1; x < (m_orderedSteps.size()+2); x++)
			{
				if(locationInt == x)
				{
					keyString = "" + x;
					newOrder.put(keyString, newStep.getReference());
				}
				else if(locationInt < x)
				{
					keyString = "" + (x-1);
					stepReference = (String)m_orderedSteps.get(keyString);
					keyString = "" + (x);
					newOrder.put(keyString, stepReference);
				}
				else
				{
					keyString = "" + x;
					stepReference = (String)m_orderedSteps.get(keyString);
					newOrder.put(keyString, stepReference);
				}
			}
			m_orderedSteps = newOrder;
		}

		/**
		* Deep copy.
		* @param dissertation The dissertation object to be copied.
		*/
		protected void set(Dissertation dissertation)
		{
			setAll(dissertation);

		}   // set

		/**
		* Access the event code for this edit.
		* @return The event code for this edit.
		*/
		protected String getEvent() { return m_event; }
	
		/**
		* Set the event code for this edit.
		* @param event The event code for this edit.
		*/
		protected void setEvent(String event) { m_event = event; }

		/**
		* Access the resource's properties for modification.
		* @return The resource's properties.
		*/
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;
		}
		
		/**
		* Enable editing.
		*/
		protected void activate()
		{
			m_active = true;

		}	// activate

		/**
		* Check to see if the edit is still active, or has already been closed.
		* @return true if the edit is active, false if it's been closed.
		*/
		public boolean isActiveEdit()
		{
			return m_active;

		}	// isActiveEdit

		/**
		* Close the edit object - it cannot be used after this.
		*/
		protected void closeEdit()
		{
			m_active = false;

		}	// closeEdit


		/*******************************************************************************
		* SessionStateBindingListener implementation
		*******************************************************************************/
	
		/**
		* Accept notification that this object has been bound as a SessionState attribute.
		* @param sessionStateKey The id of the session state which holds the attribute.
		* @param attributeName The id of the attribute to which this object is now the value.
		*/
		public void valueBound(String sessionStateKey, String attributeName) {}
	
		/**
		* Accept notification that this object has been removed from a SessionState attribute.
		* @param sessionStateKey The id of the session state which held the attribute.
		* @param attributeName The id of the attribute to which this object was the value.
		*/
		public void valueUnbound(String sessionStateKey, String attributeName)
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
	
		}	// valueUnbound
		
	}//BaseDissertationEdit

	
	
	/*******************************************************************************
	* DissertationStep implementation
	*******************************************************************************/

	public class BaseDissertationStep
		implements DissertationStep
	{
		/** The DissertationStep id. */
		protected String m_id;
		
		/** The site id. */
		protected String m_site;
		
		/** The instructions for the step. */
		protected String m_instructionsText;
		
		/** The id for auto-validation from the Rackham database. */
		protected String m_autoValidationId;
		
		/** The time the DissertationStep was last modified. */
		protected Time m_timeLastModified;
		
		/** Type of permissions for marking the step as completed.
			See the DissertationStep interface for types. */
		protected String m_validationType;
		
		/** The id for the step's checklist section. 
			See the DissertationService for section heads.
		 */
		protected String m_section;
		
		/** The references of the prerequisites for this step. */
		protected List m_prerequisiteSteps;
		
		/** The properties. */
		protected BaseResourcePropertiesEdit m_properties;
		


		/**
		* Constructor.
		* @param stepId The DissertationStep id.
		* @param site The site id.
		*/
		public BaseDissertationStep(String stepId, String site)
		{
			m_id = stepId;
			m_site = site;
			m_instructionsText = "";
			m_autoValidationId = "";
			m_section = "0";
			m_timeLastModified = TimeService.newTime();
			m_validationType = "0";
			m_prerequisiteSteps = new Vector();
			m_properties = new BaseResourcePropertiesEdit();
			addLiveProperties(m_properties);
		}
		
		/**
		* Copy constructor.
		* @param step The DissertationStep to copy.
		*/
		public BaseDissertationStep(DissertationStep step)
		{
			setAll(step);

		}	// BaseDissertationStep
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The DissertationStep in XML in a DOM element.
		*/
		public BaseDissertationStep(Element el)
		{
			String valueString = null;
			m_properties = new BaseResourcePropertiesEdit();
			m_prerequisiteSteps = new Vector();

			m_id = el.getAttribute("id");
			m_site = el.getAttribute("site");
			m_autoValidationId = el.getAttribute("autovalid");
			if(el.getAttribute("section")!=null)
			{
				m_section = el.getAttribute("section");
			}
			else
			{
				m_section = "1";
			}
			m_timeLastModified = getTimeObject(el.getAttribute("timelastmod"));
			m_validationType = el.getAttribute("validationtype");

			m_instructionsText = Xml.decodeAttribute(el, "instructionstext");

				// READ THE CHILD ELEMENTS
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element)child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					// re-create properties
					m_properties = new BaseResourcePropertiesEdit(element);
				}
				else if(element.getTagName().equals("prereqsteps"))
				{
					NodeList psChildren = element.getChildNodes();
					final int psLength = psChildren.getLength();
					Node psChild = null;
					Element psElement = null;
					for(int x = 0; x < psLength; x++)
					{
						psChild = psChildren.item(x);
						if (psChild.getNodeType() != Node.ELEMENT_NODE) continue;
						psElement = (Element)psChild;
						if(psElement.getTagName().equals("prereq"))
						{
							valueString = psElement.getAttribute("prereqstepreference");
							if(valueString != null)
							{
								m_prerequisiteSteps.add(valueString);
							}
						}
					}
				}
				// old style of encoding
				else if(element.getTagName().equals("instructionstext"))
				{
					if ((element.getChildNodes() != null) && (element.getChildNodes().item(0) != null))
					{
						m_instructionsText = element.getChildNodes().item(0).getNodeValue();
					}
					if (m_instructionsText == null)
					{
						m_instructionsText = "";
					}
				}
			}

		}// storage constructor

		/**
		* Deep copy of this object.
		* @param step - The DisserationStep object to be copied.
		*/
		protected void setAll(DissertationStep step)
		{
			m_id = step.getId();
			m_site = step.getSite();
			m_instructionsText = step.getInstructionsText();
			m_autoValidationId = step.getAutoValidationId();
			m_timeLastModified = step.getTimeLastModified();
			m_validationType = step.getValidationType();
			m_section = step.getSection();
			m_prerequisiteSteps = step.getPrerequisiteStepReferences();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(step.getProperties());

		}   // setAll

		
		/**
		* Serialize the resource into XML, adding an element to the doc under the top of the stack element.
		* @param doc The DOM doc to contain the XML (or null for a string return).
		* @param stack The DOM elements, the top of which is the containing element of the new "resource" element.
		* @return The newly added element.
		*/
		public Element toXml(Document doc, Stack stack)
		{
			Element step = doc.createElement("step");
			
			if (stack.isEmpty())
			{
				doc.appendChild(step);
			}
			else
			{
				((Element)stack.peek()).appendChild(step);
			}
			
			stack.push(step);

			step.setAttribute("id", m_id);
			step.setAttribute("site", m_site);
			step.setAttribute("validationtype", m_validationType);
			step.setAttribute("section", m_section);
			step.setAttribute("autovalid", m_autoValidationId);
			
			
				// SAVE THE PREREQUISISTE STEPS
			Element prereq = null;
			Element prereqsteps = doc.createElement("prereqsteps");
			for(int x = 0; x < m_prerequisiteSteps.size(); x++)
			{
				prereq = doc.createElement("prereq");
				prereq.setAttribute("prereqstepreference", (String) m_prerequisiteSteps.get(x));
				prereqsteps.appendChild(prereq);
			}
			step.appendChild(prereqsteps);
			
			
				// SAVE THE INSTRUCTIONS TEXT
			Xml.encodeAttribute(step, "instructionstext", m_instructionsText);
			
				// SAVE THE PROPERTIES
			m_properties.toXml(doc, stack);

			stack.pop();
			
			return step;

		}//toXml

		
		/*******************************************************************************
		* Reference Implementation
		*******************************************************************************/
		
		/**
		* Access the DissertationStep id.
		* @return The DissertationStep id string.
		*/
		public String getId()
		{
			return m_id;
		}

		/**
		* Access the URL which can be used to access the resource.
		* @return The URL which can be used to access the resource.
		*/
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}
		
		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The the internal reference which can be used to access the resource from within the system.
		*/
		public String getReference()
		{
			return stepReference(m_site, m_id);
		}

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		* Access the resource's properties.
		* @return The resource's properties.
		*/
		public ResourceProperties getProperties()
		{
			return m_properties;
		}

		/**
		* Access the creator of this DissertationStep.
		* @return The user id of the creator.
		*/
		public String getCreator()
		{
			return m_properties.getProperty(ResourceProperties.PROP_CREATOR);
		}

		/**
		* Access the user who last modified this DisserationStep.
		* @return the user id the the author who last modified the DissertationStep.
		*/
		public String getAuthorLastModified()
		{
			return m_properties.getProperty(ResourceProperties.PROP_MODIFIED_BY);
		}
		
		/**
		* Access the Time that this DissertationStep was last modified.
		* @return the Time last modified.
		*/
		public Time getTimeLastModified()
		{
			return m_timeLastModified;
		}
		
		/**
		* Access the site id for the DissertationStep's site.
		* @return the site id.
		*/
		public String getSite()
		{
			return m_site;
		}
		
		/**
		* Access the section id for the DissertationStep's checklist section.
		* @return the section id.
		*/
		public String getSection()
		{
			return m_section;
		}

		/**
		* Access the instructions for the DissertationStep with html links for user-entered links.
		* @return the step's instructions with html links for user-entered links.
		*/
		public String getInstructions()
		{
			StringBuffer retVal = new StringBuffer();
			boolean goodSyntax = true;
			String tempString = null;
			String linkString = null;
			String linkTextString = null;
			String fullLinkString = null;
			int midIndex = -1;
			int endIndex = -1;
			int beginIndex = m_instructionsText.indexOf("http");

			if(beginIndex == -1)
			{
				retVal.append(m_instructionsText);
			}
			else
			{
				do
				{
					// FIRST ADD THE TEXT UP TO THE LINK TO THE BUFFER
					retVal.append(m_instructionsText.substring((endIndex+1), beginIndex));
					midIndex = m_instructionsText.indexOf("{", beginIndex);
					if(midIndex == -1)
			{
				// LINK IS NOT IN PROPER FORMAT - RETURN ORIGINAL STRING
				goodSyntax = false;
				beginIndex = -1;
			}
			else
			{
				// FIND THE END TAG
				endIndex = m_instructionsText.indexOf("}", midIndex);
				if(endIndex == -1)
				{
					goodSyntax = false;
					beginIndex = -1;
				}
				else
				{
					linkString = m_instructionsText.substring(beginIndex, midIndex);
					linkTextString = m_instructionsText.substring((midIndex+1), endIndex);
					fullLinkString = "<a href='" + linkString + "' target='_blank' " + ">" + linkTextString + "</a>";
					retVal.append(fullLinkString);
					beginIndex = m_instructionsText.indexOf("http", endIndex);
				}
			}
			
			if(beginIndex == -1)
			{
				tempString = m_instructionsText.substring((endIndex+1), m_instructionsText.length());
				retVal.append(tempString);
			}

		}while(beginIndex != -1);

			}//else

			if(!goodSyntax)
			{
				retVal = new StringBuffer();
				retVal.append(m_instructionsText);
			}

			return retVal.toString();

		}//getInstructions


		/**
		* Access the instructions as entered by the user.
		* @return the instructions as entered by the user.
		*/
		public String getInstructionsText()
		{
			return m_instructionsText;
		}

		/**
		* Access the instructions as entered by the user, with a maximum length of 80 characters for display in selects.
		* Remove links to save space in the select drop down list and prevent truncation from breaking an active link
		* @return the instructions as entered by the user, with a maximum length of 80 characters for display in selects.
		*/
		public String getShortInstructionsText()
		{
			StringBuffer buffer = new StringBuffer();
			String retVal = null;
			String tempString = null;
			String linkTextString = null;
			boolean goodSyntax = true;
			int midIndex = -1;
			int endIndex = -1;
			int beginIndex = m_instructionsText.indexOf("http");
			if(beginIndex == -1)
			{
				//NO LINKS - RETURNING ORIGINAL TEXT
				buffer.append(m_instructionsText);
			}
			else
			{
				do
				{
					// FIRST ADD THE TEXT UP TO THE LINK TO THE BUFFER
					buffer.append(m_instructionsText.substring((endIndex+1), beginIndex));
					
					//FIND THE BEGIN TAG
					midIndex = m_instructionsText.indexOf("{", beginIndex);
					if(midIndex == -1)
					{
						// LINK IS NOT IN PROPER FORMAT - RETURN ORIGINAL STRING
						goodSyntax = false;
						beginIndex = -1;
						// MISSING {
					}
					else
					{
						// FIND THE END TAG
						endIndex = m_instructionsText.indexOf("}", midIndex);
						if(endIndex == -1)
						{
							goodSyntax = false;
							// MISSING }
							beginIndex = -1;
						}
						else
						{
							// SYNTAX IS GOOD THIS PASS - KEEP LINK TEXT STRING
							linkTextString = m_instructionsText.substring((midIndex+1), endIndex);
							buffer.append(linkTextString);
							beginIndex = m_instructionsText.indexOf("http", endIndex);
						}
					}
					if(beginIndex == -1)
					{
						// NO MORE LINKS - ADDING REMAINING TEXT
						tempString = m_instructionsText.substring((endIndex+1), m_instructionsText.length());
						
						// ADDING REMAINDER OF TEXT
						buffer.append(tempString);
					}

				}while(beginIndex != -1);
				
			}//else
		
			if(!goodSyntax)
			{
				//BAD SYNTAX : RETURNING ORIGINAL STRING
				buffer = new StringBuffer();
				buffer.append(m_instructionsText);
			}

			//RESULT STRING
			if(buffer.length() > 80)
			{
				retVal = ((String)buffer.toString()).substring(0, 79) + " . . . ";
			}
			else
			{
				retVal = buffer.toString();
			}
			return retVal;
			
		}//getShortInstructionsText
		
		/**
		* Access the id used for Rackham auto-validation.
		* @return the auto-validation id.
		*/
		public String getAutoValidationId()
		{
			return m_autoValidationId;
		}
		
		/**
		* Access the prerequisites for this step.
		* @return A List containing the references of the prerequisites for this step.
		*/
		public List getPrerequisiteStepReferences()
		{
			return m_prerequisiteSteps;
		}
		
		/**
		* Access the prerequisites for this step.
		* @return A List containing the prerequisite DissertationStep objects.
		*/
		public List getPrerequisiteSteps()
		{
			List retVal = new Vector();
			DissertationStep tempStep = null;
			
			for(int x = 0; x < m_prerequisiteSteps.size(); x++)
			{
				try
				{
					tempStep = getDissertationStep((String) m_prerequisiteSteps.get(x));
					if(tempStep != null)
						retVal.add(tempStep);
				}
				catch(Exception e){}
			}
			
			return retVal;
		}

		/**
		* See whether this step contains this prerequisite.
		* @return true if this step has this prerequisite, false otherwise.
		*/
		public boolean hasPrerequisiteStep(String stepReference)
		{
			return m_prerequisiteSteps.contains(stepReference);
		}
		
		/**
		* See whether this step has prerequisites.
		* @return true if the step has prerequisites, false otherwise.
		*/
		public boolean hasPrerequisites()
		{
			if(m_prerequisiteSteps.size() > 0)
				return true;
			else
				return false;
		}
		
		/**
		* Access the validation type of this step.
		* @return the type of permissions required to mark a step as completed.  See the DissertationStep interface.
		*/
		public String getValidationType()
		{
			return m_validationType;
		}
		
		/**
		* Access the validation type as a human-readable string.
		* @return The human-readable string corresonding to this step's validation type.
		*/
		public String getValidationTypeString()
		{
			String retVal = null;

			if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT))
				retVal = DissertationStep.STUDENT_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_CHAIR))
				retVal = DissertationStep.CHAIR_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE))
				retVal = DissertationStep.COMMITTEE_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT))
				retVal = DissertationStep.DEPARTMENT_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_SCHOOL))
				retVal = DissertationStep.SCHOOL_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR))
				retVal = DissertationStep.STUDENT_CHAIR_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE))
				retVal = DissertationStep.STUDENT_COMMITTEE_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT))
				retVal = DissertationStep.STUDENT_DEPARTMENT_VALIDATION_STRING;
			else if(m_validationType.equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_SCHOOL))
				retVal = DissertationStep.STUDENT_SCHOOL_VALIDATION_STRING;
			else
				retVal = "Unknown Validation Type";


			return retVal;
		}

		/**
		* Are these objects equal?  If they are both DissertationStep objects, and they have
		* matching id's, they are.
		* @return true if they are equal, false if not.
		*/
		public boolean equals(Object obj)
		{
			if (!(obj instanceof DissertationStep)) return false;
			return ((DissertationStep)obj).getId().equals(getId());

		}   // equals

		/**
		* Make a hash code that reflects the equals() logic as well.
		* We want two objects, even if different instances, if they have the same id to hash the same.
		*/
		public int hashCode()
		{
			return getId().hashCode();

		}	// hashCode

		/**
		* Compare this object with the specified object for order.
		* @return A negative integer, zero, or a positive integer as this object is
		* less than, equal to, or greater than the specified object.
		*/
		public int compareTo(Object obj)
		{
			if (!(obj instanceof DissertationStep)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their sort names
			int compare = getInstructionsText().compareTo(((DissertationStep)obj).getInstructionsText());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((DissertationStep)obj).getId());
			}

			return compare;

		}	// compareTo

	}//BaseDissertationStep

	

	/*******************************************************************************
	* DissertationStepEdit implementation
	*******************************************************************************/	
	
	public class BaseDissertationStepEdit
		extends BaseDissertationStep
		implements DissertationStepEdit, SessionStateBindingListener
	{
		/** The event code for the edit. */
		protected String m_event = null;
		
		/** Active flag. */
		protected boolean m_active = false;


		/**
		* Constructor.
		* @param id The DissertationStepEdit id.
		* @param site The site id.
		*/
		public BaseDissertationStepEdit(String id, String site)
		{
			super(id, site);

		}   // BaseDissertationStepEdit

		
		/**
		* Copy constructor.
		* @param step The DissertationStepEdit to be copied.
		*/
		public BaseDissertationStepEdit(DissertationStep step)
		{
			super(step);

		}	// BaseDissertationStepEdit
		
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The DissertationStepEdit in XML in a DOM element.
		*/
		public BaseDissertationStepEdit(Element el)
		{
			super(el);

		}	// BaseDissertationStepEdit


		/**
		* Clean up.
		*/
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}

		}	// finalize
		
		/**
		* Set the site for this DissertationStepEdit.
		* @param site The site id.
		*/
		public void setSite(String site)
		{
			m_site = site;
		}
		
		/**
		* Set the checklist section for this DissertationStepEdit.
		* @param section The section id.
		*/
		public void setSection(String section)
		{
			m_section = section;
		}
		
		/**
		* Set the instructions for this DissertationStepEdit.
		* @param instructionsText The instructions.
		*/
		public void setInstructionsText(String instructionsText)
		{
			if(instructionsText != null)
				m_instructionsText = Validator.escapeHtmlFormattedText(instructionsText);
		}

		/**
		* Add a prerequisite to this step.
		* @param stepReference The reference for the prerequisite DissertationStep to be added.
		*/
		public void addPrerequisiteStep(String stepReference)
		{
			m_prerequisiteSteps.add(stepReference);
		}

		/**
		* Remove a prerequisite to this step.
		* @param stepReference The reference for the prerequisite DissertationStep to be removed.
		*/
		public void removePrerequisiteStep(String stepReference)
		{
			m_prerequisiteSteps.remove(stepReference);
		}
		
		/**
		* Set the validation type for this step.
		* @param type The type of permissions required to mark this step as completed.  See the DissertationStep interface for values.
		*/
		public void setValidationType(String type)
		{
			m_validationType = type;
		}

		/**
		* Set the auto-validation id for this step.
		* @param validId The id for auto-validation from the Rackham database.
		*/
		public void setAutoValidationId(String validId)
		{
			if(validId != null)
				m_autoValidationId = validId;
		}

		/**
		* Take all values from this object.
		* @param user The user object to take values from.
		*/
		protected void set(DissertationStep step)
		{
			setAll(step);

		}   // set

		/**
		* Access the event code for this edit.
		* @return The event code for this edit.
		*/
		protected String getEvent() { return m_event; }
	
		/**
		* Set the event code for this edit.
		* @param event The event code for this edit.
		*/
		protected void setEvent(String event) { m_event = event; }

		/**
		* Access the resource's properties for modification
		* @return The resource's properties.
		*/
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;

		}	// getPropertiesEdit

		/**
		* Enable editing.
		*/
		protected void activate()
		{
			m_active = true;

		}	// activate

		/**
		* Check to see if the edit is still active, or has already been closed.
		* @return true if the edit is active, false if it's been closed.
		*/
		public boolean isActiveEdit()
		{
			return m_active;

		}	// isActiveEdit

		/**
		* Close the edit object - it cannot be used after this.
		*/
		protected void closeEdit()
		{
			m_active = false;

		}	// closeEdit

		/*******************************************************************************
		* SessionStateBindingListener implementation
		*******************************************************************************/
	
		/**
		* Accept notification that this object has been bound as a SessionState attribute.
		* @param sessionStateKey The id of the session state which holds the attribute.
		* @param attributeName The id of the attribute to which this object is now the value.
		*/
		public void valueBound(String sessionStateKey, String attributeName) {}
	
		/**
		* Accept notification that this object has been removed from a SessionState attribute.
		* @param sessionStateKey The id of the session state which held the attribute.
		* @param attributeName The id of the attribute to which this object was the value.
		*/
		public void valueUnbound(String sessionStateKey, String attributeName)
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
	
		}	// valueUnbound


	}//BaseDissertationStepEdit


	
	/*******************************************************************************
	* CandidatePath implementation
	*******************************************************************************/	
	
	public class BaseCandidatePath
		implements CandidatePath
	{
		/** The CandidatePath id. */
		protected String m_id;
		
		/** The candidate user id. */
		protected String m_candidate;
		
		/** The candidate's advisor's user id. */
		protected String m_advisor;
		
		/** The site id. */
		protected String m_site;
		
		/** The parent department site id. */
		protected String m_parentSite;
		
		/** The alphabetical candidate chooser letter. */
		protected String m_sortLetter;
		
		/** The dissertation type set when created. */
		protected String m_type;
		
		/** The StepStatus references, keyed by display order, starting with 1. */
		protected Hashtable m_orderedStatus;
		
		/** The List objects containing StepStatus references, which are
			the prerequisites added to School steps by department administrators for
			their department only.  Keyed by StepStatus reference. */
		protected Hashtable m_schoolPrereqs;
		
		/** The properties. */
		protected ResourcePropertiesEdit m_properties;


		/**
		* Constructor.
		* @param pathId The CandidatePath id.
		* @param site The site id.
		*/
		public BaseCandidatePath(String pathId, String site)
		{
			m_id = pathId;
			m_site = site;
			m_advisor = "";
			m_sortLetter = "";
			m_parentSite = "";
			m_properties = new BaseResourcePropertiesEdit();
			m_orderedStatus = new Hashtable();
			m_schoolPrereqs = new Hashtable();
			String currentUser = SessionManager.getCurrentSessionUserId();
			if(currentUser != null)
			{
				m_candidate = currentUser;
			}
			else
			{
				m_candidate = "";
				m_logger.warn("In BaseDissertationService, BaseCandidatePath constructor : call to UsageSessionService.getSessionUserId() returns null.");
			}
		}
		
		/**
		* Copy constructor.
		* @param path The CandidatePath to copy.
		*/
		public BaseCandidatePath(CandidatePath path)
		{
			setAll(path);

		}	// BaseCandidatePath

		/**
		* Construct from an existing definition, in xml.
		* @param el The CandidatePath in XML in a DOM element.
		*/
		public BaseCandidatePath(Element el)
		{
			m_properties = new BaseResourcePropertiesEdit();
			String keyString = null;
			String valueString = null;
			List schoolPrereqs = null;
			m_orderedStatus = new Hashtable();
			m_schoolPrereqs = new Hashtable();

			m_id = el.getAttribute("id");
			m_candidate = el.getAttribute("candidate");
			m_advisor = el.getAttribute("advisor");
			m_site = el.getAttribute("site");
			m_parentSite = el.getAttribute("parentSite");
			m_sortLetter = el.getAttribute("sortLetter");
			m_type = el.getAttribute("type");
			
			/*
			if(el.getAttribute("sortLetter") != null)
			{
				char[] ch = el.getAttribute("sortLetter").toCharArray();
				if(ch.length > 0)
				{
					m_sortLetter = ch[0];
				}
			}
			*/

			// READ THE PROPERTIES
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element)child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					// re-create properties
					m_properties = new BaseResourcePropertiesEdit(element);
				}
				else if(element.getTagName().equals("orderedstatus"))
				{
					NodeList osChildren = element.getChildNodes();
					final int osLength = osChildren.getLength();
					Node osChild = null;
					Element osElement = null;
					for(int x = 0; x < osLength; x++)
					{
						osChild = osChildren.item(x);
						if (osChild.getNodeType() != Node.ELEMENT_NODE) continue;
						osElement = (Element)osChild;
						if(osElement.getTagName().equals("order"))
						{
							keyString = osElement.getAttribute("ordernum");
							valueString = osElement.getAttribute("statusid");
							if((keyString != null) && (valueString != null))
							{
								m_orderedStatus.put(keyString, valueString);
							}
						}
					}
				}
				else if(element.getTagName().equals("schoolstatusprereqs"))
				{
					keyString = element.getAttribute("statusid");
					schoolPrereqs = new Vector();
					NodeList spChildren = element.getChildNodes();
					final int spLength = spChildren.getLength();
					Node spChild = null;
					Element spElement = null;
					for(int x = 0; x < spLength; x++)
					{
						spChild = spChildren.item(x);
						if (spChild.getNodeType() != Node.ELEMENT_NODE) continue;
						spElement = (Element)spChild;
						if(spElement.getTagName().equals("prereq"))
						{
							valueString = spElement.getAttribute("prereqstatusreference");
							if(valueString != null)
							{
								schoolPrereqs.add(valueString);
							}
						}
					}
					if(keyString != null)
						m_schoolPrereqs.put(keyString, schoolPrereqs);

				}
			}
			
		}// storage constructor

		/**
		* Deep copy of this object.
		* @param path - The CandidatePath object to be copied.
		*/
		protected void setAll(CandidatePath path)
		{
			m_id = path.getId();
			m_candidate = path.getCandidate();
			m_site = path.getSite();
			m_parentSite = path.getParentSite();
			m_sortLetter = path.getSortLetter();
			m_type = path.getType();
			m_advisor = path.getAdvisor();
			m_orderedStatus = path.getOrderedStatus();
			m_schoolPrereqs = path.getSchoolPrereqs();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(path.getProperties());

		}   // setAll
		
		
		/**
		* Serialize the resource into XML, adding an element to the doc under the top of the stack element.
		* @param doc The DOM doc to contain the XML (or null for a string return).
		* @param stack The DOM elements, the top of which is the containing element of the new "resource" element.
		* @return The newly added element.
		*/
		public Element toXml(Document doc, Stack stack)
		{
			Element path = doc.createElement("path");
			
			if (stack.isEmpty())
			{
				doc.appendChild(path);
			}
			else
			{
				((Element)stack.peek()).appendChild(path);
			}

			stack.push(path);

			String itemString = null;
			String keyString = null;
			
			path.setAttribute("id", m_id);
			path.setAttribute("candidate", m_candidate);
			path.setAttribute("advisor", m_advisor);
			path.setAttribute("site", m_site);
			path.setAttribute("parentSite", m_parentSite);
			path.setAttribute("sortLetter", m_sortLetter);
			path.setAttribute("type", m_type);
			
				// SAVE THE ORDERED STATUS
			Element order = null;
			Element orderedStatus = doc.createElement("orderedstatus");
			for(int x = 1; x < (m_orderedStatus.size()+1); x++)
			{
				order = doc.createElement("order");
				keyString = "" + x;
				order.setAttribute("ordernum", keyString);
				itemString = (String)m_orderedStatus.get(keyString);
				if(itemString == null)
					itemString = "";
				order.setAttribute("statusid", itemString);
				orderedStatus.appendChild(order);
			}
			path.appendChild(orderedStatus);

				// SAVE THE SCHOOL STATUS PREREQS
			Element schoolStatusPrereqs = null;
			Element prereq = null;
			Enumeration keys = m_schoolPrereqs.keys();
			List schoolPrereqs = null;
			while(keys.hasMoreElements())
			{
				schoolStatusPrereqs = doc.createElement("schoolstatusprereqs");
				keyString = (String)keys.nextElement();
				schoolStatusPrereqs.setAttribute("statusid", keyString);
				schoolPrereqs = (List)m_schoolPrereqs.get(keyString);
				for(int x = 0; x < schoolPrereqs.size(); x++)
				{
					prereq = doc.createElement("prereq");
					prereq.setAttribute("prereqstatusreference", (String) schoolPrereqs.get(x));
					schoolStatusPrereqs.appendChild(prereq);
				}
				path.appendChild(schoolStatusPrereqs);
			}

			
			m_properties.toXml(doc, stack);
			
			stack.pop();
			
			return path;
		}
		
		
		/*******************************************************************************
		* Reference Implementation
		*******************************************************************************/
		
		/**
		* Access the CandidatePath id.
		* @return The CandidatePath id string.
		*/
		public String getId()
		{
			return m_id;
		}

		/**
		* Access the URL which can be used to access the resource.
		* @return The URL which can be used to access the resource.
		*/
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}

		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The the internal reference which can be used to access the resource from within the system.
		*/
		public String getReference()
		{
			return pathReference(m_site, m_id);
		}

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		* Access the resource's properties.
		* @return The resource's properties.
		*/
		public ResourceProperties getProperties()
		{
			return m_properties;
		}

		/**
		* Access the candidate's advisor.
		* @return The candidate's advisor's user id.
		*/
		public String getAdvisor()
		{
			return m_advisor;
		}
		
		/**
		* Access the site id for the CandidatePath's site.
		* @return the site id.
		*/
		public String getSite()
		{
			return m_site;
		}
		
		/**
		* Access the parent department site id for the CandidatePath.
		* @return the site id.
		*/
		public String getParentSite()
		{
			return m_parentSite;
		}
		
		/**
		* Access the alphabetical candidate chooser letter.
		* @return the letter.
		*/
		public String getSortLetter()
		{
			return m_sortLetter;
		}
		
		/**
		* Access the dissertation type set at CandidatePath creation.
		* @return the type String.
		*/
		public String getType()
		{
			return m_type;
		}

		/**
		* Access the candidate who owns this CandidatePath.
		* @return the candidate's user id.
		*/
		public String getCandidate()
		{
			return m_candidate;
		}
		
		/**
		* Access the prerequisites added by department administrators to School steps for their department.
		* @return the Hashtable of Lists containing the references of prerequisite steps, keyed by DissertationStep reference.
		*/
		public Hashtable getSchoolPrereqs()
		{
			return m_schoolPrereqs;
		}

		
		/**
		* Access The completion status for a step.
		* @param status The StepStatus.
		* @return The completion status of the StepStatus, as a String.  See the StepStatus interface for the status Strings.
		*/
		public String getStatusStatus(StepStatus status)
		{
			String retVal = "";
			List deptAddedPrereqs = null;
			StepStatus aStatus = null;
			boolean prereqsCompleted;

			if(status != null)
			{
				if(status.getCompleted())
				{
					retVal = STEP_STATUS_COMPLETED;
				}
				else
				{
					prereqsCompleted = true;
					List prereqs = status.getPrereqs();
					for(int x = 0; x < prereqs.size(); x++)
					{
						try
						{
							aStatus = getStepStatus((String) prereqs.get(x));
						}
						catch(Exception e){}
						if(!aStatus.getCompleted())
						{
							prereqsCompleted = false;
						}
					}
					
					deptAddedPrereqs = getSchoolPrereqs(status.getReference());
					if(deptAddedPrereqs != null)
					{
						for(int x = 0; x < deptAddedPrereqs.size(); x++)
						{
							try
							{
								aStatus = getStepStatus((String) deptAddedPrereqs.get(x));
							}
							catch(Exception e){}
							if(!aStatus.getCompleted())
							{
								prereqsCompleted = false;
							}
						}
					}
					
					if(prereqsCompleted)
					{
						retVal = STEP_STATUS_PREREQS_COMPLETED;
					}
					else
					{
						retVal = STEP_STATUS_PREREQS_NOT_COMPLETED;
					}
				}
			}

			return retVal;
		}

		/**
		* Access the Hashtable containing the StepStatus.
		* @return The StepStatus keyed by display order.
		*/
		public Hashtable getOrderedStatus()
		{
			return m_orderedStatus;
		}
		
		/**
		* Access the reference of the StepStatus by display order.
		* @param order The order number as a String.
		* @return The reference of the StepStatus, or null if there is no match.
		*/
		public String getStatusReferenceByOrder(String order)
		{
			if(order == null)
				return null;
			else
				return (String)m_orderedStatus.get(order);
		}
		
		/**
		* Access the display order for a StepStatus.
		* @param statusReference The reference of the StepStatus.
		* @return The display order number as a String.
		*/
		public int getOrderForStatus(String statusReference)
		{
			int retVal = 0;
			try
			{
				String keyString = null;
				String valueString = null;
				Enumeration keys = m_orderedStatus.keys();
				while(keys.hasMoreElements())
				{
					keyString = (String)keys.nextElement();
					valueString = (String)m_orderedStatus.get(keyString);
					if(statusReference.equals(valueString))
					{
						retVal = Integer.parseInt(keyString);
					}
				}
			}
			catch(Exception e){}
			return retVal;
		}

		/**
		* Access the references of the prerequisite steps for a School step.
		* @return List containing the prerequisite step references.
		*/
		public List getSchoolPrereqs(String schoolStepRef)
		{
			
			List retVal = null;
			if(m_schoolPrereqs == null)
			{
				;
				//m_logger.debug("DISSERTATION : BASE SERVICE : BASE CP : M_SCHOOLPREREQS IS NULL !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}
			else
				retVal = (List)m_schoolPrereqs.get(schoolStepRef);

			if(retVal == null)
				retVal = new Vector();
			return retVal;
		}		


		/**
		* Access the StepStatus that corresponds with the DissertationStep.
		* @param step The DissertationStep.
		* @return The reference of the StepStatus object that corresponds with the DissertationStep.
		*/
		public String matchStepWithStatus(DissertationStep step)
		{
			String retVal = null;
			String key = null;
			String value = null;
			StepStatus tempStatus = null;
			Enumeration keys = m_orderedStatus.keys();
			while(keys.hasMoreElements())
			{
				try
				{
					key = (String)keys.nextElement();
					value = (String)m_orderedStatus.get(key);
					tempStatus = getStepStatus(value);
					if(matches(step, tempStatus))
					{
						retVal = value;
					}
				}
				catch(Exception e){}
			}
			return retVal;
		}

		
		/**
		* Determine whether the DissertationStep is the parent of the StepStatus.
		* Used when making retroactive changes.
		* @return True if the DissertationStep matches the StepStatus, false otherwise.
		*/
		protected boolean matches(DissertationStep step, StepStatus status)
		{
			boolean retVal = false;
				// FIRST SEE IF THE STATUS' PARENT IS THE STEP
			if(step.getReference().equals(status.getParentStepReference()))
			{
				retVal = true;
			}
			else
			{
				if((step != null) && (status != null))
				{
					int numMatches = 0;
					if(step.getInstructionsText().equals(status.getInstructions()))
						numMatches++;
					if(step.getValidationType() == status.getValidationType())
						numMatches++;
					if(step.getSite().equals(status.getSite()))
						numMatches++;
					if(numMatches == 3)
						retVal = true;
				}
			}
			return retVal;
		}

		/**
		* Access the order numbers of the prerequisites in a comma-delimited string for display.
		* @param status The StepStatus.
		* @return The prerequisite steps display string.
		*/
		public String getPrerequisiteStepsDisplayString(StepStatus status)
		{
			StringBuffer retVal = new StringBuffer();
			
			if(status != null)
			{
				String keyString = "";
				String statusReference = "";
				boolean firstAdded = true;
				boolean addToString = false;
				List deptAddedPrereqs = (List)m_schoolPrereqs.get(status.getReference());
				for(int x = 1; x < (m_orderedStatus.size()+1); x++)
				{
					addToString = false;
					keyString = "" + x;
					statusReference = (String)m_orderedStatus.get(keyString);
					
					if(status.hasPrerequisite(statusReference))
					{
						addToString = true;
					}
					else
					{
						if(deptAddedPrereqs != null)
						{
							if(deptAddedPrereqs.contains(statusReference))
							{
								addToString = true;
							}
						}
					}
					
					if(addToString)
					{
						if(firstAdded)
						{
							retVal.append(keyString);
							firstAdded = false;
						}
						else
						{
							retVal.append(", " + keyString);
						}
					}
				}
			}
			
			return retVal.toString();		
		}

		/**
		* Are these objects equal?  If they are both User objects, and they have
		* matching id's, they are.
		* @return true if they are equal, false if not.
		*/
		public boolean equals(Object obj)
		{
			if (!(obj instanceof CandidatePath)) return false;
			return ((CandidatePath)obj).getId().equals(getId());

		}   // equals

		/**
		* Make a hash code that reflects the equals() logic as well.
		* We want two objects, even if different instances, if they have the same id to hash the same.
		*/
		public int hashCode()
		{
			return getId().hashCode();

		}	// hashCode

		/**
		* Compare this object with the specified object for order.
		* @return A negative integer, zero, or a positive integer as this object is
		* less than, equal to, or greater than the specified object.
		*/
		public int compareTo(Object obj)
		{
			if (!(obj instanceof CandidatePath)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their sort names
			int compare = getCandidate().compareTo(((CandidatePath)obj).getCandidate());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((CandidatePath)obj).getId());
			}

			return compare;

		}	// compareTo

	}//BaseCandidatePath



	/*******************************************************************************
	* CandidatePathEdit implementation
	*******************************************************************************/	
	
	public class BaseCandidatePathEdit
		extends BaseCandidatePath
		implements CandidatePathEdit, SessionStateBindingListener
	{
		/** The event code for the edit. */
		protected String m_event = null;
		
		/** Active flag. */
		protected boolean m_active = false;

		/**
		* Constructor.
		* @param pathId The CandidatePath id.
		* @param site The site id.
		*/
		public BaseCandidatePathEdit(String pathId, String site)
		{
			super(pathId, site);

		}   // BaseCandidatePathEdit

		/**
		* Copy constructor.
		* @param path The CandidatePath to be copied.
		*/
		public BaseCandidatePathEdit(CandidatePath path)
		{
			super(path);

		}	// BaseCandidatePathEdit
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The CandidatePathEdit in XML in a DOM element.
		*/
		public BaseCandidatePathEdit(Element el)
		{
			super(el);

		}	// BaseCandidatePathEdit
		

		/**
		* Clean up.
		*/
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}

		}	// finalize


		/**
		* Set the candidate for this CandidatePath.
		* @param candidateId The candidate's user id.
		*/
		public void setCandidate(String candidateId)
		{
			m_candidate = candidateId;
		}
		
		/**
		* Set the candidate for this CandidatePath.
		* @param candidateId The candidate's user id.
		*/
		public void setAdvisor(String advisor)
		{
			m_advisor = advisor;
		}

		/**
		* Set the site for this CandidatePath.
		* @param siteId The site id.
		*/
		public void setSite(String siteId)
		{
			m_site = siteId;
		}
		
		/**
		* Set the parent department site for this CandidatePath.
		* @param siteId The parent site id.
		*/
		public void setParentSite(String siteId)
		{
			m_parentSite = siteId;
		}
		
		/**
		* Set the alphabetical candidate chooser letter.
		* @param letter The letter to associate with the CandidatePath.
		*/
		public void setSortLetter(String letter)
		{
			m_sortLetter = letter;
		}
		
		/**
		* Set the dissertation type for this CandidatePath.
		* @param type The type String.
		*/
		public void setType(String type)
		{
			m_type = type;
		}
		
		/**
		* Set the StepStatus for this CandidatePath.
		* @param hash The Hashtable of StepStatus references, keyed by display order as a String.
		*/
		public void setOrderedStatus(Hashtable hash)
		{
			if(hash != null)
				m_orderedStatus = hash;
		}

		/**
		* Set the prerequisites for School steps.
		* @param The Hashtable of Lists containing the references
		* to the prerequisite steps, keyed by School step reference.
		*/
		public void setSchoolPrereqs(Hashtable schoolPrereqs)
		{
			if(schoolPrereqs != null)
				m_schoolPrereqs = schoolPrereqs;
		}

		
		/**
		* Add a StepStatus object to the ordered status HashTable.
		* @param newStatus - the StepStatus to be added.
		* @param location - the order number.
		*/
		public void addToOrderedStatus(StepStatus newStatus, int locationInt)
		{
			Hashtable newOrder = new Hashtable();
			String keyString = null;
			String statusReference = null;
			for(int x = 1; x < (m_orderedStatus.size()+2); x++)
			{
				if(locationInt == x)
				{
					keyString = "" + x;
					newOrder.put(keyString, newStatus.getReference());
				}
				else if(locationInt < x)
				{
					keyString = "" + (x-1);
					statusReference = (String)m_orderedStatus.get(keyString);
					keyString = "" + (x);
					newOrder.put(keyString, statusReference);
				}
				else
				{
					keyString = "" + x;
					statusReference = (String)m_orderedStatus.get(keyString);
					newOrder.put(keyString, statusReference);
				}
			}
			m_orderedStatus = newOrder;
		}

		
		/**
		* Remove a StepStatus object from the ordered status HashTable.
		* @param statusToRemoveReference - the reference of the StepStatus to be removed.
		*/
		public void removeFromOrderedStatus(String statusToRemoveReference)
		{
			if(statusToRemoveReference != null)
			{
				Hashtable newOrder = new Hashtable();
				String keyString = null;
				String statusId = null;
				boolean foundIt = false;
				for(int y = 1; y < (m_orderedStatus.size()+1); y++)
				{
					keyString = "" + y;
					statusId = (String)m_orderedStatus.get(keyString);
					
						// REMOVE THE STEP FROM ORDERED STEPS
					if(statusId.equals(statusToRemoveReference))
					{
						foundIt = true;
					}
					else
					{
						if(foundIt)
						{
							keyString = "" + (y-1);
							newOrder.put(keyString, statusId);
						}
						else
						{
							newOrder.put(keyString, statusId);
						}
					}
				}
				m_orderedStatus = newOrder;
			}
		}

		
		/**
		* Move a StepStatus object within the ordered status HashTable.
		* @param statusToMoveReference - the reference of the StepStatus to be moved.
		* @param location - the new order number.
		*/
		public void moveStatus(String statusToMoveReference, String location)
		{
			Hashtable newOrder = new Hashtable();
			String keyString = null;
			String statusReference = null;
			
				// FIND THE ORDER NUMBERS FOR THE STATUS TO MOVE AND LOCATION
			int oldPosition = -1;
			int newPosition = -1;
			String aKey = null;
			String aValue = null;
			Enumeration keys = m_orderedStatus.keys();
			if(location.equals("start"))
				newPosition = 1;

			while(keys.hasMoreElements())
			{
				aKey = (String)keys.nextElement();
				aValue = (String)m_orderedStatus.get(aKey);
				if(aValue.equals(statusToMoveReference))
				{
					try
					{
						oldPosition = Integer.parseInt(aKey);
					}
					catch(Exception e){}
				}
				if(aValue.equals(location))
				{
					try
					{
						newPosition = Integer.parseInt(aKey);
						newPosition++;
					}
					catch(Exception e){}
				}
			}

			if((oldPosition != -1) && (newPosition != -1))
			{
				if(newPosition > oldPosition)     // MOVING UP
				{
					newPosition--;
					for(int x = 1; x < (m_orderedStatus.size()+1); x++)
					{
						if((x < oldPosition) || (x > newPosition))
						{
							keyString = "" + x;
							statusReference = (String)m_orderedStatus.get(keyString);
							newOrder.put(keyString, statusReference);
						}
						else if(x == newPosition)
						{
							keyString = "" + x;
							newOrder.put(keyString, statusToMoveReference);
						}
						else
						{
							keyString = "" + (x+1);
							statusReference = (String)m_orderedStatus.get(keyString);
							keyString = "" + x;
							newOrder.put(keyString, statusReference);
						}
					}
					m_orderedStatus = newOrder;
				}
				else if(oldPosition > newPosition)		// MOVING DOWN
				{
					for(int x = 1; x < (m_orderedStatus.size()+1); x++)
					{
						if((x < newPosition) || (x > oldPosition))
						{
							keyString = "" + x;
							statusReference = (String)m_orderedStatus.get(keyString);
							newOrder.put(keyString, statusReference);
						}
						else if(x == newPosition)
						{
							keyString = "" + x;
							newOrder.put(keyString, statusToMoveReference);
						}
						else
						{
							keyString = "" + (x-1);
							statusReference = (String)m_orderedStatus.get(keyString);
							keyString = "" + x;
							newOrder.put(keyString, statusReference);
						}
					}
					m_orderedStatus = newOrder;
				}
				
				for(int y = 1; y < (newOrder.size()+1); y++)
				{
					keyString = "" + y;
					statusReference = (String)newOrder.get(keyString);
				}
			}
		}//moveStatus
		
		/**
		* Add a step to a CandidatePath in use.
		* @param step - The DissertationStep to be added.
		* @param previousStepReferences - The references of the steps preceeding the step to be added in the parent Dissertation.
		* @param siteId - the site id at the time of creation.
		*/
		public void liveAddStep(DissertationStep step, String[] previousStepReferences, String siteId)
		{
			try
			{
				int numPrevSteps = 0;
				if(previousStepReferences != null)
				{
					numPrevSteps = previousStepReferences.length;
					//for(int a = 0; a < numPrevSteps; a++)
					//{
					//	Log.info("chef", "DISSERTATION : BASE SERVICE : BASE CANDIDATE PATH : LIVE ADD STEP : PREV STEP ID " + a + " : " + previousStepReferences[a]);
					//}
				}
				
				if(step != null)
				{
					boolean oardValidated = false;
					if(!(step.getAutoValidationId().equals("")))
						oardValidated = true;
					
					StepStatusEdit statusEdit = addStepStatus(siteId, step, oardValidated);
					
					List stepPrereqs = step.getPrerequisiteSteps();
					String statusPrereq = null;
					DissertationStep tempStep = null;
					if(stepPrereqs.size() > 0)
					{
						for(int x = 0; x < stepPrereqs.size(); x++)
						{
							tempStep = (DissertationStep)stepPrereqs.get(x);
							statusPrereq = matchStepWithStatus(tempStep);
							if(statusPrereq != null)
								statusEdit.addPrerequisiteStatus(statusPrereq);
						}
					}
					commitEdit(statusEdit);
					
					if(statusEdit.getSite().equals(getSchoolSite()))
					{
						// FIRST CREATE A SPOT IN SCHOOL-STEP-PREREQS
						m_schoolPrereqs.put(statusEdit.getReference(), new Vector());
					}

					// NOW LOOK FOR THE PREVIOUS STATUS IDS IN THE CANDIDATE PATH
					String previousStatusReference = null;
					boolean notFound = true;
					DissertationStep previousStep = null;
					for(int x = (numPrevSteps-1); x > -1; x--)
					{
						if(notFound)
						{
							if(previousStepReferences[x].equals("start"))
							{
								previousStatusReference = "start";
								notFound = false;
							}
							else
							{
								previousStep = getDissertationStep(previousStepReferences[x]);
								previousStatusReference = matchStepWithStatus(previousStep);
								if(previousStatusReference != null)
									notFound = false;
							}
						}
					}
				
					// PLACE THE NEW STATUS OBJECT IN THE CANDIDATE PATH
					int position = 0;
					if(previousStatusReference == null)
					{
						position = m_orderedStatus.size();
						addToOrderedStatus(statusEdit, (position+1));
					}
					else
					{
						if(previousStatusReference.equals("start"))
						{
							addToOrderedStatus(statusEdit, 1);
						}
						else
						{
							if(m_orderedStatus.contains(previousStatusReference))
							{
									// FIND THE KEY FOR THE PREVIOUS STEP
								String aKey = null;
								String aValue = null;
								String previousStepKey = null;
								Enumeration keys = m_orderedStatus.keys();
								while(keys.hasMoreElements())
								{
									aKey = (String)keys.nextElement();
									aValue = (String)m_orderedStatus.get(aKey);
									if(aValue.equals(previousStatusReference))
									{
										previousStepKey = aKey;
									}
								}

								position = Integer.parseInt(previousStepKey);
								addToOrderedStatus(statusEdit, (position+1));
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".BaseDissertationService.liveAddStep " + e);
			}
		}


		/**
		* Add a step to a CandidatePath in use.
		* @param step - The DissertationStep to be added.
		* @param previousStepReferences - The references of the steps preceeding the step to be added in the parent Dissertation.
		* @param siteId - the site id at the time of creation.
		*/
		public void liveAddStep(DissertationStep step, List previousStepReferences, String siteId)
		{
			try
			{
				int numPrevSteps = 0;
				if(previousStepReferences != null)
				{
					numPrevSteps = previousStepReferences.size();
				}
				if(step != null)
				{
					boolean oardValidated = false;
					if(!(step.getAutoValidationId().equals("")))
						oardValidated = true;
					
					StepStatusEdit statusEdit = addStepStatus(siteId, step, oardValidated);
					List stepPrereqs = step.getPrerequisiteSteps();
					String statusPrereq = null;
					DissertationStep tempStep = null;
					if(stepPrereqs.size() > 0)
					{
						for(int x = 0; x < stepPrereqs.size(); x++)
						{
							tempStep = (DissertationStep)stepPrereqs.get(x);
							statusPrereq = matchStepWithStatus(tempStep);
							if(statusPrereq != null)
								statusEdit.addPrerequisiteStatus(statusPrereq);
						}
					}
					commitEdit(statusEdit);
					
					if(statusEdit.getSite().equals(getSchoolSite()))
					{
						// FIRST CREATE A SPOT IN SCHOOL-STEP-PREREQS
						m_schoolPrereqs.put(statusEdit.getReference(), new Vector());
					}

					// NOW LOOK FOR THE PREVIOUS STATUS IDS IN THE CANDIDATE PATH
					String previousStatusReference = null;
					boolean notFound = true;
					DissertationStep previousStep = null;
					for(int x = (numPrevSteps-1); x > -1; x--)
					{
						if(notFound)
						{
							//if(previousStepReferences[x].equals("start"))
							if(previousStepReferences.get(x).equals("start"))
							{
								previousStatusReference = "start";
								notFound = false;
							}
							else
							{
								previousStep = getDissertationStep((String)previousStepReferences.get(x));
								previousStatusReference = matchStepWithStatus(previousStep);
								if(previousStatusReference != null)
									notFound = false;
							}
						}
					}
				
					// PLACE THE NEW STATUS OBJECT IN THE CANDIDATE PATH
					int position = 0;
					if(previousStatusReference == null)
					{
						position = m_orderedStatus.size();
						addToOrderedStatus(statusEdit, (position+1));
					}
					else
					{
						if(previousStatusReference.equals("start"))
						{
							addToOrderedStatus(statusEdit, 1);
						}
						else
						{
							if(m_orderedStatus.contains(previousStatusReference))
							{
									// FIND THE KEY FOR THE PREVIOUS STEP
								String aKey = null;
								String aValue = null;
								String previousStepKey = null;
								Enumeration keys = m_orderedStatus.keys();
								while(keys.hasMoreElements())
								{
									aKey = (String)keys.nextElement();
									aValue = (String)m_orderedStatus.get(aKey);
									if(aValue.equals(previousStatusReference))
									{
										previousStepKey = aKey;
									}
								}

								position = Integer.parseInt(previousStepKey);
								addToOrderedStatus(statusEdit, (position+1));
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				m_logger.warn("DISSERTATION : BASE SERVICE : BASE CANDIDATE PATH : LIVE ADD STEP : EXCEPTION : " + e);
			}
		}
		
		/**
		* Remove a StepStatus to a CandidatePath in use, based on the DissertationStep.
		* @param step - The DissertationStep to be added.
		*/
		public void liveRemoveStep(DissertationStep step)
		{
			try
			{
					// FIND THE STATUS FOR THIS STEP
				String statusReference = matchStepWithStatus(step);
				
					// REMOVE ANY PREREQUISITE REFERENCES TO THIS STATUS
				StepStatusEdit statusToRemoveEdit = editStepStatus(statusReference);
				StepStatus status = null;
				StepStatusEdit statusEdit = null;
				List prereqs = null;
				Enumeration keys = m_orderedStatus.keys();
				String key = null;
				String value = null;
				while(keys.hasMoreElements())
				{
					key = (String)keys.nextElement();
					value = (String)m_orderedStatus.get(key);
					status = getStepStatus(value);
					prereqs = status.getPrereqs();
					if(prereqs.contains(statusReference))
					{
						statusEdit = editStepStatus(status.getReference());
						prereqs.remove(statusReference);
						commitEdit(statusEdit);
					}
				}
				
				keys = m_schoolPrereqs.keys();
				while(keys.hasMoreElements())
				{
					key = (String)keys.nextElement();
					prereqs = (List)m_schoolPrereqs.get(key);
					prereqs.remove(statusReference);
				}
				
					// REMOVE THE STATUS FROM ALL DATA STRUCTURES AND STORAGE
				removeFromOrderedStatus(statusReference);
				m_schoolPrereqs.remove(statusReference);
				removeStepStatus(statusToRemoveEdit);
			}
			catch(Exception e)
			{
				m_logger.warn("DISSERTATION : BASE SERVICE : BASE CP : LIVE REMOVE FROM ORDERED STATUS : EXCEPTION :" + e);
			}
		}
		
		/**
		* Update a StepStatus in this CandidatePath, while the CandidatePath is in use.
		* @param before - The DissertationStep to be updated, before changes were made.  Used to find the matching StepStatus object.
		* @param after - The same DissertationStep, after changes.
		*/
		public void liveUpdateStep(DissertationStep before, DissertationStepEdit after)
		{
			try
			{
					// FIND THE STATUS FOR THIS STEP
				String statusReference = matchStepWithStatus(before);
				if(statusReference != null)
				{
					StepStatusEdit statusEdit = editStepStatus(statusReference);
					statusEdit.setInstructions(after.getInstructionsText());
					statusEdit.setValidationType(after.getValidationType());
					List prereqStatus = new Vector();
					List prereqSteps = after.getPrerequisiteStepReferences();
					DissertationStep step = null;
					String prereqStatusReference = null;
					for(int x = 0; x < prereqSteps.size(); x++)
					{
						step = getDissertationStep((String) prereqSteps.get(x));
						prereqStatusReference = matchStepWithStatus(step);
						if(prereqStatusReference != null)
							prereqStatus.add(prereqStatusReference);
					}

					statusEdit.setPrereqs(prereqStatus);
					commitEdit(statusEdit);
				}
			}
			catch(Exception e)
			{
				m_logger.warn("DISSERTATION : BASE SERVICE : BASE CP : LIVE UPDATE STEP : EXCEPTION :" + e);
			}
			
		}//liveUpdateStep
		
		
		/**
		* Move a StepStatus within a CandidatePath in use.
		* @param step - The DissertationStep which is the parent of the StepStatus to be moved.
		* @param location - The reference of the DissertationStep location previous to the new position.
		* @param previousStepPosition - The reference of the step preceeding the step to be moved in the parent Dissertation.
		*/
		public void liveMoveStep(DissertationStep step, String location, String previousStepPosition)
		{
			try
			{
					// FIND THE STATUS FOR THIS STEP
				String statusRef = matchStepWithStatus(step);
				
				DissertationStep locationStep = null;
				Hashtable newOrder = new Hashtable();
				String keyString = null;
				String stepRef = null;
				
				
				int oldPosition = -1;
				int newPosition = -1;
				String locationStatusRef = "";				
				if(location.equals("start"))
				{
					newPosition = 1;
				}
				else
				{
					locationStep = getDissertationStep(location);
					locationStatusRef = matchStepWithStatus(locationStep);
				}

					// FIND THE ORDER NUMBERS FOR THE STEP TO MOVE AND LOCATION
				String aKey = null;
				String aValue = null;
				Enumeration keys = m_orderedStatus.keys();
				while(keys.hasMoreElements())
				{
					aKey = (String)keys.nextElement();
					aValue = (String)m_orderedStatus.get(aKey);
					if(aValue.equals(statusRef))
					{
						try
						{
							oldPosition = Integer.parseInt(aKey);
						}
						catch(Exception e){}
					}
					if(aValue.equals(locationStatusRef))
					{
						try
						{
							newPosition = Integer.parseInt(aKey);
							newPosition++;
						}
						catch(Exception e){}
					}
				}

				if((oldPosition != -1) && (newPosition != -1))
				{
					if(newPosition > oldPosition)     // MOVING UP
					{
						newPosition--;
						for(int x = 1; x < (m_orderedStatus.size()+1); x++)
						{
							if((x < oldPosition) || (x > newPosition))
							{
								keyString = "" + x;
								stepRef = (String)m_orderedStatus.get(keyString);
								newOrder.put(keyString, stepRef);
							}
							else if(x == newPosition)
							{
								keyString = "" + x;
								newOrder.put(keyString, statusRef);
							}
							else
							{
								keyString = "" + (x+1);
								stepRef = (String)m_orderedStatus.get(keyString);
								keyString = "" + x;
								newOrder.put(keyString, stepRef);
							}
						}
						m_orderedStatus = newOrder;
					}
					else if(oldPosition > newPosition)		// MOVING DOWN
					{
						for(int x = 1; x < (m_orderedStatus.size()+1); x++)
						{
							if((x < newPosition) || (x > oldPosition))
							{
								keyString = "" + x;
								stepRef = (String)m_orderedStatus.get(keyString);
								newOrder.put(keyString, stepRef);
							}
							else if(x == newPosition)
							{
								keyString = "" + x;
								newOrder.put(keyString, statusRef);
							}
							else
							{
								keyString = "" + (x-1);
								stepRef = (String)m_orderedStatus.get(keyString);
								keyString = "" + x;
								newOrder.put(keyString, stepRef);
							}
						}
						m_orderedStatus = newOrder;
					}
				
					for(int y = 1; y < (newOrder.size()+1); y++)
					{
						keyString = "" + y;
						stepRef = (String)newOrder.get(keyString);
					}
				}		
			}
			catch(Exception e)
			{
				m_logger.warn("DISSERTATION : BASE SERVICE : BASE CP : LIVE MOVE STEP : EXCEPTION :" + e);
			}
			
		}//liveMoveStep

		
		/**
		* Add a prerequisite to a StepStatus in a CandidatePath in use.
		* @param stepRef - the reference of the step to which the prerequisite is to be added.
		* @param prereqRef - the reference of the prerequisite to be added.
		*/
		public void liveAddSchoolPrereq(String stepRef, String prereqRef)
		{
			
			// FIRST MAKE SURE THESE STEPS EXIST
			try
			{
				DissertationStep step = getDissertationStep(stepRef);
				DissertationStep prereq = getDissertationStep(prereqRef);

				List schoolPrereqs = null;
	
				String statusRef = matchStepWithStatus(step);
				String prereqStatusReference = matchStepWithStatus(prereq);
				
				if((statusRef != null) && (prereqStatusReference != null))
				{
					schoolPrereqs = (List)m_schoolPrereqs.get(statusRef);
					if(schoolPrereqs == null)
						schoolPrereqs = new Vector();
					
					if(schoolPrereqs != null)
					{
						schoolPrereqs.add(prereqStatusReference);
						m_schoolPrereqs.put(statusRef, schoolPrereqs);
					}
				}
			}
			catch(Exception e){}

		}//liveAddSchoolPrereq


		/**
		* Remove a prerequisite from a StepStatus in a CandidatePath in use.
		* @param stepRef - the reference of the step from which the prerequisite is to be removed.
		* @param prereqRef - the reference of the prerequisite to be removed.
		*/
		public void liveRemoveSchoolPrereq(String stepRef, String prereqRef)
		{
			// FIRST MAKE SURE THESE STEPS EXIST
			try
			{
				DissertationStep step = getDissertationStep(stepRef);
				DissertationStep prereq = getDissertationStep(prereqRef);
				List schoolPrereqs = null;
				String statusRef = matchStepWithStatus(step);
				String prereqStatusReference = matchStepWithStatus(prereq);
				if((statusRef != null) && (prereqStatusReference != null))
				{
					schoolPrereqs = (List)m_schoolPrereqs.get(statusRef);
					if(schoolPrereqs != null)
					{
						schoolPrereqs.remove(prereqStatusReference);
					}
				}
			}
			catch(Exception e){}

		}//liveRemoveSchoolPrereq


		/**
		* Take all values from this object.
		* @param path The CandidatePath object to take values from.
		*/
		protected void set(CandidatePath path)
		{
			setAll(path);

		}   // set

		/**
		* Access the event code for this edit.
		* @return The event code for this edit.
		*/
		protected String getEvent() { return m_event; }
	
		/**
		* Set the event code for this edit.
		* @param event The event code for this edit.
		*/
		protected void setEvent(String event) { m_event = event; }

		/**
		* Access the resource's properties for modification
		* @return The resource's properties.
		*/
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;

		}	// getPropertiesEdit

		/**
		* Enable editing.
		*/
		protected void activate()
		{
			m_active = true;

		}	// activate

		/**
		* Check to see if the edit is still active, or has already been closed.
		* @return true if the edit is active, false if it's been closed.
		*/
		public boolean isActiveEdit()
		{
			return m_active;

		}	// isActiveEdit

		/**
		* Close the edit object - it cannot be used after this.
		*/
		protected void closeEdit()
		{
			m_active = false;

		}	// closeEdit

		/*******************************************************************************
		* SessionStateBindingListener implementation
		*******************************************************************************/
	
		/**
		* Accept notification that this object has been bound as a SessionState attribute.
		* @param sessionStateKey The id of the session state which holds the attribute.
		* @param attributeName The id of the attribute to which this object is now the value.
		*/
		public void valueBound(String sessionStateKey, String attributeName) {}
	
		/**
		* Accept notification that this object has been removed from a SessionState attribute.
		* @param sessionStateKey The id of the session state which held the attribute.
		* @param attributeName The id of the attribute to which this object was the value.
		*/
		public void valueUnbound(String sessionStateKey, String attributeName)
		{
	
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
	
		}	// valueUnbound


	}//BaseCandidatePathEdit


	
	/*******************************************************************************
	* StepStatus implementation
	*******************************************************************************/
	
	public class BaseStepStatus
		implements StepStatus
	{
		/** The StepStatus id. */
		protected String m_id;
		
		/** The user id of the user who marked the step as completed. */
		protected String m_validator;
		
		/** The instructions for the step. */
		protected String m_instructions;
		
		/** The site id. */
		protected String m_site;
		
		/** The reference of the DissertationStep on which this StepStatus is created from. */
		protected String m_parentStepReference;
		
		/** The id for auto-validation from the Rackham database. */
		protected String m_autoValidationId;
		
		/** The additional lines of text to display with step status. */
		protected List m_auxiliaryText;
		
		/** Time the step is marked as completed. */
		protected Time m_timeCompleted;
		
		/** String text to append to Time completed. */
		protected String m_timeCompletedText;
		
		/** Time of completion recommended by the creator of the step. (Not yet used.) */
		protected Time m_recommendedDeadline;
		
		/** Absolute deadline for completion set by the creator of the step. (Not yet used.) */
		protected Time m_hardDeadline;
		
		/** Type of permissions for marking the step as completed.
			See the DissertationStep interface for types. */
		protected String m_validationType;
		
		/** The references of the prerequisites for this step. */
		protected List m_prereqStatus;
		
		/** Signifies whether the step has been marked as completed. */
		protected boolean m_completed;
		
		/** Signifies whether this step is auto-validated from the Rackham database. */
		protected boolean m_oardValidated;
		
		/** The properties. */
		protected ResourcePropertiesEdit m_properties;
		

		/**
		* Constructor.
		* @param statusId The StepStatus id.
		* @param site The site id.
		*/
		public BaseStepStatus(String statusId, String site)
		{
			m_id = statusId;
			m_site = site;
			m_validator = "";
			m_parentStepReference = "-1";
			m_validationType = "0";
			m_instructions = "";
			m_autoValidationId = "";
			m_prereqStatus = new Vector();
			m_auxiliaryText = new Vector();
			m_completed = false;
			m_timeCompletedText = "";
			m_oardValidated = false;
			m_properties = new BaseResourcePropertiesEdit();
			addLiveProperties(m_properties);
		}
		
		/**
		* Copy constructor.
		* @param status The StepStatus to copy.
		*/
		public BaseStepStatus(StepStatus status)
		{
			setAll(status);

		}	// BaseStepStatus
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The StepStatus in XML in a DOM element.
		*/
		public BaseStepStatus(Element el)
		{
			m_properties = new BaseResourcePropertiesEdit();
			m_prereqStatus = new Vector();
			m_auxiliaryText = new Vector();
			String valueString = null;

			m_id = el.getAttribute("id");
			m_site = el.getAttribute("site");
			m_validator = el.getAttribute("validator");
			m_parentStepReference = el.getAttribute("parentstepreference");
			m_autoValidationId = el.getAttribute("autovalid");
			m_timeCompleted = getTimeObject(el.getAttribute("timecompleted"));
			m_timeCompletedText = el.getAttribute("timecompletedtext");
			m_recommendedDeadline = getTimeObject(el.getAttribute("recommendeddeadline"));
			m_hardDeadline = getTimeObject(el.getAttribute("harddeadline"));
			m_completed = getBool(el.getAttribute("completed"));
			m_oardValidated = getBool(el.getAttribute("oardvalidated"));
			m_validationType = el.getAttribute("validtype");
			m_instructions = Xml.decodeAttribute(el, "instructions");

				// READ THE CHILD ELEMENTS
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element)child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					// re-create properties
					m_properties = new BaseResourcePropertiesEdit(element);
				}
				else if(element.getTagName().equals("prereqstatus"))
				{
					NodeList ssChildren = element.getChildNodes();
					int ssLength = ssChildren.getLength();
					Node ssChild = null;
					Element ssElement = null;
					for(int x = 0; x < ssLength; x++)
					{
						ssChild = ssChildren.item(x);
						if (ssChild.getNodeType() != Node.ELEMENT_NODE) continue;
						ssElement = (Element)ssChild;
						if(ssElement.getTagName().equals("prereq"))
						{
							valueString = ssElement.getAttribute("prereqstatusreference");
							m_prereqStatus.add(valueString);
						}
					}
				}
				else if(element.getTagName().equals("auxiliarytext"))
				{
					NodeList ssChildren = element.getChildNodes();
					int ssLength = ssChildren.getLength();
					Node ssChild = null;
					Element ssElement = null;
					for(int x = 0; x < ssLength; x++)
					{
						ssChild = ssChildren.item(x);
						if (ssChild.getNodeType() != Node.ELEMENT_NODE) continue;
						ssElement = (Element)ssChild;
						if(ssElement.getTagName().equals("item"))
						{
							valueString = ssElement.getAttribute("text");
							m_auxiliaryText.add(valueString);
						}
					}
				}
				// old way of encoding
				else if(element.getTagName().equals("instructions"))
				{
					if ((element.getChildNodes() != null) && (element.getChildNodes().item(0) != null))
					{
						m_instructions = element.getChildNodes().item(0).getNodeValue();
					}
					if (m_instructions == null)
					{
						m_instructions = "";
					}
				}
					
			}
		}
		
		/**
		* Deep copy of this object.
		* @param status - The StepStatus object to be copied.
		*/
		protected void setAll(StepStatus status)
		{
			m_id = status.getId();
			m_validator = status.getValidator();
			m_instructions = status.getInstructions();
			m_site = status.getSite();
			m_parentStepReference = status.getParentStepReference();
			m_autoValidationId = status.getAutoValidationId();
			m_validationType = status.getValidationType();
			m_prereqStatus = status.getPrereqs();
			m_completed = status.getCompleted();
			m_timeCompleted = status.getTimeCompleted();
			m_timeCompletedText = status.getTimeCompletedText();
			m_auxiliaryText = status.getAuxiliaryText();
			m_recommendedDeadline = status.getRecommendedDeadline();
			m_hardDeadline = status.getHardDeadline();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(status.getProperties());

		}   // setAll

		
		/**
		* Serialize the resource into XML, adding an element to the doc under the top of the stack element.
		* @param doc The DOM doc to contain the XML (or null for a string return).
		* @param stack The DOM elements, the top of which is the containing element of the new "resource" element.
		* @return The newly added element.
		*/
		public Element toXml(Document doc, Stack stack)
		{
			Element status = doc.createElement("status");

			if (stack.isEmpty())
			{
				doc.appendChild(status);
			}
			else
			{
				((Element)stack.peek()).appendChild(status);
			}

			stack.push(status);

			status.setAttribute("id", m_id);
			status.setAttribute("site", m_site);
			status.setAttribute("validator", m_validator);
			status.setAttribute("parentstepreference", m_parentStepReference);
			status.setAttribute("validtype", m_validationType);
			status.setAttribute("autovalid", m_autoValidationId);
			status.setAttribute("timecompleted", getTimeString(m_timeCompleted));
			status.setAttribute("timecompletedtext", m_timeCompletedText);
			status.setAttribute("recommendeddeadline", getTimeString(m_recommendedDeadline));
			status.setAttribute("harddeadline", getTimeString(m_hardDeadline));
			status.setAttribute("completed", getBoolString(m_completed));
			status.setAttribute("oardvalidated", getBoolString(m_oardValidated));

			// SAVE THE PREREQUISISTE STEPS
			Element prereq = null;
			Element prereqStatus = doc.createElement("prereqstatus");
			for(int x = 0; x < m_prereqStatus.size(); x++)
			{
				prereq = doc.createElement("prereq");
				prereq.setAttribute("prereqstatusreference", (String) m_prereqStatus.get(x));
				prereqStatus.appendChild(prereq);
			}
			status.appendChild(prereqStatus);
			
			// SAVE THE AUXILIARY LINES OF TEXT
			Element aux = null;
			Element auxiliaryText = doc.createElement("auxiliarytext");
			if(m_auxiliaryText != null)
			{
				for(int x = 0; x < m_auxiliaryText.size(); x++)
				{
					aux = doc.createElement("item");
					aux.setAttribute("text", (String) m_auxiliaryText.get(x));
					auxiliaryText.appendChild(aux);
				}
			}
			status.appendChild(auxiliaryText);
			
			// SAVE THE INSTRUCTIONS
			Xml.encodeAttribute(status, "instructions", m_instructions);
/*
			Element instructions = doc.createElement("instructions");
			status.appendChild(instructions);
			instructions.appendChild(doc.createCDATASection(m_instructions));
*/
			
			// SAVE THE PROPERTIES
			m_properties.toXml(doc, stack);
			
			stack.pop();

			return status;

		}//toXml


		/*******************************************************************************
		* Reference Implementation
		*******************************************************************************/
		
		/**
		* Access the StepStatus id.
		* @return The StepStatus id string.
		*/
		public String getId()
		{
			return m_id;
		}

		/**
		* Access the URL which can be used to access the resource.
		* @return The URL which can be used to access the resource.
		*/
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}

		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The the internal reference which can be used to access the resource from within the system.
		*/
		public String getReference()
		{
			return statusReference(m_site, m_id);
		}

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		* Access the resource's properties.
		* @return The resource's properties.
		*/
		public ResourceProperties getProperties()
		{
			return m_properties;
		}
		
		/**
		* Access the reference of the parent DissertationStep.
		* @return The reference of the parent DissertationStep, if it exists, -1 otherwise.
		*/
		public String getParentStepReference()
		{
			return m_parentStepReference;
		}

		/**
		* Access the creator of this StepStatus.
		* @return The user id of the creator.
		*/
		public String getCreator()
		{
			return m_properties.getProperty(ResourceProperties.PROP_CREATOR);
		}
		
		/**
		* Access the recommended deadline for completion of this step.
		* @return The Time recommended by the creator for completion.
		*/
		public Time getRecommendedDeadline()
		{
			return m_recommendedDeadline;
		}

		/**
		* Access the absolute deadline for completion of the step.
		* @return The absolute deadline set by the creator for completion.
		*/
		public Time getHardDeadline()
		{
			return m_hardDeadline;
		}

		/**
		* Access the time the step was marked as completed.
		* @return The time the step was marked as completed, or null if it is not completed.
		*/
		public Time getTimeCompleted()
		{
			return m_timeCompleted;
		}
		
		/**
		* Access text to append to the time competed.
		* @return The text, or null if it is not set.
		*/
		public String getTimeCompletedText()
		{
			return m_timeCompletedText;
		}
		
		/**
		* Access the user who marked the step as completed.
		* @return The user id of the user who marked the step as completed.
		*/
		public String getValidator()
		{
			return m_validator;
		}

		/**
		* Access the id for auto-validation by the Rackham database.
		* @return the id used for auto-validation by the Rackham database,
		* or -1 if the step is not auto-validated.
		*/
		public String getAutoValidationId()
		{
			return m_autoValidationId;
		}
		
		/**
		* Has this step been marked completed by the candidate yet ?
		* @return True if the step has been marked as completed, false otherwise.
		*/
		public boolean getCompleted()
		{
			return m_completed;
		}
		
		/**
		* Access any additional lines fo text to display with the step status.
		* @return Vector of String lines of text to display, or null if none.
		*/
		public List getAuxiliaryText()
		{
			return m_auxiliaryText;
		}

		/**
		* Access whether this step is auto-validated by the Rackham database.
		* @return True if the step is auto-validated by the Rackham database, false otherwise.
		*/
		public boolean getOardValidated()
		{
			return m_oardValidated;
		}
		
		/**
		* Access the prerequisites for this step.
		* @return A List containing the references of the prerequisites for this step.
		*/
		public List getPrereqs()
		{
			return m_prereqStatus;
		}
		
		/**
		* See whether this step contains this prerequisite.
		* @param statusRef The reference of the prerequisite.
		* @return true if this step has this prerequisite, false otherwise.
		*/
		public boolean hasPrerequisite(String statusRef)
		{
			boolean retVal = false;
			if(statusRef != null)
			{
				for(int x = 0; x < m_prereqStatus.size(); x++)
				{
					if(statusRef.equals(m_prereqStatus.get(x)))
						retVal = true;
				}
			}

			return retVal;
		}

		/**
		* Access the validation type of this step.
		* @return the type of permissions required to mark a step as completed.  See the DissertationStep interface.
		*/
		public String getValidationType()
		{
			return m_validationType;
		}

		/**
		* Access the instructions for the StepStatus with html links for user-entered links.
		* @return the step's instructions with html links for user-entered links.
		*/
		public String getInstructions()
		{
			StringBuffer retVal = new StringBuffer();
			boolean goodSyntax = true;
			String tempString = null;
			String linkString = null;
			String linkTextString = null;
			String fullLinkString = null;
			int midIndex = -1;
			int endIndex = -1;
			int beginIndex = m_instructions.indexOf("http");
		
			if(beginIndex == -1)
			{
				retVal.append(m_instructions);
			}
			else
			{
				do
				{
					// FIRST ADD THE TEXT UP TO THE LINK TO THE BUFFER
					retVal.append(m_instructions.substring((endIndex+1), beginIndex));
					midIndex = m_instructions.indexOf("{", beginIndex);
					if(midIndex == -1)
					{
						// LINK IS NOT IN PROPER FORMAT - RETURN ORIGINAL STRING
						goodSyntax = false;
						beginIndex = -1;
					}
					else
					{
						// FIND THE END TAG
						endIndex = m_instructions.indexOf("}", midIndex);
						if(endIndex == -1)
						{
							goodSyntax = false;
							beginIndex = -1;
						}
						else
						{
							linkString = m_instructions.substring(beginIndex, midIndex);
							linkTextString = m_instructions.substring((midIndex+1), endIndex);
							fullLinkString = "<a href='" + linkString + "' target='_blank' " + ">" + linkTextString + "</a>";
							retVal.append(fullLinkString);
							beginIndex = m_instructions.indexOf("http", endIndex);
						}
					}
					
					if(beginIndex == -1)
					{
						tempString = m_instructions.substring((endIndex+1), m_instructions.length());
						retVal.append(tempString);
					}

				}while(beginIndex != -1);
				
			}//else
		
			if(!goodSyntax)
			{
				retVal = new StringBuffer();
				retVal.append(m_instructions);
			}

			return retVal.toString();
		}

		/**
		* Access the instructions as entered by the user.
		* @return the instructions as entered by the user.
		*/
		public String getInstructionsText()
		{
			return m_instructions;
		}
		
		/**
		* Access the instructions as entered by the user, with a maximum length of 80 characters for display in selects.
		* Remove links to save space in the select drop down list and prevent truncation from breaking an active link
		* @return the instructions as entered by the user, with a maximum length of 80 characters for display in selects.
		*/
		public String getShortInstructionsText()
		{
			StringBuffer buffer = new StringBuffer();
			String retVal = null;
			String tempString = null;
			String linkTextString = null;
			boolean goodSyntax = true;
			int midIndex = -1;
			int endIndex = -1;
			int tagIndex = -1;
			
			int beginIndex = m_instructions.indexOf("http");
			if(beginIndex == -1)
			{
				//No links, so return the original text
				buffer.append(m_instructions);
			}
			else
			{
				try
				{
					do
					{
						//First, add the text up to the link to the buffer
						tagIndex = m_instructions.lastIndexOf("<", beginIndex);
						buffer.append(m_instructions.substring((endIndex+1), tagIndex));
						
						//Find the begin tag
						midIndex = m_instructions.indexOf(">", beginIndex);
						if(midIndex == -1)
						{
							//Link is not in the proper format, so return the original string
							goodSyntax = false;
							beginIndex = -1;
							//Missing >
						}
						else
						{
							//Find the end tag
							endIndex = m_instructions.indexOf("<", midIndex);
							if(endIndex == -1)
							{
								goodSyntax = false;
								//Missing <
								beginIndex = -1;
							}
							else
							{
								//Syntax is good this pass, so keep the link text string
								linkTextString = m_instructions.substring((midIndex+1), endIndex);
								buffer.append(linkTextString);
								endIndex = m_instructions.indexOf(">", endIndex);
								beginIndex = m_instructions.indexOf("http", endIndex);
							}
						}
						if(beginIndex == -1)
						{
							//No more links, so add remaining text
							tempString = m_instructions.substring((endIndex+1), m_instructions.length());
						
							//Add remainder of text
							buffer.append(tempString);
						}

					}while(beginIndex != -1);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getShortInstructionsText Exception " + e);
				}
				
			}//else
		
			if(!goodSyntax)
			{
				//Bad syntax, so return original string
				buffer = new StringBuffer();
				buffer.append(m_instructions);
			}

			//Result string
			if(buffer.length() > 80)
			{
				retVal = ((String)buffer.toString()).substring(0, 79) + " . . . ";
			}
			else
			{
				retVal = buffer.toString();
			}
			return retVal;
			
		}//getShortInstructionsText


		/**
		* Access the site id for the StepStatus's site.
		* @return the site id.
		*/
		public String getSite()
		{
			return m_site;
		}

		/**
		* Are these objects equal?  If they are both StepStatus objects, and they have
		* matching id's, they are.
		* @return true if they are equal, false if not.
		*/
		public boolean equals(Object obj)
		{
			if (!(obj instanceof StepStatus)) return false;
			return ((StepStatus)obj).getId().equals(getId());

		}   // equals

		/**
		* Make a hash code that reflects the equals() logic as well.
		* We want two objects, even if different instances, if they have the same id to hash the same.
		*/
		public int hashCode()
		{
			return getId().hashCode();

		}	// hashCode

		/**
		* Compare this object with the specified object for order.
		* @return A negative integer, zero, or a positive integer as this object is
		* less than, equal to, or greater than the specified object.
		*/
		public int compareTo(Object obj)
		{
			if (!(obj instanceof StepStatus)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their sort names
			int compare = getInstructions().compareTo(((StepStatus)obj).getInstructions());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((StepStatus)obj).getId());
			}

			return compare;

		}	// compareTo

	}


	
	/*******************************************************************************
	* StepStatusEdit implementation
	*******************************************************************************/
	
	public class BaseStepStatusEdit
		extends BaseStepStatus
		implements StepStatusEdit, SessionStateBindingListener
	{
		/** The event code for the edit. */
		protected String m_event = null;
		
		/** Active flag. */
		protected boolean m_active = false;
		
		
		/**
		* Constructor.
		* @param statusId The StepStatusEdit id.
		* @param site The site id.
		*/
		public BaseStepStatusEdit(String statusId, String site)
		{
			super(statusId, site);

		}   // BaseStepStatusEdit

		/**
		* Copy constructor.
		* @param status The StepStatusEdit to be copied.
		*/
		public BaseStepStatusEdit(StepStatus status)
		{
			super(status);

		}	// BaseStepStatusEdit
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The StepStatusEdit in XML in a DOM element.
		*/
		public BaseStepStatusEdit(Element el)
		{
			super(el);

		}	// BaseStepStatusEdit


		/**
		* Fill the properties of the StepStatus based on the parent DissertationStep.
		* @param site The site id of the current site at the time of creation.
		* @param step The parent DissertationStep from which this StepStatus is spawned.
		* @param oardstep Signifies whether this step is auto-validated by the Rackham database.
		*/
		public void initialize(String site, DissertationStep step, boolean oardstep)
		{
			m_validator = "";
			m_parentStepReference = step.getReference();
			m_validationType = step.getValidationType();
			m_instructions = step.getInstructions();
			m_site = site;
			m_autoValidationId = step.getAutoValidationId();
			//m_logger.debug("****************************************************************************************************");
			//m_logger.debug("DISSERTATION : BASE SERVICE : BASE STEP STATUS : INITIALIZE : AUTO VALIDATION ID : " + m_autoValidationId);
			//m_logger.debug("****************************************************************************************************");
			m_prereqStatus = new Vector();
			m_properties = new BaseResourcePropertiesEdit();
			m_completed = false;
			m_oardValidated = oardstep;
			//m_logger.debug("DISSERTATION : BASE SERVICE : BASE STEP STATUS EDIT : INITIALIZE : STATUS OBJECT INITIALIZED");
		}
		
		/**
		* Clean up.
		*/
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}

		}	// finalize

		/**
		* Set the recommended deadline for completion of this step.
		* @param deadline The Time recommended by the creator for completion.
		*/
		public void setRecommendedDeadline(Time deadline)
		{
			m_recommendedDeadline = deadline;
		}

		/**
		* Set the absolute deadline for completion of the step.
		* @param hardDeadline The absolute deadline set by the creator for completion.
		*/
		public void setHardDeadline(Time hardDeadline)
		{
			m_hardDeadline = hardDeadline;
		}

		/**
		* Set the time the step was marked as completed.
		* @param time The time the step was marked as completed, or null if it is not completed.
		*/
		public void setTimeCompleted(Time time)
		{
			m_timeCompleted = time;
		}
		
		/**
		* Set the text to accompany the time completed.
		* @param text The text to append to time completed, or null if it is not set.
		*/
		public void setTimeCompletedText(String text)
		{
			m_timeCompletedText = text;
		}

		/**
		* Set the user who marked the step as completed.
		* @param validator The user id of the user who marked the step as completed.
		*/
		public void setValidator(String validator)
		{
			m_validator = validator;
		}

		/**
		* Set the id for auto-validation by the Rackham database.
		* @param validId the id used for auto-validation by the Rackham database,
		* or -1 if the step is not auto-validated.
		*/
		public void setAutoValidationId(String validId)
		{
			if(validId != null)
				m_autoValidationId = validId;
		}
		
		/**
		* Set the additional lines of text to display with step status.
		* @param text the List of String containing the lines of text to display.
		*/
		public void setAuxiliaryText(List text)
		{
			m_auxiliaryText = text;
		}

		/**
		* Has this step been marked completed by the candidate yet ?
		* @param completed True if the step has been marked as completed, false otherwise.
		*/
		public void setCompleted(boolean completed)
		{
			m_timeCompleted = TimeService.newTime();
			m_completed = completed;
		}

		/**
		* Add a prerequisite to this step.
		* @param statusReference The reference for the prerequisite StepStatus to be added.
		*/
		public void addPrerequisiteStatus(String statusReference)
		{
			if(statusReference != null)
				m_prereqStatus.add(statusReference);
		}

		/**
		* Remove a prerequisite to this step.
		* @param statusReference The reference for the prerequisite StepStatus to be removed.
		*/
		public void removePrerequisiteStatus(String statusReference)
		{
			if(statusReference != null)
				m_prereqStatus.remove(statusReference);
		}
		
		/**
		* Set the collection of prerequisites to this step.
		* @param prereqs List containing the references to the prerequisite StepStatus objects.
		*/
		public void setPrereqs(List prereqs)
		{
			if(prereqs != null)
				m_prereqStatus = prereqs;
		}

		/**
		* Set the validation type for this step.
		* @param type The type of permissions required to mark this step as completed.  See the DissertationStep interface for values.
		*/
		public void setValidationType(String type)
		{
			m_validationType = type;
		}

		/**
		* Set the instructions for this StepStatusEdit.
		* @param instructions The instructions.
		*/
		public void setInstructions(String instructions)
		{
			if(instructions != null)
				//m_instructions = Validator.escapeHtml(instructions);
				m_instructions = Validator.escapeHtmlFormattedText(instructions);
		}
		
		/**
		* Set the instructions for this StepStatusEdit.
		* @param instructions The instructions.
		*/
		public void setInstructionsHtml(String instructions)
		{
			if(instructions != null)
				m_instructions = instructions;
		}

		/**
		* Set the site for this StepStatusEdit.
		* @param site The site id.
		*/
		public void setSite(String site)
		{
			m_site = site;
		}
		
		/**
		* Take all values from this object.
		* @param status The StepStatus object to take values from.
		*/
		protected void set(StepStatus status)
		{
			setAll(status);

		}   // set

		/**
		* Access the event code for this edit.
		* @return The event code for this edit.
		*/
		protected String getEvent() { return m_event; }
	
		/**
		* Set the event code for this edit.
		* @param event The event code for this edit.
		*/
		protected void setEvent(String event) { m_event = event; }

		/**
		* Access the resource's properties for modification
		* @return The resource's properties.
		*/
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;

		}	// getPropertiesEdit

		/**
		* Enable editing.
		*/
		protected void activate()
		{
			m_active = true;

		}	// activate

		/**
		* Check to see if the edit is still active, or has already been closed.
		* @return true if the edit is active, false if it's been closed.
		*/
		public boolean isActiveEdit()
		{
			return m_active;

		}	// isActiveEdit

		/**
		* Close the edit object - it cannot be used after this.
		*/
		protected void closeEdit()
		{
			m_active = false;

		}	// closeEdit

		/*******************************************************************************
		* SessionStateBindingListener implementation
		*******************************************************************************/
	
		/**
		* Accept notification that this object has been bound as a SessionState attribute.
		* @param sessionStateKey The id of the session state which holds the attribute.
		* @param attributeName The id of the attribute to which this object is now the value.
		*/
		public void valueBound(String sessionStateKey, String attributeName) {}
	
		/**
		* Accept notification that this object has been removed from a SessionState attribute.
		* @param sessionStateKey The id of the session state which held the attribute.
		* @param attributeName The id of the attribute to which this object was the value.
		*/
		public void valueUnbound(String sessionStateKey, String attributeName)
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
	
		}	// valueUnbound

		
	}//BaseStepStatusEdit

	
	/*******************************************************************************
	* MPRecord implementation
	*******************************************************************************/	
	/**
	*
	* Contains a Rackham MPathways data record
	* 
	*
	private class MPRecord
	{
		/**
		* All dates are formatted as mm/dd/ccyy.  
		* MPEXT.txt Data Structure:
		* 1 ¦  Emplid					¦  A9	| Student's emplid
		* 2 ¦  Acad_prog				¦  A9	| Academic Program Code
		* 3 ¦  Anticipate_Anticipate_1	¦  A15	| Adv to cand term code
		* 4	¦  Date_compl				¦  D	| Date milestone was completed 
		* 5 ¦  Milestone				¦  A10	| name of milestone PRELIM or ADVCAND
		* 6 | Academic plan				|  A4	| Field of study and degree (e.g. 1220PHD1)
		* 7 | Campus id					|  A1-A8| Student's uniqname (Chef id)
		*
		public String m_umid = null;
		public String m_acad_prog = null;
		public String m_anticipate = null;
		public String m_date_compl = null;
		public String m_milestone = null;
		public String m_academic_plan = null;
		public String m_campus_id = null;
		
		//methods
		public String getUmid(){ return m_umid; }
		public String getAcad_prog(){ return m_acad_prog; }
		public String getAnticipate(){ return m_anticipate; }
		public String getDate_compl(){ return m_date_compl; }
		public String getMilestone(){ return m_milestone; }
		public String getAcademicPlan() { return m_academic_plan; }
		public String getCampusId() { return m_campus_id; }
		
	}//MPRecord

	/*******************************************************************************
	* OARDRecord implementation
	*******************************************************************************/	
	
	/**
	*
	* Contains a Rackham OARD data record
	* 
	*
	private class OARDRecord
	{
		/**
		* corresponding to OARD db output fields
		* All dates are formatted as mm/dd/ccyy.  
		* 
		* OARDEXT.txt Data Structure:
		* STRUCT ¦ Field Name            ¦Field Type
		* 1 ¦  Emplid                    ¦  A8 - Student's emplid
		* 2 ¦  Fos                       ¦  A4    - Students field of study code
		* 3 ¦  Lname                     ¦  A25 - Students last name
		* 4 ¦  Fname                     ¦  A30 - Students first name
		* 5 ¦  Degterm trans             ¦  A7 - Students degree term as TT-CCYY (e.g. FA-2003)
		* 6 ¦  Oral exam date            ¦  D - Date of oral defense
		* 7 ¦  Oral exam time            ¦  A7 - Time of oral defense
		* 8 ¦  Oral exam place           ¦  A25 - Place of oral defense
		* 9 ¦  Committee approved date   ¦  D - date committee was approved
		*10 ¦  First format date         ¦  D - date of pre defense meeting in Rackham
		*11 ¦  Binder receipt date       ¦  D 
		*12 ¦  Fee requirement met       ¦  A1 - Y or N 
		*13 ¦  Fee date receipt seen     ¦  D 
		*14 ¦  Pub fee date received     ¦  D
		*15 ¦  Oral report return date   ¦  D
		*16 ¦  Unbound date              ¦  D
		*17 ¦  Abstract date             ¦  D
		*18 ¦  Bound copy received date  ¦  D
		*19 ¦  Diploma application date  ¦  D
		*20 ¦  Contract received date    ¦  D
		*21 ¦  NSF Survey date           ¦  D
		*22 ¦  Degree conferred date     ¦  D - date the degree was conferred in OARD system
		*23 ¦  Final format recorder     ¦  A3 - initials
		*24 ¦  Update date               ¦  D - date record was last modified
		*25 ¦  Comm cert date            ¦  D -
		*26 ¦  Role                      ¦  A2 - role code
		*27 ¦  Member                    ¦  A40 - faculty member name
		*28 ¦  Eval date                 ¦  D - evaluation received date
		*29 |  Campus id                 |  A1-A8 - student's uniqname (Chef id)
		*
		public String m_umid = null;
		public String m_fos = null;
		public String m_lname = null;
		public String m_fname = null;
		public String m_degreeterm_trans = null;
		public String m_oral_exam_date = null;
		public String m_oral_exam_time = null;
		public String m_oral_exam_place = null;
		public String m_committee_approved_date = null;
		public String m_first_format_date = null;
		public String m_binder_receipt_date = null;
		public String m_fee_requirement_met = null;
		public String m_fee_date_receipt_seen = null;
		public String m_pub_fee_date_received = null;
		public String m_oral_report_return_date = null;
		public String m_unbound_date = null;
		public String m_abstract_date = null;
		public String m_bound_copy_received_date = null;
		public String m_diploma_application_date = null;
		public String m_contract_received_date = null;
		public String m_nsf_survey_date = null;
		public String m_degree_conferred_date = null;
		public String m_final_format_recorder = null;
		public String m_update_date = null;
		public String m_comm_cert_date = null;
		public String m_role = null;
		public String m_member = null;
		public String m_eval_date = null;
		public String m_campus_id = null;
		
		//methods
		public String getUmid(){ return m_umid; }
		public String getFos(){ return m_fos; }
		public String getLname(){ return m_lname; }
		public String getFname(){ return m_fname; }
		public String getDegreeterm_trans(){ return m_degreeterm_trans; }
		public String getOral_exam_date(){ return m_oral_exam_date; }
		public String getOral_exam_time(){ return m_oral_exam_time; }
		public String getOral_exam_place(){ return m_oral_exam_place; }
		public String getCommittee_approved_date(){ return m_committee_approved_date; }
		public String getFirst_format_date(){ return m_first_format_date; }
		public String getBinder_receipt_date(){ return m_binder_receipt_date; }
		public String getFee_requirement_met(){ return m_fee_requirement_met; }
		public String getFee_date_receipt_seen(){ return m_fee_date_receipt_seen; }
		public String getPub_fee_date_received(){ return m_pub_fee_date_received; }
		public String getOral_report_return_date(){ return m_oral_report_return_date; }
		public String getUnbound_date(){ return m_unbound_date; }
		public String getAbstract_date(){ return m_abstract_date; }
		public String getBound_copy_received_date(){ return m_bound_copy_received_date; }
		public String getDiploma_application_date(){ return m_diploma_application_date; }
		public String getContract_received_date(){ return m_contract_received_date; }
		public String getNsf_survey_date(){ return m_nsf_survey_date; }
		public String getDegree_conferred_date(){ return m_degree_conferred_date; }
		public String getFinal_format_recorder(){ return m_final_format_recorder; }
		public String getUpdate_date(){ return m_update_date; }
		public String getComm_cert_date(){ return m_comm_cert_date; }
		public String getRole(){ return m_role; }
		public String getMember(){ return m_member; }
		public String getEval_date() { return m_eval_date; }
		public String getCampusId() { return m_campus_id; }
		
	} // OARDRecord
	*/
	
	/**
	* Comparator for alphabetizing User's Display Name
	*/
	private class UserComparator
		implements Comparator
	{
		/**
		* Compare the two objects
		* @return int 
		*/
		public int compare(Object o1, Object o2)
		{
			int result = 0;
			try
			{
				User user1 = (User)o1;
				User user2 = (User)o2;
				String name1 = user1.getSortName();
				String name2 = user2.getSortName();
				result = name1.compareTo(name2);
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".UserComparator compare(Object o1, Object o2) " + e);
			}
			return result;
		}

		public boolean equals(Object o1, Object o2)
		{
			boolean retVal = false;
			try
			{
				User user1 = (User)o1;
				User user2 = (User)o2;
				String name1 = user1.getSortName();
				String name2 = user2.getSortName();
				if(name1.compareTo(name2) == 0)
					retVal = true;
			}
			catch(Exception e)
			{
				m_logger.warn(this + ".UserComparator equals(Object o1, Object o2) " + e);
			}
			return retVal;
		}
	}

	/*******************************************************************************
	* CandidateInfo implementation
	*******************************************************************************/	
	protected class BaseCandidateInfo
		implements CandidateInfo
	{
		/** The CandidateInfo id. */
		public String m_id;
		
		/** The user's University id - from extracts. */
		public String m_emplid;
		
		/** The users' CHEF user id. */
		public String m_chefId;
		
		/** The id for the user's Rackham program - from the Rackham database. */
		public String m_program;
		
		/** The id for the user's parent(department) site. */
		public String m_parentSiteId;
		
		/** The id of the user's site. */
		public String m_site;
		
		/** The properties. */
		public ResourcePropertiesEdit m_properties;

		
		/** RACKHAM DATA */
		public String m_advCandDesc;
		public String m_oralExamPlace;
		public String m_oralExamTime;
		//public String m_finalFormatRecorder;
		public String m_degreeTermTrans;
		public String m_milestone;
		public Vector m_committeeEvalsCompleted;
		public Time m_milestoneCompleted;
		public String m_prelimMilestone;
		public Time m_prelimTimeMilestoneCompleted;
		public String m_advcandMilestone;
		public Time m_advcandTimeMilestoneCompleted;
		public Time m_committeeApproval;
		public Time m_oralExam;
		public Time m_firstFormat;
		public Time m_oralReportReturned;
		public Time m_committeeCert;
		//public Time m_binderReceipt;
		//public Time m_receiptSeen;
		//public Time m_pubFee;
		//public Time m_unbound;
		//public Time m_abstract;
		//public Time m_bound;
		//public Time m_diplomaApp;
		//public Time m_contract;
		//public Time m_survey;
		public Time m_degreeConferred;
		//public boolean m_feeRequirementMet;
		protected Vector m_timeCommitteeEval;
		public boolean m_MPRecInExtract;
		public boolean m_OARDRecInExtract;
		
		/**
		* Constructor.
		* @param infoId The CandidateInfo id.
		* @param site The site id.
		*/
		public BaseCandidateInfo(String infoId, String site)
		{
			m_id = infoId;
			m_site = site;
			m_chefId = "";
			m_program = "";
			m_parentSiteId = "";
			m_properties = new BaseResourcePropertiesEdit();
			addLiveProperties(m_properties);
			//m_feeRequirementMet = false;
			m_committeeEvalsCompleted = new Vector();
			m_timeCommitteeEval = new Vector();
			m_MPRecInExtract = false;
			m_OARDRecInExtract = false;
		}

		/**
		* Copy constructor.
		* @param info The CandidateInfo to copy.
		*/
		public BaseCandidateInfo(CandidateInfo info)
		{
			setAll(info);

		}	// BaseCandidateInfo
		
		/**
		* Construct from an existing definition, in xml.
		* @param el The CandidateInfo in XML in a DOM element.
		*/
		public BaseCandidateInfo(Element el)
		{
			m_properties = new BaseResourcePropertiesEdit();

			m_id = el.getAttribute("id");
			m_emplid = el.getAttribute("emplid");
			m_chefId = el.getAttribute("chefid");
			m_program = el.getAttribute("program");
			m_parentSiteId = el.getAttribute("siteid");
			m_site = el.getAttribute("site");
			m_committeeEvalsCompleted = new Vector();
			m_timeCommitteeEval = new Vector();
			m_MPRecInExtract = false;
			m_OARDRecInExtract = false;
			
			// the children (roles, properties)
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for(int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element)child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					// re-create properties
					m_properties = new BaseResourcePropertiesEdit(element);
				}
			}
		}

		/**
		* Deep copy of this object.
		* @param info - The CandidateInfo object to be copied.
		*/
		protected void setAll(CandidateInfo info)
		{
			m_id = info.getId();
			m_emplid = info.getEmplid();
			m_chefId = info.getChefId();
			m_program = info.getProgram();
			m_parentSiteId = info.getParentSite();
			m_site = info.getSite();
			m_MPRecInExtract = info.getMPRecInExtract();
			m_OARDRecInExtract = info.getOARDRecInExtract();
			m_timeCommitteeEval = new Vector();
			m_committeeEvalsCompleted = new Vector();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(info.getProperties());

		}   // setAll
		
		/**
		* Serialize the resource into XML, adding an element to the doc under the top of the stack element.
		* @param doc The DOM doc to contain the XML (or null for a string return).
		* @param stack The DOM elements, the top of which is the containing element of the new "resource" element.
		* @return The newly added element.
		*/
		public Element toXml(Document doc, Stack stack)
		{
			Element info = doc.createElement("info");
			
			if (stack.isEmpty())
			{
				doc.appendChild(info);
			}
			else
			{
				((Element)stack.peek()).appendChild(info);
			}

			stack.push(info);
			
			info.setAttribute("id", m_id);
			info.setAttribute("chefid", m_chefId);
			info.setAttribute("emplid", m_emplid);
			info.setAttribute("siteid", m_parentSiteId);
			info.setAttribute("program", m_program);
			info.setAttribute("site", m_site);
			
			m_properties.toXml(doc, stack);
			
			stack.pop();
			
			return info;
		}
	
	
		public String getId()
		{
			return m_id;
		}

		/**
		* Access the URL which can be used to access the resource.
		* @return The URL which can be used to access the resource.
		*/
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;

		}   // getUrl

		/**
		* Access the internal reference which can be used to access the resource from within the system.
		* @return The the internal reference which can be used to access the resource from within the system.
		*/
		public String getReference()
		{
			return infoReference(m_site, m_id);

		}   // getReference

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		* Access the resources's properties.
		* @return The resources's properties.
		*/
		public ResourceProperties getProperties()
		{
			return m_properties;

		}   // getProperties
		
		/**
		* Access the user's advanced to candidacy term
		* @return The user's term string.
		*/
		public String getAdvCandDesc()
		{
			return m_advCandDesc;
		}


		/**
		* Access the user's CHEF user id.
		* @return The user id string.
		*/
		public String getChefId()
		{
			return m_chefId;
		}

		/**
		* Access the user's university id.
		* @return The user's university id string.
		*/
		public String getEmplid()
		{
			return m_emplid;
		}
		
		/**
		* Access the students degree term as TT-CCYY (e.g. FA-2003)
		* @return The degree term string.
		*/
		public String getDegreeTermTrans()
		{
			return m_degreeTermTrans;
		}
		
		/**
		* Access the user's Rackham program id.
		* @return The user's program id string.
		*/
		public String getProgram()
		{
			return m_program;
		}

		/**
		* Access the partent department site id.
		* @return The site id string.
		*/
		public String getParentSite()
		{
			return m_parentSiteId;
		}

		/**
		* Access the site id for the CandidateInfo.
		* @return the site id.
		*/
		public String getSite()
		{
			return m_site;
		}
		
		/**
		* Access the place of the oral exam.
		* @return the place of the oral exam.
		*/
		public String getOralExamPlace()
		{
			return m_oralExamPlace;
		}
		
		/**
		* Access the oral exam date.
		* @return the date of the oral exam.
		*/
		public Time getOralExam()
		{
			return m_oralExam;
		}
		
		/**
		* Access the oral exam time.
		* @return The oral exam time.
		*/
		public String getOralExamTime()
		{
			return m_oralExamTime;
		}
		
		/**
		* Access the flag indicating values from MP record.
		* @return true if MP record in extract, false otehrwise.
		*/
		public boolean getMPRecInExtract()
		{
			return m_MPRecInExtract;
		}
		
		/**
		* Access the flag indicating values from OARD records.
		* @return true if OARD record in extract, false otherwise.
		*/
		public boolean getOARDRecInExtract()
		{
			return m_OARDRecInExtract;
		}
		
		/**
		* Access the time the committee evaluations were completed.
		* @return The latest time held in the m_timeCommitteeEval vector.
		*/
		public Time getTimeCommitteeEvalCompleted()
		{
			boolean completed = true;
			Time latestTime = null;
			Time aTime = null;
			for(int x = 0; x < m_timeCommitteeEval.size(); x++)
			{
				aTime = (Time)m_timeCommitteeEval.get(x);
				if(aTime == null)
				{
					completed = false;
				}
				else
				{
					if(latestTime == null)
					{
						latestTime = aTime;
					}

					if(latestTime.before(aTime))
					{
						latestTime = aTime;
					}
				}
			}

			if(!completed)
			{
				latestTime = null;
			}

			return latestTime;
		}

		/**
		* Access whether the committee evaluations are completed.
		* @return False if the m_timeCommitteeEval vector is empty, true otherwise.
		*/
		public boolean getCommitteeEvalCompleted()
		{
			boolean completed = true;
			Time aTime = null;
			for(int x = 0; x < m_timeCommitteeEval.size(); x++)
			{
				aTime = (Time)m_timeCommitteeEval.get(x);
				if(aTime == null)
					completed = false;
			}
			return completed;
		}
		
		/**
		* Access the committee members and times evaluations are completed.
		* @return List of members and eval completed times, or null if not set.
		*/
		public Vector getCommitteeEvalsCompleted()
		{
			return m_committeeEvalsCompleted;
		}
	
		/**
		* Access the time a Rackham auto-validated step was completed.
		* @param The validation id for the Rackham auto-validated step.
		* @return Time - the Time of completion, or null if the step is not completed.
		*/
		public Time getExternalValidation(int stepNum)
		{
			Time retVal = null;
			
			switch(stepNum)
			{
				case 1:
					if(( ("prelim".equalsIgnoreCase(m_prelimMilestone)) && (m_prelimTimeMilestoneCompleted != null) ))
					{
						retVal = m_prelimTimeMilestoneCompleted;
					}
					break;
				
				case 2:
					if(("advcand".equalsIgnoreCase(m_advcandMilestone)) && (m_advcandTimeMilestoneCompleted != null) && (m_advCandDesc != null))
					{
						retVal = m_advcandTimeMilestoneCompleted;
					}
					break;
				
				case 3:
					retVal = m_committeeApproval;
					break;
				
				case 4:
					if((m_oralExam != null) && (m_oralExamPlace != null))
					{
						retVal = m_firstFormat;
					}
					break;
				
				case 5:
					retVal = m_firstFormat;
					break;
				
				case 6:
					retVal = getTimeCommitteeEvalCompleted();
					break;


				case 7:
					retVal = m_oralReportReturned;
					break;


				case 8:
					retVal = m_committeeCert;
					break;

				/*
				case 9:
					if(m_finalFormatRecorder != null && !m_finalFormatRecorder.equals(""))
					{
						retVal = TimeService.newTime();
					}
					break;

				case 10:
					if((m_committeeApproval != null) && 
					   (m_binderReceipt != null) &&
					   ((m_feeRequirementMet && m_receiptSeen==null) || 
					   (!m_feeRequirementMet && m_receiptSeen!=null)) &&
					   (m_pubFee != null) && 
					   (m_oralReportReturned != null) &&
					   (m_abstract != null) &&
					   (m_diplomaApp != null) && 
					   (m_contract != null) && 
					   (m_survey != null) && 
					   (m_committeeCert != null))
					{
						retVal = m_committeeCert;
					}
					break;
				*/

				case 11:
					if((m_degreeTermTrans != null) && (m_degreeConferred != null))
					{
						retVal = m_degreeConferred;
					}
					break;

				/*
				case 12:
					retVal = m_bound;
					break;
				*/
			}
			
			return retVal;
		}
		
		/**
		* Are these objects equal?  If they are both CandidateInfo objects, and they have
		* matching id's, they are.
		* @return true if they are equal, false if not.
		*/
		public boolean equals(Object obj)
		{
			if (!(obj instanceof CandidateInfo)) return false;
			return ((CandidateInfo)obj).getId().equals(getId());

		}   // equals

		/**
		* Make a hash code that reflects the equals() logic as well.
		* We want two objects, even if different instances, if they have the same id to hash the same.
		*/
		public int hashCode()
		{
			return getId().hashCode();

		}	// hashCode

		/**
		* Compare this object with the specified object for order.
		* @return A negative integer, zero, or a positive integer as this object is
		* less than, equal to, or greater than the specified object.
		*/
		public int compareTo(Object obj)
		{
			if (!(obj instanceof CandidateInfo)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their sort names
			int compare = getChefId().compareTo(((CandidateInfo)obj).getChefId());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((CandidateInfo)obj).getId());
			}

			return compare;

		}	// compareTo


	}//BaseCandidateInfo

	
	
	/*******************************************************************************
	* CandidateInfoEdit implementation
	*******************************************************************************/
	protected class BaseCandidateInfoEdit
		extends BaseCandidateInfo
		implements CandidateInfoEdit, SessionStateBindingListener
	{
		/** The event code for the edit. */
		protected String m_event = null;
		
		/** Active flag. */
		protected boolean m_active = false;
		
		/**
		* Constructor.
		* @param infoId The CandidateInfo id.
		* @param site The site id.
		*/
		public BaseCandidateInfoEdit(String infoId, String site)
		{
			super(infoId, site);

		}   // BaseCandidateInfoEdit

		/**
		* Construct from an existing definition, in xml.
		* @param el The CandidateInfoEdit in XML in a DOM element.
		*/
		public BaseCandidateInfoEdit(Element el)
		{
			super(el);

		}	// BaseCandidateInfoEdit

		/**
		* Copy constructor.
		* @param info The CandidateInfo to be copied.
		*/
		public BaseCandidateInfoEdit(CandidateInfo info)
		{
			super(info);

		}	// BaseCandidateInfoEdit

		/**
		* Clean up.
		*/
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}

		}	// finalize


		/**
		* Set the user's university id.
		* @param emplid The user's university id string.
		*/
		public void setEmplid(String emplid)
		{
			m_emplid = emplid;
		}
		
		/**
		* Set the user's CHEF user id.
		* @param chefId The user id string.
		*/
		public void setChefId(String chefid)
		{
			m_chefId = chefid;
		}
		
		/**
		* Set the text for display with the time committee eval completed
		* @param evals The collection of individual member/evaluation completed Strings.
		*/
		public void addCommitteeEvalsCompleted(Collection evals)
		{
			if(m_committeeEvalsCompleted != null)
			{
				m_committeeEvalsCompleted.addAll(evals);
			}
		}
		
		/**
		* Set the user's Rackham program id.
		* @param program The user's program id string.
		*/
		public void setProgram(String program)
		{
			m_program = program;
		}
		
		/**
		* Set the user's parent (department) site.
		* @param parentSiteId The site id of the user's parent (department) site.
		*/
		public void setParentSite(String parentSiteId)
		{
			m_parentSiteId = parentSiteId;
		}
		
		/**
		* Set the site for the CandidateInfo.
		* @param site The site id of the CandidateInfo's site.
		*/
		public void setSite(String site)
		{
			m_site = site;
		}
		
		/**
		* Set the Rackham data value.
		* @param desc The Advance to Candidacy Description.
		*/
		public void setAdvCandDesc(String desc)
		{
			m_advCandDesc = desc;
		}

		/**
		* Set the Rackham data value.
		* @param milestone The milestone.
		*/
		public void setMilestone(String milestone)
		{
			m_milestone = milestone;
		}
		
		/**
		* Set the flag indicating MP record in extract.
		* @param present true if MP record in extract, false otherwise.
		*/
		public void setMPRecInExtract(boolean present)
		{
			m_MPRecInExtract = present;
		}
		
		/**
		* Set the flag indicating OARD record in extract.
		* @param present true if OARD record in extract, false otherwise.
		*/
		public void setOARDRecInExtract(boolean present)
		{
			m_OARDRecInExtract = present;
		}
		
		/**
		* Set the Rackham data value.
		* @param milestone The prelim milestone.
		*/
		public void setPrelimMilestone(String milestone)
		{
			m_prelimMilestone = milestone;
		}
		
		/**
		* Set the Rackham data value.
		* @param completed The prelim milestone completion time.
		*/
		public void setPrelimTimeMilestoneCompleted(Time completed)
		{
			m_prelimTimeMilestoneCompleted = completed;
		}
		
		/**
		* Set the Rackham data value.
		* @param milestone The advcand milestone.
		*/
		public void setAdvcandMilestone(String milestone)
		{
			m_advcandMilestone = milestone;
		}
		
		/**
		* Set the Rackham data value.
		* @param completed The advcand milestone completion time.
		*/
		public void setAdvcandTimeMilestoneCompleted(Time completed)
		{
			m_advcandTimeMilestoneCompleted = completed;
		}
		
		/**
		* Set the Rackham data value.
		* @param completed The milestone completion time.
		*/
		public void setTimeMilestoneCompleted(Time completed)
		{
			m_milestoneCompleted = completed;
		}

		/**
		* Set the time of completion of the Rackham milestone.
		* @param approval The committee approval time.
		*/
		public void setTimeCommitteeApproval(Time approval)
		{
			m_committeeApproval = approval;
		}

		/**
		* Set the time of completion of the Rackham milestone.
		* @param exam The oral exam time.
		*/
		public void setTimeOralExam(Time exam)
		{
			m_oralExam = exam;
		}

		/**
		* Set the Rackham data value.
		* @param place The oral exam place.
		*/
		public void setOralExamPlace(String place)
		{
			if(place != null)
				m_oralExamPlace = place;
		}
		
		/**
		* Set the Rackham data value.
		* @param time The oral exam time (e.g., 11:30 am).
		*/
		public void setOralExamTime(String time)
		{
			if(time != null)
				m_oralExamTime = time;
		}

		/**
		* Set the time of completion of the Rackham milestone.
		* @param format The first format time.
		*/
		public void setTimeFirstFormat(Time format)
		{
			m_firstFormat = format;
		}

		/**
		* Add a Rackham data value.
		* @param timeCompleted The time of committee evaluation.
		*/
		public void addTimeCommitteeEval(Time timeCompleted)
		{
			m_timeCommitteeEval.add(timeCompleted);
		}

		/**
		* Set the time of completion of the Rackham milestone.
		* @param timeReturned The time oral report returned.
		*/
		public void setTimeOralReportReturned(Time timeReturned)
		{
			m_oralReportReturned = timeReturned;
		}

		/**
		* Set the time of completion of the Rackham milestone.
		* @param timeOfCert The time of committee certification.
		*/
		public void setTimeCommitteeCert(Time timeOfCert)
		{
			m_committeeCert = timeOfCert;
		}

		/**
		* Set the Rackham data value.
		* @param recorder The final format recorder.
		*
		public void setFinalFormatRecorder(String recorder)
		{
			if(recorder != null)
				m_finalFormatRecorder = recorder;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param receipt The binder receipt time.
		*
		public void setTimeBinderReceipt(Time receipt)
		{
			m_binderReceipt = receipt;
		}
		*/

		/**
		* Set the Rackham data value.
		* @param met The fee requirement.
		*
		public void setFeeRequirementMet(boolean met)
		{
			m_feeRequirementMet = met;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param seen The time receipt seen.
		*
		public void setTimeReceiptSeen(Time seen)
		{
			m_receiptSeen = seen;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param fee The time the publication fee paid.
		*
		public void setTimePubFee(Time fee)
		{
			m_pubFee = fee;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param unbound The unbound time.
		*
		public void setTimeUnbound(Time unbound)
		{
			m_unbound = unbound;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param abst The abstract time.
		*
		public void setTimeAbstract(Time abst)
		{
			m_abstract = abst;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param bound The bound time.
		*
		public void setTimeBound(Time bound)
		{
			m_bound = bound;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param app The diploma app time.
		*
		public void setTimeDiplomaApp(Time app)
		{
			m_diplomaApp = app;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param contract The contract time.
		*
		public void setTimeContract(Time contract)
		{
			m_contract = contract;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param survey The survey time.
		*
		public void setTimeSurvey(Time survey)
		{
			m_survey = survey;
		}
		*/

		/**
		* Set the time of completion of the Rackham milestone.
		* @param conferred The degree conferred time.
		*/
		public void setTimeDegreeConferred(Time conferred)
		{
			m_degreeConferred = conferred;
		}

		/**
		* Set the Rackham data value.
		* @param trans The degree term trans.
		*/
		public void setDegreeTermTrans(String trans)
		{
			if(trans != null)
				m_degreeTermTrans = trans;
		}

		/**
		* Take all values from this object.
		* @param info The CandidateInfo object to take values from.
		*/
		protected void set(CandidateInfo info)
		{
			setAll(info);

		}   // set

		/**
		* Access the event code for this edit.
		* @return The event code for this edit.
		*/
		protected String getEvent() { return m_event; }
	
		/**
		* Set the event code for this edit.
		* @param event The event code for this edit.
		*/
		protected void setEvent(String event) { m_event = event; }

		/**
		* Access the resource's properties for modification
		* @return The resource's properties.
		*/
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			return m_properties;

		}	// getPropertiesEdit

		/**
		* Enable editing.
		*/
		protected void activate()
		{
			m_active = true;

		}	// activate

		/**
		* Check to see if the edit is still active, or has already been closed.
		* @return true if the edit is active, false if it's been closed.
		*/
		public boolean isActiveEdit()
		{
			return m_active;

		}	// isActiveEdit

		/**
		* Close the edit object - it cannot be used after this.
		*/
		protected void closeEdit()
		{
			m_active = false;

		}	// closeEdit

		/*******************************************************************************
		* SessionStateBindingListener implementation
		*******************************************************************************/
	
		/**
		* Accept notification that this object has been bound as a SessionState attribute.
		* @param sessionStateKey The id of the session state which holds the attribute.
		* @param attributeName The id of the attribute to which this object is now the value.
		*/
		public void valueBound(String sessionStateKey, String attributeName) {}
	
		/**
		* Accept notification that this object has been removed from a SessionState attribute.
		* @param sessionStateKey The id of the session state which held the attribute.
		* @param attributeName The id of the attribute to which this object was the value.
		*/
		public void valueUnbound(String sessionStateKey, String attributeName)
		{
	
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
	
		}	// valueUnbound

	}//BaseCandidateInfoEdit
	
	

	/*******************************************************************************
	* Storage implementations
	*******************************************************************************/
	
	
	/*******************************************************************************
	* BlockGrantGroupStorage
	*******************************************************************************/

	protected interface BlockGrantGroupStorage
	{
		/**
		* Open.
		*/
		public void open();

		/**
		* Close.
		*/
		public void close();

		/**
		* Check if a Block Grant Group by this id exists.
		* @param id The Block Grant Group id.
		* @return true if a Block Grant Group by this id exists, false if not.
		*/
		public boolean check(String id);

		/**
		* Get the Block Grant Group with this id, or null if not found.
		* @param id The Block Grant Group id.
		* @param id The Block Grant Group site.
		* @return The Block Grant Group with this id, or null if not found.
		*/
		public BlockGrantGroup get(String id);

		/**
		* Get all Block Grant Groups.
		* @return The list of all Block Grant Groups.
		*/
		public List getAll();

		/**
		* Add a new Block Grant Group with this id.
		* @param id The Block Grant Group id.
		* @param site The Block Grant Group site.
		* @return The locked BlockGrantGroup object with this id, or null if the id is in use.
		*/
		public BlockGrantGroupEdit put(String id, String site);

		/**
		* Get a lock on the Block Grant Group with this id, or null if a lock cannot be gotten.
		* @param id The Block Grant Group id.
		* @return The locked BlockGrantGroup with this id, or null if this records cannot be locked.
		*/
		public BlockGrantGroupEdit edit(String id);

		/**
		* Commit the changes and release the lock.
		* @param blockGrantGroupEdit The blockGrantGroupEdit to commit.
		*/
		public void commit(BlockGrantGroupEdit blockGrantGroupEdit);

		/**
		* Cancel the changes and release the lock.
		* @param blockGrantGroupEdit The BlockGrantGroupEdit to commit.
		*/
		public void cancel(BlockGrantGroupEdit blockGrantGroupEdit);

		/**
		* Remove this BlockGrantGroup.
		* @param blockGrantGroup The BlockGrantGroup to remove.
		*/
		public void remove(BlockGrantGroupEdit blockGrantGroupEdit);

	}   // BlockGrantGroupStorage


	/*******************************************************************************
	* DissertationStorage
	*******************************************************************************/

	protected interface DissertationStorage
	{
		/**
		* Open.
		*/
		public void open();

		/**
		* Close.
		*/
		public void close();

		/**
		* Check if a dissertation by this id exists.
		* @param id The dissertation id.
		* @return true if a dissertation by this id exists, false if not.
		*/
		public boolean check(String id);

		/**
		* Get the dissertation with this id, or null if not found.
		* @param id The dissertation id.
		* @return The dissertation with this id, or null if not found.
		*/
		public Dissertation get(String id);

		/**
		* Get all dissertations.
		* @return The list of all dissertations.
		*/
		public List getAll();

		/**
		* Add a new dissertation with this id.
		* @param id The dissertation id.
		* @return The locked Dissertation object with this id, or null if the id is in use.
		*/
		public DissertationEdit put(String id, String site);

		/**
		* Get a lock on the dissertation with this id, or null if a lock cannot be gotten.
		* @param id The dissertation id.
		* @return The locked Dissertation with this id, or null if this records cannot be locked.
		*/
		public DissertationEdit edit(String id);

		/**
		* Commit the changes and release the lock.
		* @param dissertation The dissertation to commit.
		*/
		public void commit(DissertationEdit dissertation);

		/**
		* Cancel the changes and release the lock.
		* @param dissertation The dissertation to commit.
		*/
		public void cancel(DissertationEdit dissertation);

		/**
		* Remove this dissertation.
		* @param dissertation The dissertation to remove.
		*/
		public void remove(DissertationEdit dissertation);
		
		/**
		* Get dissertations of type (e.g., "Dissertation Steps", "Disseration Steps: Music Performance").
		* @param type The type of dissertation.
		* @return The list of such dissertations.
		*/
		public List getAllOfType(String type);
		
		/**
		* Get the dissertation for this site.
		* @param id The site id.
		* @return The dissertation.
		*/
		public Dissertation getForSite(String id);
		
		/**
		* Get the dissertation for this site that is of this type (e.g., "Dissertation Steps", "Disseration Steps: Music Performance").
		* @param id The site id.
		* @param type The dissertation type.
		* @return The dissertation.
		*/
		public Dissertation getForSiteOfType(String id, String type);

	}   // DissertationStorage

	
	/*******************************************************************************
	* DissertationStepStorage
	*******************************************************************************/

	protected interface DissertationStepStorage
	{
		/**
		* Open.
		*/
		public void open();

		/**
		* Close.
		*/
		public void close();

		/**
		* Check if a DissertationStep by this id exists.
		* @param id The DissertationStep id.
		* @return true if a DissertationStep by this id exists, false if not.
		*/
		public boolean check(String id);

		/**
		* Get the DissertationStep with this id, or null if not found.
		* @param id The DissertationStep id.
		* @return The DissertationStep with this id, or null if not found.
		*/
		public DissertationStep get(String id);

		/**
		* Get all DissertationSteps.
		* @return The list of all DissertationSteps.
		*/
		public List getAll();

		/**
		* Add a new DissertationStep with this id.
		* @param id The DissertationStep id.
		* @return The locked DissertationStep object with this id, or null if the id is in use.
		*/
		public DissertationStepEdit put(String id, String site);

		/**
		* Get a lock on the DissertationStep with this id, or null if a lock cannot be gotten.
		* @param id The DissertationStep id.
		* @return The locked DissertationStep with this id, or null if this records cannot be locked.
		*/
		public DissertationStepEdit edit(String id);

		/**
		* Commit the changes and release the lock.
		* @param DissertationStep The DissertationStep to commit.
		*/
		public void commit(DissertationStepEdit step);

		/**
		* Cancel the changes and release the lock.
		* @param DissertationStep The DissertationStep to commit.
		*/
		public void cancel(DissertationStepEdit step);

		/**
		* Remove this DissertationStep.
		* @param DissertationStep The DissertationStep to remove.
		*/
		public void remove(DissertationStepEdit step);

	}   // DissertationStepStorage


	/*******************************************************************************
	* CandidatePathStorage
	*******************************************************************************/

	protected interface CandidatePathStorage
	{
		/**
		* Open.
		*/
		public void open();

		/**
		* Close.
		*/
		public void close();

		/**
		* Check if a CandidatePath by this id exists.
		* @param id The CandidatePath id.
		* @return true if a CandidatePath by this id exists, false if not.
		*/
		public boolean check(String id);

		/**
		* Get the CandidatePath with this id, or null if not found.
		* @param id The CandidatePath id.
		* @return The CandidatePath with this id, or null if not found.
		*/
		public CandidatePath get(String id);

		/**
		* Get all CandidatePaths.
		* @return The list of all CandidatePaths.
		*/
		public List getAll();

		/**
		* Add a new CandidatePath with this id.
		* @param id The CandidatePath id.
		* @return The locked CandidatePath object with this id, or null if the id is in use.
		*/
		public CandidatePathEdit put(String id, String site);

		/**
		* Get a lock on the CandidatePath with this id, or null if a lock cannot be gotten.
		* @param id The CandidatePath id.
		* @return The locked CandidatePath with this id, or null if this records cannot be locked.
		*/
		public CandidatePathEdit edit(String id);

		/**
		* Commit the changes and release the lock.
		* @param path The CandidatePath to commit.
		*/
		public void commit(CandidatePathEdit path);

		/**
		* Cancel the changes and release the lock.
		* @param path The CandidatePath to commit.
		*/
		public void cancel(CandidatePathEdit path);

		/**
		* Remove this CandidatePath.
		* @param path The CandidatePath to remove.
		*/
		public void remove(CandidatePathEdit path);

		/**
		 * Determine if empty.
		 * @return true if empty, false if not.
		 */
		public boolean isEmpty();
		
		/**
		 * Determine if a CandidatePath of this Type exists.
		 * @param The type of the CandiatePath (e.g., "Dissertation Steps", "Dissertation Steps: Music Performance")
		 * @return true if exists, false if not.
		 */
		public boolean existsPathOfType(String type);
		
		/**
		 * Determine if a CandidatePath belonging to the parent site exists.
		 * @param The parent site id to use in checking.
		 * @return true if sucha  path exists, false if not.
		 */
		public boolean existsPathForParent(String siteId);
		
		/**
		* Determine if a CandidatePath of Type with candidate SortName mathing letter exists.
		* @param The type of the CandiatePath (e.g., "Dissertation Steps", "Dissertation Steps: Music Performance")
		* @param A letter of the alphabet, A-Za-z.
		* @return true if exists, false if not.
		*
		public boolean existsUserOfTypeForLetter(String type, char letter);
		*/
		
		/**
		* Get all CandidatePaths of this Type.
		* @param The type of the CandiatePath (e.g., "Dissertation Steps", "Dissertation Steps: Music Performance")
		* @return The list of such CandidatePaths.
		*/
		public List getAllOfType(String type);
		
		/**
		* Get sorted Users with CandidatePath of type and first letter of last name equal to letter.
		* @param The type of the CandiatePath (e.g., "Dissertation Steps", "Dissertation Steps: Music Performance")
		* @param The letter to compare with start of last name.
		* @return The list of such User objects.
		*
		public List getUsersOfTypeForLetter(String type, char letter);
		*/
		
		/**
		* Get CandidatePaths with parent site equal to site.
		* @param The id of the parent site.
		* @return The list of such CandidatePaths.
		*/
		public List getAllForParent(String site);
		
		/**
		* Get CandidatePath for candidate with id.
		* @param The id of the candidate.
		* @return The corresponding CandidatePath.
		*/
		public CandidatePath getForCandidate(String id);
		
		/**
		* Get CandidatePath for candidate with this site id.
		* @param The id of the Grad Tools student site.
		* @return The corresponding CandidatePath.
		*/
		public CandidatePath getForSite(String id);
		
		/**
		* Get List of CandidatePaths with SortLetter of letter.
		* @param The letter A-Za-z (SortLetter is set to upper case at CandidatePath creation).
		* @return The list of such CandidatePaths.
		*/
		public List getAllOfTypeForLetter(String type, String letter);
		
		/**
		* Get List of CandidatePaths with ParentSite of parentSite and SortLetter of letter.
		* @param The candidate's parent department site.
		* @param The letter A-Za-z (SortLetter is set to upper case at CandidatePath creation).
		* @return The list of such CandidatePaths.
		*/
		public List getAllOfParentForLetter(String parentSite, String letter);

	}   // CandidatePathStorage


	/*******************************************************************************
	* StepStatusStorage
	*******************************************************************************/

	protected interface StepStatusStorage
	{
		/**
		* Open.
		*/
		public void open();

		/**
		* Close.
		*/
		public void close();

		/**
		* Check if a StepStatus by this id exists.
		* @param id The StepStatus id.
		* @return true if a StepStatus by this id exists, false if not.
		*/
		public boolean check(String id);

		/**
		* Get the StepStatus with this id, or null if not found.
		* @param id The StepStatus id.
		* @return The StepStatus with this id, or null if not found.
		*/
		public StepStatus get(String id);

		/**
		* Get all StepStatuss.
		* @return The list of all StepStatuss.
		*/
		public List getAll();

		/**
		* Add a new StepStatus with this id.
		* @param id The StepStatus id.
		* @return The locked StepStatus object with this id, or null if the id is in use.
		*/
		public StepStatusEdit put(String id, String site);

		/**
		* Get a lock on the StepStatus with this id, or null if a lock cannot be gotten.
		* @param id The StepStatus id.
		* @return The locked StepStatus with this id, or null if this records cannot be locked.
		*/
		public StepStatusEdit edit(String id);

		/**
		* Commit the changes and release the lock.
		* @param status The StepStatus to commit.
		*/
		public void commit(StepStatusEdit status);

		/**
		* Cancel the changes and release the lock.
		* @param status The StepStatus to commit.
		*/
		public void cancel(StepStatusEdit status);

		/**
		* Remove this StepStatus.
		* @param status The StepStatus to remove.
		*/
		public void remove(StepStatusEdit status);

	}   // StepStatusStorage

	
	/*******************************************************************************
	* CandidateInfoStorage
	*******************************************************************************/

	protected interface CandidateInfoStorage
	{
		/**
		* Open.
		*/
		public void open();

		/**
		* Close.
		*/
		public void close();

		/**
		* Check if a CandidateInfo by this id exists.
		* @param id The CandidateInfo id.
		* @return true if a CandidateInfo by this id exists, false if not.
		*/
		public boolean check(String id);
		
		/**
		* Check if user is a Music Performance student.
		* @param id The user id.
		* @return true if a Music Performance student, false if not.
		*/
		public boolean checkMusic(String id);
		
		/**
		* Check if user with this id has a CandidateInfo record.
		* @param id The user id.
		* @return true if a CandidateInfo record exists, false if not.
		*/
		public boolean checkCandidate(String id);

		/**
		* Get the CandidateInfo with this id, or null if not found.
		* @param id The CandidateInfo id.
		* @return The CandidateInfo with this id, or null if not found.
		*/
		public CandidateInfo get(String id);
		
		/**
		* Get the employee id for the user with this id.
		* @param id The user id.
		* @return The employee id, or "" if not found.
		*/
		public String getEmplid(String id);
		
		/**
		* Get the CandidateInfo for the candidate with this id, or null if not found.
		* @param id The candidate id.
		* @return The CandidateInfo for candidiate with this id, or null if not found.
		*/
		public CandidateInfo getForCandidate(String id);
		
		/**
		* Get the CandidateInfo for the candidate with this employee id, or null if not found.
		* @param emplid The employee id.
		* @return The CandidateInfo for candidiate with this id, or null if not found.
		*/
		public CandidateInfo getForEmplid(String emplid);
		
		/**
		* Get the site id for the user's Rackham department.
		* @param idd The user's CHEF id.
		* @return The site id of the parent site.
		*/
		public String getParent(String id);

		/**
		* Get all CandidateInfos.
		* @return The list of all CandidateInfos.
		*/
		public List getAll();

		/**
		* Add a new CandidateInfo with this id.
		* @param id The CandidateInfo id.
		* @return The locked CandidateInfo object with this id, or null if the id is in use.
		*/
		public CandidateInfoEdit put(String id, String site);

		/**
		* Get a lock on the CandidateInfo with this id, or null if a lock cannot be gotten.
		* @param id The CandidateInfo id.
		* @return The locked CandidateInfo with this id, or null if this records cannot be locked.
		*/
		public CandidateInfoEdit edit(String id);

		/**
		* Commit the changes and release the lock.
		* @param info The CandidateInfo to commit.
		*/
		public void commit(CandidateInfoEdit info);

		/**
		* Cancel the changes and release the lock.
		* @param info The CandidateInfo to commit.
		*/
		public void cancel(CandidateInfoEdit info);

		/**
		* Remove this CandidateInfo.
		* @param info The CandidateInfo to remove.
		*/
		public void remove(CandidateInfoEdit info);

		/**
		 * Determine if empty
		 * @return true if empty, false if not.
		 */
		public boolean isEmpty();

	}   // CandidateInfoStorage


	/*******************************************************************************
	* StorageUser implementations
	*******************************************************************************/	
	
	/*******************************************************************************
	* BlockGrantGroupStorageUser implementation
	*******************************************************************************/

	protected class BlockGrantGroupStorageUser 
		implements StorageUser
	{
		/**
		* Construct a new container given just an id.
		* @param id The id for the new object.
		* @return The new container Resource.
		*/
		public Entity newContainer(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Entity newContainer(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Entity newContainer(Entity other) { return null; }

		/**
		* Construct a new resource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Entity newResource(Entity container, String id, Object[] others)
		{ return new BaseBlockGrantGroup(id, (String) others[0]); }

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Entity newResource(Entity container, Element element)
		{ return new BaseBlockGrantGroup(element); }

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Entity newResource(Entity container, Entity other)
		{ return new BaseBlockGrantGroup((BlockGrantGroup) other); }

		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new containe Resource.
		*/
		public Edit newContainerEdit(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Entity other) { return null; }

		/**
		* Construct a new rsource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseBlockGrantGroupEdit e = new BaseBlockGrantGroupEdit(id, (String)others[0]);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseBlockGrantGroupEdit e =  new BaseBlockGrantGroupEdit(element);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseBlockGrantGroupEdit e = new BaseBlockGrantGroupEdit((BlockGrantGroup) other);
			e.activate();
			return e;
		}

		/**
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		*/
		public Object[] storageFields(Entity r) { return null; }

		/**
		 * Check if this resource is in draft mode.
		 * @param r The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * @param r The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * @param r The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}

	}//BlockGrantGroupStorageUser
	
	
	/*******************************************************************************
	* DissertationStorageUser implementation
	*******************************************************************************/

	protected class DissertationStorageUser 
		implements StorageUser
	{
		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new container Resource.
		*/
		public Entity newContainer(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Entity newContainer(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Entity newContainer(Entity other) { return null; }

		/**
		* Construct a new resource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Entity newResource(Entity container, String id, Object[] others)
		{ return new BaseDissertation(id, (String) others[0]); }

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Entity newResource(Entity container, Element element)
		{ return new BaseDissertation(element); }

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Entity newResource(Entity container, Entity other)
		{ return new BaseDissertation((Dissertation) other); }

		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new containe Resource.
		*/
		public Edit newContainerEdit(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Entity other) { return null; }

		/**
		* Construct a new rsource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseDissertationEdit e = new BaseDissertationEdit(id, (String)others[0]);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseDissertationEdit e =  new BaseDissertationEdit(element);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseDissertationEdit e = new BaseDissertationEdit((Dissertation) other);
			e.activate();
			return e;
		}

		/**
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		*
		public Object[] storageFields(Resource r) { return null; }
		*/
		
		/* 
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		* DISSERTATION_FIELDS = "SITE", "TYPE"
		*/
		public Object[] storageFields(Entity r)
		{
			Object[] rv = new Object[2];
			rv[0] = ((Dissertation) r).getSite();
			rv[1] = ((Dissertation) r).getType();

			return rv;
		}

		/**
		 * Check if this resource is in draft mode.
		 * @param r The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * @param r The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * @param r The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}

	}//DissertationStorageUser
	
	
	/*******************************************************************************
	* DissertationStepStorageUser implementation
	*******************************************************************************/

	protected class DissertationStepStorageUser 
		implements StorageUser
	{
		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new container Resource.
		*/
		public Entity newContainer(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Entity newContainer(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Entity newContainer(Entity other) { return null; }

		/**
		* Construct a new resource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Entity newResource(Entity container, String id, Object[] others)
		{ return new BaseDissertationStep(id, (String)others[0]); }

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Entity newResource(Entity container, Element element)
		{ return new BaseDissertationStep(element); }

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Entity newResource(Entity container, Entity other)
		{ return new BaseDissertationStep((DissertationStep) other); }

		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new containe Resource.
		*/
		public Edit newContainerEdit(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Entity other) { return null; }

		/**
		* Construct a new rsource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseDissertationStepEdit e = new BaseDissertationStepEdit(id, (String)others[0]);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseDissertationStepEdit e =  new BaseDissertationStepEdit(element);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseDissertationStepEdit e = new BaseDissertationStepEdit((DissertationStep) other);
			e.activate();
			return e;
		}

		/**
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		*/
		public Object[] storageFields(Entity r) { return null; }

		/**
		 * Check if this resource is in draft mode.
		 * @param r The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * @param r The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * @param r The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}

	}//DissertationStepStorageUser

	
	/*******************************************************************************
	* CandidatePathStorageUser implementation
	*******************************************************************************/

	protected class CandidatePathStorageUser 
		implements StorageUser
	{
		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new container Resource.
		*/
		public Entity newContainer(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Entity newContainer(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Entity newContainer(Entity other) { return null; }

		/**
		* Construct a new resource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Entity newResource(Entity container, String id, Object[] others)
		{ return new BaseCandidatePath(id, (String)others[0]); }

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Entity newResource(Entity container, Element element)
		{ return new BaseCandidatePath(element); }

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Entity newResource(Entity container, Entity other)
		{ return new BaseCandidatePath((CandidatePath) other); }

		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new containe Resource.
		*/
		public Edit newContainerEdit(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Entity other) { return null; }

		/**
		* Construct a new rsource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseCandidatePathEdit e = new BaseCandidatePathEdit(id, (String)others[0]);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseCandidatePathEdit e =  new BaseCandidatePathEdit(element);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseCandidatePathEdit e = new BaseCandidatePathEdit((CandidatePath) other);
			e.activate();
			return e;
		}

		/**
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		*
		public Object[] storageFields(Resource r) { return null; }
		*/
		
		/* 
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		* PATH_FIELDS = "CANDIDATE", "SITE", "PARENTSITE", "SORTLETTER", "TYPE"
		*/
		public Object[] storageFields(Entity r)
		{
			Object[] rv = new Object[5];
			rv[0] = ((CandidatePath) r).getCandidate();
			rv[1] = ((CandidatePath) r).getSite();
			rv[2] = ((CandidatePath) r).getParentSite();
			rv[3] = ((CandidatePath) r).getSortLetter();
			rv[4] = ((CandidatePath) r).getType();

			return rv;
		}

		/**
		 * Check if this resource is in draft mode.
		 * @param r The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * @param r The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * @param r The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}

	}//CandidatePathStorageUser

	
	/*******************************************************************************
	* StepStatusStorageUser implementation
	*******************************************************************************/

	protected class StepStatusStorageUser 
		implements StorageUser
	{
		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new container Resource.
		*/
		public Entity newContainer(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Entity newContainer(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Entity newContainer(Entity other) { return null; }

		/**
		* Construct a new resource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Entity newResource(Entity container, String id, Object[] others)
		{ return new BaseStepStatus(id, (String)others[0]); }

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Entity newResource(Entity container, Element element)
		{ return new BaseStepStatus(element); }

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Entity newResource(Entity container, Entity other)
		{ return new BaseStepStatus((StepStatus) other); }

		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new containe Resource.
		*/
		public Edit newContainerEdit(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Entity other) { return null; }

		/**
		* Construct a new rsource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseStepStatusEdit e = new BaseStepStatusEdit(id, (String)others[0]);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseStepStatusEdit e =  new BaseStepStatusEdit(element);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseStepStatusEdit e = new BaseStepStatusEdit((StepStatus) other);
			e.activate();
			return e;
		}

		/**
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		*/
		public Object[] storageFields(Entity r) { return null; }

		/**
		 * Check if this resource is in draft mode.
		 * @param r The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * @param r The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * @param r The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}

	}//StepStatusStorageUser

	
	/*******************************************************************************
	* CandidateInfoStorageUser implementation
	*******************************************************************************/

	protected class CandidateInfoStorageUser 
		implements StorageUser
	{
		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new container Resource.
		*/
		public Entity newContainer(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Entity newContainer(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Entity newContainer(Entity other) { return null; }

		/**
		* Construct a new resource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Entity newResource(Entity container, String id, Object[] others)
		{ return new BaseCandidateInfo(id, (String)others[0]); }

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Entity newResource(Entity container, Element element)
		{ return new BaseCandidateInfo(element); }

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Entity newResource(Entity container, Entity other)
		{ return new BaseCandidateInfo((CandidateInfo) other); }

		/**
		* Construct a new continer given just an id.
		* @param id The id for the new object.
		* @return The new containe Resource.
		*/
		public Edit newContainerEdit(String ref) { return null; }

		/**
		* Construct a new container resource, from an XML element.
		* @param element The XML.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Element element) { return null; }

		/**
		* Construct a new container resource, as a copy of another
		* @param other The other contianer to copy.
		* @return The new container resource.
		*/
		public Edit newContainerEdit(Entity other) { return null; }

		/**
		* Construct a new rsource given just an id.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param id The id for the new object.
		* @return The new resource.
		*/
		public Edit newResourceEdit(Entity container, String id, Object[] others)
		{
			BaseCandidateInfoEdit e = new BaseCandidateInfoEdit(id, (String)others[0]);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource, from an XML element.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param element The XML.
		* @return The new resource from the XML.
		*/
		public Edit newResourceEdit(Entity container, Element element)
		{
			BaseCandidateInfoEdit e =  new BaseCandidateInfoEdit(element);
			e.activate();
			return e;
		}

		/**
		* Construct a new resource from another resource of the same type.
		* @param container The Resource that is the container for the new resource (may be null).
		* @param other The other resource.
		* @return The new resource as a copy of the other.
		*/
		public Edit newResourceEdit(Entity container, Entity other)
		{
			BaseCandidateInfoEdit e = new BaseCandidateInfoEdit((CandidateInfo) other);
			e.activate();
			return e;
		}

		/**
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		*
		public Object[] storageFields(Resource r) { return null; }
		*/
		
		/* 
		* Collect the fields that need to be stored outside the XML (for the resource).
		* @return An array of field values to store in the record outside the XML (for the resource).
		* INFO_FIELDS = "CHEFID", "PARENTSITE", "EMPLID"
		*/
		public Object[] storageFields(Entity r)
		{
			Object[] rv = new Object[3];
			rv[0] = ((CandidateInfo) r).getChefId();
			rv[1] = ((CandidateInfo) r).getParentSite();
			rv[2] = ((CandidateInfo) r).getEmplid();

			return rv;
		}
		
		/**
		 * Check if this resource is in draft mode.
		 * @param r The resource.
		 * @return true if the resource is in draft mode, false if not.
		 */
		public boolean isDraft(Entity r)
		{
			return false;
		}

		/**
		 * Access the resource owner user id.
		 * @param r The resource.
		 * @return The resource owner user id.
		 */
		public String getOwnerId(Entity r)
		{
			return null;
		}

		/**
		 * Access the resource date.
		 * @param r The resource.
		 * @return The resource date.
		 */
		public Time getDate(Entity r)
		{
			return null;
		}

	}//CandidateInfoStorageUser

	
	/*******************************************************************************
	* CacheRefresher implementations (no container)
	*******************************************************************************/

	/*******************************************************************************
	* BlockGrantGroupCacheRefresher implementation
	*******************************************************************************/

	protected class BlockGrantGroupCacheRefresher
		implements CacheRefresher
	{
		/**
		* Get a new value for this key whose value has already expired in the cache.
		* @param key The key whose value has expired and needs to be refreshed.
		* @param oldValue The old expired value of the key.
		* @return a new value for use in the cache for this key; if null, the entry will be removed.
		*/	
		public Object refresh(Object key, Object oldValue, Event event)
		{
			// key is a reference, but our storage wants an id
			String id = blockGrantGroupId((String) key);

			// get whatever we have from storage for the cache for this value
			BlockGrantGroup blockGrantGroup = m_groupStorage.get(id);

			return blockGrantGroup;

		}	// refresh

	}//BlockGrantGroupCacheRefresher


	/*******************************************************************************
	* DissertationCacheRefresher implementation
	*******************************************************************************/

	protected class DissertationCacheRefresher
		implements CacheRefresher
	{
		/**
		* Get a new value for this key whose value has already expired in the cache.
		* @param key The key whose value has expired and needs to be refreshed.
		* @param oldValue The old expired value of the key.
		* @return a new value for use in the cache for this key; if null, the entry will be removed.
		*/	
		public Object refresh(Object key, Object oldValue, Event event)
		{
		
			// key is a reference, but our storage wants an id
			String id = dissertationId((String) key);

			// get whatever we have from storage for the cache for this value
			Dissertation dissertation = m_dissertationStorage.get(id);
			//Dissertation dissertation = getDissertation(id);

			return dissertation;

		}	// refresh

	}//DissertationCacheRefresher

	
	/*******************************************************************************
	* DissertationStepCacheRefresher implementation
	*******************************************************************************/

	protected class DissertationStepCacheRefresher
		implements CacheRefresher
	{
		/**
		* Get a new value for this key whose value has already expired in the cache.
		* @param key The key whose value has expired and needs to be refreshed.
		* @param oldValue The old expired value of the key.
		* @return a new value for use in the cache for this key; if null, the entry will be removed.
		*/	
		public Object refresh(Object key, Object oldValue, Event event)
		{
		
			// key is a reference, but our storage wants an id
			String id = stepId((String) key);

			// get whatever we have from storage for the cache for this vale
			DissertationStep step = m_stepStorage.get(id);

			return step;

		}	// refresh

	}//DissertationStepCacheRefresher


	/*******************************************************************************
	* CandidatePathCacheRefresher implementation
	*******************************************************************************/

	protected class CandidatePathCacheRefresher
		implements CacheRefresher
	{
		/**
		* Get a new value for this key whose value has already expired in the cache.
		* @param key The key whose value has expired and needs to be refreshed.
		* @param oldValue The old expired value of the key.
		* @return a new value for use in the cache for this key; if null, the entry will be removed.
		*/	
		public Object refresh(Object key, Object oldValue, Event event)
		{
		
			// key is a reference, but our storage wants an id
			String id = pathId((String) key);

			// get whatever we have from storage for the cache for this vale
			CandidatePath path = m_pathStorage.get(id);

			return path;

		}	// refresh

	}//CandidatePathCacheRefresher


	/*******************************************************************************
	* StepStatusCacheRefresher implementation
	*******************************************************************************/

	protected class StepStatusCacheRefresher
		implements CacheRefresher
	{
		/**
		* Get a new value for this key whose value has already expired in the cache.
		* @param key The key whose value has expired and needs to be refreshed.
		* @param oldValue The old expired value of the key.
		* @return a new value for use in the cache for this key; if null, the entry will be removed.
		*/	
		public Object refresh(Object key, Object oldValue, Event event)
		{
		
			// key is a reference, but our storage wants an id
			String id = statusId((String) key);

			// get whatever we have from storage for the cache for this vale
			StepStatus status = m_statusStorage.get(id);

			return status;

		}	// refresh

	}//StepStatusCacheRefresher

	
	/*******************************************************************************
	* CandidateInfoCacheRefresher implementation
	*******************************************************************************/

	protected class CandidateInfoCacheRefresher
		implements CacheRefresher
	{
		/**
		* Get a new value for this key whose value has already expired in the cache.
		* @param key The key whose value has expired and needs to be refreshed.
		* @param oldValue The old expired value of the key.
		* @return a new value for use in the cache for this key; if null, the entry will be removed.
		*/	
		public Object refresh(Object key, Object oldValue, Event event)
		{
		
			// key is a reference, but our storage wants an id
			String id = infoId((String) key);

			// get whatever we have from storage for the cache for this vale
			CandidateInfo info = m_infoStorage.get(id);

			return info;

		}	// refresh

	}//CandidateInfoCacheRefresher


	/**
	* Utility function which returns the string representation of the
	* long value of the time object.
	* @param t - the Time object.
	* @return A String representation of the long value of the time object.
	*/
	protected String getTimeString(Time t)
	{
		String retVal = "";
		if(t != null)
			retVal = String.valueOf(t.getTime());
		return retVal;
	}

	/**
	* Utility function which returns a string from a boolean value.
	* @param b - the boolean value.
	* @return - "True" if the input value is true, "false" otherwise.
	*/
	protected String getBoolString(boolean b)
	{
		if(b)
			return "true";
		else
			return "false";
	}

	/**
	* Utility function which returns the user's id.
	* @param User object.
	* @return The id for the user.
	*/
	protected String getUserId(User u)
	{
		String retVal = "";
		if(u != null)
			retVal = u.getId();
		return retVal;
	}

	/**
	* Utility function which returns a boolean value from a string.
	* @param s - The input string.
	* @return the boolean true if the input string is "true", false otherwise.
	*/
	protected boolean getBool(String s)
	{
		boolean retVal = false;
		if(s != null)
		{
			if(s.equalsIgnoreCase("true"))
				retVal = true;
		}
		return retVal;
	}

	/**
	* Utility function which find a User from an id.
	* @param id - The User's id.
	* @return The User, or null if one does not correspond with the id.
	*/		
	protected User getUserObject(String id)
	{
		User retVal = null;
		if(id != "")
		{

			try
			{
				retVal = UserDirectoryService.getUser(id);
			}
			catch(IdUnusedException iue)
			{
				m_logger.warn(this + " Exception getting user from user dir service with id : " + id + " : " + iue);
			}
		}
		return retVal;
	}

	/**
	* Utility function which converts a string into a chef time object.
	* @param timeString - String version of a time in long format, representing the standard ms since the epoch, Jan 1, 1970 00:00:00.
	* @return A chef Time object.
	*/
	protected Time getTimeObject(String timeString)
	{
		Time aTime = null;
		if(timeString != "")
		{
			try
			{
				long longTime = Long.parseLong(timeString);
				aTime = TimeService.newTime(longTime);
			}
			catch(Exception e)
			{
				m_logger.warn(this + " Exception creating time object from xml file : " + e);
			}
		}
		return aTime;
	}
	
	/**
	* Get the collection of Checklist Section Headings for display.
	* @return Vector of ordered String objects, one for each section head.
	*/
	public Vector getSectionHeads()
	{
		Vector retVal = new Vector();
		retVal.add("None");
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING1);
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING2);
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING3);
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING4);
		return retVal;
	}
	
	/**
	* Get the section head identifier for the checklist section heading.
	* @param String head The text of the section heading.
	* @return String containing the section identifier.
	*/
	public String getSectionId(String head)
	{
		String retVal = "";
		if(head!=null && !head.equals(""))
		{
			if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING1))
			{
				retVal = "1";
			}
			else if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING2))
			{
				retVal = "2";
			}
			else if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING3))
			{
				retVal = "3";
			}
			else if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING4))
			{
				retVal = "4";
			}
		}
		return retVal;
	}
	
	/**
	* Add a new dissertation step to dissertation(s) and paths
	* Long-running process runs in a separate thread under Quartz.
	* Returns status messages to Announcements.
	* 
	* @param retro - Whether or not to apply the change to existing 
	* 	dissertations and paths
	* @param parm - Job execution parameters:
	* Object[0] retro - Boolean value true is retroactive change
	* - Whether or not to apply the change to existing 
	* 	dissertations and paths
	* Object[1] dissref - String reference to the current dissertation.
	* Object[2] currentSite - String id of the current site.
	* Object[3] location - String key to location of new step.
	* Object[4] section - String checklist section description.
	* Object[5] prereqs - String[] prerequisites.
	* Object[6] instructionsText - String step instructions.
	* Object[7] validType - String type of step completion validator.
	* Object[8] autoValid - String key for automatic step status update.
	* @throws JobExceptionException - Required by Quartz.
	*/
	private String execNewStepJob(Object[] parm)
		throws JobExecutionException
	{
		
		/** update the current Dissertation so display may be refreshed.
		 * take current Dissertation out of Collection to be updated in job. */
		
		String dissRef = (String)parm[1];
		String currentSite = (String)parm[2];
		String location = (String)parm[3];
		String section = (String)parm[4];
		String[] prereqs = (String[])parm[5];
		String instructionsText = (String)parm[6];
		String validType = (String)parm[7];
		String autoValid = (String)parm[8];
		Scheduler scheduler = null;
		
		//TODO pass String for Quartz serialization to db
		
		boolean notdone = true;
		Dissertation dissertation = null;
		DissertationEdit dissEdit = null;
		String stepRef = null;
		DissertationStepEdit stepEdit = null;
		String keystring = null;
		String refstring = null;
		
		/** update the current Dissertation so display may be refreshed, and
		 *  take current Dissertation out of Collection to be updated in job. */
		try
		{
			stepEdit = addDissertationStep(currentSite);
		}
		catch(Exception e)
		{
			if(stepEdit != null && stepEdit.isActiveEdit())
				cancelEdit(stepEdit);
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".execute() addDissertationStep(" + currentSite + ") " + e);
			return e.toString();
		}
		if(stepEdit == null)
		{
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".execute() addDissertationStep(" + currentSite + ") step edit null");
			return "exception creating new step: stepEdit == null";
		}
		
		stepEdit.setInstructionsText(instructionsText);
		stepEdit.setValidationType(validType);
		stepEdit.setAutoValidationId(autoValid);

		//school steps have a section, department steps do not
		if(section != null)
		{
			//convert from section header to key
			stepEdit.setSection((String)getSectionId(section));
		}
		
		//add step prerequisites if any
		if(prereqs != null)
		{
			for(int y = 0; y < prereqs.length; y++)
			{
				if(!("".equals(prereqs[y])))
				{
					stepEdit.addPrerequisiteStep(prereqs[y]);
				}
			}
		}
		
		//add the step to the system
		commitEdit(stepEdit);
		
		//get step reference to pass to job
		stepRef = stepEdit.getReference();
		
		//can we get the current Dissertation
		try
		{
			dissertation = getDissertation(dissRef);
		}
		catch(Exception e)
		{
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".execNewStep()  dissRef " + dissRef + " " + e);
			return (e.toString());
		}

		//get the key of step location within ordered steps
		List previousStepRefs = new Vector();
		previousStepRefs.add("start");
		Hashtable orderedsteps = dissertation.getOrderedSteps();
		if("start".equals(location))
			notdone = false;
		for(int z = 1; z < (orderedsteps.size()+1); z++)
		{
			if(notdone)
			{
				keystring = "" + z;
				refstring = (String)orderedsteps.get(keystring);
				if(refstring != null)
				{
					previousStepRefs.add(refstring);
					if(refstring.equals(location))
						notdone = false;
				}
			}
		}
		
		//add new step to the current Dissertation at that location
		try
		{
			dissEdit = editDissertation(dissertation.getReference());
			dissEdit.addStep(stepEdit, location);
			
			//save the change to the system
			commitEdit(dissEdit);
		}
		catch(Exception e)
		{
			if(dissEdit != null && dissEdit.isActiveEdit())
				cancelEdit(dissEdit);
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".execute() School change - dissertation id " + dissEdit.getId() + " " + e);

			return (e.toString());
		}

		/** start a Quartz job to add the step to all derived checklists */
		
		//pass the parameters to the job as job detail data
		JobDetail jobDetail = new JobDetail("NewStepJob",
				Scheduler.DEFAULT_GROUP, m_stepChangeJob.getClass());
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		jobDataMap.put("RETROACTIVE", (Boolean)parm[0]);
		jobDataMap.put("DISSERTATION_REF", (String)parm[1]);
		jobDataMap.put("STEP_REF", stepRef);
		jobDataMap.put("CURRENT_SITE", (String)parm[2]);
		jobDataMap.put("LOCATION", (String)parm[3]);
		jobDataMap.put("JOB_TYPE", "New");
		jobDataMap.put("PREVIOUS_STEP_REFS", (List)previousStepRefs);
		jobDataMap.put("CURRENT_USER", (String)SessionManager.getCurrentSessionUserId());
		
		//job name + group should be unique
		String jobGroup = IdService.getUniqueId();
		
		//associate a trigger with the job
		SimpleTrigger trigger = new SimpleTrigger("NewStepTrigger", jobGroup, new Date());
		try
		{
			//get a scheduler instance from the factory
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
			//associate job with schedule
			scheduler.scheduleJob(jobDetail, trigger);
			
			//start the scheduler
			scheduler.start();
		}
		catch(SchedulerException e)
		{
			throw new JobExecutionException(e);
		}
		return "NewStepJob started. Announcements will have a job report later.";
		
	}//execNewStepJob
	
	/**
	* Revise a dissertation step in dissertation(s) and paths
	* Long-running process runs in a separate thread under Quartz.
	* Returns status messages to Announcements.
	* 
	* @param retro - Whether or not to apply the change to existing 
	* 	dissertations and paths
	* @param parm - Job execution parameters:
	* Object[0] retro - Boolean value true is retroactive change
	* - Whether or not to apply the change to existing 
	* 	dissertations and paths
	* Object[1] dissertation.getReference() - String reference to the current dissertation.
	* Object[2] currentSite - String id of the current site.
	* Object[3] section - String checklist section description.
	* Object[4] addprereqs - String[] prerequisites to add to step.
	* Object[5] removeprereqs - String[] prerequisites to remove from step.
	* Object[6] instructionsText - String step instructions.
	* Object[7] validType - String type of step completion approver.
	* Object[8] autoValid - String automatic validation of step completion id.
	* Object[9] stepReference - String reference to the step to revise.
	* Object[10] dissertation.getType() - String type of dissertation containing step.
	* @throws JobExceptionException - Required by Quartz.
	*/
	private String execReviseStepJob(Object[] parm)
		throws JobExecutionException
	{
		/** update the current Dissertation so display may be refreshed.
		 * take current Dissertation out of Collection to be updated in job. */
		
		String m_section = (String)parm[3];
		String[] m_addPrereqs = (String[])parm[4];
		String[] m_removePrereqs = (String[])parm[5];
		String m_instructionsText = (String)parm[6];
		String m_validType = (String)parm[7];
		String m_autoValid = (String)parm[8];
		String m_stepRef = (String)parm[9];

		DissertationStep before = null;
		DissertationStepEdit stepEdit = null;
		
		//see if this is a school or department step change
		boolean school;
		if((String)parm[2] != null)
			school = ((String)parm[2]).equals(m_schoolSite) ? true: false;
		else
			throw new JobExecutionException("(String)parm[2] is null.");
		
		if(m_stepRef != null)
		{
			try
			{
				//get the step before change(s) are made
				before = getDissertationStep(m_stepRef);
				if(before != null)
				{
					stepEdit = editDissertationStep(m_stepRef);
					if(stepEdit != null)
					{
						stepEdit.setInstructionsText(m_instructionsText);
						stepEdit.setValidationType(m_validType);
						
						//school steps have a section, department steps do not
						if(school)
							stepEdit.setSection(getSectionId(m_section));
						
						if(m_autoValid != null)
							stepEdit.setAutoValidationId(m_autoValid);
						
						//remove prerequisites
						if(m_removePrereqs != null)
						{
							for(int z = 0; z < m_removePrereqs.length; z++)
							{
								 stepEdit.removePrerequisiteStep(m_removePrereqs[z]);
								 //TODO continue
								 continue;
							}//for all prerequisites
						}
						
						//add prerequisites
						if(m_addPrereqs != null)
						{
							for(int z = 0; z < m_addPrereqs.length; z++)
							{
								stepEdit.addPrerequisiteStep(m_addPrereqs[z]);
								continue;
							}
						}
						
						//add step revision to the system
						commitEdit(stepEdit);
					}//stepEdit != null
					
				}//before != null
			}
			catch(Exception e)
			{
				if(stepEdit != null && stepEdit.isActiveEdit())
					cancelEdit(stepEdit);
				
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() exception - step id " + stepEdit.getId() + " " + e);
			}
		}//m_stepref != null
		
		//TODO pass String for Quartz serialization to db
		
		//TODO get distype from the current dissertation instead
		
		Scheduler scheduler = null;
		
		//pass the parameters to the job as job detail data
		JobDetail jobDetail = new JobDetail("ReviseStepJob",
				Scheduler.DEFAULT_GROUP, m_stepChangeJob.getClass());
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		jobDataMap.put("RETROACTIVE", (Boolean)parm[0]);
		jobDataMap.put("DISSERTATION_REF", (String)parm[1]);
		jobDataMap.put("CURRENT_SITE", (String)parm[2]);
		jobDataMap.put("SECTION", (String)parm[3]);
		jobDataMap.put("INSTRUCTIONS_TEXT", (String)parm[6]);
		jobDataMap.put("VALID_TYPE", (String)parm[7]);
		jobDataMap.put("AUTO_VALID", (String)parm[8]);
		jobDataMap.put("STEP_REF", (String)parm[9]);
		jobDataMap.put("BEFORE", (DissertationStep)before);
		jobDataMap.put("DISSERTATION_TYPE", (String)parm[10]);
		jobDataMap.put("JOB_TYPE", "Revise");
		jobDataMap.put("CURRENT_USER",(String)SessionManager.getCurrentSessionUserId());
		
		//job name + group should be unique
		String jobGroup = IdService.getUniqueId();
		
		//associate a trigger with the job
		SimpleTrigger trigger = new SimpleTrigger("ReviseStepTrigger", jobGroup, new Date());
		try
		{
			//get a scheduler instance from the factory
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
			//associate job with schedule
			scheduler.scheduleJob(jobDetail, trigger);
			
			//start the scheduler
			scheduler.start();
		}
		catch(SchedulerException e)
		{
			throw new JobExecutionException(e);
		}
		return "ReviseStepJob started. Announcements will have a job report later.";
		
	}//execReviseStepJob
	
	/**
	* Move a dissertation step in dissertation(s) and paths
	* Long-running process runs in a separate thread under Quartz.
	* Returns status messages to Announcements.
	* 
	* @param parm - Job execution parameters:
	* Object[0] retro - Boolean value true is retroactive change
	*  - Whether or not to apply the change to existing 
	* 	dissertations and paths
	* Object[1] currentDissRef - String reference to the current dissertation.
	* Object[2] currentSite - String key to location of new step.
	* Object[3] location - 
	* Object[4] section - String checklist section description.
	* Object[5] stepToMoveRef - String reference to the step to move.
	* @throws JobExceptionException - Required by Quartz.
	*/
 	private String execMoveStepJob(Object[] parm)
 		throws JobExecutionException
	{
 		
		/** update the current Dissertation so display may be refreshed.
		 * take current Dissertation out of Collection to be updated in job. */
		
 		DissertationEdit dissEdit = null;
 		String dissRef = (String)parm[1];
 		String location = (String)parm[3];
 		String section = (String)parm[4];
 		String stepRef = (String)parm[5];
 		DissertationStepEdit stepEdit = null;
 		
		//see if this is a school or department step move
		boolean school;
		if((String)parm[2] != null)
			school = ((String)parm[2]).equals(m_schoolSite) ? true: false;
		else
			throw new JobExecutionException("(String)parm[2] is null.");
 		
		//school steps have an m_section, department steps do not
		if(school)
		{
			try
			{
				//get the step being moved
				stepEdit = editDissertationStep(stepRef);
				
				//edit the m_section of the step being moved
				stepEdit.setSection(getSectionId(section));
				commitEdit(stepEdit);
			}
			catch(Exception e)
			{
				if(stepEdit != null && stepEdit.isActiveEdit())
					cancelEdit(stepEdit);
				if(m_logger.isWarnEnabled())
					m_logger.warn(this + ".execute() setSection() " + e);
			}
		}
 		
 		//move step in the current dissertation so view is current
		try
		{
			dissEdit = editDissertation(dissRef);
			dissEdit.moveStep(stepRef, location);
			commitEdit(dissEdit);
		}
		catch(Exception e)
		{
			if(dissEdit != null && dissEdit.isActiveEdit())
				cancelEdit(dissEdit);
			
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".execute() " + e);
		}
 		
		Scheduler scheduler = null;
		
		//TODO pass String for Quartz serialization to db
		
		//pass the parameters to the job as job detail data
		JobDetail jobDetail = new JobDetail("MoveStepJob",
				Scheduler.DEFAULT_GROUP, m_stepChangeJob.getClass());
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		jobDataMap.put("RETROACTIVE", (Boolean)parm[0]);
		jobDataMap.put("DISSERTATION_REF", (String)parm[1]);
		jobDataMap.put("CURRENT_SITE", (String)parm[2]);
		jobDataMap.put("LOCATION", (String)parm[3]);
		jobDataMap.put("SECTION", (String)parm[4]);
		jobDataMap.put("STEP_REF", (String)parm[5]);
		jobDataMap.put("JOB_TYPE", "Move");
		jobDataMap.put("CURRENT_USER",(String)SessionManager.getCurrentSessionUserId());
		
		/** update the current Dissertation so display may be refreshed.
		 * take current Dissertation out of Collection to be updated in job. */
		//job name + group should be unique
		String jobGroup = IdService.getUniqueId();
		
		//associate a trigger with the job
		SimpleTrigger trigger = new SimpleTrigger("MoveStepTrigger", jobGroup, new Date());
		try
		{
			//get a scheduler instance from the factory
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
			//associate job with schedule
			scheduler.scheduleJob(jobDetail, trigger);
			
			//start the scheduler
			scheduler.start();
		}
		catch(SchedulerException e)
		{
			throw new JobExecutionException(e);
		}
		return "MoveStepJob started. Announcements will have a job report later.";
 		
	}//execMoveStepJob

	/**
	* Delete one or more steps in dissertation(s) and paths
	* Long-running process runs in a separate thread under Quartz.
	* Returns status messages to Announcements.
	* 
	* @param parm - Job execution parameters:
	* Object[0] retro Boolean value true is retroactive change
	*  - Whether or not to apply the change to existing 
	* 	dissertations and paths
	* Object[1] dissref - String reference to the current dissertation.
	* Object[2] currentSite - String site.
	* Object[3] selectedStepRefs - String[] references to steps to delete .
	* @throws JobExceptionException - Required by Quartz.
	*/
 	private String execDeleteStepJob(Object[] parm)
 			throws JobExecutionException
	{
		/** update the current Dissertation so display may be refreshed.
		 * take current Dissertation out of Collection to be updated in job. */
 		
 		String[] stepRefs = (String[])parm[3];
 		String dissRef = (String)parm[1];
 		String stepRef = null;
		DissertationEdit dissEdit = null;
		DissertationStepEdit stepEdit = null;
		DissertationStep aStep = null;
		Dissertation dissertation = null;
		List prereqs = null;
	
		/** first, cycle through all steps in the current Dissertation 
		 *  and remove prerequisite references to these steps.
		 */
		//TODO just get steps that HAVE prerequisites
		try
		{
			dissertation = getDissertation(dissRef);
		}
		catch(Exception e)
		{
			throw new JobExecutionException(e);
		}
		if(dissertation == null)
			throw new JobExecutionException("current dissertation was null");
		
		//get the step references for the steps in the current Dissertation
		Hashtable steps = dissertation.getOrderedSteps();
		
			//for each step being deleted 
			for(int counter = 0; counter < stepRefs.length; counter++)
			{
				//for each steps in the current Dissertation
				for(Iterator i = steps.values().iterator(); i.hasNext();)
				{
					stepRef = (String)i.next();
					try
					{
						aStep = getDissertationStep(stepRef);
					}
					catch(Exception e)
					{
						
					}
					
					//the prereqs for this step
					prereqs = aStep.getPrerequisiteStepReferences();
					
					//for each prereq of this step
					for(int x = 0; x < prereqs.size(); x++)
					{
						//if step has step being deleted as a prereq
						if(prereqs.get(x).equals(stepRefs[counter]))
						{
							try
							{
								stepEdit = editDissertationStep(stepRefs[counter]);
								
								//remove the to-be-deleted step as prereq
								stepEdit.removePrerequisiteStep(stepRefs[counter]);
								commitEdit(stepEdit);
							}
							catch(Exception e)
							{
								if(stepEdit != null && stepEdit.isActiveEdit())
									cancelEdit(stepEdit);
								
								if(m_logger.isWarnEnabled())
									m_logger.warn(this + ".execute() exception - step id " + stepEdit.getId() + " " + e);
								
								//keep going
								continue;
								
							}
						}
					}
				}
			}
			
			//now remove the step(s) from the current Dissertation
			for(int x = 0; x < stepRefs.length; x++)
			{
				try
				{
					//delete step from current Dissertation
					dissEdit = editDissertation(dissRef);
					dissEdit.removeStep(stepRefs[x]);
					commitEdit(dissEdit);
				}
				catch(Exception e)
				{
					//keep going
					continue;
				}
			}
			
		//start Quartz job to remove step(s) from derived checklists
		Scheduler scheduler = null;
		
		//TODO pass String for Quartz serialization to db
		
		//pass the parameters to the job as job detail data
		JobDetail jobDetail = new JobDetail("DeleteStepJob",
				Scheduler.DEFAULT_GROUP, m_stepChangeJob.getClass());
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		jobDataMap.put("RETROACTIVE",(Boolean)parm[0]);
		jobDataMap.put("DISSERTATION_REF",(String)parm[1]);
		jobDataMap.put("CURRENT_SITE",(String)parm[2]);
		jobDataMap.put("STEP_REFS", (String[])parm[3]);
		jobDataMap.put("JOB_TYPE", "Delete");
		jobDataMap.put("CURRENT_USER",(String)SessionManager.getCurrentSessionUserId());
		
		/** update the current Dissertation so display may be refreshed.
		 * take current Dissertation out of Collection to be updated in job. */
		//job name + group should be unique
		String jobGroup = IdService.getUniqueId();
		
		//associate a trigger with the job
		SimpleTrigger trigger = new SimpleTrigger("DeleteStepTrigger", jobGroup, new Date());
		try
		{
			//get a scheduler instance from the factory
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
			//associate job with schedule
			scheduler.scheduleJob(jobDetail, trigger);
			
			//start the scheduler
			scheduler.start();
		}
		catch(SchedulerException e)
		{
			throw new JobExecutionException(e);
		}
		return "DeleteStepJob started. Announcements will have a job report later.";
 		
	}//execDeleteStepJob
 	
 	/*
 	 *  (non-Javadoc)
 	 * @see org.sakaiproject.api.app.dissertation.DissertationService#executeStepChangeJob(java.lang.String, java.lang.Object[])
 	 */
	public String executeStepChangeJob(
			String type,
			Object[] params)
		throws JobExecutionException
	{
		//check that there isn't a batch operation already in progress
		if(isLoading())
			return "A data upload is in progress. Please wait for it to complete.";
		else if(isChangingStep())
			return "A step change is in progress. Please wait for it to complete.";
		
		//take out a lock
		//TODO replace CandidateInfoEdit as lock object
		try
		{
			/** we are starting a load - add a well-known record to CandidateInfo
			 * that persists across sessions
			 */
			CandidateInfoEdit lock = addStepChangeLock(getSchoolSite());
			if(lock == null)
				throw new JobExecutionException("Unable to lock db for batch update. Please contact support.");
			commitEdit(lock);
		}
		catch(Exception e)
		{
			//TODO throw InUseException
			//but we should not get here because tool checks earlier
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".loadData addStepChangeLock " + e);
			throw new JobExecutionException("Exception starting batch upload. " + e);
		}
		
		if(type == null)
			throw new JobExecutionException("no job type");
		if(params == null || params.length == 0)
			throw new JobExecutionException("no job parameters");
		
		String msg = null;
 		
		//dispatch based on job type
		if(type.equals("New"))
			try
			{
				msg = execNewStepJob(params);
			}
			catch(Exception e)
			{
				msg = e.toString();
			}
		else if (type.equals("Revise"))
			try
			{
				msg = execReviseStepJob(params);
			}
			catch(Exception e)
			{
				msg = e.toString();
			}
		else if (type.equals("Move"))
			try
			{
				msg = execMoveStepJob(params);
			}
			catch(Exception e)
			{
				msg = e.toString();
			}
		else if (type.equals("Delete"))
			try
			{
				msg = execDeleteStepJob(params);
			}
			catch(Exception e)
			{
				msg = e.toString();
			}
		else
			throw new JobExecutionException("unrecognized job type");
		
		//return instructions to check Announcements for job report later
		return msg;
			
	}//executeStepChangeJob
	
	/*
	 *  (non-Javadoc)
	 * @see org.sakaiproject.api.app.dissertation.DissertationService#executeUploadExtractsJob(byte[], byte[])
	 */
	public String executeUploadExtractsJob(String currentSite, byte[] o, byte[] m)
		throws JobExecutionException
	{
		/** replaces loadData()
		 *  two byte arrays are expected, but one might be a dummy
		 *  of length 1 indicating no corresponding extract file */
		
		//check that we have at least one file
		if((o == null || o.length <= 1) && (m == null || m.length <= 1))
			throw new JobExecutionException("no data");
		
		//if set up as a Quartz stateful job we could set concurrent=false
		
		//check that there isn't a batch operation already in progress
		if(isLoading())
			return "A data upload is in progress. Please wait for it to complete.";
		else if(isChangingStep())
			return "A step change is in progress. Please wait for it to complete.";
		
		//TODO replace CandidateInfoEdit as lock object
		try
		{
			/** we are starting a load - add a well-known record to CandidateInfo
			 * that persists across sessions
			 */
			CandidateInfoEdit lock = addUploadLock(getSchoolSite());
			if(lock == null)
				throw new JobExecutionException("Unable to lock db for batch update. Please contact support.");
			commitEdit(lock);
		}
		catch(Exception e)
		{
			//but we should not get here because tool checks earlier
			if(m_logger.isWarnEnabled())
				m_logger.warn(this + ".loadData addCandidateInfoLock " + e);
			throw new JobExecutionException("Exception starting batch upload. " + e);
		}
		
		//pass data as standard Java objects for db job detail serialization
		String[] oardRecords = 	((String)new String(o)).split("\n");
		String[] mpRecords = ((String)new String(m)).split("\n");
		
		//a last check that there is data to pass to job
		if((oardRecords == null || oardRecords.length < 1) && 
				(mpRecords == null || mpRecords.length < 1))
			throw new JobExecutionException("no data");
		
		Scheduler scheduler = null;
		
		//job name + group should be unique
		String jobGroup = IdService.getUniqueId();
		
		//pass the parameters to the job as job detail data
		JobDetail jobDetail = new JobDetail("UploadExtractJob",
				jobGroup, m_uploadExtractsJob.getClass());
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		jobDataMap.put("OARD_RECORDS", (String[])oardRecords);
		jobDataMap.put("MP_RECORDS", (String[])mpRecords);
		jobDataMap.put("SCHOOL_SITE", m_schoolSite);
		jobDataMap.put("MUSIC_PERFORMANCE_SITE", m_musicPerformanceSite);
		jobDataMap.put("SCHOOL_GROUPS", (Hashtable) m_schoolGroups);
		jobDataMap.put("CURRENT_USER", (String)SessionManager.getCurrentSessionUserId());
		jobDataMap.put("CURRENT_SITE", currentSite);
		
		//associate a trigger with the job
		SimpleTrigger trigger = new SimpleTrigger("NewStepTrigger", jobGroup, new Date());
		try
		{
			//get a scheduler instance from the factory
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
			//associate job with schedule
			scheduler.scheduleJob(jobDetail, trigger);
			
			//start the scheduler
			scheduler.start();
		}
		catch(SchedulerException e)
		{
			throw new JobExecutionException(e);
		}
		
		//return instructions to check Announcements for job report later
		return "UploadExtractsJob started. Announcements will have a job report later.";
		
	}//executeUploadExtractsJob
	
	/** 
	* Adds a well-known CandidateInfo as a lock during uploads.
	* @param site - The site for which permissions are being checked.
	* @throws PermissionException if the current User does not have permission
	*  to do this.
	* @return CandidateInfoEdit The edit used to lock uploads.
	*/
	public CandidateInfoEdit addUploadLock(String site)
		throws PermissionException
	{
		String infoId = null;
		CandidateInfoEdit info = null;
			
		//set an id that can be easily identified in the datbase
		infoId = DissertationService.IS_LOADING_LOCK_ID;

		//check for the locked object
		if(m_infoStorage.check(infoId))
			return info;
		
		String key = infoReference(site, infoId);
		unlock(SECURE_ADD_DISSERTATION_CANDIDATEINFO, key);
		info = m_infoStorage.put(infoId, site);
		info.setSite(site);
		try
		{
			((BaseCandidateInfoEdit) info).setEvent(SECURE_ADD_DISSERTATION_CANDIDATEINFO);
		}
		catch(Exception e)
		{
			m_logger.warn(this + ".addUploadLock setEvent " + e);
		}
		
		return info;
		
	}//addUploadLock

	/** 
	* Adds a well-known CandidateInfo as a lock during admin step changes.
	* @param site - The site for which permissions are being checked.
	* @throws PermissionException if the current User does not have permission
	*  to do this.
	* @return CandidateInfoEdit The edit used to lock step changes.
	*/
	public CandidateInfoEdit addStepChangeLock(String site)
		throws PermissionException
	{
		String infoId = null;
		CandidateInfoEdit info = null;
			
		//set an id that can be easily identified in the datbase
		infoId = DissertationService.IS_CHANGING_STEP_LOCK_ID;

		//check for the locked object
		if(m_infoStorage.check(infoId))
			return info;
		
		String key = infoReference(site, infoId);
		unlock(SECURE_ADD_DISSERTATION_CANDIDATEINFO, key);
		info = m_infoStorage.put(infoId, site);
		info.setSite(site);
		try
		{
			((BaseCandidateInfoEdit) info).setEvent(SECURE_ADD_DISSERTATION_CANDIDATEINFO);
		}
		catch(Exception e)
		{
			m_logger.warn(this + ".addStepChangeLock setEvent " + e);
		}
		
		return info;
		
	}//addStepChangeLock


		 	
}//BaseDissertationService

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/component/src/java/org/sakaiproject/component/app/dissertation/BaseDissertationService.java,v 1.2 2005/05/12 02:08:36 ggolden.umich.edu Exp $
*
**********************************************************************************/
