/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/component/src/java/org/sakaiproject/component/app/dissertation/DbDissertationService.java,v 1.1 2005/05/04 20:27:13 ggolden.umich.edu Exp $
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

// import
import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.api.app.dissertation.BlockGrantGroup;
import org.sakaiproject.api.app.dissertation.BlockGrantGroupEdit;
import org.sakaiproject.api.app.dissertation.CandidateInfo;
import org.sakaiproject.api.app.dissertation.CandidateInfoEdit;
import org.sakaiproject.api.app.dissertation.CandidatePath;
import org.sakaiproject.api.app.dissertation.CandidatePathEdit;
import org.sakaiproject.api.app.dissertation.Dissertation;
import org.sakaiproject.api.app.dissertation.DissertationEdit;
import org.sakaiproject.api.app.dissertation.DissertationStep;
import org.sakaiproject.api.app.dissertation.DissertationStepEdit;
import org.sakaiproject.api.app.dissertation.StepStatus;
import org.sakaiproject.api.app.dissertation.StepStatusEdit;
import org.sakaiproject.service.framework.sql.SqlService;
import org.sakaiproject.util.storage.BaseDbSingleStorage;
//import org.chefproject.core.Resource;


/**
* <p>DbCachedDissertationService is the database-storing service class for Dissertations.</p>
*
* @author University of Michigan, CHEF Software Development Team
* @version $Revision: 44 $
* @see org.chefproject.core.Dissertation
* @see org.chefproject.core.DissertationStep
* @see org.chefproject.core.CandidatePath
* @see org.chefproject.core.StepStatus
*/
public class DbDissertationService extends BaseDissertationService
{
	/** The name of the db table holding Block Grant Group objects. */
	protected String m_groupsTableName = "DISSERTATION_GROUP";
	
	/** The name of the db table holding dissertation objects. */
	protected String m_dissertationsTableName = "DISSERTATION_DISSERTATION";

	/** The name of the db table holding dissertation step objects. */
	protected String m_stepsTableName = "DISSERTATION_STEP";

	/** The name of the db table holding CandidatPath objects. */
	protected String m_pathsTableName = "DISSERTATION_PATH";

	/** The name of the db table holding StepStatus objects. */
	protected String m_statusTableName = "DISSERTATION_STATUS";
	
	/** The name of the db table holding CandidateInfo objects. */
	protected String m_infosTableName = "DISSERTATION_CANDIDATEINFO";

	/** If true, we do our locks in the remote database, otherwise we do them here. */
	protected boolean m_locksInDb = true;
	
	/** The extra field(s) to write to the DISSERTATION_CANDIDATEINFO table in the database. */
	protected static final String[] INFO_FIELDS = { "CHEFID", "PARENTSITE", "EMPLID" };
	
	/** The extra field(s) to write to DISSERTATION_PATH table in the database. */
	protected static final String[] PATH_FIELDS = { "CANDIDATE", "SITE", "PARENTSITE", "SORTLETTER", "TYPE" };
	
	/** The extra field(s) to write to the DISSERTATION_DISSERTATION table in database. */
	protected static final String[] DISSERTATION_FIELDS = { "SITE", "TYPE" };
	
	/*******************************************************************************
	* Constructors, Dependencies and their setter methods
	*******************************************************************************/

	/**
	 * Configuration: set the table name for dissertations.
	 * @param path The table name for dissertations.
	 */
	public void setDissertationTableName(String name)
	{
		m_dissertationsTableName = name;
	}

	/**
	 * Configuration: set the table name for steps.
	 * @param path The table name for steps.
	 */
	public void setStepTableName(String name)
	{
		m_stepsTableName = name;
	}

	/**
	 * Configuration: set the table name for paths.
	 * @param path The table name for paths.
	 */
	public void setPathTableName(String name)
	{
		m_pathsTableName = name;
	}

	/**
	 * Configuration: set the table name for status.
	 * @param path The table name for status.
	 */
	public void setStatusTableName(String name)
	{
		m_statusTableName = name;
	}
	
   /**
    * 
    * @param name
    */
	/**
	* Configuration: set the table name for Block Grant Groups.
	* @param name The table name for Block Grant Groups.
	*/
   public void setGroupTableName(String name)
   {
	   m_groupsTableName = name;
   }

   /**
	/**
	 * Configuration: set the table name for infos.
	 * @param path The table name for infos.
	 */
	public void setInfoTableName(String name)
	{
		m_infosTableName = name;
	}

	/**
	 * Configuration: set the locks-in-db
	 * @param value The locks-in-db value.
	 */
	public void setLocksInDb(String value)
	{
		m_locksInDb = new Boolean(value).booleanValue();
	}

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		m_autoDdl = new Boolean(value).booleanValue();
	}

	/*******************************************************************************
	* Service implementation
	*******************************************************************************/

	/*******************************************************************************
	* Init and Destroy
	*******************************************************************************/

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/**
	 * Dependency: SqlService.
	 * @param service The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// if we are auto-creating our schema, check and create
			if (m_autoDdl)
			{
				m_sqlService.ddl(this.getClass().getClassLoader(), "ctools_dissertation");
			}

			super.init();

			m_logger.info(this + ".init(): dissertations table: "
			+ m_dissertationsTableName + " Block Grant Group table: "
			+ m_groupsTableName + " steps table: "
				+ m_stepsTableName + " paths table : " + m_pathsTableName + " status table: " 
				+ m_statusTableName + " canididateinfo table : " + m_infosTableName
				+ " locks-in-db" + m_locksInDb);
		}
		catch (Throwable t)
		{
			m_logger.warn(this +".init(): ", t);
		}
	}

	/*******************************************************************************
	* BaseDissertationService extensions
	*******************************************************************************/

	/**
	 * Construct a Storage object for Dissertations.
	 * @return The new storage object for Dissertations.
	 */
	protected DissertationStorage newDissertationStorage()
	{
		return new DbCachedDissertationStorage(new DissertationStorageUser());

	}	// newDissertationStorage

	/**
	 * Construct a Storage object for DissertationSteps.
	 * @return The new storage object for DissertationSteps.
	 */
	protected DissertationStepStorage newDissertationStepStorage()
	{
		return new DbCachedDissertationStepStorage(new DissertationStepStorageUser());

	}	// newDissertationStepStorage

	/**
	 * Construct a Storage object for CandidatePaths.
	 * @return The new storage object for CandidatePaths.
	 */
	protected CandidatePathStorage newCandidatePathStorage()
	{
		return new DbCachedCandidatePathStorage(new CandidatePathStorageUser());

	}	// newCandidatePathStorage

	/**
	 * Construct a Storage object for StepStatus.
	 * @return The new storage object for StepStatus.
	 */
	protected StepStatusStorage newStepStatusStorage()
	{
		return new DbCachedStepStatusStorage(new StepStatusStorageUser());

	}	// newStepStatusStorage
	
	/**
	 * Construct a Storage object for BlockGrantGroups.
	 * @return The new storage object for BlockGrantGroups.
	 */
	protected BlockGrantGroupStorage newBlockGrantGroupStorage()
	{
		return new DbCachedBlockGrantGroupStorage(new BlockGrantGroupStorageUser());
	}	// newBlockGrantGroupStorage
	
	/**
	 * Construct a Storage object for CandidateInfos.
	 * @return The new storage object for CandidateInfos.
	 */
	protected CandidateInfoStorage newCandidateInfoStorage()
	{
		return new DbCachedCandidateInfoStorage(new CandidateInfoStorageUser());

	}	// newCandidateInfoStorage

	/*******************************************************************************
	* Storage implementations
	*******************************************************************************/

	
	/*******************************************************************************
	* DissertationStorage implementation
	*******************************************************************************/

	/**
	* Covers for the BaseDbSingleStorage, providing Dissertation and DissertationEdit parameters
	*/
	protected class DbCachedDissertationStorage
		extends BaseDbSingleStorage
		implements DissertationStorage
	{
		/**
		* Construct.
		* @param dissertation The StorageUser class to call back for creation of Resource and Edit objects.
		*/
		public DbCachedDissertationStorage(DissertationStorageUser dissertation)
		{
			//super(m_dissertationsTableName, "DISSERTATION_ID", null, m_locksInDb, "dissertation", dissertation, m_sqlService);
			super(m_dissertationsTableName, "DISSERTATION_ID", DISSERTATION_FIELDS, m_locksInDb, "dissertation", dissertation, m_sqlService);

		}	// DbCachedDissertationStorage

		public boolean check(String id) { return super.checkResource(id); }

		public Dissertation get(String id) { return (Dissertation) super.getResource(id); }

		public List getAll() 
		{ 
			return super.getAllResources(); 
		}
		
		public List getAllOfType(String type)
		{
			List retVal = new ArrayList();
			if(type != null)
			{
				try
				{
					retVal = super.getAllResourcesWhere("TYPE", type);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getAllOfType type = " + type + " Exception " + e);
				}
			}
			return retVal;
			
		}//getAllOfType
			
		public Dissertation getForSite(String id)
		{
			Dissertation retVal = null;
			if(id != null)
			{
				try
				{
					List all = super.getAllResourcesWhere("SITE", id);
					if(all != null && all.size() == 1)
					{
						retVal = (Dissertation)all.get(0);
					}
					else if(all != null && all.size() > 1)
					{
						//getForSite(String) should only return one Dissertation
						m_logger.warn(this + ".getForSite(String) returned " + all.size() + " for id = " + id);
						return retVal;
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getForSite id = " + id + " Exception " + e);
				}
			}
			return retVal;
			
		}//getForSite
			
		public Dissertation getForSiteOfType(String id, String type)
		{
			Dissertation retVal = null;
			if(id != null && type != null)
			{
				try
				{
					String sqlWhere = "WHERE SITE = '" + id + "' AND TYPE = '" + type + "'";
					List all = super.getSelectedResourcesWhere(sqlWhere);
					if(all != null && all.size() == 1)
					{
						retVal = (Dissertation)all.get(0);
					}
					else if (all != null && all.size() == 1)
					{
						//getForSiteOfType(String, String) should only return one Dissertation
						m_logger.warn(this + ".getForSiteOfType(String, String) returned " + all.size() + " Dissertations for id = " + id + " type = " + type);
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getForSiteOfType id = " + id + " type = " + type + " Exception " + e);
				}
			}
			return retVal;
			
		}//getForSiteOfType
			
		public DissertationEdit put(String id, String context) 
		{
			Object[] other = new Object[1];
			other[0] = context;
			return (DissertationEdit) super.putResource(id, other);
		}

		public DissertationEdit edit(String id) { return (DissertationEdit) super.editResource(id); }

		public void commit(DissertationEdit edit) { super.commitResource(edit); }

		public void cancel(DissertationEdit edit) { super.cancelResource(edit); }

		public void remove(DissertationEdit edit) { super.removeResource(edit); }

	}   // DbCachedDissertationStorage


	
	/*******************************************************************************
	* DissertationStepStorage implementation
	*******************************************************************************/

	/**
	* Covers for the BaseDbSingleStorage, providing DissertationStep and DissertationStepEdit parameters
	*/
	protected class DbCachedDissertationStepStorage
		extends BaseDbSingleStorage
		implements DissertationStepStorage
	{
		/**
		* Construct.
		* @param step The StorageUser class to call back for creation of Resource and Edit objects.
		*/
		public DbCachedDissertationStepStorage(DissertationStepStorageUser step)
		{
			super(m_stepsTableName, "STEP_ID", null, m_locksInDb, "step", step, m_sqlService);

		}	// DbCachedDissertationStepStorage

		public boolean check(String id) { return super.checkResource(id); }

		public DissertationStep get(String id) { return (DissertationStep) super.getResource(id); }

		public List getAll() 
		{ 
			return super.getAllResources(); 
		}

		public DissertationStepEdit put(String id, String context) 
		{
			Object[] other = new Object[1];
			other[0] = context;
			return (DissertationStepEdit) super.putResource(id, other);
		}

		public DissertationStepEdit edit(String id) { return (DissertationStepEdit) super.editResource(id); }

		public void commit(DissertationStepEdit edit) { super.commitResource(edit); }

		public void cancel(DissertationStepEdit edit) { super.cancelResource(edit); }

		public void remove(DissertationStepEdit edit) { super.removeResource(edit); }

	}   // DbCachedDissertationStepStorage

	
	
	/*******************************************************************************
	* CandidatePathStorage implementation
	*******************************************************************************/

	/**
	* Covers for the BaseDbSingleStorage, providing CandidatePath and CandidatePathEdit parameters
	*/
	protected class DbCachedCandidatePathStorage
		extends BaseDbSingleStorage
		implements CandidatePathStorage
	{
		/**
		* Construct.
		* @param path The StorageUser class to call back for creation of Resource and Edit objects.
		*/
		public DbCachedCandidatePathStorage(CandidatePathStorageUser path)
		{
			//super(m_pathsTableName, "PATH_ID", null, m_locksInDb, "path", path, m_sqlService);
			super(m_pathsTableName, "PATH_ID", PATH_FIELDS, m_locksInDb, "path", path, m_sqlService);

		}	// DbCachedCandidatePathStorage

		public boolean check(String id) { return super.checkResource(id); }

		public CandidatePath get(String id) { return (CandidatePath) super.getResource(id); }

		public List getAll() 
		{ 
			return super.getAllResources(); 
		}

		public CandidatePathEdit put(String id, String context) 
		{
			Object[] other = new Object[1];
			other[0] = context;
			return (CandidatePathEdit) super.putResource(id, other);
		}

		public CandidatePathEdit edit(String id) { return (CandidatePathEdit) super.editResource(id); }

		public void commit(CandidatePathEdit edit) 
		{ 
			super.commitResource(edit);
		}

		public void cancel(CandidatePathEdit edit) { super.cancelResource(edit); }

		public void remove(CandidatePathEdit edit) { super.removeResource(edit); }
		
		/**
		* Determine if a CandidatePath belonging to the parent site exists.
		* @param The parent site id to use in checking.
		* @return true if such a path exists, false if not.
		*/
		public boolean existsPathForParent(String siteId)
		{
			int count = 0;
			if(siteId != null)
			{
				try
				{
					String sqlWhere = "WHERE PARENTSITE = '" + siteId + "'";
					count = super.countSelectedResourcesWhere(sqlWhere);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".existsPathForParent siteId = " + siteId + " Exception " + e);
				}
			}
			if(count==0) return false;
			return true;
			
		}//existsPathForParent
		
		public boolean existsPathOfType(String type)
		{
			int count = 0;
			if(type != null)
			{
				try
				{
					String sqlWhere = "WHERE TYPE = '" + type + "'";
					count = super.countSelectedResourcesWhere(sqlWhere);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".existsPathOfType type = " + type + " Exception " + e);
				}
			}
			if(count==0) return false;
			return true;
			
		}//existsPathOfType
		
		public List getAllOfType(String type)
		{
			List retVal = new ArrayList();
			if(type != null)
			{
				try
				{
					retVal = super.getAllResourcesWhere("TYPE", type);
					return retVal;
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getAllOfType type = " + type + " Exception " + e);
				}
			}
			return retVal;
			
		}//getAllOfType
			
		public List getAllOfTypeForLetter(String type, String letter)
		{
			List retVal = new ArrayList();
			
			//check that we have an alphabetical character
			if(!letter.matches("[A-Za-z]"))
			{
				return retVal;
			}
			if(type==null)
			{
				return retVal;
			}
			else
			{
				try
				{
					String sqlWhere = "WHERE TYPE = '" + type + "' AND SORTLETTER = '" + letter + "'";
					retVal = super.getSelectedResourcesWhere(sqlWhere);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getAllOfTypeForLetter type = " + type + "letter = " + letter + " Exception " + e);
				}
			}
			return retVal;
			
		}//getAllOfTypeForLetter
		
		public List getAllOfParentForLetter(String parent, String letter)
		{
			List retVal = new ArrayList();
			
			//check that we have an alphabetical character
			if(!letter.matches("[A-Za-z]"))
			{
				return retVal;
			}
			if(parent==null)
			{
				return retVal;
			}
			else
			{
				try
				{
					String sqlWhere = "WHERE PARENTSITE = '" + parent + "' AND SORTLETTER = '" + letter + "'";
					retVal = super.getSelectedResourcesWhere(sqlWhere);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getAllOfParentForLetter type = " + parent + "letter = " + letter + " Exception " + e);
				}
			}
			return retVal;
			
		}//getAllOfParentForLetter
		
		public List getAllForParent(String site)
		{
			List retVal = new ArrayList();
			if(site != null)
			{
				try
				{
					retVal = super.getAllResourcesWhere("PARENTSITE", site);
					return retVal;
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getAllForParent site = " + site + " Exception " + e);
				}
			}
			return retVal;
			
		}//getAllForParent
			
		public CandidatePath getForCandidate(String id)
		{
			CandidatePath retVal = null;
			if(id != null)
			{
				try
				{
					List all = super.getAllResourcesWhere("CANDIDATE", id);
					if(all != null && all.size()==1)
					{
						retVal = (CandidatePath)all.get(0);
					}
					else if (all != null && all.size() > 1)
					{
						//getForCandidate(String) should only return one CandidatePath
						m_logger.warn(this + ".getForCandidate(String) returned " + all.size() + " CandidatePaths for id " + id);
					}
				}
				catch(Exception e) 
				{
					m_logger.warn(this + ".getForCandidate id = " + id + " Exception " + e);
				}
			}
			return retVal;
			
		}//getForCandidate
		
		/**
		* Get CandidatePath for candidate with this site id.
		* @param The id of the Grad Tools student site.
		* @return The corresponding CandidatePath.
		*/
		public CandidatePath getForSite(String id)
		{
			CandidatePath retVal = null;
			if(id != null)
			{
				try
				{
					List all = super.getAllResourcesWhere("SITE", id);
					if(all != null && all.size()==1)
					{
						retVal = (CandidatePath)all.get(0);
					}
					else if (all != null && all.size() > 1)
					{
						//getForSite(String) should only return one CandidatePath
						m_logger.warn(this + ".getForSite returned " + all.size() + " CandidatePaths for id = " + id);
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getForSite id = " + id + " Exception " + e);
				}
			}
			return retVal;
			
		}//getForSite

	}   // DbCachedCandidatePathStorage

	
	
	/*******************************************************************************
	* StepStatusStorage implementation
	*******************************************************************************/

	/**
	* Covers for the BaseDbSingleStorage, providing StepStatus and StepStatusEdit parameters
	*/
	protected class DbCachedStepStatusStorage
		extends BaseDbSingleStorage
		implements StepStatusStorage
	{
		/**
		* Construct.
		* @param status The StorageUser class to call back for creation of Resource and Edit objects.
		*/
		public DbCachedStepStatusStorage(StepStatusStorageUser status)
		{
			super(m_statusTableName, "STATUS_ID", null, m_locksInDb, "status", status, m_sqlService);

		}	// DbCachedStepStatusStorage

		public boolean check(String id) { return super.checkResource(id); }

		public StepStatus get(String id) { return (StepStatus) super.getResource(id); }

		public List getAll() 
		{ 
			return super.getAllResources(); 
		}

		public StepStatusEdit put(String id, String context) 
		{
			Object[] other = new Object[1];
			other[0] = context;
			return (StepStatusEdit) super.putResource(id, other);
		}

		public StepStatusEdit edit(String id) { return (StepStatusEdit) super.editResource(id); }

		public void commit(StepStatusEdit edit) { super.commitResource(edit); }

		public void cancel(StepStatusEdit edit) { super.cancelResource(edit); }

		public void remove(StepStatusEdit edit) { super.removeResource(edit); }

	}   // DbCachedStepStatusStorage

	
	/*******************************************************************************
	* BlockGrantGroupStorage implementation
	*******************************************************************************/

	/**
	* Covers for the BaseDbSingleStorage, providing BlockGrantGroup and BlockGrantGroupEdit parameters
	*/
	protected class DbCachedBlockGrantGroupStorage
		extends BaseDbSingleStorage
		implements BlockGrantGroupStorage
	{
		/**
		* Construct.
		* @param info The StorageUser class to call back for creation of Resource and Edit objects.
		*/
		public DbCachedBlockGrantGroupStorage(BlockGrantGroupStorageUser group)
		{
			super(m_groupsTableName, "GROUP_ID", null, m_locksInDb, "group", group, m_sqlService);

		}	// DbCachedBlockGrantGroupStorage

		public boolean check(String id) { return super.checkResource(id); }

		public BlockGrantGroup get(String id) { return (BlockGrantGroup) super.getResource(id); }

		public List getAll() 
		{ 
			return super.getAllResources(); 
		}

		public BlockGrantGroupEdit put(String id, String context)
		{
			Object[] other = new Object[1];
			other[0] = context;
			return (BlockGrantGroupEdit) super.putResource(id, other);
		}

		public BlockGrantGroupEdit edit(String id) { return (BlockGrantGroupEdit) super.editResource(id); }

		public void commit(BlockGrantGroupEdit edit) { super.commitResource(edit); }

		public void cancel(BlockGrantGroupEdit edit) { super.cancelResource(edit); }

		public void remove(BlockGrantGroupEdit edit) { super.removeResource(edit); }
	}   // DbCachedBlockGrantGroupStorage
	
	/*******************************************************************************
	* CandidateInfoStorage implementation
	*******************************************************************************/

	/**
	* Covers for the BaseDbSingleStorage, providing CandidateInfo and CandidateInfoEdit parameters
	*/
	protected class DbCachedCandidateInfoStorage
		extends BaseDbSingleStorage
		implements CandidateInfoStorage
	{

		/**
		* Construct.
		* @param info The StorageUser class to call back for creation of Resource and Edit objects.
		*/
		public DbCachedCandidateInfoStorage(CandidateInfoStorageUser info)
		{
			//super(m_infosTableName, "INFO_ID", null, m_locksInDb, "info", info, m_sqlService);
			super(m_infosTableName, "INFO_ID", INFO_FIELDS, m_locksInDb, "info", info, m_sqlService);

		}	// DbCachedCandidateInfoStorage
		
		public boolean check(String id) { return super.checkResource(id); }

		public CandidateInfo get(String id) { return (CandidateInfo) super.getResource(id); }
		
		public CandidateInfo getForCandidate(String id)
		{
			CandidateInfo retVal = null;
			if(id != null)
			{
				try
				{
					List all = super.getAllResourcesWhere("CHEFID", id);
					if(all != null && all.size() == 1)
					{
						retVal = (CandidateInfo)all.get(0);
					}
					else if(all != null && all.size() > 1)
					{
						//getForCandidate(String) should only return one CandidateInfo
						m_logger.warn(this + ".getForCandidate returned " + all.size() + " CandidateInfos for id = " + id);
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getForCandidate id = " + id + " Exception " + e);
				}
			}
			return retVal;
			
		}//getForCandidate
		
		public CandidateInfo getForEmplid(String emplid)
		{
			CandidateInfo retVal = null;
			if(emplid != null)
			{
				try
				{
					List all = super.getAllResourcesWhere("EMPLID", emplid);
					if(all != null && all.size() == 1)
					{
						retVal = (CandidateInfo)all.get(0);
					}
					else if(all != null && all.size() > 1)
					{
						//getForEmplid(String) should only return one CandidateInfo
						m_logger.warn(this + ".getForEmplid returned " + all.size() + " CandidateInfos for emplid = " + emplid);
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getForEmplid emplid = " + emplid + " Exception " + e);
				}
			}
			return retVal;
			
		}//getForEmplid
				
		public String getEmplid(String id)
		{
			String retVal = "";
			if(id != null)
			{
				try
				{
					//String sqlWhere = "WHERE CANDIDATE = '" + id + "'";
					//List all = super.getSelectedResourcesWhere(sqlWhere);
					List all = super.getAllResourcesWhere("CHEFID", id);
					if(all != null && all.size()==1)
					{
						CandidateInfo info = (CandidateInfo)all.get(0);
						return (String)info.getEmplid();
					}
					else if(all != null && all.size() > 1)
					{
						//getEmplid(String) should only return one CandidateInfo
						m_logger.warn(this + ".getEmplid returned " + all.size() + " CandidateInfos for id = " + id);
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getEmplid id = " + id + " Exception " + e);
				}
			}
			return retVal;
			
		}//getEmplid
		
		public String getParent(String id)
		{
			String retVal = "";
			if(id != null)
			{
				try
				{
					List all = super.getAllResourcesWhere("CHEFID", id);
					if(all != null && all.size()==1)
					{
						CandidateInfo info = (CandidateInfo)all.get(0);
						return (String)info.getParentSite();
					}
					else if(all != null && all.size()>1)
					{
						m_logger.warn(this + ".getParent returned " + all.size() + " CandidateInfos for id = " + id);
					}
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".getParent id = " + id + " Exception " + e);
				}
			}
			return retVal;
			
		}//getParent
			
		public boolean checkMusic(String id)
		{
			int count = 0;
			if(id != null)
			{
				try
				{
					String sqlWhere = "WHERE CHEFID = '" + id + "' AND PARENTSITE = '" + getMusicPerformanceSite() + "'";
					count = super.countSelectedResourcesWhere(sqlWhere);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".checkMusic id = " + id + " Exception " + e);
				}
			}
			if(count==0) return false;
			return true;
			
		}//checkMusic
			
		public boolean checkCandidate(String id)
		{
			int count = 0;
			if(id != null)
			{
				try
				{
					String sqlWhere = "WHERE CHEFID = '" + id + "'";
					count = super.countSelectedResourcesWhere(sqlWhere);
				}
				catch(Exception e)
				{
					m_logger.warn(this + ".checkCandidate id = " + id + " Exception " + e);
				}
			}
			if(count==0) return false;
			return true;
			
		}//checkCandidate

		public List getAll() 
		{ 
			return super.getAllResources();
		}

		public CandidateInfoEdit put(String id, String context)
		{
			Object[] other = new Object[1];
			other[0] = context;
			return (CandidateInfoEdit) super.putResource(id, other);
		}

		public CandidateInfoEdit edit(String id) { return (CandidateInfoEdit) super.editResource(id); }

		public void commit(CandidateInfoEdit edit) { super.commitResource(edit); }

		public void cancel(CandidateInfoEdit edit) { super.cancelResource(edit); }

		public void remove(CandidateInfoEdit edit) { super.removeResource(edit); }

	}   // DbCachedCandidateInfoStorage


} // DbCachedDissertationService

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/component/src/java/org/sakaiproject/component/app/dissertation/DbDissertationService.java,v 1.1 2005/05/04 20:27:13 ggolden.umich.edu Exp $
*
**********************************************************************************/
